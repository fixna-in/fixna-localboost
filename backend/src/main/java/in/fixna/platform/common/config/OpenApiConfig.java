package in.fixna.platform.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Base OpenAPI document. Paths are contributed by controllers via
 * springdoc annotations once feature workflows land; this keeps the
 * contract-first rule (every visible API change updates the docs).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fixnaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fixna LocalBoost API")
                        .version("v1")
                        .description(
                                "Multi-tenant local advertising orchestration API. "
                                        + "Base path /api/v1. Tenant scope is resolved from the "
                                        + "authenticated identity, never from client parameters."));
    }
}
