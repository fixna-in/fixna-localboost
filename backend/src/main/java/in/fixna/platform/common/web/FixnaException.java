package in.fixna.platform.common.web;

import org.springframework.http.HttpStatus;

/**
 * Application exception carrying a stable machine-readable code and an HTTP
 * status. Handled by {@link GlobalExceptionHandler} — controllers and
 * services throw this instead of leaking provider or persistence details.
 */
public class FixnaException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public FixnaException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public FixnaException(String code, HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
