package br.com.lucas.loginseguro;

import br.com.lucas.loginseguro.model.Perfil;
import br.com.lucas.loginseguro.model.Usuario;
import br.com.lucas.loginseguro.repository.UsuarioRepository;
import br.com.lucas.loginseguro.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração: sobem a aplicação inteira e usam um MongoDB real.
 *
 * Rodam automaticamente no GitHub Actions (variável CI=true), que fornece
 * um MongoDB descartável. Para rodar localmente, defina CI=true e
 * MONGODB_URI apontando para um banco de TESTE (os dados são apagados).
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
@TestPropertySource(properties = {"app.admin.email=", "app.admin.senha=",
        "app.jwt.segredo=chave-usada-somente-nos-testes-de-integracao"})
class LoginSeguroIntegracaoTest {

    private static final String SENHA = "Senha123";

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository repository;
    @Autowired private UsuarioService usuarioService;
    @Autowired private MongoTemplate mongoTemplate;

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
        mongoTemplate.dropCollection("sessoes");
    }

    private Usuario criar(String email, Perfil perfil) {
        return usuarioService.criar("Teste " + perfil, email, SENHA, perfil);
    }

    // ------------------------------------------------------------------
    // Cadastro
    // ------------------------------------------------------------------

    @Test
    void cadastroValidoGravaSomenteOHashDaSenha() throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "Maria")
                        .param("email", "Maria@Teste.com")
                        .param("senha", SENHA)
                        .param("confirmacaoSenha", SENHA))
                .andExpect(redirectedUrl("/login"));

        Usuario salvo = repository.findByEmail("maria@teste.com").orElseThrow();
        assertThat(salvo.getSenhaHash()).startsWith("{bcrypt}").doesNotContain(SENHA);
        assertThat(salvo.getPerfil()).isEqualTo(Perfil.USUARIO);
    }

    @Test
    void cadastroComSenhaFracaEhRejeitado() throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "Maria")
                        .param("email", "maria@teste.com")
                        .param("senha", "123")
                        .param("confirmacaoSenha", "123"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("cadastroForm", "senha"));
        assertThat(repository.count()).isZero();
    }

    @Test
    void cadastroComSenhasDiferentesEhRejeitado() throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "Maria")
                        .param("email", "maria@teste.com")
                        .param("senha", SENHA)
                        .param("confirmacaoSenha", "Outra123"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("cadastroForm", "senhasConferem"));
    }

    @Test
    void cadastroComEmailRepetidoEhRejeitado() throws Exception {
        criar("maria@teste.com", Perfil.USUARIO);
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "Maria")
                        .param("email", "MARIA@teste.com")
                        .param("senha", SENHA)
                        .param("confirmacaoSenha", SENHA))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("cadastroForm", "email"));
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void cadastroNaoPermiteEscolherPerfil() throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "Hacker")
                        .param("email", "hacker@teste.com")
                        .param("senha", SENHA)
                        .param("confirmacaoSenha", SENHA)
                        .param("perfil", "ADMIN"))
                .andExpect(redirectedUrl("/login"));
        assertThat(repository.findByEmail("hacker@teste.com").orElseThrow().getPerfil())
                .isEqualTo(Perfil.USUARIO);
    }

    @Test
    void formularioSemTokenCsrfEhBloqueado() throws Exception {
        mvc.perform(post("/cadastro").param("nome", "X"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Login, bloqueio e sessão
    // ------------------------------------------------------------------

    @Test
    void loginCorretoRedirecionaParaPainelEGravaSessaoNoMongo() throws Exception {
        criar("joao@teste.com", Perfil.USUARIO);

        mvc.perform(formLogin("/login").userParameter("email").passwordParam("senha")
                        .user("joao@teste.com").password(SENHA))
                .andExpect(redirectedUrl("/painel"));

        assertThat(mongoTemplate.getCollection("sessoes").countDocuments()).isPositive();
        assertThat(repository.findByEmail("joao@teste.com").orElseThrow().getUltimoLogin()).isNotNull();
    }

    @Test
    void loginComSenhaErradaFalha() throws Exception {
        criar("joao@teste.com", Perfil.USUARIO);
        mvc.perform(formLogin("/login").userParameter("email").passwordParam("senha")
                        .user("joao@teste.com").password("Errada123"))
                .andExpect(redirectedUrl("/login?erro"));
    }

    @Test
    void contaEhBloqueadaAposCincoTentativasErradas() throws Exception {
        criar("joao@teste.com", Perfil.USUARIO);
        for (int i = 0; i < 4; i++) {
            mvc.perform(formLogin("/login").userParameter("email").passwordParam("senha")
                            .user("joao@teste.com").password("Errada123"))
                    .andExpect(redirectedUrl("/login?erro"));
        }
        mvc.perform(formLogin("/login").userParameter("email").passwordParam("senha")
                        .user("joao@teste.com").password("Errada123"))
                .andExpect(redirectedUrl("/login?bloqueado"));

        // Mesmo com a senha certa, a conta continua bloqueada
        mvc.perform(formLogin("/login").userParameter("email").passwordParam("senha")
                        .user("joao@teste.com").password(SENHA))
                .andExpect(redirectedUrl("/login?bloqueado"));
    }

    @Test
    void contaDesativadaNaoConsegueEntrar() throws Exception {
        Usuario usuario = criar("joao@teste.com", Perfil.USUARIO);
        usuario.setAtivo(false);
        repository.save(usuario);

        mvc.perform(formLogin("/login").userParameter("email").passwordParam("senha")
                        .user("joao@teste.com").password(SENHA))
                .andExpect(redirectedUrl("/login?inativo"));
    }

    @Test
    void logoutRedirecionaParaLogin() throws Exception {
        mvc.perform(post("/logout").with(csrf()).with(user("joao@teste.com").roles("USUARIO")))
                .andExpect(redirectedUrl("/login?saiu"));
    }

    // ------------------------------------------------------------------
    // Autorização por perfil
    // ------------------------------------------------------------------

    @Test
    void paginaProtegidaExigeLogin() throws Exception {
        mvc.perform(get("/painel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void painelExibeDadosDoUsuarioComTemaPadrao() throws Exception {
        criar("joao@teste.com", Perfil.USUARIO);
        mvc.perform(get("/painel").with(user("joao@teste.com").roles("USUARIO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("joao@teste.com")))
                .andExpect(content().string(containsString("/temas/claro/tema.css")))
                .andExpect(content().string(not(containsString("Gerenciar usuários"))));
    }

    @Test
    void usuarioNaoAcessaModeracaoNemAdministracao() throws Exception {
        mvc.perform(get("/moderador/usuarios").with(user("u@teste.com").roles("USUARIO")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin").with(user("u@teste.com").roles("USUARIO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void moderadorAcessaModeracaoMasNaoAdministracao() throws Exception {
        criar("mod@teste.com", Perfil.MODERADOR);
        mvc.perform(get("/moderador/usuarios").with(user("mod@teste.com").roles("MODERADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Excluir"))));
        mvc.perform(get("/admin").with(user("mod@teste.com").roles("MODERADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminHerdaAcessoDeModeradorPelaHierarquia() throws Exception {
        criar("admin@teste.com", Perfil.ADMIN);
        criar("joao@teste.com", Perfil.USUARIO);
        mvc.perform(get("/moderador/usuarios").with(user("admin@teste.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Excluir")));
        mvc.perform(get("/admin").with(user("admin@teste.com").roles("ADMIN")))
                .andExpect(status().isOk());
        // sec:authorize="hasRole('MODERADOR')" no template também respeita a hierarquia
        mvc.perform(get("/painel").with(user("admin@teste.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Gerenciar usuários")));
    }

    @Test
    void moderadorNaoPodeDesativarAdministrador() throws Exception {
        criar("mod@teste.com", Perfil.MODERADOR);
        Usuario admin = criar("admin@teste.com", Perfil.ADMIN);

        mvc.perform(post("/moderador/usuarios/" + admin.getId() + "/status").with(csrf())
                        .with(user("mod@teste.com").roles("MODERADOR"))
                        .param("ativo", "false"))
                .andExpect(redirectedUrl("/moderador/usuarios"));

        assertThat(repository.findById(admin.getId()).orElseThrow().isAtivo()).isTrue();
    }

    @Test
    void adminAlteraPerfilDeOutroUsuario() throws Exception {
        criar("admin@teste.com", Perfil.ADMIN);
        Usuario joao = criar("joao@teste.com", Perfil.USUARIO);

        mvc.perform(post("/admin/usuarios/" + joao.getId() + "/perfil").with(csrf())
                        .with(user("admin@teste.com").roles("ADMIN"))
                        .param("perfil", "MODERADOR"))
                .andExpect(redirectedUrl("/moderador/usuarios"));

        assertThat(repository.findById(joao.getId()).orElseThrow().getPerfil()).isEqualTo(Perfil.MODERADOR);
    }

    @Test
    void temaEscolhidoPeloUsuarioEhGravado() throws Exception {
        criar("joao@teste.com", Perfil.USUARIO);
        mvc.perform(post("/perfil").with(csrf())
                        .with(user("joao@teste.com").roles("USUARIO"))
                        .param("nome", "Joao")
                        .param("tema", "escuro"))
                .andExpect(redirectedUrl("/perfil"));
        assertThat(repository.findByEmail("joao@teste.com").orElseThrow().getTema()).isEqualTo("escuro");
    }

    @Test
    void tecnicoNaoAcessaModeracaoNemAdministracao() throws Exception {
        mvc.perform(get("/moderador/usuarios").with(user("tec@teste.com").roles("TECNICO")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin").with(user("tec@teste.com").roles("TECNICO")))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // API de token para os outros microsserviços
    // ------------------------------------------------------------------

    @Test
    void apiEmiteTokenComCredenciaisCorretasSemCsrf() throws Exception {
        Usuario tecnico = criar("tec@teste.com", Perfil.TECNICO);

        mvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"TEC@teste.com\", \"senha\": \"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuarioId").value(tecnico.getId()))
                .andExpect(jsonPath("$.perfil").value("TECNICO"));
    }

    @Test
    void apiRecusaSenhaErradaEContaATentativa() throws Exception {
        criar("joao@teste.com", Perfil.USUARIO);

        mvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"joao@teste.com\", \"senha\": \"Errada123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("E-mail ou senha inválidos"));

        assertThat(repository.findByEmail("joao@teste.com").orElseThrow().getTentativasFalhas()).isEqualTo(1);
    }

    @Test
    void apiNaoTemOutrasRotasLiberadas() throws Exception {
        mvc.perform(get("/api/qualquer-coisa"))
                .andExpect(status().isUnauthorized());
    }
}
