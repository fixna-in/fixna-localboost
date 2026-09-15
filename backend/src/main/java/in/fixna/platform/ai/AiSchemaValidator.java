package in.fixna.platform.ai;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import in.fixna.platform.common.web.FixnaException;

/**
 * Schema validation of untrusted provider OUTPUT (rules 11/13): required
 * fields and basic value rules per recommendation type. Every failure raises
 * {@code AI_RESPONSE_INVALID}; a JSON-schema engine can replace this later
 * without touching callers.
 */
@Component
public class AiSchemaValidator {

    private static final int MAX_HEADLINE_LENGTH = 60;
    private static final int MIN_RADIUS_KM = 1;
    private static final int MAX_RADIUS_KM = 100;
    private static final java.util.Set<String> ALLOWED_CTA = java.util.Set.of(
            "GET_OFFER", "LEARN_MORE", "CALL_NOW", "BOOK_NOW",
            "VISIT_STORE", "ORDER_ONLINE", "WHATSAPP_US");

    /** Throws {@code AI_RESPONSE_INVALID} when output is structurally unusable. */
    public void validate(RecommendationType type, Map<String, Object> data) {
        Map<String, Object> output = data == null ? Map.of() : data;
        switch (type) {
            case CAMPAIGN_STRATEGY -> validateStrategy(output);
            case AUDIENCE -> validateAudience(output);
            case BUDGET_ALLOCATION -> validateBudget(output);
            case CREATIVE -> validateCreative(output);
        }
    }

    private void validateStrategy(Map<String, Object> output) {
        requireText(output, "objective");
        requireText(output, "rationale");
        requirePositiveNumber(output, "recommendedBudget");
        List<?> channels = requireList(output, "channels");
        if (channels.isEmpty()) {
            throw failure("channels must not be empty");
        }
        for (Object channel : channels) {
            if (!(channel instanceof String text) || text.isBlank()) {
                throw failure("channels entries must be non-blank strings");
            }
        }
    }

    private void validateAudience(Map<String, Object> output) {
        int ageMin = requireInt(output, "ageMin");
        int ageMax = requireInt(output, "ageMax");
        if (ageMin >= ageMax) {
            throw failure("ageMin must be lower than ageMax");
        }
        if (output.get("radiusKm") instanceof Number radius
                && (radius.intValue() < MIN_RADIUS_KM || radius.intValue() > MAX_RADIUS_KM)) {
            throw failure("radiusKm must be between " + MIN_RADIUS_KM + " and " + MAX_RADIUS_KM);
        }
    }

    private void validateBudget(Map<String, Object> output) {
        List<?> allocations = requireList(output, "allocations");
        if (allocations.isEmpty()) {
            throw failure("allocations must not be empty");
        }
        for (Object item : allocations) {
            if (!(item instanceof Map<?, ?> allocation)) {
                throw failure("allocations entries must be objects");
            }
            if (!(allocation.get("channel") instanceof String channel) || channel.isBlank()) {
                throw failure("allocation channel must be a non-blank string");
            }
            if (!(allocation.get("amount") instanceof Number amount) || amount.doubleValue() <= 0) {
                throw failure("allocation amount must be a positive number");
            }
        }
    }

    private void validateCreative(Map<String, Object> output) {
        String headline = requireText(output, "headline");
        requireText(output, "description");
        String cta = requireText(output, "cta");
        if (headline.length() > MAX_HEADLINE_LENGTH) {
            throw failure("headline must be at most " + MAX_HEADLINE_LENGTH + " characters");
        }
        if (!ALLOWED_CTA.contains(cta)) {
            throw failure("cta must be one of " + ALLOWED_CTA);
        }
    }

    private String requireText(Map<String, Object> output, String field) {
        if (!(output.get(field) instanceof String text) || text.isBlank()) {
            throw failure(field + " must be a non-blank string");
        }
        return text;
    }

    private int requireInt(Map<String, Object> output, String field) {
        if (!(output.get(field) instanceof Number number)) {
            throw failure(field + " must be a number");
        }
        return number.intValue();
    }

    private void requirePositiveNumber(Map<String, Object> output, String field) {
        if (!(output.get(field) instanceof Number number) || number.doubleValue() <= 0) {
            throw failure(field + " must be a positive number");
        }
    }

    private List<?> requireList(Map<String, Object> output, String field) {
        if (!(output.get(field) instanceof List<?> list)) {
            throw failure(field + " must be a list");
        }
        return list;
    }

    private FixnaException failure(String message) {
        return new FixnaException(
                "AI_RESPONSE_INVALID", HttpStatus.BAD_GATEWAY,
                "AI output failed schema validation: " + message);
    }
}
