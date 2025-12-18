package mindustrytool.plugins.playerconnect;

import arc.Core;
import arc.Events;
import arc.struct.*;
import arc.util.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.scene.*;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.content.UnitTypes;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.net.Administration.PlayerAction;

/** Dialog for creating and managing Player Connect rooms. */
public class CreateRoomDialog extends BaseDialog {
    public static CreateRoomDialog instance;

    private PlayerConnectLink link;
    private ServerHost selected;
    private ServerSelectDialog selectDialog;
    private TextButton serverSelectBtn;

    // Config values
    private String confName, confDesc, confPin = "";
    private int confMaxPlayers;
    private boolean confApproval, confPassword, confAutoHost;

    // UI References
    private TextField pinField;

    // Approval Logic
    private ObjectSet<String> approvedUUIDs = new ObjectSet<>();

    public CreateRoomDialog() {
        super("@message.manage-room.create-room");
        instance = this;

        loadConfig();

        addCloseButton();
        setupButtons();

        selectDialog = new ServerSelectDialog((host, btn) -> {
            this.selected = host;
            updateServerBtn();
        });

        setupUI();

        // Global Action Filter
        Vars.netServer.admins.addActionFilter(this::allowAction);

        // Events
        Events.on(PlayerJoin.class, e -> {
            if (!active() || !confApproval)
                return;
            if (approvedUUIDs.contains(e.player.uuid())) {
                e.player.team(Team.sharded);
                return;
            }
            handlePending(e.player);
        });

        // Periodic tasks
        Timer.schedule(() -> {
            if (!active() || !confApproval)
                return;
            for (Player p : Groups.player) {
                if (p == Vars.player || p.con == null || approvedUUIDs.contains(p.uuid()))
                    continue;

                Call.setHudText(p.con, "[scarlet]Waiting for approval...\n[lightgray]You are in spectator mode.");
                if (p.team() != Team.derelict)
                    p.team(Team.derelict);
                if (p.unit() != null && !p.unit().spawnedByCore)
                    p.unit().kill();
            }
        }, 0f, 1f);

        // Inject UI into Player List/Admin Dialogs
        Timer.schedule(() -> {
            if (!active() || !confApproval)
                return;
            // 1. List Injection
            if (Vars.ui.listfrag.content.hasParent())
                updatePlayerListUI();
            // 2. Dialog Injection
            if (Core.scene != null && Core.scene.root != null)
                injectAdminDialogs();
        }, 0f, 0.2f);

        shown(this::autoSelectServer);
    }

    public void triggerAutoHost() {
        loadConfig();
        if (!confAutoHost)
            return;
        if (active())
            return;

        // Prevent auto-host if we are already connected (Client or Server)
        if (Vars.net.active())
            return;

        Vars.ui.hudfrag.showToast("Auto-host: Connecting to Player Network...");

        if (selected != null) {
            performCreateRoom(true);
        } else {
            // Need to select server
            PlayerConnectProviders.refreshOnline(() -> {
                final ServerHost[] best = { null };
                final int[] bestPing = { 99999 };
                final int[] processed = { 0 };
                ArrayMap<String, String> servers = PlayerConnectProviders.online;

                if (servers.size == 0) {
                    Vars.ui.hudfrag.showToast("Auto-host: No proxy servers found.");
                    return;
                }

                for (int i = 0; i < servers.size; i++) {
                    ServerHost h = new ServerHost();
                    h.name = servers.getKeyAt(i);
                    h.set(servers.getValueAt(i));

                    PlayerConnect.pingHost(h.ip, h.port, ms -> {
                        if ((int) (long) ms < bestPing[0]) {
                            bestPing[0] = (int) (long) ms;
                            best[0] = h;
                        }
                        processed[0]++;
                        if (processed[0] >= servers.size) {
                            this.selected = best[0];
                            if (this.selected != null)
                                performCreateRoom(true);
                        }
                    }, e -> {
                        processed[0]++;
                        if (processed[0] >= servers.size) {
                            this.selected = best[0];
                            if (this.selected != null)
                                performCreateRoom(true);
                        }
                    });
                }
            }, e -> Vars.ui.hudfrag.showToast("Auto-host failed: Could not fetch servers."));
        }
    }

