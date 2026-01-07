package mindustrytool.features.playerconnect;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import arc.Core;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Log;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.core.Version;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.ui.Debouncer;

public class PlayerConnectRoomsDialog extends mindustry.ui.dialogs.BaseDialog {
    private final Table roomList = new Table();
    private final Debouncer debouncer = new Debouncer(250, TimeUnit.MILLISECONDS);
    private String searchTerm = "";

    public PlayerConnectRoomsDialog() {
        super("@message.room-list.title");
        addCloseButton();

        try {
            cont.table(container -> {
                container.table(topBar -> {
                    topBar.field(searchTerm, (result) -> {
                        searchTerm = result;
                        debouncer.debounce(this::setupPlayerConnect);
                    })//
                            .left()
                            .growX()
                            .get()
                            .setMessageText(Core.bundle.format("@map.search"));

                })
                        .top()
                        .left()
                        .growX();

                container.row();
                container.add(roomList)
                        .grow()
                        .top()
                        .left();

                container.row();
            })
                    .top()
                    .left()
                    .grow();
            cont
                    .top()
                    .left();

            buttons
                    .button(Icon.refresh, Styles.squarei, () -> setupPlayerConnect())
                    .size(64)
                    .padRight(8);

            shown(() -> {
                setupPlayerConnect();
            });
        } catch (Throwable e) {
            Log.err(e);
        }
    }

    public void setupPlayerConnect() {
        roomList.clear();
        roomList.labelWrap(Core.bundle.format("message.loading"))
                .center()
                .labelAlign(0)
                .expand()
                .fill();

        PlayerConnectApi.findPlayerConnectRooms(searchTerm, rooms -> {
            roomList.clear();

            roomList.pane(list -> {
                if (rooms.isEmpty()) {
                    list.labelWrap(Core.bundle.format("message.no-rooms-found"))
                            .center()
                            .labelAlign(0)
                            .expand()
                            .fill();
                    return;
                }

                for (PlayerConnectRoom room : rooms) {
                    list.table(Styles.black5, card -> {
                        card.table(left -> {
                            left.add(
                                    room.data().name() + "(" + room.data().locale() + ") [white]"
                                            + (room.data().isSecured() ? Iconc.lock : "")
                                            + " " + getVersionString(room.data().version()))
                                    .fontScale(1.5f)
                                    .align(Align.left)
                                    .left();

                            left.row();
                            left.add(Iconc.map + " " + Core.bundle.format("save.map", room.data().mapName())
                                    + "[lightgray] / " + room.data().gamemode())
                                    .align(Align.left).left();

                            left.row();
                            left.add(
                                    Iconc.players + " " + Core.bundle.format("players", room.data().players().size))
                                    .align(Align.left)
                                    .left();

                            left.row();
                            left.add(Core.bundle.format("version") + ": " + room.data().version())
                                    .align(Align.left)
                                    .left();

                            if (room.data().mods().size > 0) {
                                left.row();
                                left.add(Iconc.book + " " + Strings.join(",", room.data().mods())).align(Align.left)
                                        .left();
                            }
                        })
                                .top()
                                .left();

                        card.add().growX().width(-1);

                        card.table(right -> {
                            right.button(Iconc.play + " " + Core.bundle.format("join"), () -> {
                                if (!room.data().isSecured()) {
                                    try {
                                        PlayerConnect.joinRoom(
                                                PlayerConnectLink.fromString(room.link()), "",
                                                () -> hide());
                                    } catch (Throwable e) {
                                        hide();
                                        setupPlayerConnect();
                                        Vars.ui.showException("@message.connect.fail", e);
                                    }

                                    return;
                                }

                                BaseDialog connect = new BaseDialog("@message.type-password.title");
                                String[] password = { "" };

                                connect.cont.table(table -> {
                                    table.add("@message.password")
                                            .padRight(5f)
                                            .right();

                                    table.field(password[0], text -> password[0] = text)
                                            .size(320f, 54f)
                                            .valid(t -> t.length() > 0 && t.length() <= 100)
                                            .maxTextLength(100)
                                            .left()
                                            .get();
                                    table.row().add();
                                }).row();

                                connect.buttons.button("@cancel", () -> {
                                    connect.hide();
                                }).minWidth(210);

                                connect.buttons.button("@ok", () -> {
                                    try {
                                        PlayerConnect.joinRoom(
                                                PlayerConnectLink.fromString(room.link()),
                                                password[0],
                                                () -> {
                                                    hide();
                                                    connect.hide();
                                                });
                                    } catch (Throwable e) {
                                        hide();
                                        connect.hide();
                                        setupPlayerConnect();
                                        Vars.ui.showException("@message.connect.fail", e);
                                    }
                                }).minWidth(210);

                                connect.show();
                            })
                                    .minWidth(150);
                        });
                    })
                            .growX()
                            .left()
                            .top()
                            .margin(8)
                            .pad(8);

                    list.row();
                }
            })
                    .top()
                    .left()
                    .fill()
                    .expandX()
                    .scrollX(false)
                    .scrollY(true);

            roomList
                    .top()
                    .left()
                    .marginTop(8)
                    .marginBottom(8);

        });
    }

    private String getVersionString(String versionString) {
        BuildInfo info = extract(versionString);
        int version = info.build;
        String versionType = info.type;

        if (version == -1) {
            return Core.bundle.format("server.version", Core.bundle.get("server.custombuild"), "");
        } else if (version == 0) {
            return Core.bundle.get("server.outdated");
        } else if (version < Version.build && Version.build != -1) {
            return Core.bundle.get("server.outdated") + "\n" +
                    Core.bundle.format("server.version", version, "");
        } else if (version > Version.build && Version.build != -1) {
            return Core.bundle.get("server.outdated.client") + "\n" +
                    Core.bundle.format("server.version", version, "");
        } else if (version == Version.build && Version.type.equals(versionType)) {
            // not important
            return "";
        } else {
            return Core.bundle.format("server.version", version, versionType);
        }
    }

    private static class BuildInfo {
        public String type = "custom";
        public int build = -1;
        public int revision = -1;
        public String modifier;

        @Override
        public String toString() {
            return "type=" + type + ", build=" + build + ", revision=" + revision + ", modifier=" + modifier;
        }
    }

    private BuildInfo extract(String combined) {
        BuildInfo info = new BuildInfo();

        if ("custom build".equals(combined)) {
            info.type = "custom";
            info.build = -1;
            info.revision = 0;
            info.modifier = null;
            return info;
        }

        Pattern pattern = Pattern.compile("^(.+?) build (\\d+)(?:\\.(\\d+))?$");
        Matcher matcher = pattern.matcher(combined);

        if (matcher.matches()) {
            String first = matcher.group(1);
            info.build = Integer.parseInt(matcher.group(2));
            info.revision = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;

            if ("official".equals(first)) {
                info.type = "official";
                info.modifier = first;
            } else {
                info.type = first;
                info.modifier = null;
            }
        } else {
            Log.warn("Invalid combined() format: " + combined);
        }

        return info;
    }
}
