package in.fixna.platform.security;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.ai.AiRecommendationService;
import in.fixna.platform.platform.AdvertisingPlatformAdapter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Static guardrails for AGENTS.md / fixna-core non-negotiables. Fails the build
 * when new code violates tenant isolation, controller layering, AI autonomy
 * boundaries or platform adapter rules.
 */
class NonNegotiablesComplianceTest {

    private static final Set<String> CLIENT_TENANT_ID_ALLOW_LIST = Set.of(
            "AdminController",
            "AuditController");

    private static final Set<String> AI_FORBIDDEN_DEPENDENCY_SUFFIXES = Set.of(
            "CampaignLaunchService",
            "CampaignLaunchTx",
            "CampaignService",
            "PlatformConnectionService",
            "NotificationService",
            "BillingService",
            "SubscriptionService");

    private static final Set<String> AI_FORBIDDEN_DIRECT_ADAPTER_IMPORTS = Set.of(
            "MockGoogleAdsAdapter",
            "MockMetaAdsAdapter",
            "MockWhatsAppAdapter");

    @Test
    void controllersDoNotInjectRepositories() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Class<?> controller : discoverControllers()) {
            for (Field field : controller.getDeclaredFields()) {
                if (field.getType().getSimpleName().endsWith("Repository")) {
                    violations.add(controller.getSimpleName() + "#" + field.getName());
                }
            }
        }
        assertThat(violations)
                .as("controllers must delegate to services, not repositories: %s", violations)
                .isEmpty();
    }

    @Test
    void requestDtosDoNotAcceptClientTenantId() throws Exception {
        Path dtoRoot = platformSourceBase();
        List<String> violations = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(dtoRoot)) {
            for (Path file : walk
                    .filter(p -> p.getFileName().toString().endsWith("Request.java")
                            && p.toString().replace('\\', '/').contains("/dto/"))
                    .toList()) {
                String relative = dtoRoot.relativize(file).toString().replace('\\', '/');
                if (relative.contains("/admin/")) {
                    continue;
                }
                String source = Files.readString(file);
                if (source.matches("(?s).*\\btenantId\\b.*")
                        && !source.contains("Tenant scope always comes from the JWT")
                        && !source.contains("never from client")) {
                    violations.add(relative);
                }
            }
        }
        assertThat(violations)
                .as("request DTOs must not accept tenantId from clients: %s", violations)
                .isEmpty();
    }

    @Test
    void clientTenantIdParametersAreAdminOnlyAndDelegated() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Class<?> controller : discoverControllers()) {
            boolean allowsClientTenantId = CLIENT_TENANT_ID_ALLOW_LIST.contains(controller.getSimpleName());
            for (Method method : controller.getDeclaredMethods()) {
                boolean hasTenantParam = Arrays.stream(method.getParameters())
                        .anyMatch(p -> isTenantIdParameter(p));
                if (hasTenantParam && !allowsClientTenantId) {
                    violations.add(controller.getSimpleName() + "#" + method.getName());
                }
            }
        }
        assertThat(violations)
                .as("only platform-admin controllers may accept tenantId: %s", violations)
                .isEmpty();
    }

    @Test
    void aiModuleDoesNotDependOnExecutionServices() throws Exception {
        Path aiRoot = platformSourceBase().resolve("ai");
        List<String> violations = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(aiRoot)) {
            for (Path file : walk.filter(p -> p.getFileName().toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                for (String forbidden : AI_FORBIDDEN_DEPENDENCY_SUFFIXES) {
                    if (source.contains("import in.fixna.platform.") && source.contains("." + forbidden)) {
                        violations.add(aiRoot.relativize(file) + " -> " + forbidden);
                    }
                }
            }
        }
        assertThat(violations)
                .as("AI must stay advisory and cannot launch/spend/notify directly: %s", violations)
                .isEmpty();
    }

    @Test
    void platformAdaptersImplementAdvertisingPlatformAdapter() throws Exception {
        Path platformRoot = platformSourceBase().resolve("platform");
        List<String> violations = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(platformRoot)) {
            for (Path file : walk.filter(p -> p.getFileName().toString().endsWith("Adapter.java")).toList()) {
                Class<?> type = Class.forName(toClassName(platformSourceBase(), file));
                if (!AdvertisingPlatformAdapter.class.isAssignableFrom(type)) {
                    violations.add(type.getName());
                }
            }
        }
        assertThat(violations)
                .as("external providers must use AdvertisingPlatformAdapter: %s", violations)
                .isEmpty();
    }

    @Test
    void aiRecommendationServiceValidatesUntrustedOutput() throws Exception {
        Method recommend = AiRecommendationService.class.getDeclaredMethod(
                "recommend",
                in.fixna.platform.ai.RecommendationType.class,
                java.util.UUID.class,
                java.util.UUID.class,
                java.util.Map.class);
        String body = readMethodBody(platformSourceBase()
                .resolve("ai")
                .resolve("AiRecommendationService.java"), "recommend");
        assertThat(body).contains("schemaValidator.validate");
        assertThat(body).contains("businessValidator.validate");
        assertThat(body).contains("businessValidator.validateOutput");
        assertThat(body).contains("platformCompatibilityValidator.validate");
        assertThat(recommend.getReturnType().getSimpleName()).isEqualTo("RecommendationResult");
    }

    @Test
    void aiModuleUsesRegistryNotConcreteAdapters() throws Exception {
        Path aiRoot = platformSourceBase().resolve("ai");
        List<String> violations = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(aiRoot)) {
            for (Path file : walk.filter(p -> p.getFileName().toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                for (String forbidden : AI_FORBIDDEN_DIRECT_ADAPTER_IMPORTS) {
                    if (source.contains("import in.fixna.platform.platform." + forbidden)) {
                        violations.add(aiRoot.relativize(file) + " -> " + forbidden);
                    }
                }
            }
        }
        assertThat(violations)
                .as("AI must resolve platforms via PlatformAdapterRegistry only: %s", violations)
                .isEmpty();
    }

    @Test
    void aiControllersStayAdvisory() throws Exception {
        Path aiRoot = platformSourceBase().resolve("ai");
        List<String> violations = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(aiRoot)) {
            for (Path file : walk.filter(p -> p.getFileName().toString().endsWith("Controller.java")).toList()) {
                String source = Files.readString(file);
                for (String forbidden : AI_FORBIDDEN_DEPENDENCY_SUFFIXES) {
                    if (source.contains("." + forbidden)) {
                        violations.add(aiRoot.relativize(file) + " -> " + forbidden);
                    }
                }
            }
        }
        assertThat(violations)
                .as("AI controllers must not call execution services: %s", violations)
                .isEmpty();
    }

    @Test
    void productionMainCodeAvoidsSystemOut() throws Exception {
        Path mainRoot = platformSourceBase();
        List<String> violations = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(mainRoot)) {
            for (Path file : walk.filter(p -> p.getFileName().toString().endsWith(".java")).toList()) {
                if (file.getFileName().toString().equals("PasswordUtility.java")) {
                    continue;
                }
                String source = Files.readString(file);
                if (source.contains("System.out.print") || source.contains("System.err.print")) {
                    violations.add(mainRoot.relativize(file).toString());
                }
            }
        }
        assertThat(violations)
                .as("use SLF4J in production code, not System.out/err: %s", violations)
                .isEmpty();
    }

    private static boolean isTenantIdParameter(java.lang.reflect.Parameter parameter) {
        if (!"tenantId".equals(parameter.getName())) {
            return false;
        }
        return parameter.isAnnotationPresent(RequestParam.class)
                || parameter.isAnnotationPresent(PathVariable.class);
    }

    private static List<Class<?>> discoverControllers() throws Exception {
        Path base = platformSourceBase();
        List<Class<?>> found = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(base)) {
            for (Path file : walk.filter(p -> p.getFileName().toString().endsWith("Controller.java")).toList()) {
                Class<?> type = Class.forName(toClassName(base, file));
                if (type.isAnnotationPresent(RestController.class)) {
                    found.add(type);
                }
            }
        }
        return found;
    }

    private static Path platformSourceBase() {
        Path module = Paths.get("").toAbsolutePath();
        Path direct = module.resolve("src").resolve("main").resolve("java")
                .resolve("in").resolve("fixna").resolve("platform");
        if (Files.isDirectory(direct)) {
            return direct;
        }
        return module.resolve("backend").resolve("src").resolve("main").resolve("java")
                .resolve("in").resolve("fixna").resolve("platform");
    }

    private static String toClassName(Path base, Path file) {
        String relative = base.relativize(file).toString().replace('\\', '/');
        return "in.fixna.platform."
                + relative.substring(0, relative.length() - ".java".length()).replace('/', '.');
    }

    private static String readMethodBody(Path sourceFile, String methodName) throws Exception {
        List<String> lines = Files.readAllLines(sourceFile);
        int start = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains(methodName + "(") && !lines.get(i).trim().startsWith("//")) {
                start = i;
                break;
            }
        }
        assertThat(start).as("method %s in %s", methodName, sourceFile).isGreaterThanOrEqualTo(0);
        StringBuilder body = new StringBuilder();
        int braceDepth = 0;
        boolean started = false;
        for (int i = start; i < lines.size(); i++) {
            String line = lines.get(i);
            body.append(line).append('\n');
            for (int c = 0; c < line.length(); c++) {
                char ch = line.charAt(c);
                if (ch == '{') {
                    braceDepth++;
                    started = true;
                } else if (ch == '}') {
                    braceDepth--;
                    if (started && braceDepth == 0) {
                        return body.toString();
                    }
                }
            }
        }
        return body.toString();
    }

}
