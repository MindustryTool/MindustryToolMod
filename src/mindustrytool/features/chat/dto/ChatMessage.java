package mindustrytool.features.chat.dto;

import lombok.Data;

@Data
public class ChatMessage {
    public String id;
    public String createdBy;
    public String createdAt;
    public String type;
    public String content;
}
