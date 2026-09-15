package in.fixna.platform.ai;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import in.fixna.platform.common.web.FixnaException;

/**
 * Request-side business rules (rules 11/13, BR-3/BR-4): validated BEFORE the
 * provider is called, so a malformed request never reaches an LLM. Failures
 * raise {@code AI_VALIDATION_FAILED}.
 */
@Component
public class AiBusinessValidator {

    private static final int MIN_AGE = 13;
    private static final int MAX_AGE = 75;

    /** Throws {@code AI_VALIDATION_FAILED} when the request violates business rules. */
    public void validate(RecommendationType type, Map<String, Object> payload) {
        Map<String, Object> context = payload == null ? Map.of() : payload;
        switch (type) {
            case CAMPAIGN_STRATEGY -> checkBudget(context, false);
            case BUDGET_ALLOCATION -> checkBudgetAllocation(context);
            case AUDIENCE -> checkAudience(context);
            case CREATIVE -> {
                // No request-side rules yet; output rules live in the schema validator.
            }
        }
    }

    /** BR-3: budget, when required/selected, must be a positive number. */
    private void checkBudget(Map<String, Object> context, boolean required) {
        Object value = context.get("budget");
        if (value == null) {
            if (required) {
                throw validation("budget is required");
            }
            return;
        }
        if (!(value instanceof Number number) || number.doubleValue() <= 0) {
            throw validation("budget must be a positive number (BR-3)");
        }
    }

    /** BR-4: per-channel allocations must be positive, unique and within budget. */
    private void checkBudgetAllocation(Map<String, Object> context) {
        checkBudget(context, true);
        if (!(context.get("channels") instanceof List<?> channels) || channels.isEmpty()) {
            throw validation("channels with per-channel amounts are required");
        }
        Set<String> seen = new HashSet<>();
        double total = 0d;
        for (Object item : channels) {
            if (!(item instanceof Map<?, ?> entry)) {
                throw validation("channels entries must be objects");
            }
            if (!(entry.get("channel") instanceof String channel) || channel.isBlank()) {
                throw validation("channel names must be non-blank");
            }
            if (!seen.add(channel)) {
                throw validation("duplicate channel: " + channel);
            }
            if (!(entry.get("amount") instanceof Number amount) || amount.doubleValue() <= 0) {
                throw validation("channel amounts must be positive numbers");
            }
            total += amount.doubleValue();
        }
        if (context.get("budget") instanceof Number budget && total > budget.doubleValue()) {
            throw validation(
                    "Channel allocations (" + total + ") exceed the requested budget ("
                            + budget.doubleValue() + ") (BR-4)");
        }
    }

    /** Audience bounds: 13 <= age < 75 and ageMin below ageMax when both given. */
    private void checkAudience(Map<String, Object> context) {
        Object rawMin = context.get("ageMin");
        Object rawMax = context.get("ageMax");
        Integer ageMin = null;
        Integer ageMax = null;
        if (rawMin instanceof Number) {
            ageMin = Integer.valueOf(((Number) rawMin).intValue());
        }
        if (rawMax instanceof Number) {
            ageMax = Integer.valueOf(((Number) rawMax).intValue());
        }
        if (ageMin != null && ageMin.intValue() < MIN_AGE) {
            throw validation("ageMin must be at least " + MIN_AGE);
        }
        if (ageMax != null && ageMax.intValue() > MAX_AGE) {
            throw validation("ageMax must be at most " + MAX_AGE);
        }
        if (ageMin != null && ageMax != null) {
            double minVal = ((Number) rawMin).doubleValue();
            double maxVal = ((Number) rawMax).doubleValue();
            if (minVal >= maxVal) {
                throw validation("ageMin must be lower than ageMax");
            }
        }
    }

    private FixnaException validation(String message) {
        return new FixnaException(
                "AI_VALIDATION_FAILED", HttpStatus.BAD_REQUEST,
                "AI request failed business validation: " + message);
    }
}
