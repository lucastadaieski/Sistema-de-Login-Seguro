package br.com.lucas.loginseguro.dto;

/** Corpo do POST /api/auth/token: as mesmas credenciais da tela de login. */
public record TokenRequest(String email, String senha) {
}
