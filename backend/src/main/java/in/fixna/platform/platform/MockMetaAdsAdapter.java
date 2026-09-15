package in.fixna.platform.platform;

import org.springframework.stereotype.Component;

/** Demo-mode Meta Ads adapter. External ids: {@code mads-<reference>}. */
@Component
public class MockMetaAdsAdapter extends AbstractMockPlatformAdapter {

    public MockMetaAdsAdapter() {
        super("META", "mads");
    }
}
