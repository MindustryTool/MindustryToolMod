package mindustrytool.dto;

import java.util.List;

import lombok.Data;

@Data
public class SchematicDetailData {
    String id;
    String itemId;
    String createdBy;
    String name;
    String description;
    int width;
    int height;
    Long likes = 0l;
    Long downloads = 0l;
    Long comments = 0l;
    List<TagData> tags;
    SchematicMetadata meta;

    @Data
    public static class SchematicMetadata {
        List<SchematicRequirement> requirements;
    }

    @Data
    public static class SchematicRequirement {
        String name;
        String color;
        Integer amount;
    }
}
