package mindustrytool.dto;

import arc.struct.Seq;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true, fluent = true)
public class TagData {
	private String id;
	private String name;
	private Integer position = 0;
	private String categoryId;
	private String icon;
	private String fullTag;
	private String color;
    private Integer count = 0;
    private Seq<String> planetIds;
}
