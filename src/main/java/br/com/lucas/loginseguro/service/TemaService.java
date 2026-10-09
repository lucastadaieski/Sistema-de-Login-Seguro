package br.com.lucas.loginseguro.service;

import br.com.lucas.loginseguro.config.TemaProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Decide qual tema visual cada requisição deve usar.
 *
 * Ordem de prioridade:
 * 1. tema guardado na sessão (preferência do usuário, carregada no login);
 * 2. tema padrão definido em app.temas.padrao.
 *
 * Nenhum controller conhece temas: quem injeta o tema nas páginas é o
 * TemaControllerAdvice. Isso mantém o visual desacoplado da lógica de negócio.
 */
@Service
public class TemaService {

    /** Nome do atributo de sessão que guarda o tema do usuário logado. */
    public static final String ATRIBUTO_SESSAO = "tema";

    private final TemaProperties properties;

    public TemaService(TemaProperties properties) {
        this.properties = properties;
    }

    public List<String> disponiveis() {
        return properties.disponiveis();
    }

    public String padrao() {
        return properties.padrao();
    }

    /** Aceita somente nomes que estão na lista (evita caminhos maliciosos no CSS). */
    public boolean isValido(String tema) {
        return tema != null && properties.disponiveis().contains(tema);
    }

    public String resolver(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        if (sessao != null) {
            Object tema = sessao.getAttribute(ATRIBUTO_SESSAO);
            if (tema instanceof String nome && isValido(nome)) {
                return nome;
            }
        }
        return properties.padrao();
    }

    /** Guarda o tema na sessão; valor nulo ou inválido volta ao padrão. */
    public void aplicarNaSessao(HttpServletRequest request, String tema) {
        HttpSession sessao = request.getSession();
        if (isValido(tema)) {
            sessao.setAttribute(ATRIBUTO_SESSAO, tema);
        } else {
            sessao.removeAttribute(ATRIBUTO_SESSAO);
        }
    }
}
