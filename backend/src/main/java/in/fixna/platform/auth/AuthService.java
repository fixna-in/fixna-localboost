package in.fixna.platform.auth;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.auth.dto.AuthResponse;
import in.fixna.platform.auth.dto.LoginRequest;
import in.fixna.platform.auth.dto.RegisterRequest;
import in.fixna.platform.common.audit.AuditEvent;
import in.fixna.platform.common.audit.AuditPublisher;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.tenant.MembershipRole;
import in.fixna.platform.tenant.Tenant;
import in.fixna.platform.tenant.TenantMembership;
import in.fixna.platform.tenant.TenantMembershipRepository;
import in.fixna.platform.tenant.TenantRepository;
import in.fixna.platform.tenant.TenantType;
import in.fixna.platform.user.User;
import in.fixna.platform.user.UserRepository;

/**
 * Authentication application service. Owns register/login/refresh/logout.
 * Tenant membership is the only source of tenant access; tokens carry the
 * server-resolved tenant/role claims.
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final TenantRepository tenants;
    private final TenantMembershipRepository memberships;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;
    private final JwtProperties jwtProperties;
    private final AuditPublisher audit;

    public AuthService(
            UserRepository users,
            TenantRepository tenants,
            TenantMembershipRepository memberships,
            RefreshTokenRepository refreshTokens,
            PasswordEncoder passwordEncoder,
            JwtService jwt,
            JwtProperties jwtProperties,
            AuditPublisher audit) {
        this.users = users;
        this.tenants = tenants;
        this.memberships = memberships;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.jwtProperties = jwtProperties;
        this.audit = audit;
    }

    /** US-001/US-002: register user + SMB tenant + owner membership atomically. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.normalizedEmail();
        if (users.existsByEmail(email)) {
            throw new FixnaException("EMAIL_TAKEN", HttpStatus.CONFLICT, "Email is already registered");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        users.save(user);

        Tenant tenant = new Tenant();
        tenant.setName(request.tenantName().trim());
        tenant.setTenantType(TenantType.SMB);
        tenants.save(tenant);

        TenantMembership membership = new TenantMembership();
        membership.setTenantId(tenant.getId());
        membership.setUserId(user.getId());
        membership.setRole(MembershipRole.TENANT_OWNER);
        memberships.save(membership);

        audit.publish(new AuditEvent(
                "auth.registered", tenant.getId(), user.getId(), "user", user.getId().toString(),
                Map.of("tenant", tenant.getId().toString()), null));
        return issueTokens(user.getId(), tenant.getId(), MembershipRole.TENANT_OWNER);
    }

    /** Login: verifies credentials, selects tenant server-side from memberships. */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmail(request.normalizedEmail())
                .orElseThrow(() -> new FixnaException(
                        "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new FixnaException(
                    "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        // Never trust a client-supplied tenant: earliest membership wins,
        // preferring an OWNER grant so a fresh registrant lands in context.
        java.util.List<TenantMembership> owned = memberships.findByUserId(user.getId());
        owned.sort((a, b) -> {
            boolean aOwner = a.getRole() == MembershipRole.TENANT_OWNER;
            boolean bOwner = b.getRole() == MembershipRole.TENANT_OWNER;
            if (aOwner && !bOwner) {
                return -1;
            }
            if (bOwner && !aOwner) {
                return 1;
            }
            return a.getCreatedAt().compareTo(b.getCreatedAt());
        });
        if (owned.isEmpty()) {
            throw new FixnaException("NO_MEMBERSHIP", HttpStatus.FORBIDDEN, "User has no tenant membership");
        }
        TenantMembership selected = owned.get(0);
        audit.publish(new AuditEvent(
                "auth.login", selected.getTenantId(), user.getId(), "user", user.getId().toString(),
                Map.of(), null));
        return issueTokens(user.getId(), selected.getTenantId(), selected.getRole());
    }

    /** Refresh: validates stored hash, rotates (single-use), re-checks membership. */
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        JwtService.VerifiedToken verified;
        try {
            verified = jwt.verify(rawRefreshToken, "refresh");
        } catch (InvalidTokenException ex) {
            throw new FixnaException(
                    "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED, "Refresh token is invalid", ex);
        }
        RefreshToken stored = refreshTokens
                .findByTokenHashAndRevokedFalse(RefreshToken.hash(rawRefreshToken))
                .orElseThrow(() -> new FixnaException(
                        "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED, "Refresh token is invalid"));
        if (stored.getExpiresAt().isBefore(java.time.OffsetDateTime.now())) {
            throw new FixnaException(
                    "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED, "Refresh token is expired");
        }
        if (!stored.getUserId().equals(verified.userId())) {
            throw new FixnaException(
                    "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED, "Refresh token is invalid");
        }
        TenantMembership membership = memberships
                .findByTenantIdAndUserId(stored.getTenantId(), stored.getUserId())
                .orElseThrow(() -> new FixnaException(
                        "NO_MEMBERSHIP", HttpStatus.FORBIDDEN, "Membership was revoked"));
        // Single-use rotation: revoke the presented token before issuing the next.
        stored.setRevoked(true);
        refreshTokens.save(stored);
        return issueTokens(stored.getUserId(), stored.getTenantId(), membership.getRole());
    }

    /** Logout: revokes all refresh tokens for the user (all devices). */
    @Transactional
    public void logout(UUID userId, UUID tenantId) {
        refreshTokens.revokeAllForUser(userId);
        audit.publish(new AuditEvent(
                "auth.logout", tenantId, userId, "user", userId.toString(), Map.of(), null));
    }

    private AuthResponse issueTokens(UUID userId, UUID tenantId, MembershipRole role) {
        String access = jwt.issueAccess(userId, tenantId, role.name());
        String refresh = jwt.issueRefresh(userId, tenantId);
        RefreshToken record = new RefreshToken();
        record.setUserId(userId);
        record.setTenantId(tenantId);
        record.setTokenHash(RefreshToken.hash(refresh));
        record.setExpiresAt(java.time.OffsetDateTime.now().plus(jwtProperties.refreshTtl()));
        record.setRevoked(false);
        refreshTokens.save(record);
        return AuthResponse.bearer(
                access, refresh, jwtProperties.accessTtl().toSeconds(), userId, tenantId, role.name());
    }
}
