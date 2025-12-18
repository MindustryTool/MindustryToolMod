package mindustrytool.plugins.browser;

import arc.struct.Seq;

public class MapDetailData {
    private String id;
    private String itemId;
    private String createdBy;
    private String name;
    private String description;
    private int width;
    private int height;
    private Seq<TagData> tags;
    private Long likes;
    private Long downloads = 0L;
    private Long comments = 0L;
    private String createdAt;
    private String status;
    private String verifiedBy;

    public String id() {
        return id;
    }

    public MapDetailData id(String id) {
        this.id = id;
        return this;
    }

    public String itemId() {
        return itemId;
    }

    public MapDetailData itemId(String itemId) {
        this.itemId = itemId;
        return this;
    }

    public String createdBy() {
        return createdBy;
    }

    public MapDetailData createdBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public String name() {
        return name;
    }

    public MapDetailData name(String name) {
        this.name = name;
        return this;
    }

    public String description() {
        return description;
    }

    public MapDetailData description(String description) {
        this.description = description;
        return this;
    }

    public int width() {
        return width;
    }

    public MapDetailData width(int width) {
        this.width = width;
        return this;
    }

    public int height() {
        return height;
    }

    public MapDetailData height(int height) {
        this.height = height;
        return this;
    }

    public Seq<TagData> tags() {
        return tags;
    }

    public MapDetailData tags(Seq<TagData> tags) {
        this.tags = tags;
        return this;
    }

    public Long likes() {
        return likes;
    }

    public MapDetailData likes(Long likes) {
        this.likes = likes;
        return this;
    }

    public Long downloads() {
        return downloads;
    }

    public MapDetailData downloads(Long downloads) {
        this.downloads = downloads;
        return this;
    }

    public Long comments() {
        return comments;
    }

    public MapDetailData comments(Long comments) {
        this.comments = comments;
        return this;
    }

    public String createdAt() {
        return createdAt;
    }

    public MapDetailData createdAt(String createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public String status() {
        return status;
    }

    public MapDetailData status(String status) {
        this.status = status;
        return this;
    }

    public String verifiedBy() {
        return verifiedBy;
    }

    public MapDetailData verifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
        return this;
    }
}
