package mindustrytool.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true, fluent = true)
public class MapData {
    String id;
    String itemId;
    String name;
    Long likes;
    Long downloads = 0l;
    Long comments = 0l;
}
