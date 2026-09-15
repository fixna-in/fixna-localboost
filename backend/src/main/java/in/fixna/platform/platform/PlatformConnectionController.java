package in.fixna.platform.platform;

import java.util.List;

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

import in.fixna.platform.platform.dto.PlatformConnectionRequest;
import in.fixna.platform.platform.dto.PlatformConnectionResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Platform connection endpoints. Tenant scope from the JWT; token values are
 * never accepted from or returned to clients.
 */
@RestController
@RequestMapping("/api/v1/platform-connections")
@Tag(name = "platform-connections", description = "Per-tenant advertising platform connections")
public class PlatformConnectionController {

    private final PlatformConnectionService connectionService;

    public PlatformConnectionController(PlatformConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @Operation(summary = "List current tenant's platform connections")
    @GetMapping
    public ResponseEntity<List<PlatformConnectionResponse>> list() {
        return ResponseEntity.ok(connectionService.list());
    }

    @Operation(summary = "Connect a platform (mock mode: no credentials)")
    @PostMapping
    public ResponseEntity<PlatformConnectionResponse> connect(
            @Valid @RequestBody PlatformConnectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(connectionService.connect(request));
    }

    @Operation(summary = "Disconnect a platform (clears token columns)")
    @DeleteMapping("/{platform}")
    public ResponseEntity<Void> disconnect(@PathVariable String platform) {
        connectionService.disconnect(platform);
        return ResponseEntity.noContent().build();
    }
}
