package mindustrytool.features.chat.global.dto;

import lombok.Data;

@Data
public class ChatMessage {
    public String id;
    public String createdBy;
    public String createdAt;
    public String content;
    public String replyTo;
    public String channelId;
}
