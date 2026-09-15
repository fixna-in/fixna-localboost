package in.fixna.platform.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Issues and verifies JWT access/refresh tokens. Tenant scope travels in the
 * {@code tenantId}/{@code role} claims — issued server-side at login from the
 * membership table, never accepted from client parameters.
 */
@Component
public class JwtService {

    static final String CLAIM_TENANT = "tenantId";
    static final String CLAIM_ROLE = "role";
    static final String CLAIM_TYPE = "typ";

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccess(UUID userId, UUID tenantId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_TENANT, tenantId.toString())
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYPE, "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTtl())))
                .signWith(key)
                .compact();
    }

    public String issueRefresh(UUID userId, UUID tenantId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_TENANT, tenantId.toString())
                .claim(CLAIM_TYPE, "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.refreshTtl())))
                .signWith(key)
                .compact();
    }

    /** Parsed token claims; throws {@link InvalidTokenException} when unusable. */
    public VerifiedToken verify(String token, String expectedType) {
        try {
            Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            Claims claims = jws.getPayload();
            if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
                throw new InvalidTokenException("Unexpected token type");
            }
            if (claims.getExpiration() == null || claims.getExpiration().before(new Date())) {
                throw new InvalidTokenException("Token expired");
            }
            UUID tenantId = claims.get(CLAIM_TENANT, String.class) != null
                    ? UUID.fromString(claims.get(CLAIM_TENANT, String.class))
                    : null;
            return new VerifiedToken(
                    UUID.fromString(claims.getSubject()),
                    tenantId,
                    claims.get(CLAIM_ROLE, String.class));
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Invalid token", ex);
        }
    }

    public record VerifiedToken(UUID userId, UUID tenantId, String role) {}
}
