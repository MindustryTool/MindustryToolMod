package mindustrytool.models.response;

import java.util.List;
import lombok.Data;

@Data
public class ContentPatchDetailData {
    String id;
    String itemId;
    String name;
    String type;
    String description;
    String data;
    String verifierId;
    String verificationStatus;
    Long likes = 0L;
    Long downloads = 0L;
    Long comments = 0L;
    String createdBy;
    String updatedBy;
    String createdAt;
    String updatedAt;
    List<TagData> tags;
    List<ContentPatchData> children;
}