    private void performCreateRoom(boolean headless) {
        approvedUUIDs.clear();
        approvedUUIDs.add(Vars.player.uuid());
        StatsUpdater.overrideName = confName;

        if (Vars.net.client())
            return; // Client cannot host

        // Ensure server is running
        if (!Vars.net.server()) {
            try {
                Vars.net.host(Vars.port);
                Vars.netServer.admins.setPlayerLimit(confMaxPlayers);
            } catch (Exception e) {
                if (!headless)
                    Vars.ui.showException(e);
                return;
            }
        } else {
            Vars.netServer.admins.setPlayerLimit(confMaxPlayers);
        }

        String finalPwd = confPassword ? confPin.trim() : "";

        if (!headless)
            Vars.ui.loadfrag.show("@message.manage-room.create-room");

        Timer.Task t = Timer.schedule(PlayerConnect::closeRoom, 10);
        PlayerConnect.createRoom(selected.ip, selected.port, finalPwd, l -> {
            if (!headless)
                Vars.ui.loadfrag.hide();
            else
                Vars.ui.hudfrag.showToast("Room Created: " + l.toString());
            t.cancel();
            link = l;
        },
                e -> {
                    if (!headless)
                        Vars.net.handleException(e);
                    else
                        Vars.ui.hudfrag.showToast("Create Room Error: " + e.getMessage());
                    t.cancel();
                },
                r -> {
                    if (!headless)
                        Vars.ui.loadfrag.hide();
                    t.cancel();
                    if (r != null) {
                        String msg = "@message.room." + Strings.camelToKebab(r.name());
                        if (!headless)
                            Vars.ui.showText("", msg);
                        else
                            Vars.ui.hudfrag.showToast(Core.bundle.get(msg, msg));
                    } else if (link == null) {
                        if (!headless)
                            Vars.ui.showErrorMessage("@message.manage-room.create-room.failed");
                    }
                });
    }

    // ... UI Injection & Admin ...
    private void injectAdminDialogs() {
        for (Element e : Core.scene.root.getChildren()) {
            if (!(e instanceof BaseDialog))
                continue;
            BaseDialog dialog = (BaseDialog) e;

            for (Player p : Groups.player) {
                if (p == Vars.player || approvedUUIDs.contains(p.uuid()))
                    continue;

                boolean hasName = dialog.find(l -> l instanceof Label && Strings
                        .stripColors(((Label) l).getText().toString()).equals(Strings.stripColors(p.name))) != null;

                if (hasName) {
                    boolean isPlayerMenu = dialog.find(b -> b instanceof TextButton &&
                            (((TextButton) b).getText().toString().equalsIgnoreCase("Kick")
                                    || ((TextButton) b).getText().toString().equalsIgnoreCase("Ban"))) != null;

                    if (isPlayerMenu && dialog.find("approve-action") == null) {
                        TextButton refBtn = dialog.find(b -> b instanceof TextButton
                                && ((TextButton) b).getText().toString().equalsIgnoreCase("Kick"));
                        if (refBtn != null && refBtn.parent instanceof Table) {
                            Table btnTable = (Table) refBtn.parent;
                            btnTable.button("Approve", Icon.ok, () -> {
                                approve(p);
                                dialog.hide();
                            }).name("approve-action").growX().height(50f).row();
                        }
                    }
                }
            }
        }
    }

    // ... active check ...
    private boolean active() {
        return link != null && !PlayerConnect.isRoomClosed();
    }

    // ... allowAction, handlePending, updatePlayerListUI ...
    private boolean allowAction(PlayerAction action) {
        if (!active() || !confApproval)
            return true;
        if (action.player == null)
            return true;
        if (action.player == Vars.player)
            return true;
        if (!approvedUUIDs.contains(action.player.uuid()))
            return false;
        return true;
    }

    private void handlePending(Player p) {
        p.team(Team.derelict);
        Call.sendMessage("[accent]" + p.name + " [white]requests to join.");
    }

