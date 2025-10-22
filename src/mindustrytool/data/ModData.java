package mindustrytool.data;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true, fluent = true)
public class ModData {
    private String id;
    private String name;
    private String icon;
    private Integer position = 0;
}
