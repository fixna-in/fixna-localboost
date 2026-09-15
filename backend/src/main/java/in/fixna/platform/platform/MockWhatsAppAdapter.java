package in.fixna.platform.platform;

import org.springframework.stereotype.Component;

/** Demo-mode WhatsApp adapter. External ids: {@code wa-<reference>}. */
@Component
public class MockWhatsAppAdapter extends AbstractMockPlatformAdapter {

    public MockWhatsAppAdapter() {
        super("WHATSAPP", "wa");
    }
}
