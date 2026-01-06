package mindustrytool.plugins.playerconnect;

import arc.struct.Seq;

/** Represents a Player Connect room with its data. */
public class PlayerConnectRoom {
    private String roomId;
    private String link;
    private PlayerConnectRoomData data;

    public String roomId() { return roomId; }
    public PlayerConnectRoom roomId(String roomId) { this.roomId = roomId; return this; }

    public String link() { return link; }
    public PlayerConnectRoom link(String link) { this.link = link; return this; }

    public PlayerConnectRoomData data() { return data; }
    public PlayerConnectRoom data(PlayerConnectRoomData data) { this.data = data; return this; }

    /** Room data containing details like name, players, map, etc. */
    public static class PlayerConnectRoomData {
        private String name;
        private String status;
        private boolean isPrivate;
        private boolean isSecured;
        private Seq<PlayerConnectRoomPlayer> players = new Seq<>();
        private String mapName;
        private String gamemode;
        private Seq<String> mods = new Seq<>();
        private String version;
        private String locale;

        public String name() { return name; }
        public PlayerConnectRoomData name(String name) { this.name = name; return this; }

        public String status() { return status; }
        public PlayerConnectRoomData status(String status) { this.status = status; return this; }

        public boolean isPrivate() { return isPrivate; }
        public PlayerConnectRoomData isPrivate(boolean isPrivate) { this.isPrivate = isPrivate; return this; }

        public boolean isSecured() { return isSecured; }
        public PlayerConnectRoomData isSecured(boolean isSecured) { this.isSecured = isSecured; return this; }

        public Seq<PlayerConnectRoomPlayer> players() { return players; }
        public PlayerConnectRoomData players(Seq<PlayerConnectRoomPlayer> players) { this.players = players; return this; }

        public String mapName() { return mapName; }
        public PlayerConnectRoomData mapName(String mapName) { this.mapName = mapName; return this; }

        public String gamemode() { return gamemode; }
        public PlayerConnectRoomData gamemode(String gamemode) { this.gamemode = gamemode; return this; }

        public Seq<String> mods() { return mods; }
        public PlayerConnectRoomData mods(Seq<String> mods) { this.mods = mods; return this; }

        public String version() { return version; }
        public PlayerConnectRoomData version(String version) { this.version = version; return this; }

        public String locale() { return locale; }
        public PlayerConnectRoomData locale(String locale) { this.locale = locale; return this; }
    }

    /** Player information within a room. */
    public static class PlayerConnectRoomPlayer {
        private String name;
        private String locale;

        public String name() { return name; }
        public PlayerConnectRoomPlayer name(String name) { this.name = name; return this; }

        public String locale() { return locale; }
        public PlayerConnectRoomPlayer locale(String locale) { this.locale = locale; return this; }
    }
}
