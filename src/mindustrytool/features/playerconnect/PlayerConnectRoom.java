package mindustrytool.features.playerconnect;

import arc.struct.Seq;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true, fluent = true)
public class PlayerConnectRoom {
    private String roomId;
    private String link;
    private PlayerConnectRoomData data;

    @Data
    @Accessors(chain = true, fluent = true)
    public static class PlayerConnectRoomData {
        private String name;
        private String status;
        private boolean isPrivate;
        private boolean isSecured;
        private Seq<PlayerConnectRoomPlayer> players;
        private String mapName;
        private String gamemode;
        private Seq<String> mods;
        private String version;
        private String locale;
    }

    @Data
    @Accessors(chain = true, fluent = true)
    public static class PlayerConnectRoomPlayer {
        private String name;
        private String locale;
    }
}
