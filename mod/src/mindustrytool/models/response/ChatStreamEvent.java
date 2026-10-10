package mindustrytool.models.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ChatStreamEvent.Message.class, name = "message"),
        @JsonSubTypes.Type(value = ChatStreamEvent.Delete.class, name = "delete")
})
public abstract class ChatStreamEvent {

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Message extends ChatStreamEvent {
        public ChatMessage data;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Delete extends ChatStreamEvent {
        public ChatDeleteEvent data;
    }
}
