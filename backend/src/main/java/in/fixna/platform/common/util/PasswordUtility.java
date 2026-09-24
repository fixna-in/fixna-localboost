package in.fixna.platform.common.util;

import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Offline developer utility. Never starts Spring or connects to a database. */
public final class PasswordUtility {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordUtility() { }

    public static String hash(String password) {
        validate(password);
        return ENCODER.encode(password);
    }

    public static boolean verify(String password, String hash) {
        validate(password);
        // Bound the work factor for this interactive local utility.
        if (hash == null || !hash.matches("\\$2[aby]\\$(0[4-9]|1[0-6])\\$[./A-Za-z0-9]{53}")) {
            throw new IllegalArgumentException("Expected a BCrypt hash with cost 04 through 16.");
        }
        return ENCODER.matches(password, hash);
    }

    private static void validate(String password) {
        if (password == null || password.isBlank() || password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must be at least 8 characters, not blank, and at most 72 UTF-8 bytes.");
        }
    }

    public static void main(String[] args) {
        if (args.length != 1 || !(args[0].equals("hash") || args[0].equals("verify"))) {
            System.err.println("Usage: password-tool.cmd hash|verify (passwords are prompted, never arguments)");
            System.exit(2);
        }
        Console console = System.console();
        if (console == null) {
            System.err.println("An interactive terminal is required for hidden password input.");
            System.exit(2);
            return;
        }
        char[] password = console.readPassword("Password: ");
        char[] confirmation = null;
        int exitCode = 0;
        try {
            if (password == null) { throw new IllegalArgumentException("Input cancelled."); }
            if (args[0].equals("hash")) {
                confirmation = console.readPassword("Confirm password: ");
                if (!Arrays.equals(password, confirmation)) {
                    throw new IllegalArgumentException("Passwords do not match.");
                }
                console.printf("BCrypt hash (copy for seed SQL):%n%s%n", hash(new String(password)));
            } else {
                String hash = console.readLine("BCrypt hash: ");
                boolean matches = verify(new String(password), hash);
                console.printf("%s%n", matches ? "MATCH" : "NO MATCH");
                exitCode = matches ? 0 : 1;
            }
        } catch (IllegalArgumentException ex) {
            console.printf("Error: %s%n", ex.getMessage());
            exitCode = 2;
        } finally {
            if (password != null) { Arrays.fill(password, '\0'); }
            if (confirmation != null) { Arrays.fill(confirmation, '\0'); }
        }
        System.exit(exitCode);
    }
}
