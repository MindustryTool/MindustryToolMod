package mindustrytool.features.playerconnect;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.func.Prov;
import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.ArrayMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Threads;
import arc.util.Timer;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CompletableFuture;
import mindustry.Vars;
import mindustry.game.EventType.ClientServerConnectEvent;
import solim.reactive.Query;
import solim.reactive.QueryKey;
import mindustry.game.EventType.ConnectionEvent;
import mindustry.game.EventType.HostEvent;
import mindustry.game.EventType.PlayerIpBanEvent;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.PlayerLeave;
import mindustry.game.EventType.WorldLoadEndEvent;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Icon;
import mindustry.gen.Player;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.playerconnect.models.HostingState;
import mindustrytool.features.playerconnect.models.JoinRequest;
import mindustrytool.features.playerconnect.net.NetworkProxy;
import arc.scene.ui.layout.Scl;
import arc.util.Reflect;
import mindustry.ui.IntFormat;
import mindustry.ui.Styles;
import mindustrytool.features.playerconnect.net.PlayerConnectClient;
import mindustrytool.features.playerconnect.net.PlayerConnectLink;
import mindustrytool.features.playerconnect.net.Packets.RoomCloseReason;
import mindustrytool.features.playerconnect.ui.HostRoomDialog;
import mindustrytool.features.playerconnect.ui.JoinApprovalHudView;
import mindustrytool.features.playerconnect.ui.JoinDialogInjector;
import mindustrytool.features.playerconnect.ui.ManageRoomDialog;
import mindustrytool.models.response.PlayerConnectProvider;
import mindustrytool.models.response.PlayerConnectRoom;
import mindustrytool.models.response.PlayerConnectRoomsResponse;
import mindustrytool.services.MindustryTool;
import mindustrytool.utils.JsonUtils;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;

import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.Signal;
import solim.reactive.Readable;

public class PlayerConnectFeature extends Feature {

    public static final String CUSTOM_PROVIDERS_KEY = "mindustrytool.player-connect.custom-providers";
    public static final String PLAYER_CONNECT_PROTOCOL = "player-connect://";
    private static final String PAUSE_BUTTON_NAME = "pc-pause-button";

    public final ConfigGroup config;
    public final ConfigValue<String> roomNameConfig;
    public final ConfigValue<String> passwordConfig;
    public final ConfigValue<Integer> maxPlayerConfig;
    public final ConfigValue<Boolean> autoAcceptConfig;

    private final Signal<HostingState> state = Signal.of(HostingState.IDLE);
    private final Signal<Integer> ping = Signal.of(0);
    private final Query<List<PlayerConnectRoom>> roomsQuery = Query.of(
            QueryKey.of("playerconnect", "rooms"),
            () -> MindustryTool.getPlayerConnectRooms("")
    ).staleTime(Duration.ofSeconds(15));
    private final Signal<List<PlayerConnectProvider>> providers = Signal.of(Collections.emptyList());
    private final Signal<JoinRequest> currentRequest = Signal.of(null);

    private final Deque<JoinRequest> pendingQueue = new ArrayDeque<>();
    private final ExecutorService worker = Threads.unboundedExecutor("PlayerConnect-Worker", 1);

    private @Nullable NetworkProxy activeProxy;
    private @Nullable Thread proxyThread;
    private @Nullable PlayerConnectLink activeLink;

    private @Nullable CompletableFuture<Void> sseRequest;
    private @Nullable Timer.Task sseReconnectTask;
    private @Nullable Timer.Task statsUpdateTask;

    private @Nullable HostRoomDialog hostDialog;
    private @Nullable ManageRoomDialog manageDialog;
    private @Nullable JoinApprovalHudView approvalHud;
    private @Nullable JoinDialogInjector joinInjector;

    public PlayerConnectFeature() {
        super(FeatureMetadata.builder()
                .id("player-connect")
                .icon(Icon.planet)
                .order(3)
                .enabledByDefault(true)
                .build());

        config = configGroup();
        roomNameConfig = config.stringValue("roomName", Vars.player != null ? Vars.player.name : "Mindustry Room");
        passwordConfig = config.stringValue("password", "");
        maxPlayerConfig = config.intValue("maxPlayers", Vars.headless ? 30 : 0);
        autoAcceptConfig = config.boolValue("autoAccept", true);

        registerEventListeners();
    }

