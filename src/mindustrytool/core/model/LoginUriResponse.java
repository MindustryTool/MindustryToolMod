package mindustrytool.core.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true, fluent = true)
public class LoginUriResponse {
    private String loginUrl;
    private String loginId;
}
