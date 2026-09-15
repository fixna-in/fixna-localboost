package in.fixna.platform.platform;

/**
 * Abstraction over an advertising platform (ADR-004). Core campaign domain
 * depends ONLY on this interface — no provider SDK types may leak into
 * campaign/business code. Implementations must:
 * - derive external ids deterministically from {@code externalReference}
 *   so repeated launches are idempotent (BR-6),
 * - never require or log credentials in mock/demo mode,
 * - throw {@link PlatformException} with retryable classification for
 *   structured error mapping.
 * Real adapters must apply timeouts and map provider errors to
 * PlatformException; nothing here blocks on network in MVP (mocks only).
 */
public interface AdvertisingPlatformAdapter {

    /** Upper-case platform/channel name this adapter serves (GOOGLE, META, WHATSAPP). */
    String platform();

    /**
     * Executes (mock) campaign creation on the platform for one channel
     * allocation. Advisory-safe: callers own all lifecycle decisions; the
     * adapter only reports what the platform would do.
     */
    LaunchReceipt launch(PlatformLaunchRequest request);
}
