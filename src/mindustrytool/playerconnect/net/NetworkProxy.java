// Khai báo package cho module networking
package mindustrytool.playerconnect.net;

// Import IOException để xử lý lỗi I/O
import java.io.IOException;
// Import InetSocketAddress để lưu địa chỉ mạng
import java.net.InetSocketAddress;
// Import ByteBuffer để đọc/ghi dữ liệu nhị phân
import java.nio.ByteBuffer;
// Import Selector cho non-blocking I/O
import java.nio.channels.Selector;

// Import Core để chạy code trên UI thread
import arc.Core;
// Import Cons là functional interface nhận 1 tham số
import arc.func.Cons;
// Import ArcNetException cho xử lý lỗi mạng
import arc.net.ArcNetException;
// Import Client là base class cho network client
import arc.net.Client;
// Import Connection đại diện cho kết nối mạng
import arc.net.Connection;
// Import DcReason là lý do ngắt kết nối
import arc.net.DcReason;
// Import NetListener để lắng nghe sự kiện mạng
import arc.net.NetListener;
// Import IntMap là map với key là int
import arc.struct.IntMap;
// Import Seq là collection của Arc
import arc.struct.Seq;
// Import Log để ghi log
import arc.util.Log;
// Import Ratekeeper để giới hạn tốc độ gửi packet
import arc.util.Ratekeeper;
// Import Reflect để truy cập private field bằng reflection
import arc.util.Reflect;
// Import ByteBufferInput để đọc từ ByteBuffer
import arc.util.io.ByteBufferInput;
// Import ByteBufferOutput để ghi vào ByteBuffer
import arc.util.io.ByteBufferOutput;

// Import Vars chứa các biến toàn cục của game
import mindustry.Vars;
// Import Call để gọi các phương thức RPC
import mindustry.gen.Call;
// Import NoopRatekeeper là ratekeeper không giới hạn
import mindustrytool.config.NoopRatekeeper;
// Import Packets chứa các packet cho Player Connect
import playerconnect.shared.Packets;

// Lớp NetworkProxy kế thừa Client và implement NetListener, dùng để proxy kết nối qua server trung gian
public class NetworkProxy extends Client implements NetListener {
    // Phiên bản protocol hiện tại
    public static String PROTOCOL_VERSION = "1";
    // Timeout mặc định cho kết nối (5 giây)
    public static int defaultTimeout = 5000; // ms

    // Map lưu các kết nối ảo theo id
    private final IntMap<VirtualConnection> connections = new IntMap<>();
    // Danh sách các kết nối ảo theo thứ tự
    private final Seq<VirtualConnection> orderedConnections = new Seq<>(false);
    // Reference đến server thực của Mindustry
    private final arc.net.Server server;
    // Listener để dispatch các sự kiện đến server
    private final NetListener serverDispatcher;

    // Mật khẩu phòng
    private String password = "";
    // ID của phòng đã tạo
    private String roomId = null;
    // Cờ đánh dấu đã shutdown chưa
    private volatile boolean isShutdown;
    // Lý do phòng bị đóng
    private Packets.RoomClosedPacket.CloseReason closeReason;

    // Callback khi phòng được tạo thành công
    private Cons<String> onRoomCreated;
    // Callback khi phòng bị đóng
    private Cons<Packets.RoomClosedPacket.CloseReason> onRoomClosed;

    /**
     * No-op rate keeper, để tránh server của người chơi bị blacklist vĩnh viễn
     * bởi hệ thống giới hạn tốc độ.
     */
    // Ratekeeper không giới hạn tốc độ
    private static final Ratekeeper noopRate = new NoopRatekeeper();

    // Constructor nhận mật khẩu phòng
    public NetworkProxy(String password) {
        // Gọi constructor cha với buffer sizes và serializer tùy chỉnh
        super(32768, 16384, new Serializer());
        // Đăng ký this làm listener
        addListener(this);

        // Lấy provider mạng từ Vars.net bằng reflection
        mindustry.net.Net.NetProvider provider = Reflect.get(Vars.net, "provider");
        // Nếu là Steam, lấy provider bên trong
        if (Vars.steam)
            provider = Reflect.get(provider, "provider");

        // Lấy server object từ provider
        server = Reflect.get(provider, "server");
        // Lấy dispatcher listener từ server
        serverDispatcher = Reflect.get(server, "dispatchListener");
        // Lưu mật khẩu
        this.password = password;
    }

