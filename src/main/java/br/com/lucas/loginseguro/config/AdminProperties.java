package br.com.lucas.loginseguro.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Dados do administrador inicial (prefixo "app.admin").
 * Os valores vêm das variáveis ADMIN_EMAIL e ADMIN_PASSWORD.
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        @DefaultValue("Administrador") String nome,
        @DefaultValue("") String email,
        @DefaultValue("") String senha) {
}
