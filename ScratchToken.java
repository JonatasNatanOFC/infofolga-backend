import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class ScratchToken {
    public static void main(String[] args) {
        String secret = "jZ5fL9sQv3xG8yW2rP6dC1bA4nU0kH7mF5eJ6vO8zT=";
        Algorithm algorithm = Algorithm.HMAC256(secret);

        String token = JWT.create()
                .withIssuer("infofolga-api")
                .withSubject("11122233344")
                .withClaim("nome", "Gerente")
                .withClaim("role", "ROLE_GERENTE")
                .withExpiresAt(LocalDateTime.now().plusHours(8).atZone(ZoneId.of("America/Sao_Paulo")).toInstant())
                .sign(algorithm);
        System.out.println(token);
    }
}
