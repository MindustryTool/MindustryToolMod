package mindustrytool.features.auth.dto;

public class SessionLoadEvent {
    public final UserSession user;
    public final Throwable error;
    public final boolean isLoading;

    public SessionLoadEvent(UserSession user, Throwable error, boolean isLoading) {
        this.user = user;
        this.error = error;
        this.isLoading = isLoading;
    }
}
