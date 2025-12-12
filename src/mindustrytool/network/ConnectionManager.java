package mindustrytool.network;

import arc.Core;
import arc.net.*;
import arc.struct.*;
import playerconnect.shared.Packets;

public class ConnectionManager {
    private final IntMap<VirtualConnection> connections;
    private final Seq<VirtualConnection> orderedConnections;
    private final NetworkProxyCore proxy;
    private final NetListener dispatcher;

    public ConnectionManager(NetworkProxyCore proxy, IntMap<VirtualConnection> connections, 
                           Seq<VirtualConnection> orderedConnections, NetListener dispatcher) {
        this.proxy = proxy;
        this.connections = connections;
        this.orderedConnections = orderedConnections;
        this.dispatcher = dispatcher;
    }

    public VirtualConnection add(int id) {
        VirtualConnection connection = new VirtualConnection(proxy, id, dispatcher);
        connections.put(id, connection);
        orderedConnections.add(connection);
        return connection;
    }

    public void remove(VirtualConnection connection) {
        connections.remove(connection.id);
        orderedConnections.remove(connection);
    }

    public void closeAll(DcReason reason) {
        orderedConnections.each(c -> c.closeQuietly(reason));
        connections.clear();
        orderedConnections.clear();
    }

    public void requestRoomCreation(String password) {
        Core.app.post(() -> {
            Packets.RoomCreationRequestPacket packet = new Packets.RoomCreationRequestPacket();
            packet.version = "1";
            packet.password = password;
            packet.data = PlayerConnect.getRoomStats();
            proxy.sendTCP(packet);
        });
    }
}
