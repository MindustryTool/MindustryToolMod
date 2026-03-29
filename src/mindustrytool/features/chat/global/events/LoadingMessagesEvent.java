package mindustrytool.features.chat.global.events;

public class LoadingMessagesEvent {
    public final boolean isLoading;

    public LoadingMessagesEvent(boolean isLoading) {
        this.isLoading = isLoading;
    }
}
