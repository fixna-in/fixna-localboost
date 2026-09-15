package in.fixna.platform.platform;

import org.springframework.stereotype.Component;

/** Demo-mode Google Ads adapter. External ids: {@code gads-<reference>}. */
@Component
public class MockGoogleAdsAdapter extends AbstractMockPlatformAdapter {

    public MockGoogleAdsAdapter() {
        super("GOOGLE", "gads");
    }
}
