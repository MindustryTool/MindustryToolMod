// Khai báo package cho module kết nối người chơi
package mindustrytool.playerconnect;

// Import ByteBuffer để làm việc với buffer nhị phân
import java.nio.ByteBuffer;
// Import Date để lấy thời gian hiện tại
import java.util.Date;
// Import ExecutorService để quản lý thread pool
import java.util.concurrent.ExecutorService;

// Import Core để chạy code trên UI thread
import arc.Core;
// Import Events để đăng ký và lắng nghe sự kiện
import arc.Events;
// Import Cons là functional interface nhận 1 tham số
import arc.func.Cons;
// Import Client cho network client
import arc.net.Client;
// Import Seq là collection của Arc
import arc.struct.Seq;
// Import Log để ghi log
import arc.util.Log;
// Import Threads để tạo và quản lý thread
import arc.util.Threads;
// Import Time để đo thời gian
import arc.util.Time;
// Import Timer để lên lịch chạy task định kỳ
import arc.util.Timer;
// Import Vars chứa các biến toàn cục của game
import mindustry.Vars;
// Import Version để lấy phiên bản game
import mindustry.core.Version;
// Import EventType chứa các loại sự kiện
import mindustry.game.EventType;
// Import PlayerJoin là sự kiện người chơi vào
import mindustry.game.EventType.PlayerJoin;
// Import PlayerLeave là sự kiện người chơi rời
import mindustry.game.EventType.PlayerLeave;
// Import WorldLoadEndEvent là sự kiện load world xong
import mindustry.game.EventType.WorldLoadEndEvent;
// Import Groups để truy cập danh sách entities
import mindustry.gen.Groups;
// Import Player để truy cập thông tin người chơi
import mindustry.gen.Player;
// Import Packets chứa các packet cho Player Connect
import playerconnect.shared.Packets;
// Import RoomPlayer đại diện cho thông tin người chơi trong phòng
import playerconnect.shared.Packets.RoomPlayer;

// Lớp chính quản lý chức năng Player Connect (CLaJ - Connect Like a Jedi)
public class PlayerConnect {
    // Khối static khởi tạo các event listener
    static {
        // Khó để biết khi nào người chơi thoát game, không có event...
        // Đăng ký listener khi pause dialog ẩn đi
        Vars.ui.paused.hidden(() -> {
            // Lên lịch kiểm tra sau 1 giây
            arc.util.Timer.schedule(() -> {
                // Nếu mạng không active hoặc đang ở menu thì đóng phòng
                if (!Vars.net.active() || Vars.state.isMenu())
                    closeRoom();
            }, 1f);
        });
        // Đóng phòng khi host game mới
        Events.run(EventType.HostEvent.class, PlayerConnect::closeRoom);
        // Đóng phòng khi bắt đầu kết nối đến server khác
        Events.run(EventType.ClientPreConnectEvent.class, PlayerConnect::closeRoom);
        // Dọn dẹp khi dispose game
        Events.run(EventType.DisposeEvent.class, () -> {
            // Dispose phòng
            disposeRoom();
            // Dispose pinger
            disposePinger();
        });

        // Cập nhật stats khi có người chơi vào
        Events.run(PlayerJoin.class, () -> {
            updateStats();
        });

        // Cập nhật stats khi có người chơi rời
        Events.run(PlayerLeave.class, () -> {
            updateStats();
        });

        // Cập nhật stats khi load world xong
        Events.run(WorldLoadEndEvent.class, () -> {
            updateStats();
        });

        // Cập nhật stats định kỳ mỗi 60 giây
        Timer.schedule(() -> {
            updateStats();
        }, 60f, 60f);
    }

