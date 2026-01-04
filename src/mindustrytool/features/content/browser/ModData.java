package mindustrytool.features.content.browser;

public class ModData {
    private String id;
    private String name;
    private String icon;
    private Integer position = 0;

    public String id() { return id; }
    public ModData id(String id) { this.id = id; return this; }

    public String name() { return name; }
    public ModData name(String name) { this.name = name; return this; }

    public String icon() { return icon; }
    public ModData icon(String icon) { this.icon = icon; return this; }

    public Integer position() { return position; }
    public ModData position(Integer position) { this.position = position; return this; }
}
