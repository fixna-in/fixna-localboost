package in.fixna.platform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Login request. Tenant is selected server-side from memberships. */
public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {

    public String normalizedEmail() {
        return email == null ? null : email.trim().toLowerCase();
    }
}
