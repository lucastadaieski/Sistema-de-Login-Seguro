package br.com.lucas.loginseguro.config;

import br.com.lucas.loginseguro.web.LoginFalhaHandler;
import br.com.lucas.loginseguro.web.LoginSucessoHandler;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Hierarquia de perfis. Quem é ADMIN automaticamente passa nas regras
     * de MODERADOR, TECNICO e USUARIO.
     * (Método estático: recomendação do Spring para beans usados pela
     * segurança de métodos, evitando inicialização antecipada da classe.)
     */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_ADMIN > ROLE_MODERADOR
                ROLE_ADMIN > ROLE_TECNICO
                ROLE_MODERADOR > ROLE_USUARIO
                ROLE_TECNICO > ROLE_USUARIO
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

    /** Mesmo autenticador do login pela tela, reutilizado pela API de token. */
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuracao) throws Exception {
        return configuracao.getAuthenticationManager();
    }

    /** Assina os tokens JWT com a chave app.jwt.segredo (HS256). */
    @Bean
    JwtEncoder jwtEncoder(JwtProperties jwt) {
        SecretKeySpec chave = new SecretKeySpec(jwt.segredo().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    /**
     * Regras da API (/api/**). Cliente de API não tem formulário nem cookie,
     * por isso não há sessão nem CSRF, e erros devolvem 401 em vez de
     * redirecionar para a página de login.
     */
    @Bean
    @Order(1)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/token").permitAll()
                .anyRequest().denyAll())
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

    /** Regras das telas (todo o resto). */
    @Bean
    @Order(2)
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
