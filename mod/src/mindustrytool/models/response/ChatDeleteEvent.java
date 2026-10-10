package mindustrytool.models.response;

import lombok.Data;

@Data
public class ChatDeleteEvent {
    public String id;
    public String channelId;
    public String lastMessageId;
}
