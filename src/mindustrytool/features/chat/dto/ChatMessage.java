package mindustrytool.features.chat.dto;

public class ChatMessage {
    public String id;
    public String createdBy;
    public String createdAt;
    public String type;
    public String content;

    public ChatMessage() {
    }

    public ChatMessage(String id, String createdBy, String createdAt, String type, String content) {
        this.id = id;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.type = type;
        this.content = content;
    }
}
