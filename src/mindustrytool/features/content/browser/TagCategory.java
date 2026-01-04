package mindustrytool.features.content.browser;

import arc.struct.Seq;

public class TagCategory {
    public String id;
    public String name;
    public String color;
    public int position;
    public boolean duplicate;
    public String createdBy;
    public String updatedBy;
    public Seq<TagData> tags;

    public String id() { return id; }
    public TagCategory id(String id) { this.id = id; return this; }

    public String name() { return name; }
    public TagCategory name(String name) { this.name = name; return this; }

    public String color() { return color; }
    public TagCategory color(String color) { this.color = color; return this; }

    public int position() { return position; }
    public TagCategory position(int position) { this.position = position; return this; }

    public boolean duplicate() { return duplicate; }
    public TagCategory duplicate(boolean duplicate) { this.duplicate = duplicate; return this; }

    public String createdBy() { return createdBy; }
    public TagCategory createdBy(String createdBy) { this.createdBy = createdBy; return this; }

    public String updatedBy() { return updatedBy; }
    public TagCategory updatedBy(String updatedBy) { this.updatedBy = updatedBy; return this; }

    public Seq<TagData> tags() { return tags; }
    public TagCategory tags(Seq<TagData> tags) { this.tags = tags; return this; }
}