    private void updatePlayerListUI() {
        Table content = Vars.ui.listfrag.content;
        if (content == null)
            return;

        SnapshotSeq<Element> children = content.getChildren();
        for (Element child : children) {
            if (!(child instanceof Table))
                continue;
            Table row = (Table) child;

            Label nameLabel = row.find(e -> e instanceof Label);
            if (nameLabel == null)
                continue;
            String rawName = Strings.stripColors(nameLabel.getText().toString());

            Player target = Groups.player.find(p -> Strings.stripColors(p.name).equals(rawName));
            if (target != null && target != Vars.player && !approvedUUIDs.contains(target.uuid())) {
                if (row.find("approve-btn") != null)
                    continue;
                row.button(Icon.ok, Styles.clearNonei, () -> {
                    approve(target);
                    Vars.ui.listfrag.rebuild();
                }).name("approve-btn").size(45f).padLeft(5f);
            }
        }
    }

    // ... Config ...
    private void loadConfig() {
        confName = Core.settings.getString("pc-room-name", Vars.player.name);
        confDesc = Core.settings.getString("pc-room-desc", "A Mindustry Server");
        confMaxPlayers = Core.settings.getInt("pc-room-max", 0);
        confApproval = Core.settings.getBool("pc-room-approval", false);
        confPassword = Core.settings.getBool("pc-room-pwd-enabled", false);
        confPin = Core.settings.getString("pc-room-pin", "");
        confAutoHost = Core.settings.getBool("pc-auto-host", false);
    }

    private void saveConfig() {
        Core.settings.put("pc-room-name", confName);
        Core.settings.put("pc-room-desc", confDesc);
        Core.settings.put("pc-room-max", confMaxPlayers);
        Core.settings.put("pc-room-approval", confApproval);
        Core.settings.put("pc-room-pwd-enabled", confPassword);
        Core.settings.put("pc-room-pin", confPin);
        Core.settings.put("pc-auto-host", confAutoHost);
    }

    private void setupButtons() {
        buttons.button("@message.manage-room.create-room", Icon.play, this::createRoom)
                .disabled(b -> !PlayerConnect.isRoomClosed() || Vars.net.client()); // Disabled if Room Open OR Client
        if (Vars.mobile)
            buttons.row();
        buttons.button("@message.manage-room.close-room", Icon.cancel, this::closeRoom)
                .disabled(b -> PlayerConnect.isRoomClosed());
        buttons.button("@message.manage-room.copy-link", Icon.copy, this::copyLink).disabled(b -> link == null);
    }

    private void setupUI() {
        cont.clear();
        cont.pane(t -> {
            t.top();
            // Server Settings
            t.table(s -> {
                s.top().left();
                s.defaults().left().padBottom(5);
                s.add("@message.manage-room.server-name").color(Vars.player.color).row();
                s.field(confName, val -> confName = val).growX().valid(x -> x.length() > 0).row();
                s.add("@message.manage-room.server-desc").color(Vars.player.color).row();
                s.area(confDesc, Styles.areaField, val -> confDesc = val).growX().height(80f).row();
                s.table(sub -> {
                    sub.add("@message.manage-room.max-players");
                    sub.field(String.valueOf(confMaxPlayers == 0 ? "" : confMaxPlayers), val -> {
                        if (val.isEmpty())
                            confMaxPlayers = 0;
                        else if (Strings.canParseInt(val))
                            confMaxPlayers = Integer.parseInt(val);
                    }).width(100f).valid(Strings::canParsePositiveInt);
                    sub.add(" (Empty = \u221E)").color(arc.graphics.Color.gray).get().setFontScale(0.8f);
                }).row();
                // Auto Host Checkbox
                s.check("Auto host when opening map", confAutoHost, b -> {
                    confAutoHost = b;
                    Core.settings.put("pc-auto-host", b);
                }).row();
                s.label(() -> confAutoHost ? "[lightgray](Automatically starts server on map load)" : "").padLeft(20f)
                        .get().setFontScale(0.8f);
                s.row();
                s.check("@message.manage-room.require-approval", confApproval, b -> confApproval = b).row();
                s.label(() -> confApproval ? "[lightgray](Spectator until approved)" : "").padLeft(20f).get()
                        .setFontScale(0.8f);
                s.row();
                // Password
                s.check("@message.password", confPassword, b -> {
                    confPassword = b;
                    setupUI();
                }).row();
                if (confPassword) {
                    s.table(p -> {
                        p.left();
                        p.add("PIN (4-6): ");
                        Cell<TextField> cf = p.field(confPin, val -> confPin = val).width(150f);
                        pinField = cf.get();
                        pinField.setFilter(TextField.TextFieldFilter.digitsOnly);
                        pinField.setMaxLength(6);
                        cf.valid(x -> x.length() >= 4 && x.length() <= 6);
                    }).padLeft(20f).row();
                } else {
                    pinField = null;
                }
            }).growX().pad(10f).top();

            t.row();
            t.image().color(arc.graphics.Color.gray).growX().height(2f).pad(10f).row();

            // Server Selection
            t.table(server -> {
                server.top().left();
                server.add("Proxy Server:").pad(5).growX().left().row();
                serverSelectBtn = server.button("Select Server", Icon.host, () -> selectDialog.show()).growX()
                        .height(50f).get();
                updateServerBtn();
            }).growX().pad(10f);

        }).grow();
    }

