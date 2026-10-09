package br.com.lucas.loginseguro.config;

import br.com.lucas.loginseguro.web.LoginFalhaHandler;
import br.com.lucas.loginseguro.web.LoginSucessoHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração central do Spring Security.
 *
 * Resumo das decisões:
 * 1. Senhas com hash BCrypt via DelegatingPasswordEncoder (o prefixo {bcrypt}
 *    permite trocar de algoritmo no futuro sem invalidar senhas antigas).
 * 2. Autorização em duas camadas:
 *    a) por URL, aqui no SecurityFilterChain;
 *    b) por método, com @PreAuthorize nos serviços (@EnableMethodSecurity).
 * 3. Hierarquia de perfis: ADMIN > MODERADOR > USUARIO, evitando repetir regras.
 * 4. CSRF ativo (padrão); o Thymeleaf insere o token nos formulários sozinho.
 * 5. Cabeçalhos de segurança: Content-Security-Policy e bloqueio de iframes.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Hierarquia de perfis. Quem é ADMIN automaticamente passa nas regras
     * de MODERADOR e de USUARIO.
     * (Método estático: recomendação do Spring para beans usados pela
     * segurança de métodos, evitando inicialização antecipada da classe.)
     */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_ADMIN > ROLE_MODERADOR
                ROLE_MODERADOR > ROLE_USUARIO
                """);
    }

    /** Faz o @PreAuthorize respeitar a hierarquia de perfis. */
    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }

    /*
     * Observação: o sec:authorize dos templates usa o "webSecurityExpressionHandler"
     * que o próprio Spring Security registra; ele já detecta o bean RoleHierarchy
     * acima, por isso não é preciso declarar outro aqui.
     */

    /** Codificador de senhas: BCrypt por padrão, com prefixo {bcrypt}. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            LoginSucessoHandler sucessoHandler,
                                            LoginFalhaHandler falhaHandler) throws Exception {
        http
            // ---- Regras de acesso por rota (avaliadas de cima para baixo) ----
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/cadastro", "/error",
                                 "/css/**", "/js/**", "/temas/**", "/favicon.ico").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/moderador/**").hasRole("MODERADOR")
                .anyRequest().authenticated())

            // ---- Login por formulário (página própria em Thymeleaf) ----
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .passwordParameter("senha")
                .successHandler(sucessoHandler)
                .failureHandler(falhaHandler)
                .permitAll())

            // ---- Logout: só via POST (protegido por CSRF), apaga a sessão no MongoDB ----
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?saiu")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("SESSION")
                .permitAll())

            // ---- Sessão: novo ID após o login (contra fixação de sessão) ----
            .sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.changeSessionId()))

            // ---- Cabeçalhos HTTP de segurança ----
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; style-src 'self'; script-src 'self'; "
                    + "img-src 'self' data:; form-action 'self'; frame-ancestors 'none'"))
                .frameOptions(frame -> frame.deny()));

        return http.build();
    }
}
