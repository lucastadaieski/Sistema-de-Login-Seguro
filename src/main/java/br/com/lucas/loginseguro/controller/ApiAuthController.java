package br.com.lucas.loginseguro.controller;

import br.com.lucas.loginseguro.dto.TokenRequest;
import br.com.lucas.loginseguro.service.RegraNegocioException;
import br.com.lucas.loginseguro.service.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private final TokenService tokenService;

    public ApiAuthController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> token(@RequestBody TokenRequest request) {
        if (request.email() == null || request.email().isBlank()
                || request.senha() == null || request.senha().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Informe o e-mail e a senha"));
        }
        try {
            return ResponseEntity.ok(tokenService.emitir(request.email(), request.senha()));
        } catch (RegraNegocioException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", e.getMessage()));
        }
    }
}
