package in.fixna.platform.campaign.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Single channel allocation. BR-4 enforced on the whole set server-side. */
public record ChannelAllocation(
        @NotBlank @Size(max = 30) String channel,
        @NotNull @DecimalMin(value = "0.01", message = "Allocation must be positive") BigDecimal allocatedBudget) {}
