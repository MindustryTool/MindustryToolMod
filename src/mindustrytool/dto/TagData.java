package mindustrytool.dto;

import java.util.List;

import arc.graphics.Color;
import lombok.Data;

@Data
public class TagData {
    private String id;
    private String name;
    private Integer position = 0;
    private String categoryId;
    private String icon;
    private String fullTag;
    private String color;
    private Integer count = 0;
    private List<String> planetIds;

    public Color color() {
        try {
            return Color.valueOf(color);
        } catch (Exception ex) {
            return Color.white;
        }
    }
}
