package mindustrytool.features.chat.global;

import java.util.concurrent.atomic.AtomicBoolean;

import arc.Core;
import arc.Events;
import arc.util.Log;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.core.GameState.State;
import mindustry.game.EventType.ClientServerConnectEvent;
import mindustry.game.EventType.StateChangeEvent;
import mindustry.game.EventType.WorldLoadEndEvent;
import mindustrytool.features.auth.AuthService;
import mindustrytool.features.playerconnect.PlayerConnectConfig;
import mindustrytool.features.playerconnect.PlayerConnectRoomConnected;
import mindustrytool.features.playerconnect.RoomCreatedEvent;
import mindustrytool.services.PlayerConnectService;

public class ChatStateManager {
    private static final String MENU_STATE = "menu";
    private static final String SERVER_PREFIX = "server: ";
    private static final String PLAYER_CONNECT_PREFIX = "player-connect: ";
    private static final String CAMPAIGN_PREFIX = "campaign: ";
    private static final String CUSTOM_GAME_STATE = "custom-game";

    private final ChatApiClient apiClient;
    private final Runnable refreshCurrentChannelUsers;
    private final AtomicBoolean isSyncing = new AtomicBoolean(false);

    private String currentState = "";
    private String syncedState = "";

    public ChatStateManager(ChatApiClient apiClient, Runnable refreshCurrentChannelUsers) {
        this.apiClient = apiClient;
        this.refreshCurrentChannelUsers = refreshCurrentChannelUsers;
    }

    public void init() {
        registerStateListeners();
        Timer.schedule(this::refreshCurrentState, 5, 5);
    }

    public void updateState(String state) {
        if (state == null || state.isEmpty()) {
            return;
        }

        currentState = state;
        syncState(false);
    }

    public void refreshCurrentState() {
        syncState(true);
    }

    private void registerStateListeners() {
        Events.on(ClientServerConnectEvent.class, event -> handleServerConnect(event));
        Events.on(StateChangeEvent.class, event -> handleStateChange(event));
        Events.on(PlayerConnectRoomConnected.class, event -> handleRoomConnected(event));
        Events.on(RoomCreatedEvent.class, event -> updateState(PLAYER_CONNECT_PREFIX + PlayerConnectConfig.getRoomName()));
        Events.on(WorldLoadEndEvent.class, event -> handleWorldLoaded());
    }

    private void handleServerConnect(ClientServerConnectEvent event) {
        Vars.net.pingHost(event.ip, event.port, result -> {
            if (result != null) {
                updateState(SERVER_PREFIX + result.name);
            }
        }, e -> Log.err("Failed to ping host", e));
    }

    private void handleStateChange(StateChangeEvent event) {
        if (event.to == State.menu && !Core.graphics.isHidden()) {
            updateState(MENU_STATE);
        }
    }

    private void handleRoomConnected(PlayerConnectRoomConnected event) {
        PlayerConnectService.getInstance().getRoomWithCache(event.link.toString())
                .thenAccept(room -> {
                    if (room != null) {
                        updateState(PLAYER_CONNECT_PREFIX + room.getData().getName());
                    }
                })
                .exceptionally(e -> {
                    Log.err("Failed to get player-connect room", e);
                    return null;
                });
    }

    private void handleWorldLoaded() {
        try {
            if (Vars.net.client()) {
                return;
            }

            if (Vars.state.isCampaign()) {
                updateState(CAMPAIGN_PREFIX + Vars.state.map.name());
                return;
            }

            updateState(CUSTOM_GAME_STATE);
        } catch (Exception e) {
            Log.err("Failed to handle state change", e);
            Vars.ui.showInfoFade(e.getMessage());
        }
    }

    private void syncState(boolean force) {
        if (currentState.isEmpty() || !AuthService.getInstance().isLoggedIn() || !ChatConfig.status()) {
            return;
        }

        if (!force && currentState.equals(syncedState)) {
            return;
        }

        if (!isSyncing.compareAndSet(false, true)) {
            return;
        }

        String stateToSync = currentState;

        apiClient.updateState(stateToSync)
                .thenRun(() -> {
                    syncedState = stateToSync;
                    refreshCurrentChannelUsers.run();
                })
                .exceptionally(e -> {
                    Log.err("Failed to update chat state", e);
                    return null;
                })
                .whenComplete((ignored, throwable) -> isSyncing.set(false));
    }
}