    public Readable<List<PlayerConnectRoom>> getRooms() {
        return roomsQuery.data().map(list -> list != null ? list : Collections.emptyList());
    }

    public Query<List<PlayerConnectRoom>> getRoomsQuery() {
        return roomsQuery;
    }

    private void registerEventListeners() {
        Events.run(HostEvent.class, this::onHostEvent);
        Events.run(WorldLoadEndEvent.class, this::updateRoomStats);
        Events.run(PlayerJoin.class, this::updateRoomStats);
        Events.run(PlayerLeave.class, this::updateRoomStats);

        Events.on(PlayerJoin.class, this::onPlayerJoin);
        Events.on(PlayerLeave.class, event -> {
            removeRequestForPlayer(event.player);
            processNextRequest();
        });

        Events.run(ConnectionEvent.class, () -> {
            if (isHosting() && activeProxy != null) {
                PlayerConnectClient.unbanProxyIp(activeProxy);
            }
        });

        Events.on(PlayerIpBanEvent.class, event -> {
            if (isHosting() && activeProxy != null) {
                PlayerConnectClient.unbanProxyIp(activeProxy);
            }
        });

        Events.on(ClientServerConnectEvent.class, event -> {
            if (isHosting()) {
                closeRoom();
                Vars.ui.showInfoFade("@feature.player-connect.auto-closed-on-client");
            }
        });

        Events.run(HostEvent.class, () -> {
            if (enabled().peek()) {
                showHostDialog();
            }
        });
    }

    @Override
    public void onEnable() {
        refreshProviders();
        startSseSync();

        Core.app.post(() -> {
            if (approvalHud == null) {
                approvalHud = new JoinApprovalHudView(this);
                Vars.ui.hudGroup.addChild(approvalHud.element());
            }
            setupPingLabel();
            injectPauseMenuButton();

            if (joinInjector == null) {
                joinInjector = new JoinDialogInjector(this);
            }
            joinInjector.inject();
        });

        statsUpdateTask = Timer.schedule(this::updateRoomStats, 30f, 30f);
    }

    @Override
    public void onDisable() {
        closeRoom();
        stopSseSync();

        if (statsUpdateTask != null) {
            statsUpdateTask.cancel();
            statsUpdateTask = null;
        }

        if (approvalHud != null) {
            approvalHud.element().remove();
            approvalHud.dispose();
            approvalHud = null;
        }

        if (Vars.ui != null && Vars.ui.hudGroup != null) {
            Element pcPing = Vars.ui.hudGroup.find("pc-ping");
            if (pcPing != null) {
                pcPing.remove();
            }
        }

        PlayerConnectClient.disposePinger();
        roomsQuery.dispose();
    }

    // ─── Signals & Properties ──────────────────────────────────────

    public Readable<Boolean> isFetching() {
        return roomsQuery.fetching();
    }

    public Signal<HostingState> stateSignal() {
        return state;
    }

    public Signal<Integer> pingSignal() {
        return ping;
    }

    public Readable<List<PlayerConnectRoom>> roomsSignal() {
        return getRooms();
    }

    public Signal<List<PlayerConnectProvider>> providersSignal() {
        return providers;
    }

    public Signal<JoinRequest> currentRequestSignal() {
        return currentRequest;
    }

    public Computed<Boolean> isHostingSignal() {
        return state.map(s -> s == HostingState.HOSTING);
    }

    public boolean isHosting() {
        return state.peek() == HostingState.HOSTING && activeProxy != null && activeProxy.isConnected();
    }

    public @Nullable PlayerConnectLink getActiveLink() {
        return activeLink;
    }

    // ─── Room Hosting & Management ─────────────────────────────────

