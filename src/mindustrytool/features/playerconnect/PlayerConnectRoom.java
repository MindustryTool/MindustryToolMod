package mindustrytool.features.playerconnect;

import java.util.List;

import lombok.Data;

@Data
public class PlayerConnectRoom {
    private String roomId;
    private String link;
    private PlayerConnectRoomData data;

    @Data
    public static class PlayerConnectRoomData {
        private String name;
        private String status;
        private boolean isPrivate;
        private boolean isSecured;
        private List<PlayerConnectRoomPlayer> players;
        private String mapName;
        private String gamemode;
        private List<String> mods;
        private String version;
        private String locale;
        private String protocolVersion;
    }

    @Data
    public static class PlayerConnectRoomPlayer {
        private String name;
        private String locale;
    }
}
