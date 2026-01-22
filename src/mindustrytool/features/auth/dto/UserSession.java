package mindustrytool.features.auth.dto;

import lombok.Data;

@Data
public class UserSession {
    String name;
    String imageUrl;

    public UserSession(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }

    public String name() {
        return name;
    }

    public String imageUrl() {
        return imageUrl;
    }
}