    // Phương thức gửi cập nhật thống kê phòng lên server
    private static void updateStats() {
        // Chỉ chạy nếu đang là server
        if (!Vars.net.server()) {
            return;
        }

        // Chạy trên UI thread
        Core.app.post(() -> {
            try {
                // Tạo packet thống kê
                Packets.StatsPacket p = new Packets.StatsPacket();
                // Lấy thống kê phòng hiện tại
                Packets.RoomStats stats = PlayerConnect.getRoomStats();
                // Gán roomId
                p.roomId = room.roomId();
                // Gán dữ liệu thống kê
                p.data = stats;

                // Kiểm tra room đã tạo chưa
                if (room == null) {
                    Log.warn("Room not created yet");
                    return;
                }

                // Kiểm tra room đã kết nối chưa
                if (!room.isConnected()) {
                    Log.warn("Room not connected yet");
                    return;
                }

                // Log thông báo gửi cập nhật
                Log.info("Send room stats update");

                // Gửi packet qua TCP
                room.sendTCP(p);
            } catch (Throwable err) {
                // Log lỗi nếu có
                Log.err(err);
            }
        });
    }

    // NetworkProxy instance để quản lý phòng
    private static NetworkProxy room;
    // Client để ping server
    private static Client pinger;
    // Worker thread pool để chạy các task async
    private static ExecutorService worker = Threads.unboundedExecutor("CLaJ Worker", 1);
    // Serializer tạm để serialize packet thủ công
    private static arc.net.NetSerializer tmpSerializer;
    // Buffer tạm để ghi packet (16 bytes cho room join packet)
    private static ByteBuffer tmpBuffer = ByteBuffer.allocate(256);// chúng ta cần 16 bytes cho room join packet
    // Thread cho room và pinger
    private static Thread roomThread, pingerThread;

    // Kiểm tra phòng đã đóng chưa
    public static boolean isRoomClosed() {
        // Trả về true nếu room null hoặc không kết nối
        return room == null || !room.isConnected();
    }

    // Phương thức tạo phòng mới
    public static void createRoom(String ip, int port,
            String password,
            Cons<PlayerConnectLink> onSucceed,
            Cons<Throwable> onFailed,
            Cons<Packets.RoomClosedPacket.CloseReason> onDisconnected//
    ) {
        // Nếu room hoặc thread chưa tồn tại hoặc đã chết thì tạo mới
        if (room == null || roomThread == null || !roomThread.isAlive()) {
            // Tạo daemon thread để chạy NetworkProxy
            roomThread = Threads.daemon("CLaJ Proxy", room = new NetworkProxy(password));
        }

        // Submit task vào worker thread
        worker.submit(() -> {
            try {
                // Kiểm tra phòng đã được tạo chưa
                if (room.isConnected()) {
                    throw new IllegalStateException("Room is already created, please close it before.");
                }
                // Kết nối đến server và tạo phòng
                room.connect(ip, port, id -> onSucceed.get(new PlayerConnectLink(ip, port, id)), onDisconnected);
            } catch (Throwable e) {
                // Gọi callback lỗi
                onFailed.get(e);
            }
        });
    }

    /** Chỉ đóng kết nối phòng, không xóa phòng */
    // Phương thức đóng phòng
    public static void closeRoom() {
        // Nếu room không null thì đóng
        if (room != null)
            room.closeRoom();
    }

    /** Xóa phòng đúng cách */
    // Phương thức dispose phòng hoàn toàn
    public static void disposeRoom() {
        // Nếu room không null
        if (room != null) {
            // Dừng room
            room.stop();
            try {
                // Chờ thread kết thúc tối đa 1 giây
                roomThread.join(1000);
            } catch (Exception ignored) {
            }
            try {
                // Dispose room
                room.dispose();
            } catch (Exception ignored) {
            }
            // Xóa các reference
            roomThread = null;
            room = null;
        }
    }

