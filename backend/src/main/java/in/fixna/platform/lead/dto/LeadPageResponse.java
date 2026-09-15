package in.fixna.platform.lead.dto;

import java.util.List;

import in.fixna.platform.lead.Lead;

/**
 * Pagination envelope (API rules: collections are paginated). Deliberately
 * not serializing Spring's {@code Page} at the API boundary.
 */
public record LeadPageResponse(
        List<LeadResponse> items, int page, int size, long totalElements, int totalPages) {

    public static LeadPageResponse of(org.springframework.data.domain.Page<Lead> result) {
        return new LeadPageResponse(
                result.getContent().stream().map(LeadResponse::from).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }
}
