package mindustrytool.features.chat.global.dto;

public class ChatMessageReceive {
    public final ChatMessage[] messages;

    public ChatMessageReceive(ChatMessage[] messages) {
        this.messages = messages;
    }
}
