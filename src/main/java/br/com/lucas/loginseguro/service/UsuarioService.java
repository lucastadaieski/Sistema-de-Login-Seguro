package br.com.lucas.loginseguro.service;

import br.com.lucas.loginseguro.config.SegurancaProperties;
import br.com.lucas.loginseguro.dto.CadastroForm;
import br.com.lucas.loginseguro.model.Perfil;
import br.com.lucas.loginseguro.model.Usuario;
import br.com.lucas.loginseguro.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Regras de negócio dos usuários.
 *
 * As operações administrativas têm @PreAuthorize: mesmo que alguém
 * conseguisse chamar o método por outro caminho, o Spring Security
 * bloquearia a execução (segunda camada de proteção, além das rotas).
 */
@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final SegurancaProperties seguranca;
    private final TemaService temaService;
    private final SessaoService sessaoService;

    public UsuarioService(UsuarioRepository repository,
                          PasswordEncoder passwordEncoder,
                          SegurancaProperties seguranca,
                          TemaService temaService,
                          SessaoService sessaoService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.seguranca = seguranca;
        this.temaService = temaService;
        this.sessaoService = sessaoService;
    }

    /** E-mails são comparados sem diferenciar maiúsculas e sem espaços nas pontas. */
    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    // ==================================================================
    //  Cadastro e consulta
    // ==================================================================

    /** Cadastra um novo usuário sempre com o perfil USUARIO. */
    public Usuario cadastrar(CadastroForm form) {
        return criar(form.getNome(), form.getEmail(), form.getSenha(), Perfil.USUARIO);
    }

    /** Criação genérica (usada também pelo administrador inicial). */
    public Usuario criar(String nome, String email, String senhaPura, Perfil perfil) {
        String emailNormalizado = normalizarEmail(email);
        if (repository.existsByEmail(emailNormalizado)) {
            throw new RegraNegocioException("Este e-mail já está cadastrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(nome.trim());
        usuario.setEmail(emailNormalizado);
        usuario.setSenhaHash(passwordEncoder.encode(senhaPura)); // só o hash é gravado
        usuario.setPerfil(perfil);
        usuario.setCriadoEm(Instant.now());

        try {
            return repository.save(usuario);
        } catch (DuplicateKeyException e) {
            // Dois cadastros simultâneos com o mesmo e-mail: o índice único barra o segundo
            throw new RegraNegocioException("Este e-mail já está cadastrado");
        }
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return repository.findByEmail(normalizarEmail(email));
    }

    public Usuario obterPorEmail(String email) {
        return buscarPorEmail(email)
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado"));
    }

    @PreAuthorize("hasRole('MODERADOR')")
    public List<Usuario> listarTodos() {
        return repository.findAll(Sort.by("nome"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Map<Perfil, Long> contarPorPerfil() {
        Map<Perfil, Long> contagem = new EnumMap<>(Perfil.class);
        for (Perfil perfil : Perfil.values()) {
            contagem.put(perfil, repository.countByPerfil(perfil));
        }
        return contagem;
    }

    // ==================================================================
    //  Controle de tentativas de login (proteção contra força bruta)
    // ==================================================================

    /**
     * Registra uma senha errada. Ao atingir o limite configurado, bloqueia a
     * conta por alguns minutos.
     *
     * @return true se a conta acabou de ser bloqueada
     */
    public boolean registrarFalhaLogin(String email) {
        Optional<Usuario> encontrado = buscarPorEmail(email);
        if (encontrado.isEmpty()) {
            return false; // e-mail inexistente: nada a contar
        }
        Usuario usuario = encontrado.get();
        usuario.setTentativasFalhas(usuario.getTentativasFalhas() + 1);

        boolean bloqueou = false;
        if (usuario.getTentativasFalhas() >= seguranca.maxTentativas()) {
            usuario.setBloqueadoAte(Instant.now().plus(Duration.ofMinutes(seguranca.minutosBloqueio())));
            usuario.setTentativasFalhas(0);
            bloqueou = true;
            log.warn("Conta {} bloqueada por {} minutos após tentativas inválidas",
                    usuario.getEmail(), seguranca.minutosBloqueio());
        }
        repository.save(usuario);
        return bloqueou;
    }

    /** Login bem-sucedido: zera o contador de falhas e registra a data. */
    public Usuario registrarLoginSucesso(String email) {
        Usuario usuario = obterPorEmail(email);
        usuario.setTentativasFalhas(0);
        usuario.setBloqueadoAte(null);
        usuario.setUltimoLogin(Instant.now());
        return repository.save(usuario);
    }

    // ==================================================================
    //  Operações do próprio usuário
    // ==================================================================

    public Usuario atualizarPerfil(String email, String nome, String tema) {
        Usuario usuario = obterPorEmail(email);
        usuario.setNome(nome.trim());
        usuario.setTema(temaService.isValido(tema) ? tema : null);
        return repository.save(usuario);
    }

    public void trocarSenha(String email, String senhaAtual, String novaSenha) {
        Usuario usuario = obterPorEmail(email);
        if (!passwordEncoder.matches(senhaAtual, usuario.getSenhaHash())) {
            throw new RegraNegocioException("A senha atual está incorreta");
        }
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        repository.save(usuario);
    }

    // ==================================================================
    //  Operações de moderação e administração
    // ==================================================================

    /**
     * Ativa ou desativa uma conta.
     * Moderadores só podem alterar contas de perfil USUARIO;
     * administradores podem alterar qualquer conta, exceto a própria.
     */
    @PreAuthorize("hasRole('MODERADOR')")
    public void alterarStatus(String idAlvo, boolean ativo, String emailExecutor) {
        Usuario executor = obterPorEmail(emailExecutor);
        Usuario alvo = obterPorId(idAlvo);

        impedirAutoAlteracao(executor, alvo);
        if (executor.getPerfil() != Perfil.ADMIN && alvo.getPerfil() != Perfil.USUARIO) {
            throw new RegraNegocioException("Moderadores só podem alterar contas de usuários comuns");
        }

        alvo.setAtivo(ativo);
        if (ativo) {
            // Reativar também libera um eventual bloqueio por tentativas
            alvo.setBloqueadoAte(null);
            alvo.setTentativasFalhas(0);
        }
        repository.save(alvo);
        if (!ativo) {
            sessaoService.encerrarSessoesDe(alvo.getEmail()); // derruba o usuário na hora
        }
        log.info("{} {} a conta {}", executor.getEmail(), ativo ? "ativou" : "desativou", alvo.getEmail());
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void alterarPerfil(String idAlvo, Perfil novoPerfil, String emailExecutor) {
        Usuario executor = obterPorEmail(emailExecutor);
        Usuario alvo = obterPorId(idAlvo);
        impedirAutoAlteracao(executor, alvo);

        alvo.setPerfil(novoPerfil);
        repository.save(alvo);
        // O perfil fica gravado na sessão; encerrá-la obriga novo login com o perfil atualizado
        sessaoService.encerrarSessoesDe(alvo.getEmail());
        log.info("{} alterou o perfil de {} para {}", executor.getEmail(), alvo.getEmail(), novoPerfil);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void excluir(String idAlvo, String emailExecutor) {
        Usuario executor = obterPorEmail(emailExecutor);
        Usuario alvo = obterPorId(idAlvo);
        impedirAutoAlteracao(executor, alvo);

        repository.delete(alvo);
        sessaoService.encerrarSessoesDe(alvo.getEmail());
        log.info("{} excluiu a conta {}", executor.getEmail(), alvo.getEmail());
    }

    // ==================================================================
    //  Auxiliares
    // ==================================================================

    private Usuario obterPorId(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado"));
    }

    private void impedirAutoAlteracao(Usuario executor, Usuario alvo) {
        if (executor.getId().equals(alvo.getId())) {
            throw new RegraNegocioException("Você não pode alterar a sua própria conta por aqui");
        }
    }
}
