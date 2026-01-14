package mindustrytool.features.chat.dto;

public class ChatUser {
    String name;
    String imageUrl;

    public ChatUser(String name, String imageUrl) {
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
