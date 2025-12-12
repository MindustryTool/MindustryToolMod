package mindustrytool.network;

import java.util.concurrent.ExecutorService;
import arc.func.Cons;
import arc.util.Threads;
import playerconnect.shared.Packets;

public class RoomManager {
    private static NetworkProxy room;
    private static Thread roomThread;
    private static ExecutorService worker = Threads.unboundedExecutor("Worker", 1);

    public static boolean isRoomClosed() { return room == null || !room.isConnected(); }

    public static void create(String ip, int port, String pwd, Cons<PlayerConnectLink> onOk, Cons<Throwable> onFail, Cons<Packets.RoomClosedPacket.CloseReason> onDc) {
        if (room == null || roomThread == null || !roomThread.isAlive()) {
            roomThread = Threads.daemon("Proxy", room = new NetworkProxy(pwd));
        }
        worker.submit(() -> {
            try {
                if (room.isConnected()) throw new IllegalStateException("Room is already created, please close it before.");
                room.connect(ip, port, id -> onOk.get(new PlayerConnectLink(ip, port, id)), onDc);
            } catch (Throwable e) { onFail.get(e); }
        });
    }

    public static void close() { if (room != null) room.closeRoom(); }

    public static void dispose() {
        if (room != null) {
            room.stop();
            try { roomThread.join(1000); } catch (Exception ignored) {}
            try { room.dispose(); } catch (Exception ignored) {}
            roomThread = null; room = null;
        }
    }

    public static NetworkProxy getRoom() { return room; }
}
