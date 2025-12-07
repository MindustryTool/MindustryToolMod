package mindustrytool.core.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true, fluent = true)
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
}
