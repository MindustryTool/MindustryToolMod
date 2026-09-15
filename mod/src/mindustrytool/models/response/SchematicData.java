package mindustrytool.models.response;

import lombok.Data;

@Data
public class SchematicData {
	String id;
	String itemId;
	String name;
	Long likes = 0L;
	Long downloads = 0L;
	Long comments = 0L;
}
