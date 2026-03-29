package mindustrytool.features.chat.global.events;

public class CurrentChannelChangeEvent {
    public final String channelId;

    public CurrentChannelChangeEvent(String channelId) {
        this.channelId = channelId;
    }
}