    /** Phương thức này phải được sử dụng thay vì các phương thức connect khác */
    // Phương thức connect với callbacks
    public void connect(String host, int udpTcpPort,
            Cons<String> onRoomCreated,
            Cons<Packets.RoomClosedPacket.CloseReason> onRoomClosed//
    ) throws IOException {
        // Lưu callback khi phòng được tạo
        this.onRoomCreated = onRoomCreated;
        // Lưu callback khi phòng bị đóng
        this.onRoomClosed = onRoomClosed;
        // Gọi connect với timeout và port
        connect(defaultTimeout, host, udpTcpPort, udpTcpPort);
    }

    /**
     * Định nghĩa lại {@link #run()} và {@link #stop()} để xử lý exception và khởi động lại
     * vòng lặp update nếu cần. <br>
     * Và để xử lý connection idling.
     */
    // Vòng lặp chính của network proxy
    @Override
    public void run() {
        // Đánh dấu chưa shutdown
        isShutdown = false;
        // Lặp cho đến khi shutdown
        while (!isShutdown) {
            try {
                // Cập nhật mạng với timeout 250ms
                update(250);
                // Cập nhật trạng thái idle cho các kết nối
                for (int i = 0; i < orderedConnections.size; i++) {
                    // Lấy kết nối tại vị trí i
                    VirtualConnection con = orderedConnections.get(i);
                    // Nếu đang kết nối và đang idle thì notify
                    if (con.isConnected() && con.isIdle())
                        con.notifyIdle0();
                }
            } catch (IOException ex) {
                // Đóng kết nối khi có lỗi I/O
                close();
            } catch (ArcNetException ex) {
                // Nếu chưa có roomId thì đóng và throw exception
                if (roomId == null) {
                    close();
                    throw ex;
                }
            }
        }
    }

    /**
     * Định nghĩa lại {@link #run()} và {@link #stop()} để xử lý exception và khởi động lại
     * vòng lặp update nếu cần.
     */
    // Phương thức dừng proxy
    @Override
    public void stop() {
        // Nếu đã shutdown thì return
        if (isShutdown)
            return;

        // Đóng kết nối
        close();
        // Đánh dấu đã shutdown
        isShutdown = true;
        // Lấy selector bằng reflection và wakeup nó
        Selector selector = Reflect.get(Client.class, this, "selector");
        selector.wakeup();
    }

    // Getter trả về roomId
    public String roomId() {
        return roomId;
    }

    // Phương thức đóng phòng
    public void closeRoom() {
        // Xóa roomId
        roomId = null;

        // Nếu đang kết nối thì gửi packet yêu cầu đóng phòng
        if (isConnected())
            sendTCP(new Packets.RoomClosureRequestPacket());

        // Đóng kết nối
        close();
    }

    // Callback khi kết nối thành công
    @Override
    public void connected(Connection connection) {
        // Tạo packet yêu cầu tạo phòng
        Packets.RoomCreationRequestPacket p = new Packets.RoomCreationRequestPacket();
        // Gán phiên bản protocol
        p.version = PROTOCOL_VERSION;
        // Gán mật khẩu
        p.password = password;
        // Chạy trên UI thread để lấy stats
        Core.app.post(() -> {
            // Lấy thống kê phòng hiện tại
            Packets.RoomStats stats = PlayerConnect.getRoomStats();
            // Gán stats vào packet
            p.data = stats;
            // Gửi packet qua TCP
            sendTCP(p);
        });
    }

    // Callback khi bị ngắt kết nối
    @Override
    public void disconnected(Connection connection, DcReason reason) {
        // Xóa roomId
        roomId = null;
        // Gọi callback nếu có
        if (onRoomClosed != null)
            onRoomClosed.get(closeReason);
        // Không thể giao tiếp với server nữa, đóng tất cả kết nối ảo
        // Đóng từng kết nối ảo một cách im lặng
        orderedConnections.each(c -> c.closeQuietly(reason));
        // Xóa toàn bộ map connections
        connections.clear();
        // Xóa danh sách kết nối
        orderedConnections.clear();
    }

