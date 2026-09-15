package mindustrytool.models.response;

import lombok.Data;

@Data
public class AuthTokenResponse {
	private String accessToken;
	private String refreshToken;
}
