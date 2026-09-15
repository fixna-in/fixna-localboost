package in.fixna.platform.platform;

/**
 * Structured external-call failure (platform integration rules): carries the
 * platform, operation, a stable machine code and whether a retry is safe.
 * The launch orchestrator catches this and maps it to campaign state + audit;
 * it must never leak provider stack traces to API clients.
 */
public class PlatformException extends RuntimeException {

    private final String platform;
    private final String operation;
    private final String code;
    private final boolean retryable;

    private PlatformException(
            String platform, String operation, String code,
            boolean retryable, String message, Throwable cause) {
        super(message, cause);
        this.platform = platform;
        this.operation = operation;
        this.code = code;
        this.retryable = retryable;
    }

    /** Retryable (transient) failure — safe to attempt again. */
    public static PlatformException retryable(String platform, String operation, String message) {
        return new PlatformException(platform, operation, "PROVIDER_TRANSIENT", true, message, null);
    }

    /** Non-retryable failure — retrying will not help (bad input, unsupported). */
    public static PlatformException permanent(String platform, String operation, String message) {
        return new PlatformException(platform, operation, "PROVIDER_REJECTED", false, message, null);
    }

    public String getPlatform() {
        return platform;
    }

    public String getOperation() {
        return operation;
    }

    public String getCode() {
        return code;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
