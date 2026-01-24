package mindustrytool.features.playerconnect;

import arc.func.Prov;
import arc.net.DcReason;
import arc.struct.ArrayMap;
import arc.util.ArcRuntimeException;
import arc.util.io.ByteBufferInput;
import arc.util.io.ByteBufferOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import mindustry.io.JsonIO;
import mindustrytool.Utils;

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
        } else {
            return (byte) id;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Packet> T newPacket(byte id) {
        if (id >= 0 && id < packets.size) {
            var prov = packets.getValueAt(id).get();
            return (T) prov;
        } else {
            throw new ArcRuntimeException("Unknown packet id: " + id);
        }
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
    }

    public static class PopupPacket extends MessagePacket {
        private PopupPacket() {
        }
    }

    public static class RoomPlayer {
        public String name = "";
        public String locale = "";

        public RoomPlayer(String name, String locale) {
            this.name = name;
            this.locale = locale;
        }
    }

    public static class RoomStats {
        public List<RoomPlayer> players = new ArrayList<>();
        public String mapName = "";
        public String name = "";
        public String gamemode = "";
        public List<String> mods = new ArrayList<>();
        public String locale;
        public String version;
        public String modVersion;
        public long createdAt;

        public RoomStats() {
        }

    }

    public static class StatsPacket extends Packet {
        public RoomStats data;

        private StatsPacket() {
        }

        public StatsPacket(RoomStats data) {
            this.data = data;
        }

        public void read(ByteBufferInput read) {
            try {
                this.data = (RoomStats) JsonIO.read(RoomStats.class, read.readUTF());
            } catch (IOException var3) {
                throw new RuntimeException(var3);
            }
        }

        public void write(ByteBufferOutput write) {
            try {
                write.writeUTF(Utils.toJson(this.data));
            } catch (IOException var3) {
                throw new RuntimeException(var3);
            }
        }
    }

    public static class Message2Packet extends Packet {
        public MessageType message;

        private Message2Packet() {
        }

        public void read(ByteBufferInput read) {
            this.message = Packets.Message2Packet.MessageType.all[read.readByte()];
        }

        public void write(ByteBufferOutput write) {
            write.writeByte(this.message.ordinal());
        }

        public static enum MessageType {
            serverClosing,
            packetSpamming,
            alreadyHosting,
            roomClosureDenied,
            conClosureDenied;

            public static final MessageType[] all = values();

            private MessageType() {
            }
        }
    }

    public static class MessagePacket extends Packet {
        public String message;

        private MessagePacket() {
        }

        public void read(ByteBufferInput read) {
            try {
                this.message = read.readUTF();
            } catch (Exception var3) {
                throw new RuntimeException(var3);
            }
        }

        public void write(ByteBufferOutput write) {
            try {
                write.writeUTF(this.message);
            } catch (Exception var3) {
                throw new RuntimeException(var3);
            }
        }
    }

    public static class RoomJoinPacket extends RoomLinkPacket {
        public String password;

        private RoomJoinPacket() {
        }

        public RoomJoinPacket(String roomId, String password) {
            this.roomId = roomId;
            this.password = password;
        }

        public void read(ByteBufferInput read) {
            try {
                this.roomId = read.readUTF();
                this.password = read.readUTF();
            } catch (IOException var3) {
                throw new RuntimeException(var3);
            }
        }

        public void write(ByteBufferOutput write) {
            try {
                write.writeUTF(this.roomId);
                write.writeUTF(this.password);
            } catch (IOException var3) {
                throw new RuntimeException(var3);
            }
        }
    }

    public static class RoomLinkPacket extends Packet {
        public String roomId = null;

        private RoomLinkPacket() {
        }

        public void read(ByteBufferInput read) {
            try {
                this.roomId = read.readUTF();
            } catch (IOException var3) {
                throw new RuntimeException(var3);
            }
        }

        public void write(ByteBufferOutput write) {
            try {
                write.writeUTF(this.roomId);
            } catch (IOException var3) {
                throw new RuntimeException(var3);
            }
        }
    }

    public static class RoomClosedPacket extends Packet {
        public RoomCloseReason reason;

        private RoomClosedPacket() {
        }

        public void read(ByteBufferInput read) {
            this.reason = Packets.RoomCloseReason.all[read.readByte()];
        }

        public void write(ByteBufferOutput write) {
            write.writeByte(this.reason.ordinal());
        }

    }

    public static enum RoomCloseReason {
        closed,
        outdatedVersion,
        serverClosed;

        public static final RoomCloseReason[] all = values();

        private RoomCloseReason() {
        }
    }

    public static class RoomClosureRequestPacket extends Packet {
        public RoomClosureRequestPacket() {
        }
    }

    public static class RoomCreationRequestPacket extends Packet {
        public String version;
        public String password;
        public RoomStats data;

        private RoomCreationRequestPacket() {
        }

        public RoomCreationRequestPacket(String version, String password, RoomStats data) {
            this.version = version;
            this.password = password;
            this.data = data;
        }

        public void read(ByteBufferInput read) {
            if (read.buffer.hasRemaining()) {
                try {
                    this.version = read.readUTF();
                    this.password = read.readUTF();
                    this.data = (RoomStats) JsonIO.read(RoomStats.class, read.readUTF());
                } catch (Exception var3) {
                    throw new RuntimeException(var3);
                }
            }

        }

        public void write(ByteBufferOutput write) {
            try {
                write.writeUTF(this.version);
                write.writeUTF(this.password);
                write.writeUTF(Utils.toJson(this.data));
            } catch (Exception var3) {
                throw new RuntimeException(var3);
            }
        }
    }

    public static class ConnectionIdlingPacket extends ConnectionWrapperPacket {
        private ConnectionIdlingPacket() {
        }
    }

    public static class ConnectionJoinPacket extends ConnectionWrapperPacket {
        public String roomId = null;

        private ConnectionJoinPacket() {
        }

        protected void read0(ByteBufferInput read) {
            try {
                this.roomId = read.readUTF();
            } catch (IOException var3) {
                throw new RuntimeException(var3);
            }
        }

        protected void write0(ByteBufferOutput write) {
            try {
                write.writeUTF(this.roomId);
            } catch (IOException var3) {
                throw new RuntimeException(var3);
            }
        }
    }

    public static enum ConnectionCloseReason {
        closed,
        timeout,
        error,
        packetSpam;

        public static final ConnectionCloseReason[] all = values();

        private ConnectionCloseReason() {
        }

        public static ConnectionCloseReason fromDcReason(DcReason reason) {
            switch (reason) {
                case closed:
                    return ConnectionCloseReason.closed;
                case timeout:
                    return ConnectionCloseReason.timeout;
                case error:
                    return ConnectionCloseReason.error;
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
                case error:
                    return DcReason.error;
                case packetSpam:
                    return DcReason.error;
                default:
                    return DcReason.error;
            }
        }
    }

    public static class ConnectionClosedPacket extends ConnectionWrapperPacket {
        private static final ConnectionCloseReason[] reasons = ConnectionCloseReason.values();
        public ConnectionCloseReason reason;

        private ConnectionClosedPacket() {
        }

        public ConnectionClosedPacket(int connectionId, ConnectionCloseReason reason) {
            this.connectionId = connectionId;
            this.reason = reason;
        }

        protected void read0(ByteBufferInput read) {
            this.reason = reasons[read.readByte()];
        }

        protected void write0(ByteBufferOutput write) {
            write.writeByte(this.reason.ordinal());
        }
    }

    public static class ConnectionPacketWrapPacket extends ConnectionWrapperPacket {
        public Object object;
        public ByteBuffer buffer;
        public boolean isTCP;

        private ConnectionPacketWrapPacket() {
        }

        public ConnectionPacketWrapPacket(int connectionId, boolean isTCP, Object object) {
            this.connectionId = connectionId;
            this.isTCP = isTCP;
            this.object = object;
        }

        protected void read0(ByteBufferInput read) {
            this.isTCP = read.readBoolean();
        }

        protected void write0(ByteBufferOutput write) {
            write.writeBoolean(this.isTCP);
        }
    }

    public abstract static class ConnectionWrapperPacket extends Packet {
        public int connectionId = -1;

        public ConnectionWrapperPacket() {
        }

        public void read(ByteBufferInput read) {
            this.connectionId = read.readInt();
            this.read0(read);
        }

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
