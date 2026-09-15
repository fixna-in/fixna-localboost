package in.fixna.platform.notification;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import in.fixna.platform.notification.NotificationProvider.Notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * WF09 notification fan-out: every registered provider receives the event and
 * a failing provider never breaks the originating flow.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationProvider first;
    @Mock NotificationProvider second;

    @Test
    void fanOutToAllProviders() {
        NotificationService service = new NotificationService(List.of(first, second));
        Notification event = new Notification(
                "lead.created", UUID.randomUUID(), "lead", UUID.randomUUID().toString(), Map.of());

        service.publish(event);

        verify(first).send(event);
        verify(second).send(event);
    }

    @Test
    void failingProviderDoesNotBreakFanOut() {
        NotificationService service = new NotificationService(List.of(first, second));
        Notification event = new Notification(
                "campaign.launched", UUID.randomUUID(), "campaign", UUID.randomUUID().toString(), Map.of());
        doThrow(new IllegalStateException("provider down")).when(first).send(event);

        service.publish(event); // must not throw

        verify(second).send(event);
    }

    @Test
    void providerListMayBeNull() {
        NotificationService service = new NotificationService(null);

        service.publish(new Notification(
                "lead.created", null, "lead", null, Map.of())); // no-op, no throw
    }

    @Test
    void notificationRejectsBlankEvent() {
        assertThatThrownBy(() -> new Notification(" ", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}