package in.fixna.platform.auth;

/** Thrown when a presented JWT is expired, malformed or of the wrong type. */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