    public void createRoom(String host, int port, Cons<PlayerConnectLink> onSuccess, Cons<Throwable> onError) {
        if (!Vars.net.server()) {
            onError.get(new IllegalStateException("You must host a game locally before publishing to relay."));
            return;
        }

        closeRoom();

        state.set(HostingState.CONNECTING);
        Vars.ui.loadfrag.show("@feature.player-connect.creating-room");

        activeProxy = new NetworkProxy();
        proxyThread = Threads.daemon("PlayerConnectProxy", activeProxy);

        String roomName = roomNameConfig.get();
        String password = passwordConfig.get();
        int maxPlayers = maxPlayerConfig.get();

        if (Vars.netServer != null && Vars.netServer.admins != null) {
            Vars.netServer.admins.setPlayerLimit(maxPlayers);
        }

        worker.submit(() -> {
            try {
                activeProxy.connect(
                        host, port,
                        roomName, password,
                        roomId -> Core.app.post(() -> {
                            Vars.ui.loadfrag.hide();
                            activeLink = new PlayerConnectLink(host, port, roomId);
                            state.set(HostingState.HOSTING);
                            PlayerConnectClient.unbanProxyIp(activeProxy);
                            Events.fire(new PcRoomOpened(roomNameConfig.get()));
                            onSuccess.get(activeLink);
                        }),
                        closeReason -> Core.app.post(() -> {
                            Vars.ui.loadfrag.hide();
                            closeRoom();
                            state.set(HostingState.IDLE);
                        }),
                        newPing -> Core.app.post(() -> ping.set(newPing)));
            } catch (Exception e) {
                Core.app.post(() -> {
                    Vars.ui.loadfrag.hide();
                    closeRoom();
                    state.set(HostingState.ERROR);
                    onError.get(e);
                });
            }
        });
    }

    public void closeRoom() {
        closeRoom(RoomCloseReason.closed);
    }

    public void closeRoom(RoomCloseReason reason) {
        boolean wasActive = activeProxy != null || activeLink != null;
        if (activeProxy != null) {
            activeProxy.closeRoom();
            activeProxy.stop();
            try {
                if (proxyThread != null) {
                    proxyThread.join(500);
                }
            } catch (Exception ignored) {
            }
            activeProxy.dispose();
            activeProxy = null;
            proxyThread = null;
        }
        activeLink = null;
        state.set(HostingState.IDLE);
        ping.set(0);
        clearPendingRequests();
        if (wasActive) {
            Events.fire(new PcRoomClosed());
        }
    }

    public void updateRoomStats() {
        if (isHosting() && activeProxy != null) {
            activeProxy.updateStats(roomNameConfig.get());
        }
    }

    private void onHostEvent() {
        if (isHosting()) {
            closeRoom();
        }
    }

    // ─── Join Request Moderation ───────────────────────────────────

    private void onPlayerJoin(PlayerJoin event) {
        if (!isHosting() || Boolean.TRUE.equals(autoAcceptConfig.get())) {
            return;
        }
        if (event.player == Vars.player) {
            return;
        }

        Team originalTeam = event.player.team();
        event.player.team(Team.derelict);
        if (event.player.unit() != null) {
            event.player.unit().kill();
        }

        Call.infoMessage(event.player.con(), Core.bundle.get("feature.player-connect.waiting-approval"));

        JoinRequest req = new JoinRequest(event.player, originalTeam);
        synchronized (pendingQueue) {
            pendingQueue.addLast(req);
        }
        processNextRequest();
    }

    public void accept(JoinRequest request) {
        if (request.player != null && request.player.con != null && request.player.con.isConnected()) {
            request.player.team(request.originalTeam);
        }
        removeAndAdvance(request);
    }

    public void reject(JoinRequest request) {
        if (request.player != null && request.player.con != null && request.player.con.isConnected()) {
            Call.infoMessage(request.player.con(), Core.bundle.get("feature.player-connect.rejected-message"));
            request.player.con.close();
        }
        removeAndAdvance(request);
    }

    private void removeAndAdvance(JoinRequest request) {
        synchronized (pendingQueue) {
            pendingQueue.remove(request);
        }
        processNextRequest();
    }

    private void removeRequestForPlayer(Player player) {
        synchronized (pendingQueue) {
            pendingQueue.removeIf(req -> req.player == player || req.player.uuid().equals(player.uuid()));
        }
    }

    private void clearPendingRequests() {
        synchronized (pendingQueue) {
            pendingQueue.clear();
        }
        currentRequest.set(null);
    }

