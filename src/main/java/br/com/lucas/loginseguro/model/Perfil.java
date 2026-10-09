package br.com.lucas.loginseguro.model;

/**
 * Perfis (roles) do sistema, do menor para o maior privilégio.
 *
 * A hierarquia é configurada em {@code SecurityConfig}:
 * ADMIN herda tudo de MODERADOR, que herda tudo de USUARIO.
 */
public enum Perfil {

    USUARIO("Usuário"),
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