    private void updateServerBtn() {
        if (serverSelectBtn == null)
            return;
        if (selected == null) {
            serverSelectBtn.setText("Select Server\n[lightgray](Auto: Best Ping)");
        } else {
            PlayerConnect.pingHost(selected.ip, selected.port,
                    ms -> serverSelectBtn.setText(selected.name + " (" + ms + "ms)"),
                    e -> serverSelectBtn.setText(selected.name + " [red](Offline)"));
            serverSelectBtn.setText(selected.name + " [lightgray](Checking...)");
        }
    }

    private void autoSelectServer() {
        if (selected != null)
            return;
        if (serverSelectBtn != null)
            serverSelectBtn.setText("Scanning Servers...");

        PlayerConnectProviders.refreshOnline(() -> {
            final ServerHost[] best = { null };
            final int[] bestPing = { 99999 };
            final int[] processed = { 0 };
            ArrayMap<String, String> servers = PlayerConnectProviders.online;
            if (servers.size == 0) {
                if (serverSelectBtn != null)
                    serverSelectBtn.setText("No Public Servers");
                return;
            }
            for (int i = 0; i < servers.size; i++) {
                String name = servers.getKeyAt(i);
                String ip = servers.getValueAt(i);
                ServerHost h = new ServerHost();
                h.name = name;
                h.set(ip);
                PlayerConnect.pingHost(h.ip, h.port, ms -> {
                    if ((int) (long) ms < bestPing[0]) {
                        bestPing[0] = (int) (long) ms;
                        best[0] = h;
                    }
                    processed[0]++;
                    if (processed[0] >= servers.size)
                        finishAutoSelect(best[0], bestPing[0]);
                }, e -> {
                    processed[0]++;
                    if (processed[0] >= servers.size)
                        finishAutoSelect(best[0], bestPing[0]);
                });
            }
        }, e -> {
            if (serverSelectBtn != null)
                serverSelectBtn.setText("Failed to load servers");
        });
    }

    private void finishAutoSelect(ServerHost best, int ms) {
        if (best != null) {
            this.selected = best;
            if (serverSelectBtn != null)
                serverSelectBtn.setText(best.name + " (" + ms + "ms)");
        } else {
            if (serverSelectBtn != null)
                serverSelectBtn.setText("Select Server (Auto failed)");
        }
    }

    private boolean validateSettings() {
        if (selected == null) {
            Vars.ui.showInfo("Error: Please select a Proxy Server first.");
            return false;
        }
        if (confPassword && pinField != null) {
            String p = pinField.getText();
            if (p == null)
                p = "";
            p = p.trim();
            confPin = p;
            if (p.length() < 4 || p.length() > 6) {
                Vars.ui.showInfo("Error: PIN must be between 4 and 6 digits.\nCurrent length: " + p.length());
                return false;
            }
        }
        return true;
    }

    void approve(Player p) {
        approvedUUIDs.add(p.uuid());
        p.team(Team.sharded);
        Call.setHudText(p.con, "");
        Call.sendMessage(p.name + " [lime]has been approved to join!");
        p.unit(UnitTypes.dagger.create(p.team()));
        if (Core.camera != null)
            p.unit().set(Core.camera.position.x, Core.camera.position.y);
        p.unit().add();
    }

    private void createRoom() {
        if (!validateSettings())
            return;
        saveConfig();
        performCreateRoom(false);
    }

    public void closeRoom() {
        PlayerConnect.closeRoom();
        link = null;
        approvedUUIDs.clear();
    }

    public void copyLink() {
        if (link == null)
            return;
        Core.app.setClipboardText(link.toString());
        Vars.ui.showInfoFade("@copied");
    }
}
