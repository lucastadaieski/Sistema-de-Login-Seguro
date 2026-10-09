package br.com.lucas.loginseguro.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados do formulário de cadastro.
 *
 * Usar um DTO separado da entidade Usuario impede que alguém envie campos
 * extras no formulário (ex.: "perfil=ADMIN") e ganhe privilégios
 * (ataque conhecido como "mass assignment").
 */
public class CadastroForm {

    /** Mínimo de 8 caracteres, com letra maiúscula, minúscula e número. */
    public static final String REGEX_SENHA_FORTE = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,64}$";
    public static final String MSG_SENHA_FORTE =
            "A senha deve ter de 8 a 64 caracteres, com letra maiúscula, letra minúscula e número";

    @NotBlank(message = "Informe seu nome")
    @Size(min = 2, max = 80, message = "O nome deve ter entre 2 e 80 caracteres")
    private String nome;

    @NotBlank(message = "Informe seu e-mail")
    @Email(message = "E-mail inválido")
    @Size(max = 120, message = "E-mail muito longo")
    private String email;

    @NotBlank(message = "Informe uma senha")
    @Pattern(regexp = REGEX_SENHA_FORTE, message = MSG_SENHA_FORTE)
    private String senha;

    @NotBlank(message = "Confirme a senha")
    private String confirmacaoSenha;

    /** Validação entre campos: a confirmação deve ser igual à senha. */
    @AssertTrue(message = "As senhas não conferem")
    public boolean isSenhasConferem() {
        return senha == null || senha.equals(confirmacaoSenha);
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getConfirmacaoSenha() { return confirmacaoSenha; }
    public void setConfirmacaoSenha(String confirmacaoSenha) { this.confirmacaoSenha = confirmacaoSenha; }
}
