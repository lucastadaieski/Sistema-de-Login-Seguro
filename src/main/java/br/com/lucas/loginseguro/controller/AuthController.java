package br.com.lucas.loginseguro.controller;

import br.com.lucas.loginseguro.dto.CadastroForm;
import br.com.lucas.loginseguro.service.RegraNegocioException;
import br.com.lucas.loginseguro.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Páginas públicas: início, login e cadastro.
 *
 * O POST de /login e o POST de /logout NÃO estão aqui: quem os processa é o
 * próprio Spring Security (ver SecurityConfig).
 */
@Controller
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/")
    public String inicio() {
        return "index";
    }

    /** Usuário já logado não precisa ver a tela de login de novo. */
    @GetMapping("/login")
    public String login(Authentication autenticacao) {
        return autenticacao != null ? "redirect:/painel" : "auth/login";
    }

    @GetMapping("/cadastro")
    public String formularioCadastro(Authentication autenticacao, Model model) {
        if (autenticacao != null) {
            return "redirect:/painel";
        }
        model.addAttribute("cadastroForm", new CadastroForm());
        return "auth/cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(@Valid @ModelAttribute("cadastroForm") CadastroForm form,
                            BindingResult resultado,
                            RedirectAttributes redirect) {
        // 1) Erros de validação (campos vazios, senha fraca, e-mail inválido...)
        if (resultado.hasErrors()) {
            return "auth/cadastro";
        }
        // 2) Regras de negócio (e-mail já existente)
        try {
            usuarioService.cadastrar(form);
        } catch (RegraNegocioException e) {
            resultado.rejectValue("email", "email.duplicado", e.getMessage());
            return "auth/cadastro";
        }
        // Padrão Post/Redirect/Get: evita reenvio do formulário ao atualizar a página
        redirect.addFlashAttribute("sucesso", "Cadastro realizado com sucesso! Faça seu login.");
        return "redirect:/login";
    }
}
