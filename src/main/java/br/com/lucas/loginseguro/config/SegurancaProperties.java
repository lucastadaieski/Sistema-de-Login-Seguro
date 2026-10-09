package br.com.lucas.loginseguro.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Parâmetros da proteção contra força bruta (prefixo "app.seguranca").
 *
 * @param maxTentativas   tentativas de senha erradas antes de bloquear a conta
 * @param minutosBloqueio duração do bloqueio temporário
 */
@ConfigurationProperties(prefix = "app.seguranca")
public record SegurancaProperties(
        @DefaultValue("5") int maxTentativas,
        @DefaultValue("15") int minutosBloqueio) {
}
