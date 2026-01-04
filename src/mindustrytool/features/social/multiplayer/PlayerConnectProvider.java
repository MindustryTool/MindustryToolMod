package mindustrytool.features.social.multiplayer;

/** Represents a Player Connect provider server. */
public class PlayerConnectProvider {
    private String id;
    private String name;
    private String address;

    public String id() { return id; }
    public PlayerConnectProvider id(String id) { this.id = id; return this; }

    public String name() { return name; }
    public PlayerConnectProvider name(String name) { this.name = name; return this; }

    public String address() { return address; }
    public PlayerConnectProvider address(String address) { this.address = address; return this; }
}
