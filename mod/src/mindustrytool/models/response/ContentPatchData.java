package mindustrytool.models.response;

import lombok.Data;

@Data
public class ContentPatchData {
    String id;
    String itemId;
    String name;
    String type;
    String description;
    Long likes = 0L;
    Long downloads = 0L;
    Long comments = 0L;
}
