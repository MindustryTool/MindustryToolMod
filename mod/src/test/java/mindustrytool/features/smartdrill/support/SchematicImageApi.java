package mindustrytool.features.smartdrill.support;

import arc.util.Nullable;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import mindustrytool.Config;
import mindustrytool.services.Request;
import mindustrytool.services.Request.BodyHandlers;
import mindustrytool.services.Request.Response;
import mindustrytool.utils.JsonUtils;

public final class SchematicImageApi {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private static final Request REQUEST = Request.builder()
            .baseUrl(Config.API_URL)
            .timeout(TIMEOUT)
            .noRetry()
            .build();

    private static boolean unavailable;

    private SchematicImageApi() {
    }

    public static synchronized @Nullable byte[] render(@Nullable String schematicBase64) {
        if (unavailable || schematicBase64 == null || schematicBase64.isEmpty()) {
            return null;
        }
        try {
            Map<String, String> payload = new LinkedHashMap<>();
            payload.put("data", schematicBase64);
            Response<byte[]> response = REQUEST.post("/schematics/image")
                    .json(JsonUtils.toJson(payload))
                    .sendAsync(BodyHandlers.ofByteArray())
                    .get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                unavailable = true;
                return null;
            }
            byte[] body = response.body();
            if (body == null || body.length == 0) {
                unavailable = true;
                return null;
            }
            return body;
        } catch (Throwable failure) {
            unavailable = true;
            return null;
        }
    }

    public static boolean isUnavailable() {
        return unavailable;
    }
}