    // Callback khi nhận được packet
    @Override
    public void received(Connection connection, Object object) {
        try {
            // Nếu không phải Packet thì bỏ qua
            if (!(object instanceof Packets.Packet)) {
                return;

            // Xử lý MessagePacket - hiển thị tin nhắn từ server
            } else if (object instanceof Packets.MessagePacket) {
                // Gửi tin nhắn đến tất cả người chơi
                Call.sendMessage("[scarlet][[CLaJ Server]:[] " + ((Packets.MessagePacket) object).message);

            // Xử lý Message2Packet - hiển thị tin nhắn được dịch
            } else if (object instanceof Packets.Message2Packet) {
                // Gửi tin nhắn được dịch từ bundle
                Call.sendMessage("[scarlet][[CLaJ Server]:[] " + arc.Core.bundle.get("claj.message." +
                        arc.util.Strings.camelToKebab(((Packets.Message2Packet) object).message.name())));

            // Xử lý PopupPacket - hiển thị popup dialog
            } else if (object instanceof Packets.PopupPacket) {
                // Hiển thị popup với nội dung từ server
                Vars.ui.showText("[scarlet][[CLaJ Server][] ", ((Packets.PopupPacket) object).message);

            // Xử lý RoomClosedPacket - phòng bị đóng
            } else if (object instanceof Packets.RoomClosedPacket) {
                // Lưu lý do đóng phòng
                closeReason = ((Packets.RoomClosedPacket) object).reason;

            // Xử lý RoomLinkPacket - nhận link phòng
            } else if (object instanceof Packets.RoomLinkPacket) {
                // Bỏ qua nếu đã nhận roomId rồi
                if (roomId != null)
                    return;

                // Lấy roomId từ packet
                roomId = ((Packets.RoomLinkPacket) object).roomId;
                // -1 không được phép vì dùng để chỉ phòng chưa tạo
                if (roomId != null && onRoomCreated != null)
                    // Gọi callback với roomId
                    onRoomCreated.get(roomId);

            // Xử lý ConnectionWrapperPacket - packet bọc cho virtual connection
            } else if (object instanceof Packets.ConnectionWrapperPacket) {
                // Bỏ qua packet cho đến khi nhận được roomId
                if (roomId == null)
                    return;

                // Lấy id của connection từ packet
                int id = ((Packets.ConnectionWrapperPacket) object).connectionId;
                // Tìm kết nối ảo theo id
                VirtualConnection con = connections.get(id);

                // Nếu kết nối chưa tồn tại
                if (con == null) {
                    // Tạo kết nối mới nếu là ConnectionJoinPacket
                    if (object instanceof Packets.ConnectionJoinPacket) {
                        // Kiểm tra roomId có khớp không
                        if (!((Packets.ConnectionJoinPacket) object).roomId.equals(roomId)) {

                            // Nếu không khớp, gửi packet đóng kết nối
                            Packets.ConnectionClosedPacket p = new Packets.ConnectionClosedPacket();
                            p.connectionId = id;
                            p.reason = DcReason.error;
                            sendTCP(p);
                            return;
                        }

                        // Thêm kết nối mới vào danh sách
                        addConnection(con = new VirtualConnection(this, id));
                        // Thông báo đã kết nối
                        con.notifyConnected0();
                        // Đổi packet rate và chat rate thành no-op để tránh bị blacklist
                        ((mindustry.net.NetConnection) con.getArbitraryData()).packetRate = noopRate;
                        ((mindustry.net.NetConnection) con.getArbitraryData()).chatRate = noopRate;
                    }

                // Xử lý ConnectionPacketWrapPacket - packet dữ liệu từ client
                } else if (object instanceof Packets.ConnectionPacketWrapPacket) {
                    // Chuyển tiếp packet đến listener
                    con.notifyReceived0(((Packets.ConnectionPacketWrapPacket) object).object);

                // Xử lý ConnectionIdlingPacket - client đang idle
                } else if (object instanceof Packets.ConnectionIdlingPacket) {
                    // Đánh dấu kết nối đang idle
                    con.setIdle();

                // Xử lý ConnectionClosedPacket - kết nối bị đóng từ server
                } else if (object instanceof Packets.ConnectionClosedPacket) {
                    // Đóng kết nối im lặng với lý do từ server
                    con.closeQuietly(((Packets.ConnectionClosedPacket) object).reason);
                    Log.info("Close connection from server");
                }
            }
        } catch (Exception error) {
            // Log lỗi khi xử lý thất bại
            Log.info("Failed to handle: " + object);
        }
    }

    // Phương thức thêm kết nối ảo vào danh sách
    protected void addConnection(VirtualConnection con) {
        // Thêm vào map theo id
        connections.put(con.id, con);
        // Thêm vào danh sách
        orderedConnections.add(con);
    }

    // Phương thức xóa kết nối ảo khỏi danh sách
    protected void removeConnection(VirtualConnection con) {
        // Xóa khỏi map
        connections.remove(con.id);
        // Xóa khỏi danh sách
        orderedConnections.remove(con);
    }

