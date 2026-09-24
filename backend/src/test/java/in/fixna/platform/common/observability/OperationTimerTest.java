package in.fixna.platform.common.observability;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import in.fixna.platform.common.tenant.TenantContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** Operation timer emits one safe, structured telemetry line on close. */
class OperationTimerTest {

    private final CapturingAppender appender = new CapturingAppender();

    @BeforeEach
    void setUp() {
        TenantContext.clear(); // never leak scope between tests
        appender.start();
        attach(appender);
    }

    @AfterEach
    void tearDown() {
        detach(appender);
        TenantContext.clear();
    }

    @Test
    void emitsStructuredLineWithOperationEntitiesAndStatus() {
        try (OperationTimer timer = OperationTimer.start("ai.recommend", "campaign", "00000000-0000-0000-0000-000000000001")) {
            timer.status("SUCCESS");
        }

        assertThat(appender.messages).hasSize(1);
        String line = appender.messages.get(0);
        assertThat(line).contains("operation=ai.recommend");
        assertThat(line).contains("status=SUCCESS");
        assertThat(line).contains("entity=campaign:00000000-0000-0000-0000-000000000001");
        assertThat(line).matches("(?s).*durationMs=\\d+.*");
    }

    @Test
    void carriesPlatformAndIsSafeWithoutRequestContext() {
        assertThatCode(() -> {
            try (OperationTimer timer = OperationTimer.start(
                    "platform.launch", "campaign", "00000000-0000-0000-0000-000000000002", "GOOGLE")) {
                timer.status("FAILED");
            }
        }).doesNotThrowAnyException();

        assertThat(appender.messages).hasSize(1);
        String line = appender.messages.get(0);
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

        assertThat(appender.messages).hasSize(2);
    }

    private static void attach(CapturingAppender appender) {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        LoggerConfig config = context.getConfiguration().getLoggerConfig("fixna.telemetry");
        config.addAppender(appender, null, null);
        context.updateLoggers();
    }

    private static void detach(CapturingAppender appender) {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        LoggerConfig config = context.getConfiguration().getLoggerConfig("fixna.telemetry");
        config.removeAppender(appender.getName());
        context.updateLoggers();
        appender.stop();
    }

    /** Minimal Log4j2 appender capturing formatted messages for assertions. */
    private static final class CapturingAppender extends AbstractAppender {

        private final List<String> messages = new CopyOnWriteArrayList<>();

        CapturingAppender() {
            super("test-capturing", null, PatternLayout.createDefaultLayout(), false, null);
        }

        @Override
        public void append(LogEvent event) {
            messages.add(event.getMessage().getFormattedMessage());
        }
    }
}