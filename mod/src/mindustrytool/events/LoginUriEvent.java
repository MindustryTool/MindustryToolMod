package mindustrytool.events;

public class LoginUriEvent {
	public final String loginUrl;
	public final String loginId;

	public LoginUriEvent(String loginUrl, String loginId) {
		this.loginUrl = loginUrl;
		this.loginId = loginId;
	}
}
