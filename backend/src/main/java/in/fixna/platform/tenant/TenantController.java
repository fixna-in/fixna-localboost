package in.fixna.platform.tenant;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.tenant.dto.MembershipResponse;
import in.fixna.platform.tenant.dto.TenantResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Tenant endpoints. Scope always derives from the JWT principal —
 * no tenant id is accepted from path, query or body.
 */
@RestController
@RequestMapping("/api/v1/tenants")
@Tag(name = "tenants", description = "Current tenant, creation and membership")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Operation(summary = "Get current tenant")
    @GetMapping("/current")
    public ResponseEntity<TenantResponse> current() {
        return ResponseEntity.ok(tenantService.current());
    }

    @Operation(summary = "Create tenant (caller becomes owner)")
    @PostMapping
    public ResponseEntity<TenantResponse> create(@Valid @RequestBody CreateTenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tenantService.create(request.name()));
    }

    @Operation(summary = "List current tenant members (admin-only)")
    @GetMapping("/current/members")
    public ResponseEntity<List<MembershipResponse>> members() {
        return ResponseEntity.ok(tenantService.members());
    }

    @Operation(summary = "Remove a member (admin-only)")
    @DeleteMapping("/current/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID userId) {
        tenantService.removeMember(userId);
        return ResponseEntity.noContent().build();
    }

    public record CreateTenantRequest(@jakarta.validation.constraints.NotBlank String name) {}
}
