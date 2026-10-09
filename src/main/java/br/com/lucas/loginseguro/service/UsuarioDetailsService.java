package br.com.lucas.loginseguro.service;

import br.com.lucas.loginseguro.model.Usuario;
import br.com.lucas.loginseguro.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Ponte entre o Spring Security e o MongoDB.
 *
 * No login, o Spring chama loadUserByUsername com o e-mail digitado e compara
 * a senha informada com o hash retornado aqui. O Spring Boot detecta este bean
 * e o PasswordEncoder automaticamente e monta o provedor de autenticação.
 *
 * Retornamos o {@link User} do próprio Spring (e não a entidade Usuario)
 * porque ele é pequeno e serializável, ideal para ser gravado na sessão
 * que fica no MongoDB.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository repository;

    public UsuarioDetailsService(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = repository.findByEmail(UsuarioService.normalizarEmail(email))
                // A mensagem não chega à tela: o Spring a converte em "credenciais inválidas",
                // assim um atacante não descobre quais e-mails existem.
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        return User.withUsername(usuario.getEmail())
                .password(usuario.getSenhaHash())
                .authorities(usuario.getPerfil().getAutoridade())
                .disabled(!usuario.isAtivo())          // conta desativada pelo moderador
                .accountLocked(usuario.isBloqueado())  // bloqueio por excesso de tentativas
                .build();
    }
}
