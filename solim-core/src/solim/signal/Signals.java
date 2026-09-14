package solim.signal;

import arc.Core;
import arc.Events;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.game.EventType.ClientServerConnectEvent;
import mindustry.game.EventType.ConnectionEvent;
import mindustry.game.EventType.DisposeEvent;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.EventType.HostEvent;
import mindustry.game.EventType.PlayerConnect;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.PlayerLeave;
import mindustry.game.EventType.ResetEvent;
import mindustry.game.EventType.ResizeEvent;
import mindustry.game.EventType.StateChangeEvent;
import mindustry.game.EventType.WorldLoadEvent;

public final class Signals {
    private static final Signal<Boolean> portrait = Signal.of(false);
    private static final Signal<Boolean> netActive = Signal.of(false);
    private static final Signal<Boolean> netServer = Signal.of(false);
    private static final Signal<Boolean> netClient = Signal.of(false);
    private static final Signal<Boolean> singlePlayer = Signal.of(true);
    private static final Signal<Boolean> localHosting = Signal.of(false);
    private static final Signal<Boolean> clientPlaying = Signal.of(false);
    private static final float netPollInterval = 1f;
    private static boolean initialized = false;

    static {
        init();
    }

    private Signals() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }

        Events.on(ResizeEvent.class, e -> {
            Core.app.post(() -> portrait.set(Core.graphics.isPortrait()));
        });

        refreshNetNow();
        Events.on(HostEvent.class, e -> requestRefresh());
        Events.on(ClientServerConnectEvent.class, e -> requestRefresh());
        Events.on(ConnectionEvent.class, e -> requestRefresh());
        Events.on(PlayerConnect.class, e -> requestRefresh());
        Events.on(PlayerJoin.class, e -> requestRefresh());
        Events.on(PlayerLeave.class, e -> requestRefresh());
        Events.on(StateChangeEvent.class, e -> requestRefresh());
        Events.on(WorldLoadEvent.class, e -> requestRefresh());
        Events.on(ResetEvent.class, e -> requestRefresh());
        Events.on(GameOverEvent.class, e -> requestRefresh());
        Events.on(DisposeEvent.class, e -> requestRefresh());
        Timer.schedule(Signals::requestRefresh, netPollInterval, netPollInterval);
        initialized = true;
    }

    public static Readable<Boolean> isPortrait() {
        return portrait;
    }

    /** Whether any multiplayer session is active. Menu-inclusive mirror of {@code Vars.net.active()}. */
    public static Signal<Boolean> netActive() {
        return netActive;
    }

    /** Whether this client hosts the session. Menu-inclusive mirror of {@code Vars.net.server()}. */
    public static Signal<Boolean> netServer() {
        return netServer;
    }

    /** Whether this client joined someone else's session. Menu-inclusive mirror of {@code Vars.net.client()}. */
    public static Signal<Boolean> netClient() {
        return netClient;
    }

    /** Whether no multiplayer session is active (menu counts as single-player). */
    public static Signal<Boolean> singlePlayer() {
        return singlePlayer;
    }

    /** Whether the local server is up, even before world play begins. */
    public static Signal<Boolean> localHosting() {
        return localHosting;
    }

    /** Whether connected as a client while in-world. */
    public static Signal<Boolean> clientPlaying() {
        return clientPlaying;
    }

    /**
     * Recomputes net signals from {@code Vars} on the app thread.
     * Visible for testing; production invokes it via lifecycle events and the poll backstop.
     */
    static void requestRefresh() {
        if (Core.app != null) {
            Core.app.post(Signals::refreshNetNow);
        } else {
            refreshNetNow();
        }
    }

    private static void refreshNetNow() {
        // Null guards: this shared library also loads in headless test JVMs where Vars is uninitialized.
        boolean active = Vars.net != null && Vars.net.active();
        boolean server = Vars.net != null && Vars.net.server();
        boolean client = Vars.net != null && Vars.net.client();
        boolean inGame = Vars.state != null && Vars.state.isGame();
        netActive.set(active);
        netServer.set(server);
        netClient.set(client);
        singlePlayer.set(!active);
        localHosting.set(server);
        clientPlaying.set(client && inGame);
    }
}
