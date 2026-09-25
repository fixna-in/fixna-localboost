package in.fixna.platform.platform;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Deterministic mock adapters + registry resolution (ADR-004, BR-6 idempotency). */
class MockPlatformAdapterTest {

    private final PlatformAdapterRegistry registry = new PlatformAdapterRegistry(
            List.of(new MockGoogleAdsAdapter(), new MockMetaAdsAdapter(), new MockWhatsAppAdapter()));

    private PlatformLaunchRequest request(String name) {
        return new PlatformLaunchRequest(
                UUID.randomUUID(), UUID.randomUUID(), "ref-123", name,
                "FOOTFALL", "GOOGLE", new BigDecimal("5000.00"), "INR",
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(7), "acct-1");
    }

    @Test
    void googleReceiptDerivesExternalIdFromReference() {
        LaunchReceipt receipt = registry.forChannel("google").launch(request("Diwali Promo"));
        assertThat(receipt.platform()).isEqualTo("GOOGLE");
        assertThat(receipt.externalCampaignId()).isEqualTo("gads-ref-123");
        assertThat(receipt.status()).isEqualTo("ACTIVE");
        assertThat(receipt.launchedAt()).isNotNull();
    }

    @Test
    void metaAndWhatsappUseOwnPrefixes() {
        assertThat(registry.forChannel("META").launch(request("Promo")).externalCampaignId())
                .isEqualTo("mads-ref-123");
        assertThat(registry.forChannel("whatsapp").launch(request("Promo")).externalCampaignId())
                .isEqualTo("wa-ref-123");
    }

    @Test
    void repeatedLaunchIsIdempotent() {
        LaunchReceipt first = registry.forChannel("GOOGLE").launch(request("Same Name"));
        LaunchReceipt second = registry.forChannel("GOOGLE").launch(request("Same Name"));
        assertThat(second.externalCampaignId()).isEqualTo(first.externalCampaignId());
    }

    @Test
    void transientFailureIsRetryable() {
        assertThatThrownBy(() -> registry.forChannel("GOOGLE").launch(request("fail-over-test")))
                .isInstanceOf(PlatformException.class)
                .extracting(ex -> ((PlatformException) ex).isRetryable())
                .isEqualTo(true);
    }

    @Test
    void blankNameIsPermanentRejection() {
        assertThatThrownBy(() -> registry.forChannel("META").launch(request("  ")))
                .isInstanceOf(PlatformException.class)
                .extracting(ex -> ((PlatformException) ex).isRetryable())
                .isEqualTo(false);
    }

    @Test
    void unknownChannelIsPermanentRejection() {
        assertThatThrownBy(() -> registry.forChannel("PINTEREST"))
                .isInstanceOf(PlatformException.class)
                .extracting(ex -> ((PlatformException) ex).isRetryable())
                .isEqualTo(false);
    }

    @Test
    void registrySupportsRegisteredPlatformsOnly() {
        assertThat(registry.supports("meta")).isTrue();
        assertThat(registry.supports(" PINTEREST ")).isFalse();
        assertThat(registry.platforms()).containsExactlyInAnyOrder("GOOGLE", "META", "WHATSAPP");
    }

    @Test
    void registryResolvesCampaignChannelAliases() {
        assertThat(registry.supports("GOOGLE_ADS")).isTrue();
        assertThat(registry.supports("META_ADS")).isTrue();
        assertThat(registry.forChannel("GOOGLE_ADS").platform()).isEqualTo("GOOGLE");
    }
}
