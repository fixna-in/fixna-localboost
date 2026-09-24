package in.fixna.platform.common.web;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies request-id propagation and the standard error envelope shape. */
class RequestIdAndErrorTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final RequestIdFilter filter = RequestIdFilter.forTests(null);

    @Test
    void exceptionMapsToEnvelope() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/businesses");
        FixnaException ex = new FixnaException(
                "NOT_FOUND",
                org.springframework.http.HttpStatus.NOT_FOUND,
                "Business not found");

        var response = handler.handleFixna(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("Business not found");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/businesses");
    }

    @Test
    void filterMintsRequestIdWhenAbsent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        org.springframework.mock.web.MockHttpServletResponse response =
                new org.springframework.mock.web.MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                (req, res) -> assertThat(req.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE))
                        .isNotNull());

        String header = response.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
        assertThat(header).isNotNull();
        assertThat(UUID.fromString(header)).isNotNull();
    }
}
