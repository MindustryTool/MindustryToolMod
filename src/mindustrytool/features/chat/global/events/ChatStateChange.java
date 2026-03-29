package mindustrytool.features.chat.global.events;

public class ChatStateChange {
    public final boolean connected;

    public ChatStateChange(boolean connected) {
        this.connected = connected;
    }
}
