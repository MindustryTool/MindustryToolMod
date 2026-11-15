package mindustrytool.features.playerconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.Events;
import arc.func.Cons;
import arc.struct.Seq;
import arc.util.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import mindustry.Vars;
import mindustry.game.EventType.ResetEvent;
import mindustry.gen.Icon;
import mindustry.gen.Player;
import arc.scene.style.TextureRegionDrawable;
import mindustry.net.Host;
import mindustry.net.Net.NetProvider;
import mindustry.net.NetConnection;
import mindustrytool.features.playerconnect.models.HostingState;
import mindustrytool.features.playerconnect.net.NetworkProxy;
import mindustrytool.features.playerconnect.net.ProxyProvider;
import mindustrytool.test.MindustryTestEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerConnectRoomLifetimeTest extends MindustryTestEnv {

    private PlayerConnectFeature feature;
    private @Nullable Player previousPlayer;

    @BeforeEach
    void setUp() {
        newScene();
        Icon.planet = new TextureRegionDrawable();
        previousPlayer = Vars.player;
        Vars.player = Player.create();
        feature = new PlayerConnectFeature(() -> CompletableFuture.completedFuture(Collections.emptyList()), false);
    }

    @AfterEach
    void tearDown() {
        Vars.player = previousPlayer;
    }

    @Test
    void closeServerNotifiesRoomClosed() {
        AtomicInteger closedCount = new AtomicInteger();
        RecordingProvider delegate = new RecordingProvider();
        ProxyProvider provider = new ProxyProvider(delegate,
                new Seq<>(NetworkProxy.VirtualConnection.class), closedCount::incrementAndGet);

        provider.closeServer();

        assertEquals(1, delegate.closeServerCalls.get(), "Delegate closeServer must be called");
        assertEquals(1, closedCount.get(), "Room close must be signalled when the local server closes");
    }

    @Test
    void closeRoomDoesNotNotifyWhenNoRoomWasActive() {
        AtomicInteger closedEvents = new AtomicInteger();
        Events.on(PcRoomClosed.class, event -> closedEvents.incrementAndGet());

        feature.closeRoom();
        feature.closeRoom();

        assertFalse(feature.isHosting(), "Closing the room must leave the feature not hosting");
        assertEquals(HostingState.IDLE, feature.stateSignal().peek());
        assertEquals(0, closedEvents.get(),
                "Closing an inactive room must not fire PcRoomClosed, so presence is not churned");
    }

    @Test
    void worldReloadDoesNotCloseTheRoom() {
        AtomicInteger closedEvents = new AtomicInteger();
        Events.on(PcRoomClosed.class, event -> closedEvents.incrementAndGet());

        Events.fire(new ResetEvent());

        assertEquals(0, closedEvents.get(),
                "A world reload such as a campaign map change must not close the relay room");
        assertFalse(feature.isHosting());
    }

    @Test
    void closeRoomIsSafeWhenNeverHosted() {
        assertFalse(feature.isHosting());
        feature.closeRoom();
        assertEquals(HostingState.IDLE, feature.stateSignal().peek());
        assertTrue(feature.getActiveLink() == null);
    }

    private static final class RecordingProvider implements NetProvider {
        final AtomicInteger closeServerCalls = new AtomicInteger();

        @Override
        public void sendAllServer(Object object, boolean reliable) {
        }

        @Override
        public void sendAllServer(Object object, Iterable<NetConnection> connections, boolean reliable) {
        }

        @Override
        public void sendExceptServer(NetConnection except, Object object, boolean reliable) {
        }

        @Override
        public void connectClient(String ip, int port, Runnable success) throws IOException {
        }

        @Override
        public void sendClient(Object object, boolean reliable) {
        }

        @Override
        public void disconnectClient() {
        }

        @Override
        public void discoverServers(Cons<Host> callback, Runnable done) {
        }

        @Override
        public void pingHost(String address, int port, Cons<Host> valid, Cons<Exception> failed) {
        }

        @Override
        public void hostServer(int port) throws IOException {
        }

        @Override
        public Iterable<? extends NetConnection> getConnections() {
            return Collections.emptyList();
        }

        @Override
        public void closeServer() {
            closeServerCalls.incrementAndGet();
        }
    }
}