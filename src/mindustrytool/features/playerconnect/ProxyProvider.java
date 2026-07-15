package mindustrytool.features.playerconnect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import arc.func.Cons;
import arc.struct.Seq;
import mindustry.net.Host;
import mindustry.net.ArcNetProvider.ArcConnection;
import mindustry.net.NetConnection;
import mindustry.net.Net.NetProvider;

public class ProxyProvider implements NetProvider {
    private final NetProvider delegate;
    private final Seq<NetworkProxy.VirtualConnection> virtualConnections;

    public ProxyProvider(NetProvider delegate, Seq<NetworkProxy.VirtualConnection> virtualConnections) {
        this.delegate = delegate;
        this.virtualConnections = virtualConnections;
    }

    @Override
    public void sendAllServer(Object object, boolean reliable) {
        delegate.sendAllServer(object, reliable);
        sendToAllVirtual(object, reliable, -1);
    }

    @Override
    public void sendAllServer(Object object, Iterable<NetConnection> connections, boolean reliable) {
        List<NetConnection> real = new ArrayList<>();
        for (NetConnection nc : connections) {
            if (nc instanceof ArcConnection ac && ac.connection instanceof NetworkProxy.VirtualConnection vc) {
                if (vc.isConnected()) {
                    nc.send(object, reliable);
                }
            } else {
                real.add(nc);
            }
        }
        if (!real.isEmpty()) {
            delegate.sendAllServer(object, real, reliable);
        }
    }

    @Override
    public void sendExceptServer(NetConnection except, Object object, boolean reliable) {
        delegate.sendExceptServer(except, object, reliable);

        int excludeId = -1;
        if (except instanceof ArcConnection ac && ac.connection instanceof NetworkProxy.VirtualConnection vc) {
            excludeId = vc.getID();
        }
        sendToAllVirtual(object, reliable, excludeId);
    }

    private void sendToAllVirtual(Object object, boolean reliable, int excludeId) {
        for (int i = 0; i < virtualConnections.size; i++) {
            NetworkProxy.VirtualConnection vc = virtualConnections.get(i);
            if (vc.isConnected() && vc.getID() != excludeId) {
                if (reliable) {
                    vc.sendTCP(object);
                } else {
                    vc.sendUDP(object);
                }
            }
        }
    }

    @Override
    public void connectClient(String ip, int port, Runnable success) throws IOException {
        delegate.connectClient(ip, port, success);
    }

    @Override
    public void sendClient(Object object, boolean reliable) {
        delegate.sendClient(object, reliable);
    }

    @Override
    public void disconnectClient() {
        delegate.disconnectClient();
    }

    @Override
    public void discoverServers(Cons<Host> callback, Runnable done) {
        delegate.discoverServers(callback, done);
    }

    @Override
    public void pingHost(String address, int port, Cons<Host> valid, Cons<Exception> failed) {
        delegate.pingHost(address, port, valid, failed);
    }

    @Override
    public void hostServer(int port) throws IOException {
        delegate.hostServer(port);
    }

    @Override
    public Iterable<? extends NetConnection> getConnections() {
        return delegate.getConnections();
    }

    @Override
    public void closeServer() {
        delegate.closeServer();
    }
}
