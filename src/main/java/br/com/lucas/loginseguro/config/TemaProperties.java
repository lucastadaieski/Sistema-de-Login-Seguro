package br.com.lucas.loginseguro.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Configuração de temas visuais (prefixo "app.temas" no application.properties).
 *
 * @param padrao      tema usado quando o usuário não escolheu nenhum
 * @param disponiveis nomes das pastas em static/temas/ que podem ser escolhidas
 */
@ConfigurationProperties(prefix = "app.temas")
public record TemaProperties(
        @DefaultValue("claro") String padrao,
        @DefaultValue({"claro", "escuro"}) List<String> disponiveis) {
}
