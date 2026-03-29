package mindustrytool.features.chat.global.events;

public class UsersUpdateEvent {
    public final String channelId;

    public UsersUpdateEvent(String channelId) {
        this.channelId = channelId;
    }
}
