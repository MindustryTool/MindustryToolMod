package mindustrytool.features.playerconnect;

import arc.func.Prov;
import arc.net.DcReason;
import arc.struct.ArrayMap;
import arc.struct.Seq;
import arc.util.ArcRuntimeException;
import arc.util.io.ByteBufferInput;
import arc.util.io.ByteBufferOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import mindustry.io.JsonIO;

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
        public PopupPacket() {
        }
    }

    public static class RoomPlayer {
        public String name = "";
        public String locale = "";

        public RoomPlayer() {
        }
    }

    public static class RoomStats {
        public Seq<RoomPlayer> players = new Seq<>();
        public String mapName = "";
        public String name = "";
        public String gamemode = "";
        public Seq<String> mods = new Seq<>();
        public String locale;
        public String version;
        public long createdAt;

        public RoomStats() {
        }
    }

    public static class StatsPacket extends Packet {
        public String roomId;
        public RoomStats data;

        public StatsPacket() {
        }

        public void read(ByteBufferInput read) {
            try {
                this.roomId = read.readUTF();
                this.data = (RoomStats) JsonIO.read(RoomStats.class, read.readUTF());
            } catch (IOException var3) {
                throw new RuntimeException(var3);
            }
        }

        public void write(ByteBufferOutput write) {
            try {
                write.writeUTF(this.roomId);
                write.writeUTF(JsonIO.json.toJson(this.data));
            } catch (IOException var3) {
                throw new RuntimeException(var3);
            }
        }
    }

    public static class Message2Packet extends Packet {
        public MessageType message;

        public Message2Packet() {
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

        public MessagePacket() {
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

        public RoomJoinPacket() {
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

        public RoomLinkPacket() {
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
        public CloseReason reason;

        public RoomClosedPacket() {
        }

        public void read(ByteBufferInput read) {
            this.reason = Packets.RoomClosedPacket.CloseReason.all[read.readByte()];
        }

        public void write(ByteBufferOutput write) {
            write.writeByte(this.reason.ordinal());
        }

        public static enum CloseReason {
            closed,
            obsoleteClient,
            outdatedVersion,
            serverClosed;

            public static final CloseReason[] all = values();

            private CloseReason() {
            }
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

        public RoomCreationRequestPacket() {
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
                write.writeUTF(JsonIO.write(this.data));
            } catch (Exception var3) {
                throw new RuntimeException(var3);
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

    public static class ConnectionClosedPacket extends ConnectionWrapperPacket {
        private static final DcReason[] reasons = DcReason.values();
        public DcReason reason;

        public ConnectionClosedPacket() {
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

        public ConnectionPacketWrapPacket() {
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
