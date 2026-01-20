package mindustrytool.features.playerconnect;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Time;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class CreateRoomDialog extends BaseDialog {
    PlayerConnectLink link;
    Server selected;
    Button selectedButton;
    Table online;
    boolean refreshingOnline;

    private Table mainTable;

    public CreateRoomDialog() {
        super("@message.manage-room.title");

        Events.run(EventType.HostEvent.class, () -> {
            show();
        });

        addCloseButton();

        mainTable = new Table();
        cont.add(mainTable).growX().maxWidth(1200f);

        shown(() -> {
            if (!PlayerConnect.isRoomClosed()) {
                setupManageView();
            } else {
                setupStep1();
            }
        });

        // Pause menu button integration
        Vars.ui.paused.shown(() -> {
            Table root = Vars.ui.paused.cont;
            @SuppressWarnings("rawtypes")
            Seq<Cell> buttons = root.getCells();

            String buttonTitle = PlayerConnect.isRoomClosed() ? "@message.create-room.title"
                    : "@message.manage-room.title";

            if (Vars.mobile) {
                root.row()
                        .buttonRow(buttonTitle, Icon.planet, this::show)
                        .disabled(button -> !Vars.net.server())
                        .row();
                return;

            } else if (arc.util.Reflect.<Integer>get(buttons.get(buttons.size - 2), "colspan") == 2) {
                root.row()
                        .button(buttonTitle, Icon.planet, this::show)
                        .colspan(2)
                        .width(450f)
                        .disabled(button -> !Vars.net.server())
                        .row();

            } else {
                root.row()
                        .button(buttonTitle, Icon.planet, this::show)
                        .disabled(button -> !Vars.net.server())
                        .row();
            }
            buttons.swap(buttons.size - 1, buttons.size - 2);
        });
    }

    private void setupManageView() {
        mainTable.clear();
        mainTable.add("@message.manage-room.title").style(Styles.defaultLabel).padBottom(20f).row();

        if (PlayerConnect.isRoomClosed()) {
            mainTable.add("@message.player-connect.room-closed").row();
        } else {
            mainTable.add("@message.player-connect.room-connected").row();
        }

        mainTable.button("@message.manage-room.close-room", Icon.cancel, () -> {
            closeRoom();
            hide();
        })
                .size(250f, 60f)
                .padBottom(10f)
                .row();

        mainTable.button("@message.manage-room.copy-link", Icon.copy, this::copyLink)
                .size(250f, 60f)
                .row();
    }

    private void setupStep1() {
        mainTable.clear();
        mainTable.add("@message.create-room.title").style(Styles.defaultLabel).padBottom(20f).row();

        mainTable.table(t -> {
            t.add(Core.bundle.format("message.create-room.server-name"))
                    .padRight(5f)
                    .ellipsis(true)
                    .left()
                    .row();

            t.field(PlayerConnectConfig.getRoomName(), text -> {
                PlayerConnectConfig.setRoomName(text);
            })
                    .height(54f)
                    .growX()
                    .padBottom(20)
                    .maxTextLength(100)
                    .left()
                    .row();

            t.add(Core.bundle.format("message.create-room.password"))
                    .padRight(5f)
                    .ellipsis(true)
                    .left()
                    .row();

            t.field(PlayerConnectConfig.getPassword(), text -> {
                PlayerConnectConfig.setPassword(text);
            })
                    .height(54f)
                    .growX()
                    .maxTextLength(100)
                    .left()
                    .row();
        }).padBottom(20f).row();

        mainTable.button("Next", Icon.right, () -> {
            setupStep2();
        }).size(200f, 60f).disabled(b -> PlayerConnectConfig.getRoomName().isEmpty());
    }

    private void setupStep2() {
        mainTable.clear();
        mainTable.add("@message.create-room.select-provider").style(Styles.defaultLabel).padBottom(10f).row();

        mainTable.pane(hosts -> {
            hosts.table(table -> {
                table.add("@message.manage-room.public-servers")
                        .pad(10)
                        .padLeft(0)
                        .color(Pal.accent)
                        .growX()
                        .left();

                table.button(Icon.refresh, Styles.emptyi, this::refreshOnline).size(40f).right().padRight(3);
            }).pad(0, 5, 0, 5).growX().row();

            hosts.image().pad(5).height(3).color(Pal.accent).growX().row();

            online = new Table();
            online.add("@loading")
                    .color(Pal.accent)
                    .labelAlign(Align.center)
                    .growX()
                    .center();
            hosts.add(online).growX().row();
        }).growX().padBottom(10f).row();

        Table buttons = new Table();

        buttons.button("@back", Icon.left, () -> {
            setupStep1();
        }).size(200f, 50f).padRight(10f);

        buttons.button("@message.create-room.create-room", Icon.play, () -> {
            createRoom(PlayerConnectConfig.getPassword());
        }).size(200f, 50f).disabled(b -> selected == null);

        mainTable.add(buttons).row();

        // Refresh providers after UI setup
        Core.app.post(() -> {
            refreshOnline();
        });
    }

    void refreshOnline() {
        if (refreshingOnline)
            return;

        refreshingOnline = true;

        if (online != null) {
            online.clear();
            online.add("@loading")
                    .color(Pal.accent)
                    .labelAlign(Align.center)
                    .growX()
                    .center();
        }

        PlayerConnectProviders.refreshOnline(() -> {
            refreshingOnline = false;
            setupServers(PlayerConnectProviders.online, online);
        }, e -> {
            refreshingOnline = false;
            Vars.ui.showInfoFade("@message.room.fetch-failed");

            if (online != null) {
                online.clear();
                online.add("fetch failed")
                        .color(Pal.accent)
                        .labelAlign(Align.center)
                        .growX()
                        .center();
            }
        });
    }

    void setupServers(arc.struct.ArrayMap<String, String> servers, Table table) {
        selected = null;

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
            table.add(button).checked(b -> selectedButton == b).growX().pad(2).row();

            Stack stack = new Stack();
            Table inner = new Table();
            inner.setColor(Pal.gray);
            Draw.reset();

            button.clearChildren();
            button.add(stack).growX().row();

            Table ping = inner.table(t -> {
            }).margin(0).pad(0).left().fillX().get();

            inner.add().expandX();

            Table label = new Table().center();
            if (Vars.mobile || (servers.getKeyAt(i) + " (" + servers.getValueAt(i) + ')').length() > 54) {
                label.add(servers.getKeyAt(i)).pad(2, 5, 0, 5).expandX().row();
                label.add(" [lightgray](" + servers.getValueAt(i) + ')').pad(2, 0, 5, 5).expandX().row();
            } else {
                label.add(servers.getKeyAt(i) + " [lightgray](" + servers.getValueAt(i) + ')').pad(2).expandX();
            }

            stack.add(label);
            stack.add(inner);

            ping.label(() -> Strings.animated(Time.time, 4, 11, ".")).pad(2).color(Pal.accent).left();

            PlayerConnect.pingHost(server.ip, server.port, ms -> {
                if (selected == null) {
                    selected = server;
                    selectedButton = button;
                }
                ping.clear();
                ping.image(Icon.ok).color(Color.green).padLeft(5).padRight(5).left();
                if (Vars.mobile) {
                    ping.row().add(ms + "ms").color(Color.lightGray).padLeft(5).padRight(5).left();
                } else {
                    ping.add(ms + "ms").color(Color.lightGray).padRight(5).left();
                }
            }, e -> {
                ping.clear();
                ping.image(Icon.cancel).color(Color.red).padLeft(5).padRight(5).left();
                Log.err(e.getMessage());
            });
        }
    }

    public void createRoom(String password) {
        if (selected == null)
            return;

        link = null;
        Vars.ui.loadfrag.show("@message.manage-room.create-room");

        Timer.Task timer = Timer.schedule(() -> {
            Log.info("Close room due to timeout 10s");
            PlayerConnect.close();
        }, 10);

        PlayerConnect.create(selected.ip, selected.port, password, link -> {
            Vars.ui.loadfrag.hide();
            timer.cancel();
            this.link = link;
            hide(); // Close dialog on success
            setupManageView(); // Reset to manage view (though dialog is hidden)
            Vars.ui.showInfoFade("@message.manage-room.create-room.success");
        }, error -> {
            Vars.net.handleException(error);
            timer.cancel();
            Vars.ui.showErrorMessage("@message.manage-room.create-room.failed");
        }, closeReason -> {
            Vars.ui.loadfrag.hide();
            link = null;
            timer.cancel();
            if (closeReason != null) {
                Vars.ui.showText("", "@message.room." + arc.util.Strings.camelToKebab(closeReason.name()));
            } else if (link == null) {
                Vars.ui.showErrorMessage("@message.manage-room.room-closed");
            }
        });
    }

    public void closeRoom() {
        PlayerConnect.close();
        link = null;
        Vars.ui.showInfoFade("@message.manage-room.room-closed");
    }

    public void copyLink() {
        if (link == null)
            return;

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
            if (!wasValid)
                return "";
            else if (Strings.count(ip, ':') > 1)
                return "[" + ip + "]:" + port;
            else
                return ip + ":" + port;
        }
    }
}
