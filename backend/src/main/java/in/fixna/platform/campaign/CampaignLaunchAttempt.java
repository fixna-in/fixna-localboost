package in.fixna.platform.campaign;

import in.fixna.platform.platform.LaunchReceipt;
import in.fixna.platform.platform.PlatformException;

/** Outcome of one channel's launch attempt: exactly one side is non-null. */
record CampaignLaunchAttempt(LaunchReceipt receipt, PlatformException failure) {
}
