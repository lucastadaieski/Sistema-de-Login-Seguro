package br.com.lucas.loginseguro.service;

/**
 * Violação de uma regra de negócio (ex.: e-mail já cadastrado, moderador
 * tentando desativar um administrador). A mensagem é exibida ao usuário.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
