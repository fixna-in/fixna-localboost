package in.fixna.platform.common.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;

import in.fixna.platform.common.logging.LoggingConstants;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** OTel trace/span ids are copied into MDC when a tracer span is active. */
class RequestIdFilterTraceTest {

    private final RequestIdFilter filter =
            RequestIdFilter.forTests(mockTracer("trace-abc", "span-def"));

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void copiesActiveTraceIntoMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            assertThat(MDC.get(LoggingConstants.TRACE_ID)).isEqualTo("trace-abc");
            assertThat(MDC.get(LoggingConstants.SPAN_ID)).isEqualTo("span-def");
        });
    }

    @Test
    void clearsTraceIdsAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(MDC.get(LoggingConstants.TRACE_ID)).isNull();
        assertThat(MDC.get(LoggingConstants.SPAN_ID)).isNull();
    }

    private static Tracer mockTracer(String traceId, String spanId) {
        TraceContext context = mock(TraceContext.class);
        when(context.traceId()).thenReturn(traceId);
        when(context.spanId()).thenReturn(spanId);

        Span span = mock(Span.class);
        when(span.context()).thenReturn(context);

        Tracer tracer = mock(Tracer.class);
        when(tracer.currentSpan()).thenReturn(span);
        return tracer;
    }
}
