package mindustrytool.features.chat.global.dto;

import arc.struct.ObjectMap;
import lombok.Data;

@Data
public class LastReadMessageStore {
    public ObjectMap<String, String> lastReadMessageIds = new ObjectMap<>();
}
