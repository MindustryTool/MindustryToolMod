package mindustrytool.models.response;

import lombok.Data;

@Data
public class ModData {
	private String id;
	private String name;
	private String icon;
	private Integer position = 0;
}