    // Lớp Serializer để serialize/deserialize packet
    public static class Serializer extends mindustry.net.ArcNetProvider.PacketSerializer {
        // Phương thức đọc packet từ buffer
        @Override
        public Object read(ByteBuffer buffer) {
            // Nếu byte đầu tiên là id của Packets
            if (buffer.get() == Packets.id) {
                // Tạo packet mới theo loại
                Packets.Packet p = Packets.newPacket(buffer.get());
                // Đọc dữ liệu packet
                p.read(new ByteBufferInput(buffer));
                // Xử lý đặc biệt cho ConnectionPacketWrapPacket
                if (p instanceof Packets.ConnectionPacketWrapPacket)
                    // Đọc thêm object bên trong
                    ((Packets.ConnectionPacketWrapPacket) p).object = super.read(buffer);
                return p;
            }

            // Nếu không phải, quay lại vị trí cũ và đọc bằng serializer cha
            buffer.position(buffer.position() - 1);
            return super.read(buffer);
        }

        // Phương thức ghi packet vào buffer
        @Override
        public void write(ByteBuffer buffer, Object object) {
            // Nếu là Packets.Packet
            if (object instanceof Packets.Packet) {
                // Cast thành Packet
                Packets.Packet p = (Packets.Packet) object;
                // Ghi id và loại packet
                buffer.put(Packets.id).put(Packets.getId(p));
                // Ghi dữ liệu packet
                p.write(new ByteBufferOutput(buffer));
                // Xử lý đặc biệt cho ConnectionPacketWrapPacket
                if (p instanceof Packets.ConnectionPacketWrapPacket)
                    // Ghi thêm object bên trong
                    super.write(buffer, ((Packets.ConnectionPacketWrapPacket) p).object);
                return;
            }

            // Nếu không phải, dùng serializer cha
            super.write(buffer, object);
        }
    }

    /**
     * Chúng ta có thể xóa và hook các thứ một cách an toàn, 
     * networking đã được reverse engineered.
     */
    // Lớp VirtualConnection đại diện cho kết nối ảo qua proxy
    public static class VirtualConnection extends Connection {
        // Danh sách các listener cho kết nối này
        final Seq<NetListener> listeners = new Seq<>();
        // ID của kết nối
        final int id;
        /**
         * Một kết nối ảo luôn được kết nối cho đến khi chúng ta đóng nó,
         * proxy sẽ thông báo cho server để đóng kết nối,
         * hoặc khi server thông báo rằng kết nối đã bị đóng.
         */
        // Cờ đánh dấu đang kết nối
        volatile boolean isConnected = true;
        /** Server sẽ thông báo nếu client đang idle */
        // Cờ đánh dấu đang idle
        volatile boolean isIdling = true;
        // Reference đến proxy
        NetworkProxy proxy;

        // Constructor nhận proxy và id
        public VirtualConnection(NetworkProxy proxy, int id) {
            // Lưu reference đến proxy
            this.proxy = proxy;
            // Lưu id
            this.id = id;
            // Đăng ký server dispatcher làm listener
            addListener(proxy.serverDispatcher);
        }

        // Phương thức gửi packet qua TCP
        @Override
        public int sendTCP(Object object) {
            // Kiểm tra object không null
            if (object == null)
                throw new IllegalArgumentException("object cannot be null.");
            // Đánh dấu không idle nữa
            isIdling = false;

            // Tạo packet bọc
            Packets.ConnectionPacketWrapPacket p = new Packets.ConnectionPacketWrapPacket();
            // Gán id kết nối
            p.connectionId = id;
            // Đánh dấu gửi qua TCP
            p.isTCP = true;
            // Gán object cần gửi
            p.object = object;
            // Gửi qua proxy bằng TCP
            return proxy.sendTCP(p);
        }

        // Phương thức gửi packet qua UDP
        @Override
        public int sendUDP(Object object) {
            // Kiểm tra object không null
            if (object == null)
                throw new IllegalArgumentException("object cannot be null.");
            // Đánh dấu không idle nữa
            isIdling = false;

            // Tạo packet bọc
            Packets.ConnectionPacketWrapPacket p = new Packets.ConnectionPacketWrapPacket();
            // Gán id kết nối
            p.connectionId = id;
            // Đánh dấu gửi qua UDP
            p.isTCP = false;
            // Gán object cần gửi
            p.object = object;
            // Gửi qua proxy bằng UDP
            return proxy.sendUDP(p);
        }

