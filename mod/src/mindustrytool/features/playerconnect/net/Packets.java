package mindustrytool.features.playerconnect.net;

import arc.func.Prov;
import arc.net.DcReason;
import arc.struct.ArrayMap;
import arc.util.ArcRuntimeException;
import arc.util.Time;
import arc.util.io.ByteBufferInput;
import arc.util.io.ByteBufferOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import mindustrytool.utils.JsonUtils;

public class Packets {
    public static final byte id = -4;
    protected static final ArrayMap<Class<?>, Prov<? extends Packet>> packets = new ArrayMap<>();

    public Packets() {
    }

    public static <T extends Packet> void register(Prov<T> cons) {
        packets.put(((Packet) cons.get()).getClass(), cons);
    }

    public static byte getId(Packet packet) {
        int id = packets.indexOfKey(packet.getClass());
        if (id == -1) {
            throw new ArcRuntimeException("Unknown packet type: " + packet.getClass());
        }
        return (byte) id;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Packet> T newPacket(byte id) {
        if (id >= 0 && id < packets.size) {
            var prov = packets.getValueAt(id).get();
            return (T) prov;
        }
        throw new ArcRuntimeException("Unknown packet id: " + id);
    }

    static {
        register(ConnectionPacketWrapPacket::new);
        register(ConnectionClosedPacket::new);
        register(ConnectionJoinPacket::new);
        register(ConnectionIdlingPacket::new);
        register(RoomCreationRequestPacket::new);
        register(RoomClosureRequestPacket::new);
        register(RoomClosedPacket::new);
        register(RoomLinkPacket::new);
        register(RoomJoinPacket::new);
        register(MessagePacket::new);
        register(Message2Packet::new);
        register(PopupPacket::new);
        register(StatsPacket::new);
        register(PingPacket::new);
    }

    public static class PingPacket extends Packet {
        public long sendAt;

        public PingPacket() {
            this.sendAt = Time.millis();
        }

        @Override
        public void read(ByteBufferInput read) {
            try {
                this.sendAt = read.readLong();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void write(ByteBufferOutput write) {
            try {
                write.writeLong(this.sendAt);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class PopupPacket extends MessagePacket {
        public PopupPacket() {
        }
    }

    @Data
    @NoArgsConstructor
    public static class RoomPlayer {
        public String name = "";
        public String locale = "";

        public RoomPlayer(String name, String locale) {
            this.name = name;
            this.locale = locale;
        }
    }

    @Data
    @NoArgsConstructor
    public static class RoomStats {
        public List<RoomPlayer> players = new ArrayList<>();
        public String mapName = "";
        public String name = "";
        public String gamemode = "";
        public List<String> mods = new ArrayList<>();
        public String locale = "";
        public String version = "";
        public String modVersion = "";
        public long createdAt;
        public String id = "";
    }

    public static class StatsPacket extends Packet {
        public RoomStats data;

        public StatsPacket() {
        }

        public StatsPacket(RoomStats data) {
            this.data = data;
        }

        @Override
        public void read(ByteBufferInput read) {
            try {
                this.data = JsonUtils.fromJson(RoomStats.class, read.readUTF());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void write(ByteBufferOutput write) {
            try {
                write.writeUTF(JsonUtils.toJson(this.data));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class Message2Packet extends Packet {
        public MessageType message;

        public Message2Packet() {
        }

        @Override
        public void read(ByteBufferInput read) {
            this.message = MessageType.all[read.readByte()];
        }

        @Override
        public void write(ByteBufferOutput write) {
            write.writeByte(this.message.ordinal());
        }

        public enum MessageType {
            serverClosing,
            packetSpamming,
            alreadyHosting,
            roomClosureDenied,
            conClosureDenied;

            public static final MessageType[] all = values();
        }
    }

    public static class MessagePacket extends Packet {
        public String message = "";

        public MessagePacket() {
        }

        public MessagePacket(String message) {
            this.message = message;
        }

        @Override
        public void read(ByteBufferInput read) {
            try {
                this.message = read.readUTF();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void write(ByteBufferOutput write) {
            try {
                write.writeUTF(this.message);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class RoomJoinPacket extends RoomLinkPacket {
        public String password = "";

        public RoomJoinPacket() {
        }

        public RoomJoinPacket(String roomId, String password) {
            this.roomId = roomId;
            this.password = password != null ? password : "";
        }

        @Override
        public void read(ByteBufferInput read) {
            try {
                this.roomId = read.readUTF();
                this.password = read.readUTF();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void write(ByteBufferOutput write) {
            try {
                write.writeUTF(this.roomId != null ? this.roomId : "");
                write.writeUTF(this.password != null ? this.password : "");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class RoomLinkPacket extends Packet {
        public String roomId = null;

        public RoomLinkPacket() {
        }

        public RoomLinkPacket(String roomId) {
            this.roomId = roomId;
        }

        @Override
        public void read(ByteBufferInput read) {
            try {
                this.roomId = read.readUTF();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void write(ByteBufferOutput write) {
            try {
                write.writeUTF(this.roomId != null ? this.roomId : "");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class RoomClosedPacket extends Packet {
        public RoomCloseReason reason;

        public RoomClosedPacket() {
        }

        public RoomClosedPacket(RoomCloseReason reason) {
            this.reason = reason;
        }

        @Override
        public void read(ByteBufferInput read) {
            this.reason = RoomCloseReason.all[read.readByte()];
        }

        @Override
        public void write(ByteBufferOutput write) {
            write.writeByte(this.reason.ordinal());
        }
    }

    public enum RoomCloseReason {
        closed,
        outdatedVersion,
        serverClosed;

        public static final RoomCloseReason[] all = values();
    }

    public static class RoomClosureRequestPacket extends Packet {
        public RoomClosureRequestPacket() {
        }
    }

    public static class RoomCreationRequestPacket extends Packet {
        public String version = "";
        public String password = "";
        public RoomStats data;

        public RoomCreationRequestPacket() {
        }

        public RoomCreationRequestPacket(String version, String password, RoomStats data) {
            this.version = version != null ? version : "";
            this.password = password != null ? password : "";
            this.data = data;
        }

        @Override
        public void read(ByteBufferInput read) {
            if (read.buffer.hasRemaining()) {
                try {
                    this.version = read.readUTF();
                    this.password = read.readUTF();
                    this.data = JsonUtils.fromJson(RoomStats.class, read.readUTF());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void write(ByteBufferOutput write) {
            try {
                write.writeUTF(this.version != null ? this.version : "");
                write.writeUTF(this.password != null ? this.password : "");
                write.writeUTF(JsonUtils.toJson(this.data));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class ConnectionIdlingPacket extends ConnectionWrapperPacket {
        public ConnectionIdlingPacket() {
        }
    }

    public static class ConnectionJoinPacket extends ConnectionWrapperPacket {
        public String roomId = null;

        public ConnectionJoinPacket() {
        }

        public ConnectionJoinPacket(int connectionId, String roomId) {
            this.connectionId = connectionId;
            this.roomId = roomId;
        }

        @Override
        protected void read0(ByteBufferInput read) {
            try {
                this.roomId = read.readUTF();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void write0(ByteBufferOutput write) {
            try {
                write.writeUTF(this.roomId != null ? this.roomId : "");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public enum ConnectionCloseReason {
        closed,
        timeout,
        error,
        packetSpam;

        public static final ConnectionCloseReason[] all = values();

        public static ConnectionCloseReason fromDcReason(DcReason reason) {
            if (reason == null) return ConnectionCloseReason.error;
            switch (reason) {
                case closed:
                    return ConnectionCloseReason.closed;
                case timeout:
                    return ConnectionCloseReason.timeout;
                default:
                    return ConnectionCloseReason.error;
            }
        }

        public DcReason toDcReason() {
            switch (this) {
                case closed:
                    return DcReason.closed;
                case timeout:
                    return DcReason.timeout;
                default:
                    return DcReason.error;
            }
        }
    }

    public static class ConnectionClosedPacket extends ConnectionWrapperPacket {
        private static final ConnectionCloseReason[] reasons = ConnectionCloseReason.values();
        public ConnectionCloseReason reason;

        public ConnectionClosedPacket() {
        }

        public ConnectionClosedPacket(int connectionId, ConnectionCloseReason reason) {
            this.connectionId = connectionId;
            this.reason = reason;
        }

        @Override
        protected void read0(ByteBufferInput read) {
            this.reason = reasons[read.readByte()];
        }

        @Override
        protected void write0(ByteBufferOutput write) {
            write.writeByte(this.reason.ordinal());
        }
    }

    public static class ConnectionPacketWrapPacket extends ConnectionWrapperPacket {
        public Object object;
        public ByteBuffer buffer;
        public boolean isTCP;

        public ConnectionPacketWrapPacket() {
        }

        public ConnectionPacketWrapPacket(int connectionId, boolean isTCP, Object object) {
            this.connectionId = connectionId;
            this.isTCP = isTCP;
            this.object = object;
        }

        @Override
        protected void read0(ByteBufferInput read) {
            this.isTCP = read.readBoolean();
        }

        @Override
        protected void write0(ByteBufferOutput write) {
            write.writeBoolean(this.isTCP);
        }
    }

    public abstract static class ConnectionWrapperPacket extends Packet {
        public int connectionId = -1;

        public ConnectionWrapperPacket() {
        }

        @Override
        public void read(ByteBufferInput read) {
            this.connectionId = read.readInt();
            this.read0(read);
        }

        @Override
        public void write(ByteBufferOutput write) {
            write.writeInt(this.connectionId);
            this.write0(write);
        }

        protected void read0(ByteBufferInput read) {
        }

        protected void write0(ByteBufferOutput write) {
        }
    }

    public abstract static class Packet {
        public Packet() {
        }

        public void read(ByteBufferInput read) {
        }

        public void write(ByteBufferOutput write) {
        }
    }
}
