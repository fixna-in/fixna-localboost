package in.fixna.platform.common.audit;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Audit log viewer (WF09). The current-tenant page is tenant-scoped from the
 * JWT; the admin variant (INTERNAL tenant) accepts an explicit tenant filter.
 * Metadata is returned as an opaque JSON string — never secrets.
 */
@RestController
@Tag(name = "audit", description = "Tenant-scoped and platform-admin audit events")
public class AuditController {

    private final AuditViewerService viewer;

    public AuditController(AuditViewerService viewer) {
        this.viewer = viewer;
    }

    @Operation(summary = "Current tenant audit events (newest first)")
    @GetMapping("/api/v1/audit")
    public ResponseEntity<AuditPage> current(
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(AuditPage.of(viewer.listForCurrentTenant(action, page, size)));
    }

    @Operation(summary = "Any tenant's audit events (INTERNAL platform admins only)")
    @GetMapping("/api/v1/admin/audit")
    public ResponseEntity<AuditPage> forTenant(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(AuditPage.of(viewer.listForTenant(tenantId, action, page, size)));
    }

    /** Audited facts — no secrets, no internal stack details. */
    public record AuditEventView(
            UUID id, UUID tenantId, UUID userId, String action, String resourceType,
            UUID resourceId, String createdAt) {
        static AuditEventView from(AuditLog row) {
            return new AuditEventView(
                    row.getId(),
                    row.getTenantId(),
                    row.getUserId(),
                    row.getAction(),
                    row.getResourceType(),
                    row.getResourceId(),
                    row.getCreatedAt() == null ? null : row.getCreatedAt().toString());
        }
    }

    public record AuditPage(List<AuditEventView> items, int page, int size, long totalElements) {
        static AuditPage of(Page<AuditLog> source) {
            return new AuditPage(
                    source.getContent().stream().map(AuditEventView::from).toList(),
                    source.getNumber(),
                    source.getSize(),
                    source.getTotalElements());
        }
    }
}