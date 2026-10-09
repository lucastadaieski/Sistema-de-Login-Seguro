package br.com.lucas.loginseguro.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Documento de usuário armazenado na coleção "usuarios" do MongoDB.
 *
 * Observações de segurança:
 * - a senha NUNCA é guardada em texto puro, apenas o hash (campo senhaHash);
 * - o e-mail tem índice único, impedindo contas duplicadas mesmo em
 *   cadastros simultâneos;
 * - os campos tentativasFalhas e bloqueadoAte implementam a proteção
 *   contra ataques de força bruta.
 */
@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;

    private String nome;

    /** Sempre gravado em minúsculas (ver UsuarioService). */
    @Indexed(unique = true)
    private String email;

    /** Hash da senha no formato {bcrypt}$2a$10$... */
    private String senhaHash;

    private Perfil perfil = Perfil.USUARIO;

    /** Contas inativas não conseguem fazer login (controle do moderador). */
    private boolean ativo = true;

    private int tentativasFalhas;

    /** Se preenchido e no futuro, a conta está temporariamente bloqueada. */
    private Instant bloqueadoAte;

    /** Tema visual preferido. Nulo significa "usar o tema padrão". */
    private String tema;

    private Instant criadoEm;

    private Instant ultimoLogin;

    /** Indica se a conta está bloqueada neste momento. */
    public boolean isBloqueado() {
        return bloqueadoAte != null && Instant.now().isBefore(bloqueadoAte);
    }

    // ------------------------------------------------------------------
    // Getters e setters
    // ------------------------------------------------------------------

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }

    public Perfil getPerfil() { return perfil; }
    public void setPerfil(Perfil perfil) { this.perfil = perfil; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public int getTentativasFalhas() { return tentativasFalhas; }
    public void setTentativasFalhas(int tentativasFalhas) { this.tentativasFalhas = tentativasFalhas; }

    public Instant getBloqueadoAte() { return bloqueadoAte; }
    public void setBloqueadoAte(Instant bloqueadoAte) { this.bloqueadoAte = bloqueadoAte; }

    public String getTema() { return tema; }
    public void setTema(String tema) { this.tema = tema; }

    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }

    public Instant getUltimoLogin() { return ultimoLogin; }
    public void setUltimoLogin(Instant ultimoLogin) { this.ultimoLogin = ultimoLogin; }
}
