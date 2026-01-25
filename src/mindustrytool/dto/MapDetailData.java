package mindustrytool.dto;

import java.util.List;

import lombok.Data;

@Data
public class MapDetailData {
    String id;
    String itemId;
    String createdBy;
    String name;
    String description;
    int width;
    int height;
    List<TagData> tags;
    Long likes;
    Long downloads = 0l;
    Long comments = 0l;
}
