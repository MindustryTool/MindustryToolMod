package mindustrytool.dto;

import arc.struct.Seq;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true, fluent = true)
public class SchematicDetailData {
    String id;
    String itemId;
    String createdBy;
    String name;
    String description;
    int width;
    int height;
    Long likes;
    Long downloads = 0l;
    Long comments = 0l;
    Seq<TagData> tags;
    SchematicMetadata meta;

    @Data
    @Accessors(chain = true, fluent = true)
    public static class SchematicMetadata {
        Seq<SchematicRequirement> requirements;
    }

    @Data
    @Accessors(chain = true, fluent = true)
    public static class SchematicRequirement {
        String name;
        String color;
        Integer amount;
    }
}
