package mindustrytool.features.content.browser;

import arc.struct.Seq;

public class TagData {
    private String id;
    private String name;
    private Integer position = 0;
    private String categoryId;
    private String icon;
    private String fullTag;
    private String color;
    private Integer count = 0;
    private Seq<String> planetIds;

    public String id() { return id; }
    public TagData id(String id) { this.id = id; return this; }

    public String name() { return name; }
    public TagData name(String name) { this.name = name; return this; }

    public Integer position() { return position; }
    public TagData position(Integer position) { this.position = position; return this; }

    public String categoryId() { return categoryId; }
    public TagData categoryId(String categoryId) { this.categoryId = categoryId; return this; }

    public String icon() { return icon; }
    public TagData icon(String icon) { this.icon = icon; return this; }

    public String fullTag() { return fullTag; }
    public TagData fullTag(String fullTag) { this.fullTag = fullTag; return this; }

    public String color() { return color; }
    public TagData color(String color) { this.color = color; return this; }

    public Integer count() { return count; }
    public TagData count(Integer count) { this.count = count; return this; }

    public Seq<String> planetIds() { return planetIds; }
    public TagData planetIds(Seq<String> planetIds) { this.planetIds = planetIds; return this; }
}
