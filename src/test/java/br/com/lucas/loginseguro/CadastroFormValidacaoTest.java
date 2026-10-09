package br.com.lucas.loginseguro;

import br.com.lucas.loginseguro.dto.CadastroForm;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários das regras de validação do cadastro.
 * Não precisam de banco de dados: rodam em qualquer máquina com "mvn test".
 */
class CadastroFormValidacaoTest {

    private static ValidatorFactory fabrica;
    private static Validator validator;

    @BeforeAll
    static void iniciar() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validator = fabrica.getValidator();
    }

    @AfterAll
    static void encerrar() {
        fabrica.close();
    }

    private CadastroForm form(String nome, String email, String senha, String confirmacao) {
        CadastroForm form = new CadastroForm();
        form.setNome(nome);
        form.setEmail(email);
        form.setSenha(senha);
        form.setConfirmacaoSenha(confirmacao);
        return form;
    }

    private Set<String> camposComErro(CadastroForm form) {
        return validator.validate(form).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    void formularioValidoNaoTemErros() {
        assertThat(camposComErro(form("Maria", "maria@teste.com", "Senha123", "Senha123"))).isEmpty();
    }

    @Test
    void senhaSemMaiusculaEhRejeitada() {
        assertThat(camposComErro(form("Maria", "maria@teste.com", "senha123", "senha123"))).contains("senha");
    }

    @Test
    void senhaSemNumeroEhRejeitada() {
        assertThat(camposComErro(form("Maria", "maria@teste.com", "SenhaForte", "SenhaForte"))).contains("senha");
    }

    @Test
    void senhaCurtaEhRejeitada() {
        assertThat(camposComErro(form("Maria", "maria@teste.com", "Ab1", "Ab1"))).contains("senha");
    }

    @Test
    void emailInvalidoEhRejeitado() {
        assertThat(camposComErro(form("Maria", "maria-sem-arroba", "Senha123", "Senha123"))).contains("email");
    }

    @Test
    void confirmacaoDiferenteEhRejeitada() {
        assertThat(camposComErro(form("Maria", "maria@teste.com", "Senha123", "Senha124"))).contains("senhasConferem");
    }
}
