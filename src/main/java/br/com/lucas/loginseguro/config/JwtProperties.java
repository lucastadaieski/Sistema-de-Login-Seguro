package br.com.lucas.loginseguro.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank(message = "Defina JWT_SECRET no arquivo .env")
        @Size(min = 32, message = "JWT_SECRET precisa ter pelo menos 32 caracteres")
        String segredo,

        @Min(1) @DefaultValue("60") long validadeMinutos) {
}
