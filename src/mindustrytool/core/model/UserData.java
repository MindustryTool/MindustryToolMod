package mindustrytool.core.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true, fluent = true)
public class UserData {
    private String id;
    private String name;
    private String imageUrl;
}
