package br.com.lucas.loginseguro.config;

import br.com.lucas.loginseguro.model.Perfil;
import br.com.lucas.loginseguro.repository.UsuarioRepository;
import br.com.lucas.loginseguro.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Cria o primeiro administrador ao iniciar a aplicação, caso ainda não
 * exista nenhum ADMIN no banco. Sem isso, ninguém conseguiria promover
 * outros usuários.
 *
 * E-mail e senha vêm das variáveis ADMIN_EMAIL e ADMIN_PASSWORD, nunca do código.
 */
@Component
public class AdminInicialRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicialRunner.class);

    private final UsuarioRepository repository;
    private final UsuarioService usuarioService;
    private final AdminProperties admin;

    public AdminInicialRunner(UsuarioRepository repository,
                              UsuarioService usuarioService,
                              AdminProperties admin) {
        this.repository = repository;
        this.usuarioService = usuarioService;
        this.admin = admin;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.existsByPerfil(Perfil.ADMIN)) {
            return;
        }
        if (admin.email().isBlank() || admin.senha().isBlank()) {
            log.warn("Nenhum ADMIN cadastrado. Defina ADMIN_EMAIL e ADMIN_PASSWORD para criar o primeiro.");
            return;
        }
        usuarioService.criar(admin.nome(), admin.email(), admin.senha(), Perfil.ADMIN);
        log.info("Administrador inicial criado: {}", admin.email());
    }
}
