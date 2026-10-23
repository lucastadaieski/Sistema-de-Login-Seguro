package br.com.lucas.loginseguro.model;

public enum Perfil {

    USUARIO("Usuário"),
    TECNICO("Técnico"),
    MODERADOR("Moderador"),
    ADMIN("Administrador");

    private final String descricao;

    Perfil(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Nome da autoridade no padrão do Spring Security (prefixo ROLE_). */
    public String getAutoridade() {
        return "ROLE_" + name();
    }
}
