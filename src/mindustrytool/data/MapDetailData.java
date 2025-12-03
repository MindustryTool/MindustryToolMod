package mindustrytool.data;

import arc.struct.Seq;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true, fluent = true)
public class MapDetailData {
    String id;
    String itemId;
    String createdBy;
    String name;
    String description;
    int width;
    int height;
    Seq<TagData> tags;
    Long likes;
    Long downloads = 0l;
    Long comments = 0l;
}
