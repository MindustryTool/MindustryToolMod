package mindustrytool.features.playerconnect;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PlayerConnectRoom {
    private String roomId;
    private String link;
    private String name;
    private String address;
    private PlayerConnectRoomData data;

    @Data
    public static class PlayerConnectRoomData {
        private String name;
        private String status;
        private List<PlayerConnectRoomPlayer> players;
        private String mapName;
        private String gamemode;
        private List<String> mods;
        private String version;
        private String locale;
        private String protocolVersion;
        
        @JsonProperty("isPrivate")
        private boolean isPrivate;
        @JsonProperty("isSecured")
        private boolean isSecured;
    }

    @Data
    public static class PlayerConnectRoomPlayer {
        private String name;
        private String locale;
    }
}
