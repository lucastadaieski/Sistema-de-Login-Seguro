package br.com.lucas.loginseguro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dados que o próprio usuário pode alterar no seu perfil. */
public class PerfilForm {

    @NotBlank(message = "Informe seu nome")
    @Size(min = 2, max = 80, message = "O nome deve ter entre 2 e 80 caracteres")
    private String nome;

    /** Nome do tema escolhido; validado contra a lista de temas disponíveis. */
    private String tema;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTema() { return tema; }
    public void setTema(String tema) { this.tema = tema; }
}
