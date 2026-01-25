package mindustrytool.dto;

import java.util.List;

import arc.graphics.Color;
import lombok.Data;

@Data
public class TagCategory {
    private String id;
    private String name;
    private String color;
    private int position;
    private boolean duplicate;
    private String createdBy;
    private String updatedBy;
    private List<TagData> tags;

    public Color color() {
        try {
            return Color.valueOf(color);
        } catch (Exception ex) {
            return Color.white;
        }
    }
}
