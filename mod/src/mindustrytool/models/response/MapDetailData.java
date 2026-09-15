package mindustrytool.models.response;

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
	Long downloads = 0L;
	Long comments = 0L;
}
