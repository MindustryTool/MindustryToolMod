package mindustrytool.features.playerconnect;

public class PcRoomOpened {
    public final String roomName;

    public PcRoomOpened(String roomName) {
        this.roomName = roomName != null ? roomName : "";
    }
}
