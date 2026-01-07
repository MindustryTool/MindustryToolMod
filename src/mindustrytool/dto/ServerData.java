package mindustrytool.dto;

import arc.struct.Seq;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true, fluent = true)
public class ServerData {
    private String id;
    private String managerId;
    private String userId;
    private int port;
    private long ramUsage;
    private long totalRam;
    private long players;
    private String address;
    private String name;
    private String description;
    private String mode;
    private String mapName;
    private String gamemode;
    private String status = "DOWN";
    private boolean isOfficial;
    private boolean isAutoTurnOff = true;
    private boolean isHub = false;
    private byte[] mapImage;
    private Seq<String> mods = new Seq<>();

    public Seq<String> mods() {
        return mods.select(v -> !v.equals("mindustrytoolplugin"));
    }
}
