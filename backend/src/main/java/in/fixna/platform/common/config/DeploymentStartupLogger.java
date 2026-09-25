package in.fixna.platform.common.config;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Logs active profile and warns when a cloud deploy is not using staging/prod. */
@Component
public class DeploymentStartupLogger implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DeploymentStartupLogger.class);

    private final Environment environment;

    public DeploymentStartupLogger(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());
        String appEnv = environment.getProperty("fixna.app-env", "unknown");
        LOG.info("Fixna startup profile={} fixna.app-env={} port={}",
                profiles.isEmpty() ? List.of("default") : profiles,
                appEnv,
                environment.getProperty("server.port", "8080"));
        if (profiles.isEmpty() || profiles.contains("default")) {
            LOG.warn("No Spring profile active — set SPRING_PROFILES_ACTIVE=staging on Render/Vercel hosts");
        }
        if ("demo".equals(appEnv) && profiles.stream().noneMatch(p -> p.equals("staging") || p.equals("prod"))) {
            LOG.warn("Shared demo should use SPRING_PROFILES_ACTIVE=staging (disables Redis + dev defaults)");
        }
    }
}
