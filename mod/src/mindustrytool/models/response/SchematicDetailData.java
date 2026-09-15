package mindustrytool.models.response;

import java.util.List;
import lombok.Data;

@Data
public class SchematicDetailData {
	String id;
	String itemId;
	String createdBy;
	String name;
	String description;
	int width;
	int height;
	Long likes = 0L;
	Long downloads = 0L;
	Long comments = 0L;
	List<TagData> tags;
	SchematicMetadata meta;

	@Data
	public static class SchematicMetadata {
		List<SchematicRequirement> requirements;
	}

	@Data
	public static class SchematicRequirement {
		String name;
		String color;
		Integer amount;
	}
}
