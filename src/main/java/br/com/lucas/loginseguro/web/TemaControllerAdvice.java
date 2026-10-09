package br.com.lucas.loginseguro.web;

import br.com.lucas.loginseguro.service.TemaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * Injeta as informações de tema em TODAS as páginas automaticamente.
 *
 * Assim os controllers ficam livres de qualquer lógica visual, e o layout
 * base (templates/layouts/base.html) usa a variável "temaAtivo" para
 * carregar o CSS do tema certo.
 */
@ControllerAdvice
public class TemaControllerAdvice {

    private final TemaService temaService;

    public TemaControllerAdvice(TemaService temaService) {
        this.temaService = temaService;
    }

    @ModelAttribute("temaAtivo")
    public String temaAtivo(HttpServletRequest request) {
        return temaService.resolver(request);
    }

    @ModelAttribute("temasDisponiveis")
    public List<String> temasDisponiveis() {
        return temaService.disponiveis();
    }
}
