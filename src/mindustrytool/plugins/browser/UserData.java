package mindustrytool.plugins.browser;

public class UserData {
    public String id;
    public String name;
    public String username;
    public String imageUrl;
    public String avatar;
    public String avatarUrl;

    public String id() {
        return id;
    }

    public UserData id(String id) {
        this.id = id;
        return this;
    }

    public String name() {
        return name != null ? name : username;
    }

    public UserData name(String name) {
        this.name = name;
        return this;
    }

    public String imageUrl() {
        return imageUrl != null ? imageUrl : (avatarUrl != null ? avatarUrl : avatar);
    }

    public UserData imageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        return this;
    }
}
