package mindustrytool.features.social.multiplayer;

import arc.func.Cons;
import playerconnect.shared.Packets;

/** Main facade for Player Connect functionality. */
public class PlayerConnect {
    
    public static void init() {
        StatsUpdater.init();
    }
    
    public static boolean isRoomClosed() { return RoomManager.isRoomClosed(); }
    
    public static void createRoom(String ip, int port, String pwd, Cons<PlayerConnectLink> onOk, Cons<Throwable> onFail, Cons<Packets.RoomClosedPacket.CloseReason> onDc) {
        RoomManager.create(ip, port, pwd, onOk, onFail, onDc);
    }
    
    public static void closeRoom() { RoomManager.close(); }
    
    public static void disposeRoom() { RoomManager.dispose(); }
    
    public static void joinRoom(PlayerConnectLink link, String pwd, Runnable onOk) {
        RoomJoiner.join(link, pwd, onOk);
    }
    
    public static void pingHost(String ip, int port, Cons<Long> onOk, Cons<Exception> onFail) {
        PingManager.ping(ip, port, onOk, onFail);
    }
    
    public static void disposePinger() { PingManager.dispose(); }
    
    public static Packets.RoomStats getRoomStats() { return StatsUpdater.getStats(); }
}
