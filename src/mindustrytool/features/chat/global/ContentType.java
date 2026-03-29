package mindustrytool.features.chat.global;

public enum ContentType {
    TEXT("chats/text"),
    SCHEMATIC("chats/msch"),
    MAP("chats/msav");

    private final String endpoint;

    ContentType(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getEndpoint() {
        return endpoint;
    }
}
