package in.fixna.platform.lead.dto;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Lead capture payload. Business comes from the path; tenant from the JWT. */
public record LeadRequest(
        @Size(max = 255) String name,
        @Size(max = 50) String phone,
        @Email @Size(max = 320) String email,
        UUID campaignId,
        @Size(max = 100) String source) {}
