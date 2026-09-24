package in.fixna.platform.auth;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.auth.dto.AuthResponse;
import in.fixna.platform.auth.dto.LoginRequest;
import in.fixna.platform.auth.dto.RefreshRequest;
import in.fixna.platform.auth.dto.RegisterRequest;
import in.fixna.platform.common.tenant.AuthenticatedUser;
import in.fixna.platform.common.web.FixnaException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Public auth endpoints. Thin HTTP translation — all logic lives in
 * {@link AuthService}. Rate limiting is enforced at the gateway (CORS/rate
 * rules per security policy) and login failures share one generic message.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "auth", description = "Registration, login, refresh and logout")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register user + tenant (US-001, US-002)")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Login with email + password")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Rotate refresh token (single-use)")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @Operation(summary = "Logout — revokes all refresh tokens",
            description = "Requires a Bearer access token and active tenant membership. "
                    + "Returns 204 on success, 401 for missing/invalid authentication, "
                    + "or 403 for revoked membership.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (principal == null) {
            throw new FixnaException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        authService.logout(principal.userId(), principal.tenantId());
        return ResponseEntity.noContent().build();
    }
}
