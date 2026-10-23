package br.com.lucas.loginseguro.service;

import br.com.lucas.loginseguro.config.JwtProperties;
import br.com.lucas.loginseguro.model.Usuario;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TokenService {

    private static final String EMISSOR = "login-seguro";

    private final AuthenticationManager authenticationManager;
    private final UsuarioService usuarioService;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwt;

    public TokenService(AuthenticationManager authenticationManager,
                        UsuarioService usuarioService,
                        JwtEncoder jwtEncoder,
                        JwtProperties jwt) {
        this.authenticationManager = authenticationManager;
        this.usuarioService = usuarioService;
        this.jwtEncoder = jwtEncoder;
        this.jwt = jwt;
    }

    /**
     * Confere e-mail e senha e devolve o token.
     *
     * @throws RegraNegocioException com a mensagem a exibir, se o login falhar
     */
    public Map<String, Object> emitir(String email, String senha) {
        try {
            authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, senha));
        } catch (LockedException e) {
            throw new RegraNegocioException("Conta temporariamente bloqueada por excesso de tentativas");
        } catch (DisabledException e) {
            throw new RegraNegocioException("Conta desativada");
        } catch (AuthenticationException e) {
            if (usuarioService.registrarFalhaLogin(email)) {
                throw new RegraNegocioException("Conta temporariamente bloqueada por excesso de tentativas");
            }
            throw new RegraNegocioException("E-mail ou senha inválidos");
        }

        Usuario usuario = usuarioService.registrarLoginSucesso(email);
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(Duration.ofMinutes(jwt.validadeMinutos()));

        JwtClaimsSet dados = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .subject(usuario.getId())
                .claim("email", usuario.getEmail())
                .claim("nome", usuario.getNome())
                .claim("perfil", usuario.getPerfil().name())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), dados)).getTokenValue();

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("token", token);
        resposta.put("tipo", "Bearer");
        resposta.put("expiraEm", expiraEm);
        resposta.put("usuarioId", usuario.getId());
        resposta.put("perfil", usuario.getPerfil().name());
        return resposta;
    }
}
