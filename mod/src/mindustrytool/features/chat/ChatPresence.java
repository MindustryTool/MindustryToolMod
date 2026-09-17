package mindustrytool.features.chat;

import arc.Core;
import arc.Events;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import mindustry.Vars;
import mindustry.core.GameState.State;
import mindustry.game.EventType.ClientServerConnectEvent;
import mindustry.game.EventType.StateChangeEvent;
import mindustry.game.EventType.WorldLoadEndEvent;
import mindustrytool.features.chat.state.ChatSession;
import mindustrytool.features.playerconnect.PcRoomClosed;
import mindustrytool.features.playerconnect.PcRoomOpened;
import mindustrytool.services.MindustryTool;
import solim.config.ConfigValue;
import solim.reactive.Signal;

public class ChatPresence {

    private static final float DEBOUNCE_SECONDS = 1f;
    private static final float HEARTBEAT_SECONDS = 5f * 60f;

    private final ChatSession session;
    private final ConfigValue<Boolean> sharePresence;
    private final Function<String, CompletableFuture<Void>> publisher;

    private @Nullable String pcRoom;
    private long generation;
    private String lastSent = ChatSession.MENU_STATE;
    private boolean dirty;
    private @Nullable Timer.Task pendingSync;

    public ChatPresence(ChatSession session, ConfigValue<Boolean> sharePresence, Signal<Boolean> featureEnabled) {
        this(session, sharePresence, featureEnabled, MindustryTool::updateChatState);
    }

    ChatPresence(ChatSession session, ConfigValue<Boolean> sharePresence, Signal<Boolean> featureEnabled,
            Function<String, CompletableFuture<Void>> publisher) {
        this.session = session;
        this.sharePresence = sharePresence;
        this.publisher = publisher;

        Events.on(ClientServerConnectEvent.class, this::onServerConnect);
        Events.on(StateChangeEvent.class, this::onStateChange);
        Events.on(WorldLoadEndEvent.class, event -> onWorldLoaded());
        Events.on(PcRoomOpened.class, this::onPcRoomOpened);
        Events.on(PcRoomClosed.class, event -> onPcRoomClosed());

        session.loggedIn().subscribe(loggedIn -> {
            if (Boolean.TRUE.equals(loggedIn)) {
                forceSync();
            }
        });
        session.connected().subscribe(connected -> {
            if (Boolean.TRUE.equals(connected)) {
                forceSync();
            }
        });
        sharePresence.signal().subscribe(optIn -> {
            if (Boolean.TRUE.equals(optIn)) {
                forceSync();
            } else {
                cancelPendingSync();
            }
        });
        featureEnabled.subscribe(enabled -> {
            if (Boolean.TRUE.equals(enabled)) {
                forceSync();
            }
        });

        Timer.schedule(this::heartbeat, HEARTBEAT_SECONDS, HEARTBEAT_SECONDS);
        attemptSend(false);
    }

    private void onServerConnect(ClientServerConnectEvent event) {
        generation++;
        final long gen = generation;
        Vars.net.pingHost(event.ip, event.port, host -> {
            Core.app.post(() -> {
                if (isStalePing(gen, generation)) {
                    return;
                }
                String name = host != null && host.name != null ? host.name : "";
                if (name.isEmpty()) {
                    return;
                }
                applyValue(ChatSession.SERVER_PREFIX + name);
            });
        }, e -> Log.err("Failed to ping server for presence", e));
    }

    private void onStateChange(StateChangeEvent event) {
        if (event == null || event.to == null) {
            return;
        }
        if (event.to == State.menu) {
            if (Core.graphics.isHidden()) {
                return;
            }
            applyMenu();
        } else if (event.to == State.playing) {
            applyRestore();
        }
    }

    private void onWorldLoaded() {
        if (Vars.net.client()) {
            return;
        }
        String mapName = Vars.state.map != null && Vars.state.map.name() != null
                ? Vars.state.map.name()
                : "unknown";
        if (Vars.state.isCampaign()) {
            applyValue(ChatSession.CAMPAIGN_PREFIX + mapName);
        } else if (Vars.state.isEditor()) {
            applyValue(ChatSession.EDITOR_STATE + mapName);
        } else {
            applyValue(ChatSession.CUSTOM_GAME_STATE);
        }
    }

    private void onPcRoomOpened(PcRoomOpened event) {
        String name = event != null ? event.roomName : "";
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        pcRoom = name;
        session.setPresence(ChatSession.PLAYER_CONNECT_PREFIX + name);
        scheduleSync();
    }

    private void onPcRoomClosed() {
        pcRoom = null;
        if (Vars.state.isMenu()) {
            session.setPresence(ChatSession.MENU_STATE);
        } else {
            session.setPresence(session.currentLastNonMenu());
        }
        scheduleSync();
    }

    private void applyValue(String value) {
        session.setLastNonMenu(value);
        session.setPresence(effectiveForValue(value, pcRoom));
        scheduleSync();
    }

    private void applyMenu() {
        session.setLastNonMenu(stashForMenu(session.currentPresence(), session.currentLastNonMenu(), pcRoom != null));
        session.setPresence(ChatSession.MENU_STATE);
        scheduleSync();
    }

    private void applyRestore() {
        session.setPresence(restoreForPlaying(session.currentLastNonMenu(), pcRoom));
        scheduleSync();
    }

    private void scheduleSync() {
        cancelPendingSync();
        pendingSync = Timer.schedule(this::flush, DEBOUNCE_SECONDS);
    }

    private void cancelPendingSync() {
        if (pendingSync != null) {
            pendingSync.cancel();
            pendingSync = null;
        }
    }

    private void flush() {
        pendingSync = null;
        attemptSend(false);
    }

    private void forceSync() {
        cancelPendingSync();
        attemptSend(true);
    }

    private void heartbeat() {
        attemptSend(true);
    }

    private void attemptSend(boolean force) {
        if (Vars.headless || !isEligible(session.isLoggedIn(), isOptedIn())) {
            return;
        }
        String candidate = session.currentPresence();
        if (!shouldSend(candidate, lastSent, dirty, force)) {
            return;
        }
        lastSent = candidate;
        dirty = false;
        try {
            publisher.apply(candidate).exceptionally(e -> {
                Log.err("Failed to update chat presence", e);
                Core.app.post(() -> dirty = true);
                return null;
            });
        } catch (Exception e) {
            dirty = true;
            Log.err("Failed to update chat presence", e);
        }
    }

    private boolean isOptedIn() {
        return Boolean.TRUE.equals(sharePresence.get());
    }

    static String stashForMenu(String presence, String lastNonMenu, boolean pcOpen) {
        return pcOpen || ChatSession.MENU_STATE.equals(presence) ? lastNonMenu : presence;
    }

    static String restoreForPlaying(String lastNonMenu, @Nullable String pcRoom) {
        return pcRoom != null && !pcRoom.trim().isEmpty()
                ? ChatSession.PLAYER_CONNECT_PREFIX + pcRoom
                : lastNonMenu;
    }

    static String effectiveForValue(String value, @Nullable String pcRoom) {
        return pcRoom != null && !pcRoom.trim().isEmpty()
                ? ChatSession.PLAYER_CONNECT_PREFIX + pcRoom
                : value;
    }

    static boolean isEligible(boolean loggedIn, boolean optIn) {
        return loggedIn && optIn;
    }

    static boolean shouldSend(String candidate, String lastSent, boolean dirty, boolean force) {
        return force || dirty || !candidate.equals(lastSent);
    }

    static boolean isStalePing(long callbackGeneration, long currentGeneration) {
        return callbackGeneration != currentGeneration;
    }
}
