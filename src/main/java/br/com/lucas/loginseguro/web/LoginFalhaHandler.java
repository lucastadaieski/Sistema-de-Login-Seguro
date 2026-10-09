package br.com.lucas.loginseguro.web;

import br.com.lucas.loginseguro.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Executado quando o login falha. Conta as tentativas erradas e escolhe a
 * mensagem exibida na tela de login:
 * - /login?erro      e-mail ou senha inválidos (mensagem genérica, de propósito)
 * - /login?bloqueado conta bloqueada temporariamente por excesso de tentativas
 * - /login?inativo   conta desativada por um moderador
 */
@Component
public class LoginFalhaHandler extends SimpleUrlAuthenticationFailureHandler {

    private final UsuarioService usuarioService;

    public LoginFalhaHandler(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String destino = "/login?erro";

        if (exception instanceof LockedException) {
            destino = "/login?bloqueado";
        } else if (exception instanceof DisabledException) {
            destino = "/login?inativo";
        } else if (exception instanceof BadCredentialsException) {
            String email = request.getParameter("email");
            if (email != null && usuarioService.registrarFalhaLogin(email)) {
                destino = "/login?bloqueado";
            }
        }

        getRedirectStrategy().sendRedirect(request, response, destino);
    }
}
