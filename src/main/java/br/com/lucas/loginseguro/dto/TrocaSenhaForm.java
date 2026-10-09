package br.com.lucas.loginseguro.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Formulário de troca de senha (exige a senha atual). */
public class TrocaSenhaForm {

    @NotBlank(message = "Informe a senha atual")
    private String senhaAtual;

    @NotBlank(message = "Informe a nova senha")
    @Pattern(regexp = CadastroForm.REGEX_SENHA_FORTE, message = CadastroForm.MSG_SENHA_FORTE)
    private String novaSenha;

    @NotBlank(message = "Confirme a nova senha")
    private String confirmacaoSenha;

    @AssertTrue(message = "As senhas não conferem")
    public boolean isSenhasConferem() {
        return novaSenha == null || novaSenha.equals(confirmacaoSenha);
    }

    public String getSenhaAtual() { return senhaAtual; }
    public void setSenhaAtual(String senhaAtual) { this.senhaAtual = senhaAtual; }

    public String getNovaSenha() { return novaSenha; }
    public void setNovaSenha(String novaSenha) { this.novaSenha = novaSenha; }

    public String getConfirmacaoSenha() { return confirmacaoSenha; }
    public void setConfirmacaoSenha(String confirmacaoSenha) { this.confirmacaoSenha = confirmacaoSenha; }
}
