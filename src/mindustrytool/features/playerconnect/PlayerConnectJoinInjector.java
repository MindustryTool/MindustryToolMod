package mindustrytool.features.playerconnect;

import arc.Core;
import arc.math.Mathf;
import arc.scene.ui.layout.Scl;
import arc.scene.Element;
import arc.scene.ui.Button;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Collapser;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Log;
import arc.util.Reflect;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.core.Version;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.dialogs.JoinDialog;
import mindustrytool.services.PlayerConnectService;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerConnectJoinInjector {
    private final PlayerConnectService playerConnectService = new PlayerConnectService();
    private final Table playerConnectTable = new Table();
    private String searchTerm = "";
    private Table hosts;

    private static final String HEADER_NAME = "pc-header";
    private static final String COLLAPSER_NAME = "pc-collapser";

    public void inject(JoinDialog dialog) {
        this.hosts = Reflect.get(dialog, "hosts");

        if (this.hosts == null) {
            return;
        }

        // Prepare our section
        Table header = new Table();
        header.name = HEADER_NAME;
        Collapser col = new Collapser(playerConnectTable, false);

        col.name = COLLAPSER_NAME;
        col.setDuration(0.1f);
        col.setCollapsed(false);

        // Build Header
        header.background(Tex.underline);
        header.button("@message.player-connect.title", Icon.downOpen, Styles.togglet, () -> {
            col.toggle();
            Element e = header.getChildren().get(0);
            if (e instanceof Button) {
                for (Element child : ((Button) e).getChildren()) {
                    if (child instanceof Image) {
                        ((Image) child).setDrawable(col.isCollapsed() ? Icon.rightOpen : Icon.downOpen);
                    }
                }
            }
        }).growX().left().labelAlign(Align.left).padBottom(3).get().getLabelCell().padRight(18).growX();

        header.button(Icon.refresh, Styles.defaulti, this::setupPlayerConnect).size(52f).padBottom(3).right();

        // Swap Trick:
        // 1. Get existing children (Snapshot to avoid modification issues)
        Seq<Element> children = new Seq<>(hosts.getChildren());

        // 2. Clear hosts table
        hosts.clear();

        // 3. Add our section at the top
        hosts.add(header).growX().padTop(10).row();
        hosts.add(col).width((targetWidth() + 5f) * columns());
        hosts.row();
        hosts.image().growX().pad(5).padLeft(10).padRight(10).height(3).color(Pal.accent);
        hosts.row();

        // 4. Add back existing children, skipping our own if they were present
        for (Element child : children) {
            if (HEADER_NAME.equals(child.name) || COLLAPSER_NAME.equals(child.name)) {
                continue;
            }

            if (child instanceof Table) {
                // It's likely a header
                hosts.add(child).growX().padTop(10).row();
            } else if (child instanceof Collapser) {
                hosts.add(child).growX().row();
            } else {
                // Fallback for other elements
                hosts.add(child).growX().row();
            }
        }

        // Initial Setup
        setupPlayerConnect();
    }

    private void setupPlayerConnect() {
        playerConnectTable.clear();
        playerConnectTable.labelWrap(Core.bundle.format("message.loading"))
                .center()
                .labelAlign(0)
                .expand()
                .fill();

        playerConnectService.findPlayerConnectRooms(searchTerm, rooms -> {
            playerConnectTable.clear();

            if (rooms != null && rooms.isEmpty()) {
                playerConnectTable.labelWrap(Core.bundle.format("message.no-rooms-found"))
                        .center()
                        .labelAlign(0)
                        .expand()
                        .fill()
                        .pad(10);
                return;
            }

            int cols = columns();

            var groups = new HashMap<String, Seq<PlayerConnectRoom>>();

            for (var room : rooms) {
                var link = PlayerConnectLink.fromString(room.link());
                var group = link.host;

                if (!groups.containsKey(group)) {
                    groups.put(group, new Seq<>());
                }
                groups.get(group).add(room);
            }

            for (var host : groups.entrySet()) {
                playerConnectTable.table(hostLabel -> {
                    hostLabel.add(host.getKey()).top().left().padLeft(10).padTop(10).padBottom(10);
                    hostLabel.image().growX().pad(10).height(3).color(Pal.gray);
                })
                        .growX().colspan(cols).row();

                playerConnectTable.table(container -> {
                    int i = 0;

                    container.center();

                    for (var room : host.getValue()) {
                        buildPlayerConnectRoom(container, room);
                        if (++i % cols == 0) {
                            container.row();
                        }
                    }
                }).growX();
                playerConnectTable.row();
            }
        });
    }

    private void buildPlayerConnectRoom(Table container, PlayerConnectRoom room) {
        float twidth = targetWidth();
        float contentWidth = twidth - 40f;

        container.table(Styles.black8, t -> {
            t.top().left();

            // Header: Name, Version, Lock
            t.table(header -> {
                header.left();
                header.setColor(Pal.gray);

                boolean isSecured = room.data().isSecured();

                header.table(info -> {
                    info.left();

                    float lockWidth = 16f;
                    float nameWidth = contentWidth - lockWidth - 10f;

                    info.add((isSecured ? Iconc.lock + " " : "") + room.data().name()).style(Styles.outlineLabel)
                            .fontScale(1.25f)
                            .width(nameWidth).left().ellipsis(true);

                }).growX().left().padLeft(10f).padTop(5f);

                // Copy Link Button (Top Right)
                header.button(Icon.copy, Styles.clearNonei, () -> {
                    Core.app.setClipboardText(room.link());
                    Vars.ui.showInfoFade("@copied");
                }).size(32).padRight(15);

            }).padTop(10f).growX().height(36f).row();

            // Body
            t.table(body -> {
                body.top().left();
                body.setColor(Pal.gray);
                body.left();
                body.margin(10);

                // Map & Mode
                String mapModeString = "[lightgray]" + Core.bundle.format("save.map", room.data().mapName()) +
                        " [lightgray]/ [accent]" + room.data().gamemode();

                body.add(mapModeString)
                        .left()
                        .width(contentWidth)
                        .padBottom(6)
                        .ellipsis(true)
                        .row();

                // Players
                String names = room.data().players().map(n -> n.name() + "[]").toString(", ");
                body.add(Iconc.players + " [accent]" + names)
                        .padBottom(6)
                        .left()
                        .width(contentWidth)
                        .get().setWrap(true);
                body.row();

                // Mods
                if (room.data().mods().size > 0) {
                    body.add(Iconc.book + " [lightgray]" + Strings.join(", ", room.data().mods())).left()
                            .padBottom(6)
                            .width(contentWidth)
                            .ellipsis(true)
                            .row();
                }

                // Mod Conflicts
                Seq<String> serverMods = room.data().mods();
                Seq<String> localModNames = Vars.mods.list().select(m -> !m.meta.hidden).map(m -> m.name);

                Seq<String> serverModNames = serverMods
                        .map(s -> s.indexOf(':') != -1 ? s.substring(0, s.indexOf(':')) : s);

                Seq<String> missing = serverMods.select(s -> {
                    String name = s.indexOf(':') != -1 ? s.substring(0, s.indexOf(':')) : s;
                    return !localModNames.contains(name);
                });

                Seq<String> unneeded = localModNames.select(m -> !serverModNames.contains(m));

                if (!missing.isEmpty()) {
                    body.labelWrap("[scarlet]Missing: " + Strings.join(", ", missing))
                            .left()
                            .labelAlign(Align.left)
                            .width(contentWidth)
                            .padBottom(6);
                    body.row();
                }

                if (!unneeded.isEmpty()) {
                    body.labelWrap("[scarlet]Unneeded: " + Strings.join(", ", unneeded))
                            .left()
                            .labelAlign(Align.left)
                            .width(contentWidth)
                            .padBottom(6);
                    body.row();
                }

                // Locale
                body.add(Iconc.chat + " [lightgray]" + room.data().locale()).left().padBottom(6)
                        .row();

                String versionString = getVersionString(room.data().version());

                body.add("[white]" + Iconc.info + " [lightgray]" + versionString).style(Styles.outlineLabel)
                        .color(Pal.lightishGray)
                        .width(contentWidth)
                        .padBottom(6)
                        .left()
                        .row();

                // Spacer

                body.add().growY().row();

                // Join Button
                body.button(Core.bundle.format("join"), Icon.play, () -> {
                    joinRoom(room);
                }).growX().height(40f).padTop(5);

            }).growY().growX().left().bottom();

        }).minWidth(twidth).padBottom(5).padRight(5).growY();
    }

    private void joinRoom(PlayerConnectRoom room) {
        var link = PlayerConnectLink.fromString(room.link());

        if (!room.data().isSecured()) {
            try {
                PlayerConnect.join(link, "", () -> Log.info("Joined room: " + link));
            } catch (Throwable e) {
                Vars.ui.showException("@message.connect.fail", e);
            }
            return;
        }

        BaseDialog connect = new BaseDialog("@message.type-password.title");
        String[] password = { "" };

        connect.cont.table(table -> {
            table.add("@message.password").padRight(5f).right();
            table.field(password[0], text -> password[0] = text)
                    .size(320f, 54f)
                    .valid(t -> t.length() > 0 && t.length() <= 100)
                    .maxTextLength(100)
                    .left()
                    .get();
            table.row().add();
        }).row();

        connect.buttons.button("@cancel", connect::hide).minWidth(210);
        connect.buttons.button("@ok", () -> {
            try {
                PlayerConnect.join(link, password[0], connect::hide);
            } catch (Throwable e) {
                connect.hide();
                Vars.ui.showException("@message.connect.fail", e);
            }
        }).minWidth(210);

        connect.show();
    }

    private int columns() {
        return Mathf.clamp((int) ((Core.graphics.getWidth() / Scl.scl() * 0.9f) / targetWidth()), 1, 4);
    }

    float targetWidth() {
        return Math.min(Core.graphics.getWidth() / Scl.scl() * 0.9f, 550f);
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

        public String toString() {
            return "BuildInfo{" +
                    "type='" + type + '\'' +
                    ", build=" + build +
                    ", revision=" + revision +
                    ", modifier='" + modifier + '\'' +
                    '}';
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
        }
        return info;
    }
}
