package br.com.lucas.loginseguro.controller;

import br.com.lucas.loginseguro.dto.PerfilForm;
import br.com.lucas.loginseguro.dto.TrocaSenhaForm;
import br.com.lucas.loginseguro.model.Usuario;
import br.com.lucas.loginseguro.service.RegraNegocioException;
import br.com.lucas.loginseguro.service.TemaService;
import br.com.lucas.loginseguro.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
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
 * Área de qualquer usuário autenticado (perfil USUARIO ou superior):
 * painel, edição do próprio perfil e troca de senha.
 */
@Controller
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final TemaService temaService;

    public UsuarioController(UsuarioService usuarioService, TemaService temaService) {
        this.usuarioService = usuarioService;
        this.temaService = temaService;
    }

    @GetMapping("/painel")
    public String painel(Authentication autenticacao, Model model) {
        model.addAttribute("usuario", usuarioService.obterPorEmail(autenticacao.getName()));
        return "usuario/painel";
    }

    @GetMapping("/perfil")
    public String perfil(Authentication autenticacao, Model model) {
        Usuario usuario = usuarioService.obterPorEmail(autenticacao.getName());

        PerfilForm perfilForm = new PerfilForm();
        perfilForm.setNome(usuario.getNome());
        perfilForm.setTema(usuario.getTema() != null ? usuario.getTema() : temaService.padrao());

        model.addAttribute("usuario", usuario);
        model.addAttribute("perfilForm", perfilForm);
        model.addAttribute("trocaSenhaForm", new TrocaSenhaForm());
        return "usuario/perfil";
    }

    @PostMapping("/perfil")
    public String salvarPerfil(@Valid @ModelAttribute("perfilForm") PerfilForm form,
                               BindingResult resultado,
                               Authentication autenticacao,
                               HttpServletRequest request,
                               Model model,
                               RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("usuario", usuarioService.obterPorEmail(autenticacao.getName()));
            model.addAttribute("trocaSenhaForm", new TrocaSenhaForm());
            return "usuario/perfil";
        }
        Usuario atualizado = usuarioService.atualizarPerfil(autenticacao.getName(), form.getNome(), form.getTema());
        temaService.aplicarNaSessao(request, atualizado.getTema()); // tema muda na hora
        redirect.addFlashAttribute("sucesso", "Perfil atualizado.");
        return "redirect:/perfil";
    }

    @PostMapping("/perfil/senha")
    public String trocarSenha(@Valid @ModelAttribute("trocaSenhaForm") TrocaSenhaForm form,
                              BindingResult resultado,
                              Authentication autenticacao,
                              Model model,
                              RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                usuarioService.trocarSenha(autenticacao.getName(), form.getSenhaAtual(), form.getNovaSenha());
                redirect.addFlashAttribute("sucesso", "Senha alterada com sucesso.");
                return "redirect:/perfil";
            } catch (RegraNegocioException e) {
                resultado.rejectValue("senhaAtual", "senha.incorreta", e.getMessage());
            }
        }
        // Volta para a página com os erros, preenchendo o outro formulário
        Usuario usuario = usuarioService.obterPorEmail(autenticacao.getName());
        PerfilForm perfilForm = new PerfilForm();
        perfilForm.setNome(usuario.getNome());
        perfilForm.setTema(usuario.getTema() != null ? usuario.getTema() : temaService.padrao());
        model.addAttribute("usuario", usuario);
        model.addAttribute("perfilForm", perfilForm);
        return "usuario/perfil";
    }
}
