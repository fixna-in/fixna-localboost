package in.fixna.platform.common.web;

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
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** Access log line carries method/path/status/duration; only for API traffic. */
class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();
    private final CapturingAppender appender = new CapturingAppender();

    @BeforeEach
    void setUp() {
        appender.start();
        attach(appender);
    }

    @AfterEach
    void tearDown() {
        detach(appender);
    }

    @Test
    void logsStructuredAccessLineForApiRequests() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/campaigns/123/launch");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(appender.messages).hasSize(1);
        String line = appender.messages.get(0);
        assertThat(line).contains("method=POST");
        assertThat(line).contains("path=/api/v1/campaigns/123/launch");
        assertThat(line).contains("status=200");
        assertThat(line).matches("(?s).*durationMs=\\d+.*");
        assertThat(line).contains("requestId=null");
    }

    @Test
    void neverLogsQueryStrings() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/businesses");
        request.setQueryString("token=sekret&name=abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(appender.messages).hasSize(1);
        String line = appender.messages.get(0);
        assertThat(line).contains("path=/api/v1/businesses");
        assertThat(line).doesNotContain("token=sekret");
        assertThat(line).doesNotContain("name=abc");
    }

    @Test
    void skipsNonApiPaths() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(appender.messages).isEmpty();
    }

    private static void attach(CapturingAppender appender) {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        LoggerConfig config = context.getConfiguration().getLoggerConfig("fixna.access");
        config.addAppender(appender, null, null);
        context.updateLoggers();
    }

    private static void detach(CapturingAppender appender) {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        LoggerConfig config = context.getConfiguration().getLoggerConfig("fixna.access");
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