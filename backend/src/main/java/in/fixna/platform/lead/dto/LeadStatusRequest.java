package in.fixna.platform.lead.dto;

import in.fixna.platform.lead.LeadStatus;
import jakarta.validation.constraints.NotNull;

/** Funnel transition payload. */
public record LeadStatusRequest(@NotNull LeadStatus status) {}
