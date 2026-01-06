package mindustrytool.plugins.browser;

public class ContentData {
    private String id;
    private String itemId;
    private String name;
    private Long likes;
    private Long downloads = 0L;
    private Long comments = 0L;

    public String id() { return id; }
    public ContentData id(String id) { this.id = id; return this; }

    public String itemId() { return itemId; }
    public ContentData itemId(String itemId) { this.itemId = itemId; return this; }

    public String name() { return name; }
    public ContentData name(String name) { this.name = name; return this; }

    public Long likes() { return likes; }
    public ContentData likes(Long likes) { this.likes = likes; return this; }

    public Long downloads() { return downloads; }
    public ContentData downloads(Long downloads) { this.downloads = downloads; return this; }

    public Long comments() { return comments; }
    public ContentData comments(Long comments) { this.comments = comments; return this; }
}
