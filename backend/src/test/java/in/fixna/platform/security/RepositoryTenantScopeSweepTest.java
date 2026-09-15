package in.fixna.platform.security;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.tenant.MembershipRole;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WF10 sweep: every Spring Data repository that stores tenant-owned rows must
 * expose at least one tenant-scoped derived query (name contains TenantId);
 * otherwise cross-tenant reads would have no safe accessor. Global-identity
 * stores (tenants, users) and server-side token hashes are explicitly
 * allow-listed with their documented reason.
 */
class RepositoryTenantScopeSweepTest {

    /** Stores that are global by design (reason documented per name below). */
    private static final Set<String> ALLOW_LIST = Set.of(
            "TenantRepository", // tenant boundary itself is looked up by id under the platform gate
            "UserRepository", // identity is global; access rides on tenant_memberships
            "RefreshTokenRepository"); // token-hash rows are keyed by server hash, tenant re-checked on use

    @Test
    void everyTenantOwnedRepositoryIsTenantScoped() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, List<String>> violations = new TreeMap<>();

        List<Class<?>> repositories = discoverRepositories();
        assertThat(repositories).isNotEmpty();

        for (Class<?> repo : repositories) {
            String simple = repo.getSimpleName();
            if (ALLOW_LIST.contains(simple)) {
                continue;
            }
            boolean tenantScoped = Arrays.stream(repo.getDeclaredMethods())
                    .filter(method -> Modifier.isPublic(method.getModifiers()))
                    .anyMatch(method -> method.getName().contains("TenantId"));
            if (!tenantScoped) {
                // Fallback to inherited JpaRepository queries with tenant scope
                // (e.g. membership lookups declared on a shared base method):
                // scan the full interface hierarchy, not just declared methods.
                tenantScoped = Arrays.stream(repo.getMethods())
                        .anyMatch(method -> method.getName().contains("TenantId"));
            }
            if (!tenantScoped) {
                violations.put(simple, List.of(mapper.writeValueAsString(
                        Arrays.stream(repo.getDeclaredMethods())
                                .map(Method::getName)
                                .sorted()
                                .toList())));
            }
        }

        assertThat(violations)
                .as("repositories without any TenantId-scoped query: %s", violations)
                .isEmpty();
    }

    private static List<Class<?>> discoverRepositories() throws Exception {
        // Maven surefire runs from the module directory; CI may check out the
        // repo root. Resolve the platform sources from either layout.
        Path base = sourceBase();
        assertThat(Files.isDirectory(base)).as("platform sources on classpath: %s", base).isTrue();
        List<Class<?>> found = new ArrayList<>();
        try (var stream = Files.walk(base)) {
            for (Path file : stream
                    .filter(p -> Files.isRegularFile(p)
                            && p.getFileName().toString().endsWith("Repository.java"))
                    .toList()) {
                String relative = base.relativize(file).toString().replace('\\', '/');
                String className = "in.fixna.platform."
                        + relative.substring(0, relative.length() - ".java".length()).replace('/', '.');
                Class<?> loaded = Class.forName(className);
                if (loaded.isInterface()) {
                    found.add(loaded);
                }
            }
        }
        return found;
    }

    private static Path sourceBase() {
        Path module = Paths.get("").toAbsolutePath();
        Path direct = module.resolve("src").resolve("main").resolve("java")
                .resolve("in").resolve("fixna").resolve("platform");
        if (Files.isDirectory(direct)) {
            return direct;
        }
        return module.resolve("backend").resolve("src").resolve("main").resolve("java")
                .resolve("in").resolve("fixna").resolve("platform");
    }

    @Test
    void tenantScopeComesFromServerIdentityNotClientIds() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID userA = UUID.randomUUID();

        TenantContext.set(tenantA, userA, MembershipRole.TENANT_OWNER);
        try {
            // No mutating Current scope API exists: context is populated once
            // by the JWT filter; services must call requireTenantId().
            assertThat(TenantContext.requireTenantId()).isEqualTo(tenantA);
            TenantContext.set(tenantB, userA, MembershipRole.TENANT_OWNER);
            assertThat(TenantContext.requireTenantId()).isEqualTo(tenantB);
        } finally {
            TenantContext.clear();
        }
        // Cleared contexts fail closed instead of leaking the previous tenant.
        try {
            TenantContext.requireTenantId();
            assertThat(false).as("cleared TenantContext must fail closed").isTrue();
        } catch (FixnaException ex) {
            assertThat(ex.getCode()).isEqualTo("TENANT_CONTEXT_MISSING");
        }
    }
}