    private void processNextRequest() {
        Core.app.post(() -> {
            synchronized (pendingQueue) {
                while (!pendingQueue.isEmpty()) {
                    JoinRequest next = pendingQueue.peekFirst();
                    if (next != null && next.player != null && next.player.con != null
                            && next.player.con.isConnected()) {
                        currentRequest.set(next);
                        return;
                    }
                    pendingQueue.pollFirst();
                }
                currentRequest.set(null);
            }
        });
    }

    // ─── Real-Time Room Directory & SSE ────────────────────────────

    private void startSseSync() {
        stopSseSync();

        fetchRoomsRest();

        CompletableFuture<Void> request = MindustryTool.playerConnectStream(this::handleSseLine);
        sseRequest = request;
        request.whenComplete((ignored, error) -> {
            if (sseRequest != request) {
                return;
            }
            if (error != null) {
                Log.err("PlayerConnect SSE stream error", error.getMessage());
            }
            scheduleSseReconnect();
        });
    }

    private void stopSseSync() {
        if (sseRequest != null) {
            CompletableFuture<Void> request = sseRequest;
            sseRequest = null;
            request.cancel(true);
        }
        if (sseReconnectTask != null) {
            sseReconnectTask.cancel();
            sseReconnectTask = null;
        }
    }

    private void handleSseLine(String item) {
        if (item == null || item.trim().isEmpty() || item.startsWith(":")) {
            return;
        }
        String json = item.trim();
        if (json.startsWith("data:")) {
            json = json.substring(5).trim();
        }
        try {
            PlayerConnectRoomsResponse response = JsonUtils.fromJson(PlayerConnectRoomsResponse.class, json);
            if (response != null && response.getRooms() != null) {
                Core.app.post(() -> roomsQuery.mutate(response.getRooms()));
                return;
            }
        } catch (Exception ignored) {
        }
        try {
            List<PlayerConnectRoom> list = JsonUtils.fromJsonArray(PlayerConnectRoom.class, json);
            if (list != null) {
                Core.app.post(() -> roomsQuery.mutate(list));
            }
        } catch (Exception e) {
            Log.debug("Failed to parse SSE room payload: @", e);
        }
    }

    private void scheduleSseReconnect() {
        if (sseReconnectTask == null && isEnabled()) {
            sseReconnectTask = Timer.schedule(() -> {
                sseReconnectTask = null;
                if (isEnabled()) {
                    startSseSync();
                }
            }, 5f);
        }
    }

    public void fetchRoomsRest() {
        roomsQuery.refetch();
    }

    // ─── Provider Management ───────────────────────────────────────

    public void refreshProviders() {
        MindustryTool.getPlayerConnectProviders()
                .thenAccept(apiProviders -> {
                    List<PlayerConnectProvider> all = new ArrayList<>();
                    if (apiProviders != null) {
                        all.addAll(apiProviders);
                    }
                    all.add(new PlayerConnectProvider("localhost", "LocalHost", "localhost:11010"));

                    List<PlayerConnectProvider> custom = loadCustomProviders();
                    all.addAll(custom);

                    Core.app.post(() -> providers.set(all));
                })
                .exceptionally(e -> {
                    List<PlayerConnectProvider> fallback = new ArrayList<>();
                    fallback.add(new PlayerConnectProvider("localhost", "LocalHost", "localhost:11010"));
                    fallback.addAll(loadCustomProviders());
                    Core.app.post(() -> providers.set(fallback));
                    return null;
                });
    }

    @SuppressWarnings("unchecked")
    public ArrayMap<String, String> getCustomProviders() {
        return Core.settings.getJson(
                CUSTOM_PROVIDERS_KEY, ArrayMap.class, String.class, ArrayMap::new);
    }

