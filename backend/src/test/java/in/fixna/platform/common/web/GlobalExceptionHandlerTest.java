package in.fixna.platform.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Error-envelope contracts: known codes map 1:1; unexpected Throwable renders
 * a generic 500 with no exception message, stack, or query string.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static MockHttpServletRequest requestWithSecretQuery() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/campaigns");
        request.setQueryString("token=abc&next=/secret");
        return request;
    }

    @Test
    void fixnaExceptionsMapStatusAndCode() {
        ResponseEntity<ApiError> response = handler.handleFixna(
                new FixnaException(
                        "PLAN_BUSINESS_LIMIT",
                        org.springframework.http.HttpStatus.FORBIDDEN,
                        "Plan allows 1 businesses"),
                new MockHttpServletRequest("POST", "/api/v1/businesses"));

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("PLAN_BUSINESS_LIMIT");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/businesses");
        assertThat(response.getBody().message()).contains("Plan allows");
    }

    @Test
    void unexpectedErrorsStayGenericWithoutQueryOrCause() {
        ResponseEntity<ApiError> response = handler.handleUnexpected(
                new IllegalStateException("SELECT * FROM refresh_tokens WHERE token_hash='x'"),
                requestWithSecretQuery());

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("INTERNAL_ERROR");
        assertThat(body.message()).isEqualTo("An unexpected error occurred");
        // Envelope path is URI-only: query (possible tokens/secrets) never echoed.
        assertThat(body.path()).isEqualTo("/api/v1/campaigns");
        assertThat(body.path()).doesNotContain("token");
    }
}