    // Phương thức join vào phòng đã có
    public static void joinRoom(PlayerConnectLink link, String password, Runnable success) {
        // Nếu link null thì return
        if (link == null)
            return;

        // Reset logic và network
        Vars.logic.reset();
        Vars.net.reset();

        // Bắt đầu quá trình kết nối
        Vars.netClient.beginConnecting();
        // Kết nối đến server
        Vars.net.connect(link.host, link.port, () -> {
            // Kiểm tra có phải client không
            if (!Vars.net.client())
                return;

            // Khởi tạo serializer nếu chưa có
            if (tmpSerializer == null)
                tmpSerializer = new NetworkProxy.Serializer();

            // Cần serialize packet thủ công
            tmpBuffer.clear();
            // Tạo packet join phòng
            Packets.RoomJoinPacket p = new Packets.RoomJoinPacket();
            // Gán mật khẩu
            p.password = password;
            // Gán roomId
            p.roomId = link.roomId;
            // Serialize packet vào buffer
            tmpSerializer.write(tmpBuffer, p);
            // Đặt limit và position
            tmpBuffer.limit(tmpBuffer.position()).position(0);
            // Gửi packet
            Vars.net.send(tmpBuffer, true);

            // Gọi callback thành công
            success.run();
        });
    }

    /**
     * @apiNote async operation nhưng blocking các task mới nếu đang ping
     */
    // Phương thức ping đến host để đo latency
    public static void pingHost(String ip, int port, Cons<Long> success, Cons<Exception> onFailed) {
        // Khởi tạo serializer nếu chưa có
        if (tmpSerializer == null)
            tmpSerializer = new NetworkProxy.Serializer();
        // Tạo pinger nếu chưa có hoặc thread đã chết
        if (pinger == null || pingerThread == null || !pingerThread.isAlive())
            pingerThread = Threads.daemon("CLaJ Pinger", pinger = new Client(8192, 8192, tmpSerializer));

        // Submit task vào worker
        worker.submit(() -> {
            // Synchronized để chỉ ping 1 lần tại 1 thời điểm
            synchronized (pingerThread) {
                // Lấy thời gian bắt đầu
                long time = Time.millis();
                try {
                    // Kết nối thành công là đủ để đo ping
                    pinger.connect(2000, ip, port);
                    // Tính thời gian đã trôi qua
                    time = Time.timeSinceMillis(time);
                    // Đóng kết nối
                    pinger.close();
                    // Gọi callback thành công với thời gian ping
                    success.get(time);
                } catch (Exception e) {
                    // Gọi callback lỗi
                    onFailed.get(e);
                }
            }
        });
    }

    // Phương thức dispose pinger
    public static void disposePinger() {
        // Nếu pinger không null
        if (pinger != null) {
            // Dừng pinger
            pinger.stop();
            try {
                // Chờ thread kết thúc tối đa 1 giây
                pingerThread.join(1000);
            } catch (Exception ignored) {
            }
            try {
                // Dispose pinger
                pinger.dispose();
            } catch (Exception ignored) {
            }
            // Xóa các reference
            pingerThread = null;
            pinger = null;
        }
    }

    // Phương thức lấy thống kê phòng hiện tại
    public static Packets.RoomStats getRoomStats() {
        // Tạo object RoomStats mới
        Packets.RoomStats stats = new Packets.RoomStats();
        try {
            // Gán gamemode hiện tại
            stats.gamemode = Vars.state.rules.mode().name();
            // Gán tên map
            stats.mapName = Vars.state.map.name();
            // Gán tên người chơi (host)
            stats.name = Vars.player.name();
            // Gán danh sách mod đang sử dụng
            stats.mods = Vars.mods.getModStrings();

            // Tạo danh sách người chơi
            Seq<RoomPlayer> players = new Seq<>();

            // Duyệt qua tất cả người chơi trong game
            for (Player player : Groups.player) {
                // Tạo RoomPlayer mới cho mỗi người chơi
                RoomPlayer pl = new RoomPlayer();
                // Gán locale của người chơi
                pl.locale = player.locale;
                // Gán tên người chơi
                pl.name = player.name();
                // Thêm vào danh sách
                players.add(pl);
            }
            // Gán locale của host
            stats.locale = Vars.player.locale;
            // Gán phiên bản game
            stats.version = Version.combined();
            // Gán danh sách người chơi
            stats.players = players;
            // Gán thời gian tạo
            stats.createdAt = new Date().getTime();
        } catch (Throwable err) {
            // Log lỗi nếu có
            Log.err(err);
        }
        // Trả về object thống kê
        return stats;
    }
}
