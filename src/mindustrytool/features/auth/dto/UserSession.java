package mindustrytool.features.auth.dto;

import lombok.Data;

@Data
public class UserSession {
    private String id;
    private String name;
    private String imageUrl;

    public UserSession() {
    }

    public String name() {
        return name;
    }

    public String imageUrl() {
        return imageUrl;
    }
}
