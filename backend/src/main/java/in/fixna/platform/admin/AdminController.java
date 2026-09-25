package in.fixna.platform.admin;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.admin.AdminService.TenantSummary;
import in.fixna.platform.tenant.dto.MembershipResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Platform admin endpoints (INTERNAL tenant only). Lists tenants and
 * memberships, and manages subscriptions. Tenant RBAC does not apply here —
 * access is gated by INTERNAL tenant membership resolved from the JWT.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "admin", description = "Platform administration (INTERNAL only)")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "List tenants (INTERNAL platform admins only)")
    @GetMapping("/tenants")
    public ResponseEntity<List<TenantSummary>> listTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.listTenants(page, size));
    }

    @Operation(summary = "List members of a tenant (INTERNAL only)")
    @GetMapping("/tenants/{tenantId}/members")
    public ResponseEntity<List<MembershipResponse>> listMembers(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(adminService.listMembers(tenantId));
    }

    @Operation(summary = "Set a tenant's plan (INTERNAL only; no payment provider)")
    @PutMapping("/tenants/{tenantId}/plan")
    public ResponseEntity<PlanView> updatePlan(
            @PathVariable UUID tenantId, @Valid @RequestBody UpdatePlanRequest request) {
        var subscription = adminService.updatePlan(tenantId, request.planCode());
        return ResponseEntity.ok(new PlanView(
                subscription.getTenantId(), subscription.getPlanCode(), subscription.getStatus()));
    }

    public record UpdatePlanRequest(@NotBlank String planCode) {}

    /** Plan view for admin updates — no payment details. */
    public record PlanView(UUID tenantId, String planCode, String status) {}

}

