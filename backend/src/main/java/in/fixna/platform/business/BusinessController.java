package in.fixna.platform.business;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.business.dto.BusinessLocationRequest;
import in.fixna.platform.business.dto.BusinessLocationResponse;
import in.fixna.platform.business.dto.BusinessRequest;
import in.fixna.platform.business.dto.BusinessResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Business + location endpoints. Controllers only translate HTTP —
 * tenant scope and RBAC live in {@link BusinessService}.
 */
@RestController
@RequestMapping("/api/v1/businesses")
@Tag(name = "businesses", description = "Tenant-scoped businesses and locations")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @Operation(summary = "List businesses of the current tenant")
    @GetMapping
    public ResponseEntity<List<BusinessResponse>> list() {
        return ResponseEntity.ok(businessService.list());
    }

    @Operation(summary = "Create business (US-004)")
    @PostMapping
    public ResponseEntity<BusinessResponse> create(@Valid @RequestBody BusinessRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(businessService.create(request));
    }

    @Operation(summary = "Get business by id (tenant-scoped)")
    @GetMapping("/{id}")
    public ResponseEntity<BusinessResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(businessService.get(id));
    }

    @Operation(summary = "Update business")
    @PutMapping("/{id}")
    public ResponseEntity<BusinessResponse> update(
            @PathVariable UUID id, @Valid @RequestBody BusinessRequest request) {
        return ResponseEntity.ok(businessService.update(id, request));
    }

    @Operation(summary = "Delete business")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        businessService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List locations of a business (US-005)")
    @GetMapping("/{id}/locations")
    public ResponseEntity<List<BusinessLocationResponse>> listLocations(@PathVariable UUID id) {
        return ResponseEntity.ok(businessService.listLocations(id));
    }

    @Operation(summary = "Add location to a business (US-005)")
    @PostMapping("/{id}/locations")
    public ResponseEntity<BusinessLocationResponse> addLocation(
            @PathVariable UUID id, @Valid @RequestBody BusinessLocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(businessService.addLocation(id, request));
    }
}
