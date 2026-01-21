package mindustrytool.features.playerconnect;

public class RoomCreatedEvent {
    private NetworkProxy room;

    public RoomCreatedEvent(NetworkProxy room) {
        this.room = room;
    }

    public NetworkProxy getRoom() {
        return room;
    }
}
