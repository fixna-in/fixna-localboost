package in.fixna.platform.common.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import in.fixna.platform.common.tenant.TenantContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** Operation timer emits one safe, structured telemetry line on close. */
class OperationTimerTest {

    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        TenantContext.clear(); // never leak scope between tests
        appender.start();
        ((Logger) LoggerFactory.getLogger("fixna.telemetry")).addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger("fixna.telemetry")).detachAndStopAllAppenders();
        TenantContext.clear();
    }

    @Test
    void emitsStructuredLineWithOperationEntitiesAndStatus() {
        try (OperationTimer timer = OperationTimer.start("ai.recommend", "campaign", "00000000-0000-0000-0000-000000000001")) {
            timer.status("SUCCESS");
        }

        assertThat(appender.list).hasSize(1);
        String line = appender.list.get(0).getFormattedMessage();
        assertThat(line).contains("operation=ai.recommend");
        assertThat(line).contains("status=SUCCESS");
        assertThat(line).contains("entity=campaign:00000000-0000-0000-0000-000000000001");
        assertThat(line).matches(".*durationMs=\\d+.*");
    }

    @Test
    void carriesPlatformAndIsSafeWithoutRequestContext() {
        assertThatCode(() -> {
            try (OperationTimer timer = OperationTimer.start(
                    "platform.launch", "campaign", "00000000-0000-0000-0000-000000000002", "GOOGLE")) {
                timer.status("FAILED");
            }
        }).doesNotThrowAnyException();

        assertThat(appender.list).hasSize(1);
        String line = appender.list.get(0).getFormattedMessage();
        assertThat(line).contains("platform=GOOGLE");
        assertThat(line).contains("status=FAILED");
        assertThat(line).contains("requestId=null");
    }

    @Test
    void closeIsIdempotentAndNeverThrows() {
        OperationTimer first = OperationTimer.start("campaign.launch", "campaign", "00000000-0000-0000-0000-000000000003");
        OperationTimer second = OperationTimer.start("audit.view", "tenant", "00000000-0000-0000-0000-000000000004");

        assertThatCode(() -> {
            first.close();
            first.close();
            second.close();
        }).doesNotThrowAnyException();

        assertThat(appender.list).hasSize(2);
    }
}