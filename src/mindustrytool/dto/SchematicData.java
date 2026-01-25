package mindustrytool.dto;

import lombok.Data;

@Data
public class SchematicData {
    String id;
    String itemId;
    String name;
    Long likes = 0l;
    Long downloads = 0l;
    Long comments = 0l;
}
