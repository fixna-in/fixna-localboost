package in.fixna.platform.audience.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * Audience payload. The definition is a JSON object with recognized criteria:
 * ageMin/ageMax (numbers), genders/interests/incomeBrackets/deviceTypes/
 * languages (non-empty string arrays). Validated structurally by
 * {@link in.fixna.platform.audience.AudienceService}.
 */
public record AudienceRequest(
        @NotBlank @Size(max = 255) String name,
        @NotEmpty Map<String, Object> definition) {}