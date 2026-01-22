package mindustrytool.features.playerconnect;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Log;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.core.Version;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerConnectRenderer {

    public static Cell<Table> render(Table container, PlayerConnectRoom room) {
        return render(container, room, -1);
    }

    public static Cell<Table> render(Table container, PlayerConnectRoom room, float targetWidth) {
        float contentWidth = targetWidth > 0 ? targetWidth - 40f : 0;
        boolean matchProtocolVersion = room.data().protocolVersion().equals(NetworkProxy.PROTOCOL_VERSION);

        return container.table(Styles.black8, t -> {
            t.top().left();

            if (!matchProtocolVersion) {
                t.setColor(Color.red);
            }

            // Header: Name, Version, Lock
            t.table(header -> {
                header.left();
                header.setColor(Pal.gray);

                boolean isSecured = room.data().isSecured();

                header.table(info -> {
                    info.left();

                    float lockWidth = 16f;

                    var label = info.add((isSecured ? Iconc.lock + " " : "") + room.data().name())
                            .style(Styles.outlineLabel)
                            .fontScale(1.25f)
                            .left();

                    if (targetWidth > 0) {
                        float nameWidth = contentWidth - lockWidth - 10f;
                        label.width(nameWidth).ellipsis(true);
                    } else {
                        label.wrap();
                    }

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

                var mapLabel = body.add(mapModeString)
                        .left()
                        .padBottom(6);

                if (targetWidth > 0) {
                    mapLabel.width(contentWidth).ellipsis(true);
                } else {
                    mapLabel.wrap().growX();
                }
                body.row();

                // Players
                String names = room.data().players().map(n -> n.name() + "[]").toString(", ");
                var playersLabel = body.add(Iconc.players + " [accent]" + names)
                        .padBottom(6)
                        .left()
                        .get();

                if (targetWidth > 0) {
                    playersLabel.setWidth(contentWidth);
                }
                playersLabel.setWrap(true);

                body.row();

                // Mods
                if (room.data().mods().size > 0) {
                    var modsLabel = body.add(Iconc.book + " [lightgray]" + Strings.join(", ", room.data().mods()))
                            .left()
                            .padBottom(6);

                    if (targetWidth > 0) {
                        modsLabel.width(contentWidth).ellipsis(true);
                    } else {
                        modsLabel.wrap().growX();
                    }
                    body.row();
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
                    var label = body.labelWrap("[scarlet]Missing: " + Strings.join(", ", missing))
                            .left()
                            .labelAlign(Align.left)
                            .padBottom(6);

                    if (targetWidth > 0) {
                        label.width(contentWidth);
                    } else {
                        label.growX();
                    }
                    body.row();
                }

                if (!unneeded.isEmpty()) {
                    var label = body.labelWrap("[scarlet]Unneeded: " + Strings.join(", ", unneeded))
                            .left()
                            .labelAlign(Align.left)
                            .padBottom(6);

                    if (targetWidth > 0) {
                        label.width(contentWidth);
                    } else {
                        label.growX();
                    }
                    body.row();
                }

                // Locale
                body.add(Iconc.chat + " [lightgray]" + room.data().locale()).left().padBottom(6)
                        .row();

                String versionString = getVersionString(room.data().version());

                var versionLabel = body.add("[white]" + Iconc.info + " [lightgray]" + versionString)
                        .style(Styles.outlineLabel)
                        .color(Pal.lightishGray)
                        .padBottom(6)
                        .left();

                if (targetWidth > 0) {
                    versionLabel.width(contentWidth);
                } else {
                    versionLabel.growX();
                }
                body.row();

                if (!matchProtocolVersion) {
                    body.add("[red]" + Iconc.info + " Protocol version mismatch, current: "
                            + NetworkProxy.PROTOCOL_VERSION + ", required: " + room.data().protocolVersion())
                            .style(Styles.outlineLabel)
                            .color(Pal.lightishGray)
                            .padBottom(6)
                            .left();
                }

                // Spacer

                body.add().growY().row();

                // Join Button
                if (matchProtocolVersion) {
                    body.button(Core.bundle.format("join"), Icon.play, () -> {
                        joinRoom(room);
                    }).growX().height(40f).padTop(5);
                } else {
                    body.button(Core.bundle.format("player-connect.unmatch-protocol-version"), Icon.play, () -> {
                        Vars.ui.showInfo("Howw");
                    })
                            .disabled(true)
                            .growX().height(40f).padTop(5);
                }

            }).growY().growX().left().bottom();

        }).padBottom(5).padRight(5).growY();
    }

    public static void joinRoom(PlayerConnectRoom room) {
        // Check for unneeded mods
        Seq<String> serverMods = room.data().mods();
        Seq<String> localModNames = Vars.mods.list().select(m -> !m.meta.hidden).map(m -> m.name);
        Seq<String> serverModNames = serverMods
                .map(s -> s.indexOf(':') != -1 ? s.substring(0, s.indexOf(':')) : s);

        Seq<String> unneeded = localModNames.select(m -> !serverModNames.contains(m));

        if (!unneeded.isEmpty()) {
            BaseDialog dialog = new BaseDialog("@warning");
            dialog.cont.add("Unneeded mods detected. Disable them?").row();
            dialog.cont.label(() -> unneeded.toString(", ")).color(Pal.lightishGray).width(400f).wrap().row();

            dialog.buttons.button("@cancel", dialog::hide).size(100, 50);

            dialog.buttons.button("Disable & Join", () -> {
                unneeded.each(name -> {
                    var mod = Vars.mods.getMod(name);
                    if (mod != null) {
                        Vars.mods.setEnabled(mod, false);
                    }
                });
                dialog.hide();
                proceedToJoin(room);
            }).size(150, 50);

            dialog.buttons.button("Ignore", () -> {
                dialog.hide();
                proceedToJoin(room);
            }).size(100, 50);

            dialog.show();
        } else {
            proceedToJoin(room);
        }
    }

    private static void proceedToJoin(PlayerConnectRoom room) {
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

    private static String getVersionString(String versionString) {
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

    private static BuildInfo extract(String combined) {
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
