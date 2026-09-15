package in.fixna.platform.common.observability;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import in.fixna.platform.common.web.ReadinessController.RunnableIsReady;

/**
 * Drives the readiness probe: flips true only after the Spring context is
 * fully refreshed (ApplicationReadyEvent), so load balancers never route to a
 * half-started application.
 */
@Component
public class AppReadiness implements RunnableIsReady {

    private final AtomicBoolean ready = new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        ready.set(true);
    }

    @Override
    public boolean get() {
        return ready.get();
    }
}