package br.com.lucas.loginseguro.web;

import br.com.lucas.loginseguro.model.Usuario;
import br.com.lucas.loginseguro.service.TemaService;
import br.com.lucas.loginseguro.service.UsuarioService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Executado após um login correto:
 * - zera o contador de tentativas falhas e grava a data do último acesso;
 * - carrega na sessão o tema preferido do usuário;
 * - redireciona para a página que o usuário tentou abrir, ou para /painel.
 */
@Component
public class LoginSucessoHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final UsuarioService usuarioService;
    private final TemaService temaService;

    public LoginSucessoHandler(UsuarioService usuarioService, TemaService temaService) {
        this.usuarioService = usuarioService;
        this.temaService = temaService;
        setDefaultTargetUrl("/painel");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        Usuario usuario = usuarioService.registrarLoginSucesso(authentication.getName());
        temaService.aplicarNaSessao(request, usuario.getTema());
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
