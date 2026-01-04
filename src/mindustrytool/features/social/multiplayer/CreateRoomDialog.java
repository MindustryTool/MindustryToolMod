package mindustrytool.features.social.multiplayer;

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
import mindustry.graphics.Pal;
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
    // Config values
    private String confName, confDesc, confPin = "", confLogoPath = "";
    private int confMaxPlayers;
    private boolean confApproval, confPassword, confAutoHost, confAllowFriends;

    // State
    private boolean connecting = false;
    private arc.graphics.Texture logoTexture;

    // UI References
    private TextField pinField;

    // Approval Logic
    private ObjectSet<String> approvedUUIDs = new ObjectSet<>();

    public CreateRoomDialog() {
        super("Player Connect Settings");
        instance = this;

        loadConfig();
        addCloseListener();

        // Buttons
        buttons.defaults().size(210f, 64f).pad(2);
        buttons.button("@back", Icon.left, this::hide);
        buttons.button("Reset to defaults", Icon.refresh, this::resetDefaults).size(250f, 64f);
        // setupButtons();

        selectDialog = new ServerSelectDialog((host, btn) -> {
            this.selected = host;
            updateServerBtn();
        });

        // Ensure texture is cleaned up
        hidden(() -> {
            saveConfig();
            if (logoTexture != null) {
                logoTexture.dispose();
                logoTexture = null;
            }
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
            if (confAllowFriends) {
                // Placeholder for logic
            }
            handlePending(e.player);
        });

        // shown(this::autoSelectServer); // Moved to end of constructor in original
        // file, keeping it here if needed or relying on original position

        // Periodic tasks for logic (keep low frequency for expensive ops like scene
        // scan)
        Timer.schedule(() -> {
            if (!active() || !confApproval)
                return;

            // 1. Enforce Spectator Mode for unapproved players
            for (Player p : Groups.player) {
                if (p == Vars.player || p.con == null || approvedUUIDs.contains(p.uuid()))
                    continue;

                if (p.team() != Team.derelict)
                    p.team(Team.derelict);
                if (p.unit() != null && !p.unit().spawnedByCore)
                    p.unit().kill();
            }

            // 2. Dialog Injection (keep this on timer as scanning scene is expensive)
            if (Core.scene != null && Core.scene.root != null)
                injectAdminDialogs();
        }, 0f, 0.2f);

        // Per-frame UI update for smooth buttons (flicker fix)
        Events.run(Trigger.update, () -> {
            if (!active() || !confApproval)
                return;

            // Only update if player list is actually visible
            if (Vars.ui.listfrag.content.hasParent()) {
                updatePlayerListUI();
            }
        });

        shown(() -> {
            setupUI();
            autoSelectServer();
        });
    }

    public void triggerAutoHost() {
        // Fix: Check setting directly to avoid overwriting current UI state with
        // loadConfig()
        // unless auto-host is actually enabled.
        if (!Core.settings.getBool("pc-auto-host", false))
            return;

        loadConfig();

        // Validation Check: If data is missing, skip auto-host but DON'T disable it
        if (confName == null || confName.trim().isEmpty() || confDesc == null || confDesc.trim().isEmpty()) {
            Vars.ui.hudfrag.showToast("Auto-Host skipped: Name/Description missing.");
            return;
        }

        if (active() || connecting)
            return;

        // Prevent auto-host if we are already connected (Client or Server)
        if (Vars.net.active())
            return;

        connecting = true;
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
                    connecting = false;
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
                            else
                                connecting = false;
                        }
                    }, e -> {
                        processed[0]++;
                        if (processed[0] >= servers.size) {
                            this.selected = best[0];
                            if (this.selected != null)
                                performCreateRoom(true);
                            else
                                connecting = false;
                        }
                    });
                }
            }, e -> {
                Vars.ui.hudfrag.showToast("Auto-host failed: Could not fetch servers.");
                connecting = false;
            });
        }
    }

    private void performCreateRoom(boolean headless) {
        approvedUUIDs.clear();
        approvedUUIDs.add(Vars.player.uuid());
        StatsUpdater.overrideName = confName;

        if (Vars.net.client()) {
            connecting = false;
            return;
        }

        // Ensure server is running
        if (!Vars.net.server()) {
            try {
                Vars.net.host(Vars.port);
                Vars.netServer.admins.setPlayerLimit(confMaxPlayers);
            } catch (Exception e) {
                if (!headless)
                    Vars.ui.showException(e);
                connecting = false;
                return;
            }
        } else {
            Vars.netServer.admins.setPlayerLimit(confMaxPlayers);
        }

        String finalPwd = confPassword ? confPin.trim() : "";

        if (!headless)
            Vars.ui.loadfrag.show("@mdt.message.manage-room.create-room");

        Timer.Task t = Timer.schedule(PlayerConnect::closeRoom, 10);
        PlayerConnect.createRoom(selected.ip, selected.port, finalPwd, l -> {
            connecting = false;
            if (!headless)
                Vars.ui.loadfrag.hide();
            else
                Vars.ui.hudfrag.showToast("Room Created: " + l.toString());
            t.cancel();
            link = l;
        },
                e -> {
                    connecting = false;
                    if (!headless) {
                        Vars.ui.loadfrag.hide();
                        Vars.ui.showException(e);
                    } else {
                        Vars.ui.hudfrag.showToast("Create Room Error: " + e.getMessage());
                    }
                    t.cancel();
                },
                r -> {
                    connecting = false;
                    link = null; // Ensure link is cleared on DC
                    if (!headless)
                        Vars.ui.loadfrag.hide();
                    t.cancel();
                    if (r != null) {
                        String msg = "@mdt.message.room." + Strings.camelToKebab(r.name());
                        msg = Core.bundle.get(msg, msg);
                        if (!headless)
                            Vars.ui.hudfrag.showToast(msg);
                        else
                            Vars.ui.hudfrag.showToast(msg);
                    } else if (link == null) {
                        if (!headless)
                            Vars.ui.hudfrag.showToast(Core.bundle.get("mdt.message.manage-room.create-room.failed"));
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

    // Check if mod's room management should be active
    private boolean active() {
        // If approval is required, we consider it active even if just hosting locally
        // (no proxy link)
        // provided the server is actually running.
        if (confApproval && Vars.net.server())
            return true;

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
        Call.infoMessage(p.con, "[scarlet]Waiting for approval...\n[lightgray]You are in spectator mode.");
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
        confName = Core.settings.getString("pc-room-name", "");
        confDesc = Core.settings.getString("pc-room-desc", "");
        confMaxPlayers = Core.settings.getInt("pc-room-max-v2", 30);
        confApproval = Core.settings.getBool("pc-room-approval", false);
        confPassword = Core.settings.getBool("pc-room-pwd-enabled", false);
        confPin = Core.settings.getString("pc-room-pin", "");
        confAutoHost = Core.settings.getBool("pc-auto-host", false);
        confLogoPath = Core.settings.getString("pc-room-logo", "");
        confAllowFriends = Core.settings.getBool("pc-allow-friends", true);
    }

    private void saveConfig() {
        Core.settings.put("pc-room-name", confName);
        Core.settings.put("pc-room-desc", confDesc);
        Core.settings.put("pc-room-max-v2", confMaxPlayers);
        Core.settings.put("pc-room-approval", confApproval);
        Core.settings.put("pc-room-pwd-enabled", confPassword);
        Core.settings.put("pc-room-pin", confPin);
        Core.settings.put("pc-auto-host", confAutoHost);
        Core.settings.put("pc-room-logo", confLogoPath);
        Core.settings.put("pc-allow-friends", confAllowFriends);
    }

    private void setupUI() {
        cont.clear();

        // Load Texture if path exists
        if (logoTexture != null) {
            logoTexture.dispose();
            logoTexture = null;
        }
        if (confLogoPath != null && !confLogoPath.isEmpty()) {
            try {
                arc.files.Fi f = new arc.files.Fi(confLogoPath);
                if (f.exists()) {
                    logoTexture = new arc.graphics.Texture(f);
                    logoTexture.setFilter(arc.graphics.Texture.TextureFilter.linear);
                }
            } catch (Exception ignored) {
            }
        }

        // --- CONTROLS ---
        Table settings = new Table();
        settings.defaults().growX().pad(5);

        // --- BASIC INFO ---
        settings.table(t -> {
            t.add("Room Information").color(Pal.accent).left().padBottom(5).row();
            t.image().color(Pal.accent).height(3f).growX().padBottom(10).row();

            t.table(info -> {
                info.left();
                info.add("Name:").left().width(100f);
                info.field(confName, val -> confName = val).growX().valid(x -> {
                    confName = x;
                    return !x.isEmpty();
                }).maxTextLength(50).with(tf -> tf.next(false)).height(40f).row();

                info.add("Description:").left().width(100f).padTop(5);
                info.area(confDesc, val -> confDesc = val).growX().valid(x -> {
                    confDesc = x;
                    return !x.isEmpty();
                }).maxTextLength(200)
                        .height(110f).with(ta -> ta.next(false)).padTop(5).row();

                info.add("Logo:").left().width(100f).padTop(5).color(arc.graphics.Color.white);
                info.table(logo -> {
                    logo.left();
                    logo.button(Icon.download, () -> {
                        Vars.platform.showFileChooser(true, "png,jpg,jpeg", f -> {
                            confLogoPath = f.absolutePath();
                            setupUI();
                        });
                    }).size(40f)
                            .tooltip(confLogoPath == null || confLogoPath.isEmpty() ? "Optional: Import Logo"
                                    : confLogoPath)
                            .left();

                    if (confLogoPath != null && !confLogoPath.isEmpty() && logoTexture != null) {
                        logo.image(new arc.graphics.g2d.TextureRegion(logoTexture)).size(40f).padLeft(10f)
                                .tooltip("Path: " + confLogoPath);
                        logo.button(Icon.cancel, Styles.clearNonei, () -> {
                            confLogoPath = "";
                            setupUI();
                        }).size(40f).padLeft(2f).tooltip("Remove Logo");
                    }
                }).growX().padTop(5).row();
            }).growX().left();
        }).growX().padBottom(20).row();

        // --- SETTINGS ---
        settings.table(t -> {
            t.left();
            t.add("Room Settings").color(Pal.accent).left().padBottom(5).row();
            t.image().color(Pal.accent).height(3f).growX().padBottom(10).row();

            t.table(opts -> {
                opts.left().defaults().left().padBottom(5);

                addCheck(opts, "Auto Host", confAutoHost, b -> {
                    confAutoHost = b;
                    Core.settings.put("pc-auto-host", b);
                }, "Automatically publishes your room to the global list so others can join without a direct link.");

                addCheck(opts, "Allow Friends", confAllowFriends, b -> {
                    confAllowFriends = b;
                    Core.settings.put("pc-allow-friends", b);
                }, "Allow friends to join immediately without waiting for approval.");

                addCheck(opts, "Require Approval", confApproval, b -> {
                    confApproval = b;
                    Core.settings.put("pc-room-approval", b);
                }, "Players entering the room will be in spectator mode until you approve them in the player list.");

                addCheck(opts, "Enable Password", confPassword, b -> {
                    confPassword = b;
                    setupUI();
                }, "Require a PIN code to join the room.");

                if (confPassword) {
                    opts.table(p -> {
                        p.left().defaults().left().center();
                        p.add("PIN (4-6): ").color(arc.graphics.Color.lightGray).padRight(10f);
                        Cell<TextField> cf = p.field(confPin, val -> confPin = val).width(180f);
                        pinField = cf.get();
                        pinField.setFilter(TextField.TextFieldFilter.digitsOnly);
                        pinField.setMaxLength(6);
                        cf.valid(x -> x.length() >= 4 && x.length() <= 6);
                    }).padLeft(40f).padTop(4f).growX().row();
                }

                opts.table(m -> {
                    m.left().defaults().left().center();
                    m.add("Max Players: ").padRight(10f);
                    m.field(String.valueOf(confMaxPlayers == 0 ? "" : confMaxPlayers), val -> {
                        if (val.isEmpty())
                            confMaxPlayers = 0;
                        else if (Strings.canParseInt(val))
                            confMaxPlayers = Integer.parseInt(val);
                    }).width(100f).valid(Strings::canParsePositiveInt);
                }).growX().padTop(10f);
            }).growX();
        }).growX().padBottom(20).row();

        // --- PROXY SERVER ---
        settings.table(t -> {
            t.add("Connection").color(Pal.accent).left().padBottom(5).row();
            t.image().color(Pal.accent).height(3f).growX().padBottom(10).row();

            serverSelectBtn = t.button("Select Proxy Server", Icon.host, () -> selectDialog.show())
                    .growX().height(60f).get();
            updateServerBtn();
        }).growX().padBottom(20).row();

        // --- CONTROLS ---
        settings.table(t -> {
            t.add("Server Control").color(Pal.accent).left().padBottom(5).row();
            t.image().color(Pal.accent).height(3f).growX().padBottom(10).row();

            t.table(ctrl -> {
                ctrl.defaults().height(60f).pad(5);

                // Pause/Unpause Button
                ctrl.button("Pause", Icon.pause, () -> {
                    if (Vars.net.server()) {
                        if (Vars.state.isPaused()) {
                            Vars.state.set(mindustry.core.GameState.State.playing);
                        } else {
                            Vars.state.set(mindustry.core.GameState.State.paused);
                        }
                    }
                }).growX().update(b -> {
                    boolean paused = Vars.state.isPaused();
                    b.setText(paused ? "Unpause" : "Pause");
                    // Safely update icon
                    if (b.getChildren().size > 0 && b.getChildren().get(0) instanceof Image) {
                        ((Image) b.getChildren().get(0)).setDrawable(paused ? Icon.play : Icon.pause);
                    }
                    b.setDisabled(!Vars.net.server());
                });

                // Create/Close Room Button
                ctrl.button("@mdt.message.manage-room.create-room", Icon.play, () -> {
                    if (PlayerConnect.isRoomClosed()) {
                        createRoom();
                    } else {
                        closeRoom();
                    }
                }).growX().update(b -> {
                    boolean closed = PlayerConnect.isRoomClosed();
                    b.setText(closed ? "@mdt.message.manage-room.create-room" : "@mdt.message.manage-room.close-room");
                    if (b.getChildren().size > 0 && b.getChildren().get(0) instanceof Image) {
                        ((Image) b.getChildren().get(0)).setDrawable(closed ? Icon.play : Icon.cancel);
                    }
                    b.setDisabled(connecting || (closed && (Vars.net.client() || Vars.state.isMenu())));
                });
            }).growX();
        }).growX().padBottom(20).row();

        ScrollPane pane = new ScrollPane(settings);
        pane.setScrollingDisabled(true, false);
        pane.setFadeScrollBars(false);

        // Responsive width: Match TouchSettingsDialog style for consistent UI
        float w = Math.min(Core.graphics.getWidth() / 1.2f, 460f);

        cont.add(pane).width(w).maxHeight(Core.graphics.getHeight() / Scl.scl() - 120f);
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
        if (confName.isEmpty()) {
            Vars.ui.showInfo("Error: Room Name is required.");
            return false;
        }
        if (confDesc.isEmpty()) {
            Vars.ui.showInfo("Error: Room Description is required.");
            return false;
        }

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
        // Removed setHudText ghost box
        Call.sendMessage(p.name + " [lime]has been approved to join!");
        p.unit(UnitTypes.dagger.create(p.team()));
        if (Core.camera != null)
            p.unit().set(Core.camera.position.x, Core.camera.position.y);
        p.unit().add();
    }

    private void createRoom() {
        if (connecting)
            return;

        // Prevent hosting if in menu (must be in a map)
        if (Vars.state.isMenu()) {
            Vars.ui.showInfo("You must enter a map before creating a room.");
            return;
        }

        if (!validateSettings())
            return;
        saveConfig();
        connecting = true; // Manual connect start
        performCreateRoom(false);
    }

    public void closeRoom() {
        PlayerConnect.closeRoom();
        link = null;
        approvedUUIDs.clear();
        connecting = false;
    }

    public void copyLink() {
        if (link == null)
            return;
        Core.app.setClipboardText(link.toString());
        Vars.ui.showInfoFade("@copied");
    }

    private void addCheck(Table t, String text, boolean val, arc.func.Boolc listener, String info) {
        t.table(r -> {
            r.left();
            r.check(text, val, listener).left();
            r.add().growX();
            r.button(Icon.info, Styles.clearNonei, () -> {
                BaseDialog d = new BaseDialog("Info");
                d.addCloseButton();
                d.cont.add(info).width(Math.min(Core.graphics.getWidth() / 1.1f, 600f)).wrap().left();
                d.show();
            }).size(32f).padLeft(5f).tooltip(tip -> tip.background(Styles.black6).add(info).width(300f).wrap());
        }).growX().left().padBottom(5f).row();
    }

    private void resetDefaults() {
        confName = "";
        confDesc = "";
        confMaxPlayers = 30;
        confApproval = false;
        confPassword = false;
        confPin = "";
        confAutoHost = false;
        confLogoPath = "";
        confAllowFriends = true;

        saveConfig();
        setupUI();
        Vars.ui.hudfrag.showToast("Settings reset to defaults");
    }
}
