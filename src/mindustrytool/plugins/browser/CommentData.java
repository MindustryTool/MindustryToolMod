package mindustrytool.plugins.browser;

public class CommentData {
    private String id;

    private String content;
    private long likes;
    private long time;
    private UserData user;

    public String id() {
        return id;
    }

    public String content() {
        return content;
    }

    public long likes() {
        return likes;
    }

    public long time() {
        return time;
    }

    public UserData user() {
        return user;
    }

    // Setter chain
    public CommentData id(String id) {
        this.id = id;
        return this;
    }

    public CommentData content(String content) {
        this.content = content;
        return this;
    }

    public CommentData user(UserData user) {
        this.user = user;
        return this;
    }
}
