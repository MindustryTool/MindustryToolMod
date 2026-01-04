package mindustrytool.features.content.browser;

/**
 * Content type enumeration for browser dialogs.
 */
public enum ContentType {
    MAP("maps", "map.search", "@mdt.message.map-browser.title"),
    SCHEMATIC("schematics", "schematic.search", "Schematic Browser");

    public final String endpoint;
    public final String searchKey;
    public final String title;

    ContentType(String endpoint, String searchKey, String title) {
        this.endpoint = endpoint;
        this.searchKey = searchKey;
        this.title = title;
    }
}
