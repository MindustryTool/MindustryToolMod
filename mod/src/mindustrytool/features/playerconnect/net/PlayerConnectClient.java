package mindustrytool.features.playerconnect.net;

import arc.Core;
import arc.func.Cons;
import arc.net.Client;
import arc.net.Connection;
import arc.net.DcReason;
import arc.net.NetListener;
import arc.net.TcpConnection;
import arc.net.UdpConnection;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Reflect;
import arc.util.Threads;
import arc.util.Time;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import mindustry.Vars;
import mindustry.net.Net.NetProvider;

public class PlayerConnectClient {

    public static final int PING_TIMEOUT = 8000;

    private static @Nullable ExecutorService pingWorker;

    private static synchronized ExecutorService getPingWorker() {
        if (pingWorker == null || pingWorker.isShutdown()) {
            pingWorker = Threads.unboundedExecutor("PlayerConnect-Pinger", 4);
        }
        return pingWorker;
    }

    public static String getSavedPassword(String roomId) {
        return Core.settings.getString("player-connect.pwd-" + roomId, "");
    }

    public static void savedPassword(String roomId, String password) {
        Core.settings.put("player-connect.pwd-" + roomId, password);
    }

    public static void join(PlayerConnectLink link, String password, Runnable success) {
        if (link == null) {
            throw new IllegalArgumentException("Link cannot be null.");
        }

        if (password != null && !password.isEmpty()) {
            savedPassword(link.roomId, password);
        }

        Vars.ui.loadfrag.show("@connecting");
        Vars.ui.loadfrag.setButton(() -> {
            Vars.ui.loadfrag.hide();
            Vars.netClient.disconnectQuietly();
        });

        Vars.logic.reset();
        Vars.net.reset();
        Vars.netClient.beginConnecting();

        NetProvider provider = Reflect.get(Vars.net, "provider");
        if (provider instanceof ProxyProvider) {
            provider = ((ProxyProvider) provider).getDelegate();
            Reflect.set(Vars.net, "provider", provider);
        }

        if (Vars.steam) {
            provider = Reflect.get(provider, "provider");
        }

        Client client = Reflect.get(provider, "client");
        TcpConnection tcp = Reflect.get(Connection.class, client, "tcp");
        if (tcp == null) {
            throw new IllegalStateException("TCP connection is null.");
        }

        setField(client, "serialization", new NetworkProxy.Serializer());
        setField(tcp, "serialization", new NetworkProxy.Serializer());

        NetListener[] listeners = Reflect.get(Connection.class, client, "listeners");
        NetListener wrap = new NetListener() {
            @Override
            public void connected(Connection connection) {
                if (listeners != null) {
                    for (NetListener listener : listeners) {
                        listener.connected(connection);
                    }
                }
            }

            @Override
            public void disconnected(Connection connection, DcReason reason) {
                if (listeners != null) {
                    for (NetListener listener : listeners) {
                        listener.disconnected(connection, reason);
                    }
                }
            }

            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof Packets.MessagePacket) {
                    Packets.MessagePacket messagePacket = (Packets.MessagePacket) object;
                    Core.app.post(() -> Vars.ui.showErrorMessage(messagePacket.message));
                    Vars.netClient.setQuiet();
                    Vars.ui.loadfrag.hide();
                    client.close();
                    Log.info("Client closed: " + messagePacket.message);
                    return;
                }

                if (listeners != null) {
                    for (NetListener listener : listeners) {
                        listener.received(connection, object);
                    }
                }
            }

            @Override
            public void idle(Connection connection) {
                if (listeners != null) {
                    for (NetListener listener : listeners) {
                        listener.idle(connection);
                    }
                }
            }
        };

        Reflect.set(Connection.class, client, "listeners", new NetListener[] { wrap });

        try {
            Vars.net.connect(link.host, link.port, () -> {
                UdpConnection udp = Reflect.get(Connection.class, client, "udp");
                if (udp == null) {
                    throw new IllegalStateException("UDP connection is null.");
                }

                setField(udp, "serialization", new NetworkProxy.Serializer());

                if (!Vars.net.client()) {
                    throw new IllegalStateException("Net client is not active.");
                }

                Packets.RoomJoinPacket packet = new Packets.RoomJoinPacket(link.roomId, password);
                Vars.net.send(packet, true);

                if (success != null) {
                    success.run();
                }
            });
        } catch (Exception e) {
            Vars.ui.loadfrag.hide();
            Vars.ui.showException(e);
        }
    }

    public static void pingHost(String ip, int port, Cons<Long> success, Cons<Throwable> onFailed) {
        getPingWorker().submit(() -> {
            try (Socket socket = new Socket()) {
                long start = Time.millis();
                socket.connect(new InetSocketAddress(ip, port), PING_TIMEOUT);
                long elapsed = Time.timeSinceMillis(start);
                Core.app.post(() -> success.get(elapsed));
            } catch (Exception e) {
                Core.app.post(() -> onFailed.get(e));
            }
        });
    }

    public static synchronized void disposePinger() {
        if (pingWorker != null) {
            pingWorker.shutdownNow();
            pingWorker = null;
        }
    }

    public static void unbanProxyIp(NetworkProxy proxy) {
        getPingWorker().submit(() -> {
            try {
                String ip = proxy.getProviderIp();
                if (ip == null || ip.isEmpty()) {
                    return;
                }

                Core.app.post(() -> {
                    Vars.netServer.admins.unbanPlayerIP(ip);
                    Log.info("PlayerConnect: Unbanned proxy relay IP @", ip);
                });
            } catch (Exception e) {
                Log.err("Failed to resolve and unban proxy relay IP for @", proxy.getProviderIp(), e);
            }
        });
    }

    private static void setField(Object object, String name, Object value) {
        try {
            Field field = getField(object.getClass(), name);
            field.setAccessible(true);
            field.set(object, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + name + " on " + object, e);
        }
    }

    private static Field getField(Class<?> type, String name) throws NoSuchFieldException {
        try {
            return type.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            Class<?> superclass = type.getSuperclass();
            if (superclass == null) {
                throw e;
            }
            return getField(superclass, name);
        }
    }
}
