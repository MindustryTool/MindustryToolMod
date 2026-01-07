package mindustrytool.dto;

import arc.struct.Seq;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true, fluent = true)
public class TagCategory {
    private String id;
    private String name;
    private String color;
    private int position;
    private boolean duplicate;
    private String createdBy;
    private String updatedBy;
    private Seq<TagData> tags;
}
