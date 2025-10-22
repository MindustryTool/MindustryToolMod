package mindustrytool.data;

import arc.struct.Seq;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true, fluent = true)
public class TagCategory {
    public String id;
    public String name;
    public String color;
    public int position;
    public boolean duplicate;
    public String createdBy;
    public String updatedBy;
    public Seq<TagData> tags;
}