        // Phương thức đóng kết nối với lý do
        @Override
        public void close(DcReason reason) {
            // Lưu trạng thái kết nối trước khi đóng
            boolean wasConnected = isConnected;
            // Đánh dấu đã ngắt kết nối và không idle
            isConnected = isIdling = false;
            // Chỉ thực hiện nếu trước đó đang kết nối
            if (wasConnected) {
                // Tạo packet thông báo đóng kết nối
                Packets.ConnectionClosedPacket p = new Packets.ConnectionClosedPacket();
                // Gán id kết nối
                p.connectionId = id;
                // Gán lý do đóng
                p.reason = reason;
                // Gửi thông báo đến server
                proxy.sendTCP(p);

                // Thông báo cho các listener về việc ngắt kết nối
                notifyDisconnected0(reason);
            }
        }

        /**
         * Đóng kết nối mà không thông báo cho server. <br>
         * Thường dùng khi chính server yêu cầu đóng kết nối.
         */
        // Phương thức đóng im lặng (không gửi thông báo đến server)
        public void closeQuietly(DcReason reason) {
            // Lưu trạng thái kết nối trước khi đóng
            boolean wasConnected = isConnected;
            // Đánh dấu đã ngắt kết nối và không idle
            isConnected = isIdling = false;
            // Chỉ thực hiện nếu trước đó đang kết nối
            if (wasConnected)
                // Thông báo cho các listener
                notifyDisconnected0(reason);
        }

        // Getter trả về id của kết nối
        @Override
        public int getID() {
            return id;
        }

        // Kiểm tra có đang kết nối không
        @Override
        public boolean isConnected() {
            return isConnected;
        }

        // Phương thức không sử dụng - keep alive TCP
        @Override
        public void setKeepAliveTCP(int keepAliveMillis) {
        }

        // Phương thức không sử dụng - timeout
        @Override
        public void setTimeout(int timeoutMillis) {
        }

        // Lấy địa chỉ TCP remote
        @Override
        public InetSocketAddress getRemoteAddressTCP() {
            // Trả về địa chỉ của proxy nếu đang kết nối
            return isConnected() ? proxy.getRemoteAddressTCP() : null;
        }

        // Lấy địa chỉ UDP remote
        @Override
        public InetSocketAddress getRemoteAddressUDP() {
            // Trả về địa chỉ của proxy nếu đang kết nối
            return isConnected() ? proxy.getRemoteAddressUDP() : null;
        }

        // Phương thức không sử dụng - TCP write buffer size
        @Override
        public int getTcpWriteBufferSize() {
            return 0;
        }

        // Kiểm tra có đang idle không
        @Override
        public boolean isIdle() {
            return isIdling;
        }

        // Phương thức không sử dụng - idle threshold
        @Override
        public void setIdleThreshold(float idleThreshold) {
        }

        // Chuyển đổi thành string
        @Override
        public String toString() {
            return "Connection " + id;
        }

        /** Chỉ dùng khi gửi world data */
        // Thêm listener vào danh sách
        public void addListener(NetListener listener) {
            // Kiểm tra listener không null
            if (listener == null)
                throw new IllegalArgumentException("listener cannot be null.");
            // Thêm vào danh sách listeners
            listeners.add(listener);
        }

        /** Chỉ dùng khi gửi world data */
        // Xóa listener khỏi danh sách
        public void removeListener(NetListener listener) {
            // Kiểm tra listener không null
            if (listener == null)
                throw new IllegalArgumentException("listener cannot be null.");
            // Xóa khỏi danh sách listeners
            listeners.remove(listener);
        }

        // Thông báo cho các listener về kết nối thành công
        public void notifyConnected0() {
            // Gọi connected() cho mỗi listener
            listeners.each(l -> l.connected(this));
        }

        // Thông báo cho các listener về ngắt kết nối
        public void notifyDisconnected0(DcReason reason) {
            // Xóa kết nối này khỏi proxy
            proxy.removeConnection(this);
            // Gọi disconnected() cho mỗi listener
            listeners.each(l -> l.disconnected(this, reason));
        }

        // Đặt trạng thái idle
        public void setIdle() {
            isIdling = true;
        }

        // Thông báo cho các listener về trạng thái idle
        public void notifyIdle0() {
            // Gọi idle() cho mỗi listener nếu đang idle
            listeners.each(l -> isIdle(), l -> l.idle(this));
        }

        // Thông báo cho các listener về packet nhận được
        public void notifyReceived0(Object object) {
            // Gọi received() cho mỗi listener với packet
            listeners.each(l -> l.received(this, object));
        }
    }
}
