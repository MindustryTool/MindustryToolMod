package mindustrytool.events;

/**
 * Fired when the user's login state changes (login, logout, token refresh).
 * Use Events.on(LoginStateChangeEvent.class, e -> ...) to listen.
 */
public class LoginStateChangeEvent {
    public final boolean isLoggedIn;

    public LoginStateChangeEvent(boolean isLoggedIn) {
        this.isLoggedIn = isLoggedIn;
    }
}
