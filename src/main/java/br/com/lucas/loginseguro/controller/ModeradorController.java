package br.com.lucas.loginseguro.controller;

import br.com.lucas.loginseguro.model.Perfil;
import br.com.lucas.loginseguro.service.RegraNegocioException;
import br.com.lucas.loginseguro.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Área de moderação (perfil MODERADOR ou ADMIN).
 * Protegida pela regra "/moderador/**" no SecurityConfig e pelos
 * @PreAuthorize do UsuarioService.
 */
@Controller
@RequestMapping("/moderador")
public class ModeradorController {

    private final UsuarioService usuarioService;

    public ModeradorController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/usuarios")
    public String listar(Authentication autenticacao, Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        model.addAttribute("perfis", Perfil.values());
        model.addAttribute("emailLogado", autenticacao.getName());
        // Usado no template para decidir quais botões mostrar
        model.addAttribute("souAdmin", autenticacao.getAuthorities().stream()
                .anyMatch(a -> Perfil.ADMIN.getAutoridade().equals(a.getAuthority())));
        return "moderador/usuarios";
    }

    @PostMapping("/usuarios/{id}/status")
    public String alterarStatus(@PathVariable("id") String id,
                                @RequestParam("ativo") boolean ativo,
                                Authentication autenticacao,
                                RedirectAttributes redirect) {
        try {
            usuarioService.alterarStatus(id, ativo, autenticacao.getName());
            redirect.addFlashAttribute("sucesso", ativo ? "Conta ativada." : "Conta desativada.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/moderador/usuarios";
    }
}
