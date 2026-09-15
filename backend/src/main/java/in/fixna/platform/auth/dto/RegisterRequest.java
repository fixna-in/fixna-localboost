package in.fixna.platform.auth.dto;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Registration request. Creates user + tenant + owner membership atomically. */
public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @NotBlank @Size(max = 200) String tenantName) {

    public String normalizedEmail() {
        return email == null ? null : email.trim().toLowerCase();
    }
}
