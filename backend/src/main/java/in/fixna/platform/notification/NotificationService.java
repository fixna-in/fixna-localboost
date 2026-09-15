package in.fixna.platform.notification;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import in.fixna.platform.notification.NotificationProvider.Notification;

/**
 * Fan-out dispatcher over all registered {@link NotificationProvider} beans.
 * Delivery failures are logged and swallowed so notifications never break the
 * originating business transaction.
 */
@Component
public class NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    private final List<NotificationProvider> providers;

    public NotificationService(List<NotificationProvider> providers) {
        this.providers = providers == null ? List.of() : List.copyOf(providers);
    }

    /** Publishes to every provider; provider failures are logged, never thrown. */
    public void publish(Notification notification) {
        for (NotificationProvider provider : providers) {
            try {
                provider.send(notification);
            } catch (RuntimeException ex) {
                LOG.warn("Notification provider {} failed for event {}", provider.channel(), notification.event());
            }
        }
    }
}
