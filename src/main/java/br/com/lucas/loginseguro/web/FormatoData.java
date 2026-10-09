package br.com.lucas.loginseguro.web;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Formata datas para exibição nos templates.
 * Uso no Thymeleaf: ${@datas.formatar(usuario.criadoEm)}
 *
 * O MongoDB grava datas em UTC; aqui convertemos para o horário de Brasília.
 */
@Component("datas")
public class FormatoData {

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Sao_Paulo"));

    public String formatar(Instant instante) {
        return instante == null ? "-" : FORMATO.format(instante);
    }
}
