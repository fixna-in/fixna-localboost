package in.fixna.platform.platform;

import java.util.List;

import org.springframework.util.MimeTypeUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import in.fixna.platform.platform.dto.PlatformConnectionResponse;

/**
 * JSON shape contracts shared between backend and Postman/fixna-api-client tests.
 */
public final class PlatformJsonShapes {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PlatformJsonShapes() {}

    public static final TypeReference<PlatformConnectionResponse> PLATFORM_CONNECTION_RESPONSE = new TypeReference<>() {};
    public static final TypeReference<List<PlatformConnectionResponse>> PLATFORM_CONNECTION_LIST = new TypeReference<>() {};

    public static final String MEDIA_TYPE = MimeTypeUtils.APPLICATION_JSON.toString();
}
