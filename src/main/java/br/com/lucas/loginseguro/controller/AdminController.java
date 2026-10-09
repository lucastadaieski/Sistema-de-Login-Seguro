package br.com.lucas.loginseguro.controller;

import br.com.lucas.loginseguro.model.Perfil;
import br.com.lucas.loginseguro.service.RegraNegocioException;
import br.com.lucas.loginseguro.service.SessaoService;
import br.com.lucas.loginseguro.service.TemaService;
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
 * Área exclusiva do perfil ADMIN: visão geral do sistema, alteração de
 * perfis e exclusão de contas.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UsuarioService usuarioService;
    private final SessaoService sessaoService;
    private final TemaService temaService;

    public AdminController(UsuarioService usuarioService, SessaoService sessaoService, TemaService temaService) {
        this.usuarioService = usuarioService;
        this.sessaoService = sessaoService;
        this.temaService = temaService;
    }

    @GetMapping
    public String painel(Model model) {
        model.addAttribute("contagemPorPerfil", usuarioService.contarPorPerfil());
        model.addAttribute("sessoesAtivas", sessaoService.contarSessoesAtivas());
        model.addAttribute("temaPadrao", temaService.padrao());
        return "admin/painel";
    }

    @PostMapping("/usuarios/{id}/perfil")
    public String alterarPerfil(@PathVariable("id") String id,
                                @RequestParam("perfil") Perfil perfil,
                                Authentication autenticacao,
                                RedirectAttributes redirect) {
        try {
            usuarioService.alterarPerfil(id, perfil, autenticacao.getName());
            redirect.addFlashAttribute("sucesso", "Perfil alterado para " + perfil.getDescricao() + ".");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/moderador/usuarios";
    }

    @PostMapping("/usuarios/{id}/excluir")
    public String excluir(@PathVariable("id") String id,
                          Authentication autenticacao,
                          RedirectAttributes redirect) {
        try {
            usuarioService.excluir(id, autenticacao.getName());
            redirect.addFlashAttribute("sucesso", "Conta excluída.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/moderador/usuarios";
    }
}
