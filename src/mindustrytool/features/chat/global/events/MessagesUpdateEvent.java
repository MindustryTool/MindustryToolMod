package mindustrytool.features.chat.global.events;

public class MessagesUpdateEvent {
    public final String channelId;
    public final boolean isPrepend;

    public MessagesUpdateEvent(String channelId, boolean isPrepend) {
        this.channelId = channelId;
        this.isPrepend = isPrepend;
    }
}
