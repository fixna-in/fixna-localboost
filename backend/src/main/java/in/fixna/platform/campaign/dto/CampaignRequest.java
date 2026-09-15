package in.fixna.platform.campaign.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import in.fixna.platform.campaign.CampaignObjective;

/**
 * Create/update campaign payload. No tenant field — scope from JWT.
 * BR-1: business must belong to the caller's tenant. BR-3: budget positive.
 */
public record CampaignRequest(
        @NotNull UUID businessId,
        @NotBlank @Size(max = 255) String name,
        @NotNull CampaignObjective objective,
        @NotNull @DecimalMin(value = "0.01", message = "Budget must be positive") BigDecimal totalBudget,
        @Size(min = 3, max = 3) String currency,
        OffsetDateTime startAt,
        OffsetDateTime endAt) {}