    public List<PlayerConnectProvider> loadCustomProviders() {
        try {
            ArrayMap<String, String> map = getCustomProviders();
            List<PlayerConnectProvider> list = new ArrayList<>();
            if (map != null) {
                for (int i = 0; i < map.size; i++) {
                    list.add(new PlayerConnectProvider("custom-" + i, map.getKeyAt(i), map.getValueAt(i)));
                }
            }
            return list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public void addCustomProvider(String name, String address) {
        ArrayMap<String, String> map = getCustomProviders();

        if (map == null) {
            map = new ArrayMap<>();
        }
        map.put(name, address);
        Core.settings.putJson(CUSTOM_PROVIDERS_KEY, String.class, map);
        refreshProviders();
    }

    public void removeCustomProvider(String name) {
        ArrayMap<String, String> map = getCustomProviders();

        if (map != null) {
            map.removeKey(name);
            Core.settings.putJson(CUSTOM_PROVIDERS_KEY, String.class, map);
            refreshProviders();
        }
    }

    // ─── Pause Menu Injection & HUD Ping ──────────────────────────

    private void setupPingLabel() {
        if (Vars.ui == null || Vars.ui.hudGroup == null) {
            return;
        }

        Table parent = Vars.ui.hudGroup.find("fps/ping");
        if (parent == null || parent.find("pc-ping") != null) {
            return;
        }

        IntFormat pingFormat = new IntFormat("ping");
        parent.label(() -> pingColor() + pingFormat.get(ping.get() != null ? ping.get() : 0))
                .visible(this::isHosting)
                .left()
                .style(Styles.outlineLabel)
                .name("pc-ping");
        parent.row();
    }

    private String pingColor() {
        Integer p = ping.get();
        if (p == null || p <= 200) {
            return "";
        }
        if (p <= 500) {
            return "[yellow]";
        }
        return "[scarlet]";
    }

    private void injectPauseMenuButton() {
        if (Vars.ui.paused == null) {
            return;
        }

        Vars.ui.paused.shown(this::ensurePauseMenuButton);
    }

    private void ensurePauseMenuButton() {
        if (Vars.ui.paused == null || Vars.ui.paused.cont == null) {
            return;
        }

        Table root = Vars.ui.paused.cont;
        if (root.find(PAUSE_BUTTON_NAME) != null) {
            return;
        }

        @SuppressWarnings("rawtypes")
        Seq<Cell> cells = root.getCells();
        if (cells.isEmpty()) {
            return;
        }

        Computed<String> btnText = state.map(s -> s == HostingState.HOSTING
                ? Core.bundle.get("feature.player-connect.manage-room", "Manage Room")
                : Core.bundle.get("feature.player-connect.host-room", "Host Room"));

        boolean hasColspan2 = cells.size >= 2
                && Reflect.<Integer>get(cells.get(cells.size - 2), "colspan") == 2;
        float btnWidth = Math.min(Core.graphics.getWidth() / Scl.scl() * 0.9f, 450f);

        Cell<?> addedCell;
        root.row();
        if (Vars.mobile) {
            addedCell = root.buttonRow(btnText.get(), Icon.planet, this::onPauseMenuButtonClicked)
                    .update(btn -> {
                        btn.name = PAUSE_BUTTON_NAME;
                        btn.setText(btnText.get());
                    })
                    .disabled(b -> Vars.net.client());
        } else if (hasColspan2) {
            addedCell = root.button(btnText.get(), Icon.planet, this::onPauseMenuButtonClicked)
                    .colspan(2)
                    .width(btnWidth)
                    .update(btn -> {
                        btn.name = PAUSE_BUTTON_NAME;
                        btn.setText(btnText.get());
                    })
                    .disabled(b -> Vars.net.client());
        } else {
            addedCell = root.button(btnText.get(), Icon.planet, this::onPauseMenuButtonClicked)
                    .update(btn -> {
                        btn.name = PAUSE_BUTTON_NAME;
                        btn.setText(btnText.get());
                    })
                    .disabled(b -> Vars.net.client());
        }
        root.row();

        if (addedCell != null && addedCell.get() != null) {
            addedCell.get().name = PAUSE_BUTTON_NAME;
        }

        // Swap with quit button if present
        if (cells.size >= 2) {
            cells.swap(cells.size - 1, cells.size - 2);
        }
    }

    private void onPauseMenuButtonClicked() {
        if (isHosting()) {
            showManageDialog();
        } else if (Vars.net.server()) {
            showHostDialog();
        } else {
            Vars.ui.host.show();
            Vars.ui.host.hidden(() -> {
                if (Vars.net.server()) {
                    showHostDialog();
                }
            });
        }
    }

    public void showHostDialog() {
        if (hostDialog == null) {
            hostDialog = new HostRoomDialog(this);
        }
        hostDialog.show();
    }

    public void showManageDialog() {
        if (manageDialog == null) {
            manageDialog = new ManageRoomDialog(this);
        }
        manageDialog.show();
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return null;
    }
}
