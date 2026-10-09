package br.com.lucas.loginseguro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Ponto de entrada da aplicação.
 *
 * {@code @ConfigurationPropertiesScan} registra automaticamente as classes
 * de configuração tipadas do pacote {@code config} (temas, segurança, admin).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LoginSeguroApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoginSeguroApplication.class, args);
    }
}
