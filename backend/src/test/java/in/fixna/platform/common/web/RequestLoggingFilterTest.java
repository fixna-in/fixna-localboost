package in.fixna.platform.common.web;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** Access log line carries method/path/status/duration; only for API traffic. */
class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        appender.start();
        ((Logger) LoggerFactory.getLogger("fixna.access")).addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger("fixna.access")).detachAndStopAllAppenders();
    }

    @Test
    void logsStructuredAccessLineForApiRequests() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/campaigns/123/launch");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(appender.list).hasSize(1);
        String line = appender.list.get(0).getFormattedMessage();
        assertThat(line).contains("method=POST");
        assertThat(line).contains("path=/api/v1/campaigns/123/launch");
        assertThat(line).contains("status=200");
        assertThat(line).matches(".*durationMs=\\d+.*");
        assertThat(line).contains("requestId=null");
    }

    @Test
    void neverLogsQueryStrings() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/businesses");
        request.setQueryString("token=sekret&name=abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(appender.list).hasSize(1);
        String line = appender.list.get(0).getFormattedMessage();
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

        assertThat(appender.list).isEmpty();
    }
}