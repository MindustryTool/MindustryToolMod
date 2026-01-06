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
        String url = imageUrl != null ? imageUrl : (avatarUrl != null ? avatarUrl : avatar);
        if (url != null && !url.isEmpty() && !url.startsWith("http")) {
            // Relative URL, prefix with IMAGE_URL
            if (url.startsWith("/"))
                url = url.substring(1);
            return Config.IMAGE_URL + url;
        }
        return url;
    }

    public UserData imageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        return this;
    }
}
