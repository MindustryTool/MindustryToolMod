package mindustrytool.plugins.browser;

public class UserData {
    private String id;
    private String name;
    private String imageUrl;

    public String id() { return id; }
    public UserData id(String id) { this.id = id; return this; }

    public String name() { return name; }
    public UserData name(String name) { this.name = name; return this; }

    public String imageUrl() { return imageUrl; }
    public UserData imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
}
