package co.edu.eci.blueprints.auth;

import co.edu.eci.blueprints.security.InMemoryUserService;
import co.edu.eci.blueprints.security.RsaKeyProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final JwtEncoder encoder;
    private final InMemoryUserService userService;
    private final RsaKeyProperties props;

    public AuthController(JwtEncoder encoder, InMemoryUserService userService, RsaKeyProperties props) {
        this.encoder = encoder;
        this.userService = userService;
        this.props = props;
    }

    public record LoginRequest(String username, String password) {}
    public record TokenResponse(String access_token, String token_type, long expires_in) {}

    /**
     * Autentica al usuario y devuelve un token JWT.
     *
     * @param req Datos de login (username, password)
     * @return Token JWT y detalles
     */
    @io.swagger.v3.oas.annotations.Operation(
        summary = "Login de usuario",
        description = "Autentica al usuario y devuelve un token JWT válido para acceder a los endpoints protegidos.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Autenticación exitosa", content = @io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\"access_token\":\"eyJ...\",\"token_type\":\"Bearer\",\"expires_in\":3600}")
        )),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Credenciales inválidas", content = @io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\"error\":\"invalid_credentials\"}")
        ))
    })
    @PostMapping("/login")
    public ResponseEntity<co.edu.eci.blueprints.api.ApiResponse<TokenResponse>> login(@RequestBody LoginRequest req) {
        if (!userService.isValid(req.username(), req.password())) {
            return ResponseEntity.status(401)
                .body(new co.edu.eci.blueprints.api.ApiResponse<>(401, "invalid_credentials", null));
        }

        Instant now = Instant.now();
        long ttl = props.tokenTtlSeconds() != null ? props.tokenTtlSeconds() : 3600;
        Instant exp = now.plusSeconds(ttl);

        String scope = "blueprints.read blueprints.write blueprints.addPoint";

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .issuedAt(now)
                .expiresAt(exp)
                .subject(req.username())
                .claim("scope", scope)
                .build();

        JwsHeader jws = JwsHeader.with(() -> "RS256").build();
        String token = this.encoder.encode(JwtEncoderParameters.from(jws, claims)).getTokenValue();

        TokenResponse response = new TokenResponse(token, "Bearer", ttl);
        return ResponseEntity.ok(new co.edu.eci.blueprints.api.ApiResponse<>(200, "Success", response));
    }
}
