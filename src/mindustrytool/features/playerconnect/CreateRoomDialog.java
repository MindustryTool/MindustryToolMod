package mindustrytool.features.playerconnect;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.scene.ui.Button;
import arc.scene.ui.Dialog;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import arc.util.Time;
import arc.util.Timer;

import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class CreateRoomDialog extends BaseDialog {
    PlayerConnectLink link;
    Server selected, renaming;
    int renamingOldKey;
    Button selectedButton;
    Dialog add, create;
    Table custom, online;
    boolean customShown = true, onlineShown = true, refreshingOnline;

    public CreateRoomDialog() {
        super("@message.manage-room.title");
        arc.Events.run(mindustry.game.EventType.HostEvent.class, this::closeRoom);

        cont.defaults().width(Vars.mobile ? 480f : 850f);

        makeButtonOverlay();
        addCloseButton();

        shown(() -> {
            // Just to give time to this dialog to open
            arc.util.Time.run(7f, () -> {
                refreshCustom();
                refreshOnline();
            });
        });

        // Add custom server dialog
        Server tmp = new Server();
        String[] lastEdit = { "", "" };
        TextField[] fieldEdit = { null, null };

        String[] roomConfig = { "", "" };
        TextField[] fieldCreate = { null, null };

        roomConfig[0] = Core.settings.getString("playerConnectRoomName", Vars.player.name());
        roomConfig[1] = Core.settings.getString("playerConnectRoomPassword", "");

        create = new BaseDialog("@message.create-room.title");

        create.buttons.defaults().size(140f, 60f).pad(4f);
        create.cont.table(table -> {
            table.add("@message.create-room.server-name")
                    .padRight(5f)
                    .right();

            fieldCreate[0] = table.field(roomConfig[0], text -> {
                roomConfig[0] = text;
                Core.settings.put("playerConnectRoomName", text);
            })
                    .size(320f, 54f)
                    .valid(t -> t.length() > 0 && t.length() <= 100)
                    .maxTextLength(100)
                    .left()
                    .get();

            table.row()
                    .add("@message.password")
                    .padRight(5f)
                    .right();

            fieldCreate[1] = table.field(roomConfig[1], text -> {
                roomConfig[1] = text;
                Core.settings.put("playerConnectRoomPassword", text);
            })
                    .size(320f, 54f)
                    .maxTextLength(100)
                    .left()
                    .get();

            table.row().add();
        }).row();

        create.buttons.button("@cancel", () -> {
            create.hide();
        });

        create.buttons.button("@ok", () -> {
            createRoom(roomConfig[1]);
            create.hide();
        })
                .disabled(b -> roomConfig[0].isEmpty()
                        || roomConfig[0].length() > 100
                        || roomConfig[1].length() > 100);

        buttons.button("@message.manage-room.create-room", Icon.add, create::show)
                .disabled(b -> !PlayerConnect.isRoomClosed() || selected == null);
        if (Vars.mobile)
            buttons.row();

        buttons.button("@message.manage-room.close-room", Icon.cancel, this::closeRoom)
                .disabled(b -> PlayerConnect.isRoomClosed());

        buttons.button("@message.manage-room.copy-link", Icon.copy, this::copyLink)
                .disabled(b -> link == null);

        add = new BaseDialog("@joingame.title");

        add.buttons.defaults().size(140f, 60f).pad(4f);
        add.cont.table(table -> {
            table.add("@message.manage-room.server-name")
                    .padRight(5f)
                    .right();

            fieldEdit[0] = table.field(lastEdit[0], text -> lastEdit[0] = text)
                    .size(320f, 54f)
                    .maxTextLength(100)
                    .left()
                    .get();

            table.row()
                    .add("@joingame.ip")
                    .padRight(5f)
                    .right();

            fieldEdit[1] = table.field(lastEdit[1], tmp::set)
                    .size(320f, 54f)
                    .valid(t -> tmp.set(lastEdit[1] = t))
                    .maxTextLength(100)
                    .left()
                    .get();

            table.row().add();
            table.label(() -> tmp.error)
                    .width(320f)
                    .left()
                    .row();
        }).row();

        add.buttons.button("@cancel", () -> {
            if (renaming != null) {
                renaming = null;
                lastEdit[0] = lastEdit[1] = "";
            }
            add.hide();
        });

        add.buttons.button("@ok", () -> {
            if (renaming != null) {
                PlayerConnectProviders.custom.removeIndex(renamingOldKey);
                PlayerConnectProviders.custom.insert(renamingOldKey, lastEdit[0], lastEdit[1]);
                renaming = null;
                renamingOldKey = -1;
            } else
                PlayerConnectProviders.custom.put(lastEdit[0], lastEdit[1]);
            PlayerConnectProviders.saveCustom();
            refreshCustom();
            add.hide();
            lastEdit[0] = lastEdit[1] = "";
        }).disabled(b -> !tmp.wasValid || lastEdit[0].isEmpty() || lastEdit[1].isEmpty());

        add.shown(() -> {
            add.title.setText(renaming != null ? "@server.edit" : "@server.add");
            if (renaming != null) {
                fieldEdit[0].setText(renaming.name);
                fieldEdit[1].setText(renaming.get());
                lastEdit[0] = renaming.name;
                lastEdit[1] = renaming.get();
            } else {
                fieldEdit[0].clearText();
                fieldEdit[1].clearText();
            }
        });

        cont.pane(hosts -> {
            hosts.table(table -> {
                table.add("@message.manage-room.custom-servers")
                        .pad(10)
                        .padLeft(0)
                        .color(Pal.accent)
                        .growX()
                        .left();

                table.button(Icon.add, Styles.emptyi, add::show)
                        .size(40f)
                        .right()
                        .padRight(3);

                table.button(Icon.refresh, Styles.emptyi, this::refreshCustom)
                        .size(40f)
                        .right()
                        .padRight(3);

                table.button(Icon.downOpen, Styles.emptyi, () -> customShown = !customShown)
                        .update(i -> i.getStyle().imageUp = !customShown ? Icon.upOpen : Icon.downOpen)
                        .size(40f)
                        .right();

            })
                    .pad(0, 5, 0, 5)
                    .growX()
                    .row();

            hosts.image()
                    .pad(5)
                    .height(3)
                    .color(Pal.accent)
                    .growX()
                    .row();

            hosts.collapser(table -> custom = table, false, () -> customShown)
                    .pad(0, 5, 10, 5)
                    .growX();

            hosts.row();

            // Online Public servers
            hosts.table(table -> {
                table.add("@message.manage-room.public-servers")
                        .pad(10)
                        .padLeft(0)
                        .color(Pal.accent)
                        .growX()
                        .left();

                table.button(Icon.refresh, Styles.emptyi, this::refreshOnline)
                        .size(40f)
                        .right()
                        .padRight(3);

                table.button(Icon.downOpen, Styles.emptyi, () -> onlineShown = !onlineShown)
                        .update(i -> i.getStyle().imageUp = !onlineShown ? Icon.upOpen : Icon.downOpen)
                        .size(40f)
                        .right();

            })
                    .pad(0, 5, 0, 5)
                    .growX()
                    .row();

            hosts.image()
                    .pad(5)
                    .height(3)
                    .color(Pal.accent)
                    .growX()
                    .row();

            hosts.collapser(table -> online = table, false, () -> onlineShown)
                    .pad(0, 5, 10, 5)
                    .growX();

            hosts.row();

            hosts.marginBottom(Vars.mobile ? 140f : 70f);
        })
                .get()
                .setScrollingDisabled(true, false);

        Vars.ui.paused.shown(() -> {
            Table root = Vars.ui.paused.cont;
            @SuppressWarnings("rawtypes")
            arc.struct.Seq<arc.scene.ui.layout.Cell> buttons = root.getCells();

            if (Vars.mobile) {
                root.row()
                        .buttonRow("@message.manage-room.title", Icon.planet, this::show)
                        .disabled(button -> !Vars.net.server()).row();
                return;

            } else if (arc.util.Reflect.<Integer>get(buttons.get(buttons.size - 2), "colspan") == 2) {
                root.row()
                        .button("@message.manage-room.title", Icon.planet, this::show)
                        .colspan(2)
                        .width(450f)
                        .disabled(button -> !Vars.net.server())
                        .row();

            } else {
                root.row()
                        .button("@message.manage-room.title", Icon.planet, this::show)
                        .disabled(button -> !Vars.net.server())
                        .row();
            }
            buttons.swap(buttons.size - 1, buttons.size - 2);
        });

    }

    void refreshCustom() {
        PlayerConnectProviders.loadCustom();
        setupServers(PlayerConnectProviders.custom, custom, true, () -> {
            PlayerConnectProviders.saveCustom();
            refreshCustom();
        });
    }

    void refreshOnline() {
        if (refreshingOnline) {
            return;
        }

        refreshingOnline = true;
        PlayerConnectProviders.refreshOnline(() -> {
            refreshingOnline = false;
            setupServers(PlayerConnectProviders.online, online, false, null);
        }, e -> {
            refreshingOnline = false;
            Vars.ui.showException("@message.room.fetch-failed", e);
        });
    }

    void setupServers(arc.struct.ArrayMap<String, String> servers, Table table, boolean editable, Runnable onDelete) {
        selected = null;// in case of
        table.clear();
        for (int i = 0; i < servers.size; i++) {
            Server server = new Server();
            server.name = servers.getKeyAt(i);
            server.set(servers.getValueAt(i));

            Button button = new Button();
            button.getStyle().checkedOver = button.getStyle().checked = button.getStyle().over;
            button.setProgrammaticChangeEvents(true);
            button.clicked(() -> {
                selected = server;
                selectedButton = button;
            });
            table.add(button).checked(b -> selectedButton == b).growX().padTop(5).padBottom(5).row();

            Stack stack = new Stack();
            Table inner = new Table();
            inner.setColor(Pal.gray);
            Draw.reset();

            button.clearChildren();
            button.add(stack).growX().row();

            Table ping = inner.table(t -> {
            })
                    .margin(0)
                    .pad(0)
                    .left()
                    .fillX()
                    .get();

            inner.add().expandX();
            Table label = new Table().center();
            // Cut in two line for mobiles or if the name is too long
            if (Vars.mobile || (servers.getKeyAt(i) + " (" + servers.getValueAt(i) + ')').length() > 54) {
                label.add(servers.getKeyAt(i))
                        .pad(5, 5, 0, 5)
                        .expandX()
                        .row();

                label.add(" [lightgray](" + servers.getValueAt(i) + ')').pad(5, 0, 5, 5).expandX();
            } else
                label.add(servers.getKeyAt(i) + " [lightgray](" + servers.getValueAt(i) + ')').pad(5).expandX();

            stack.add(label);
            stack.add(inner);

            if (editable) {
                final int i0 = i;
                if (Vars.mobile) {
                    inner.button(Icon.pencil, Styles.emptyi, () -> {
                        renaming = server;
                        renamingOldKey = i0;
                        add.show();
                    })
                            .size(30f)
                            .pad(2, 5, 2, 5)
                            .right();

                    inner.button(Icon.trash, Styles.emptyi, () -> {
                        Vars.ui.showConfirm("@confirm", "@server.delete", () -> {
                            servers.removeKey(server.name);
                            if (onDelete != null) {
                                onDelete.run();
                            }
                        });
                    })
                            .size(30f)
                            .pad(2, 5, 2, 5)
                            .right();
                } else {
                    inner.button(Icon.pencilSmall, Styles.emptyi, () -> {
                        renaming = server;
                        renamingOldKey = i0;
                        add.show();
                    })
                            .pad(4)
                            .right();

                    inner.button(Icon.trashSmall, Styles.emptyi, () -> {
                        Vars.ui.showConfirm("@confirm", "@server.delete", () -> {
                            servers.removeKey(server.name);
                            if (onDelete != null)
                                onDelete.run();
                        });
                    }).pad(2).right();
                }
            }

            ping.label(() -> Strings.animated(Time.time, 4, 11, "."))
                    .pad(2)
                    .color(Pal.accent)
                    .left();

            PlayerConnect.pingHost(server.ip, server.port, ms -> {
                ping.clear();
                ping.image(Icon.ok)
                        .color(Color.green)
                        .padLeft(5)
                        .padRight(5)
                        .left();

                if (Vars.mobile) {
                    ping.row()
                            .add(ms + "ms")
                            .color(Color.lightGray)
                            .padLeft(5)
                            .padRight(5)
                            .left();
                } else {
                    ping.add(ms + "ms")
                            .color(Color.lightGray)
                            .padRight(5)
                            .left();
                }

            }, e -> {
                ping.clear();
                ping.image(Icon.cancel)
                        .color(Color.red)
                        .padLeft(5)
                        .padRight(5)
                        .left();
            });
        }
    }

    public void createRoom(String password) {
        if (selected == null)
            return;

        Vars.ui.loadfrag.show("@message.manage-room.create-room");
        link = null;
        // Disconnect the client if the room is not created until 10 seconds
        Timer.Task t = Timer.schedule(PlayerConnect::closeRoom, 10);
        PlayerConnect.createRoom(selected.ip, selected.port, password, l -> {
            Vars.ui.loadfrag.hide();
            t.cancel();
            link = l;
        }, e -> {
            Vars.net.handleException(e);
            t.cancel();
        }, r -> {
            Vars.ui.loadfrag.hide();
            t.cancel();
            if (r != null) {
                Vars.ui.showText("", "@message.room." + arc.util.Strings.camelToKebab(r.name()));
            } else if (link == null) {
                Vars.ui.showErrorMessage("@message.manage-room.create-room.failed");
            }
            link = null;
        });
    }

    public void closeRoom() {
        PlayerConnect.closeRoom();
        link = null;
    }

    public void copyLink() {
        if (link == null) {
            return;
        }

        arc.Core.app.setClipboardText(link.toString());
        Vars.ui.showInfoFade("@copied");
    }

    static class Server {
        public String ip, name, error, last;
        public int port;
        public boolean wasValid;

        public synchronized boolean set(String ip) {
            if (ip.equals(last))
                return wasValid;
            this.ip = this.error = null;
            this.port = 0;
            last = ip;

            if (ip.isEmpty()) {
                this.error = "@message.room.missing-host";
                return wasValid = false;
            }
            try {
                boolean isIpv6 = Strings.count(ip, ':') > 1;
                if (isIpv6 && ip.lastIndexOf("]:") != -1 && ip.lastIndexOf("]:") != ip.length() - 1) {
                    int idx = ip.indexOf("]:");
                    this.ip = ip.substring(1, idx);
                    this.port = Integer.parseInt(ip.substring(idx + 2));
                    if (port < 0 || port > 0xFFFF)
                        throw new Exception();
                } else if (!isIpv6 && ip.lastIndexOf(':') != -1 && ip.lastIndexOf(':') != ip.length() - 1) {
                    int idx = ip.lastIndexOf(':');
                    this.ip = ip.substring(0, idx);
                    this.port = Integer.parseInt(ip.substring(idx + 1));
                    if (port < 0 || port > 0xFFFF)
                        throw new Exception();
                } else {
                    this.error = "@message.room.missing-port";
                    return wasValid = false;
                }
                return wasValid = true;
            } catch (Exception e) {
                this.error = "@message.room.invalid-port";
                return wasValid = false;
            }
        }

        public String get() {
            if (!wasValid) {
                return "";
            } else if (Strings.count(ip, ':') > 1) {
                return "[" + ip + "]:" + port;
            } else {
                return ip + ":" + port;
            }
        }
    }
}
