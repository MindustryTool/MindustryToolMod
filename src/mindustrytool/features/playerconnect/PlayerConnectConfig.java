package mindustrytool.features.playerconnect;

import arc.Core;
import mindustry.Vars;

public class PlayerConnectConfig {
    private static final String PREFIX = "mindustrytool.playerConnect.";
    private static final String KEY_ROOM_NAME = PREFIX + "roomName";
    private static final String KEY_PASSWORD = PREFIX + "password";

    public static String getRoomName() {
        return Core.settings.getString(KEY_ROOM_NAME, Vars.player.name());
    }

    public static void setRoomName(String name) {
        Core.settings.put(KEY_ROOM_NAME, name);
    }

    public static String getPassword() {
        return Core.settings.getString(KEY_PASSWORD, "");
    }

    public static void setPassword(String password) {
        Core.settings.put(KEY_PASSWORD, password);
    }
}
