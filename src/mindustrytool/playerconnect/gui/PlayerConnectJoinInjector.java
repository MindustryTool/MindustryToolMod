// Khai báo package cho GUI của module Player Connect
package mindustrytool.playerconnect.gui;

import arc.Core;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.ui.layout.Collapser;
import arc.scene.ui.layout.Scl;
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
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.dialogs.JoinDialog;
// Import từ data package
import mindustrytool.playerconnect.data.PlayerConnectLink;
import mindustrytool.playerconnect.data.PlayerConnectRoom;
// Import từ net package
import mindustrytool.playerconnect.net.PlayerConnect;
import mindustrytool.playerconnect.net.PlayerConnectApi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Injects a PlayerConnect section into the game's native JoinDialog.
 * Mimics the game's section() pattern to add rooms from PlayerConnect API.
 */
public class PlayerConnectJoinInjector {
    private final Table playerConnectTable = new Table();
    private JoinDialog dialog;
    private Table hosts;

    private static final String HEADER_NAME = "pc-header";
    private static final String COLLAPSER_NAME = "pc-collapser";

    /**
     * Inject PlayerConnect section into the join dialog.
     * Must be called after dialog.shown() to ensure hosts table exists.
     */
    public void inject(JoinDialog dialog) {
        this.dialog = dialog;

        try {
            this.hosts = Reflect.get(dialog, "hosts");
        } catch (Throwable e) {
            Log.err("[PlayerConnectJoinInjector] Failed to get hosts table", e);
            return;
        }

        if (this.hosts == null) {
            Log.warn("[PlayerConnectJoinInjector] hosts table is null");
            return;
        }

        // Check if already injected - look for our header
        for (Element child : hosts.getChildren()) {
            if (child.name != null && child.name.equals(HEADER_NAME)) {
                // Already injected, just refresh
                refreshPlayerConnect();
                return;
            }
        }

        // Create header similar to game's section() method
        Table header = new Table();
        header.name = HEADER_NAME;

        Collapser collapser = new Collapser(playerConnectTable, Core.settings.getBool("collapsed-pc", false));
        collapser.name = COLLAPSER_NAME;
        collapser.setDuration(0.1f);

        // Build header with toggle button (similar to game's section method)
        header.table(name -> {
            name.add("@message.player-connect.title").pad(10).growX().left().color(Pal.accent);

            // Refresh button
            name.button(Icon.refresh, Styles.emptyi, this::refreshPlayerConnect)
                .size(40f).right().padRight(3);

            // Collapse toggle button
            name.button(Icon.downOpen, Styles.emptyi, () -> {
                collapser.toggle(false);
                Core.settings.put("collapsed-pc", collapser.isCollapsed());
            }).update(i -> i.getStyle().imageUp = (!collapser.isCollapsed() ? Icon.upOpen : Icon.downOpen))
              .size(40f).right().padRight(10f);
        }).growX();

        // Store existing children
        Seq<Element> existingChildren = new Seq<>(hosts.getChildren());

        // Clear and rebuild with our section at the TOP
        hosts.clear();

        // Add our PlayerConnect section first
        hosts.add(header).growX().row();
        hosts.image().growX().pad(5).padLeft(10).padRight(10).height(3).color(Pal.accent);
        hosts.row();
        hosts.add(collapser).width((targetWidth() + 5f) * columns());
        hosts.row();

        // Re-add all existing children (local, remote, global sections)
        for (Element child : existingChildren) {
            if (child.name != null && (child.name.equals(HEADER_NAME) || child.name.equals(COLLAPSER_NAME))) {
                continue; // Skip our own elements if present
            }
            hosts.add(child).growX().row();
        }

        // Initial data load
        refreshPlayerConnect();

        Log.info("[PlayerConnectJoinInjector] Successfully injected into JoinDialog");
    }

    private void refreshPlayerConnect() {
        playerConnectTable.clear();
        playerConnectTable.labelWrap("@message.loading")
            .center()
            .labelAlign(Align.center)
            .expand()
            .fill()
            .pad(10);

        PlayerConnectApi.findPlayerConnectRooms("", rooms -> {
            playerConnectTable.clear();

            if (rooms.isEmpty()) {
                playerConnectTable.labelWrap("@message.no-rooms-found")
                    .center()
                    .labelAlign(Align.center)
                    .expand()
                    .fill()
                    .pad(10);
                return;
            }

            int i = 0;
            int cols = columns();
            for (PlayerConnectRoom room : rooms) {
                buildPlayerConnectRoom(room);
                if (++i % cols == 0) {
                    playerConnectTable.row();
                }
            }
        });
    }

    private void buildPlayerConnectRoom(PlayerConnectRoom room) {
        float twidth = targetWidth();
        float contentWidth = twidth - 40f;

        playerConnectTable.table(Styles.black8, t -> {
            t.top().left();

            // Header row: name + version + lock icon
            t.table(header -> {
                header.left();
                header.setColor(Pal.gray);

                boolean isSecured = room.data() != null && room.data().isSecured();

                header.table(info -> {
                    info.left();
                    String name = room.data() != null ? room.data().name() : "Unknown";
                    info.add((isSecured ? Iconc.lock + " " : "") + name)
                        .style(Styles.outlineLabel)
                        .fontScale(1.15f)
                        .width(contentWidth - 50f)
                        .left()
                        .ellipsis(true);
                }).growX().left().padLeft(10f).padTop(5f);

                // Copy link button
                header.button(Icon.copy, Styles.clearNonei, () -> {
                    Core.app.setClipboardText(room.link());
                    Vars.ui.showInfoFade("@copied");
                }).size(32).padRight(10);

            }).padTop(10f).growX().height(36f).row();

            // Body content
            t.table(body -> {
                body.top().left();
                body.setColor(Pal.gray);
                body.left();
                body.margin(10);

                if (room.data() != null) {
                    // Map & Mode
                    String mapName = room.data().mapName() != null ? room.data().mapName() : "Unknown";
                    String gamemode = room.data().gamemode() != null ? room.data().gamemode() : "";
                    String mapModeString = "[lightgray]" + Core.bundle.format("save.map", mapName) +
                        " [lightgray]/ [accent]" + gamemode;
                    body.add(mapModeString)
                        .left()
                        .width(contentWidth)
                        .padBottom(2)
                        .ellipsis(true)
                        .row();

                    // Players
                    int playerCount = room.data().players() != null ? room.data().players().size : 0;
                    body.add(Iconc.players + " [accent]" + playerCount)
                        .padBottom(2)
                        .left()
                        .row();

                    // Mods
                    if (room.data().mods() != null && room.data().mods().size > 0) {
                        body.add(Iconc.book + " [lightgray]" + Strings.join(", ", room.data().mods()))
                            .left()
                            .padBottom(2)
                            .width(contentWidth)
                            .ellipsis(true)
                            .row();
                    }

                    // Version
                    String versionString = getVersionString(room.data().version());
                    if (!versionString.isEmpty()) {
                        body.add("[white]" + Iconc.info + " [lightgray]" + versionString)
                            .style(Styles.outlineLabel)
                            .color(Pal.lightishGray)
                            .width(contentWidth)
                            .padBottom(2)
                            .left()
                            .row();
                    }
                }

                // Join button
                body.button(Core.bundle.format("join"), Icon.play, () -> {
                    joinRoom(room);
                }).growX().height(40f).padTop(5);

            }).growY().growX().left().bottom();

        }).minWidth(twidth).pad(5).growY();
    }

    private void joinRoom(PlayerConnectRoom room) {
        if (room.data() != null && room.data().isSecured()) {
            // Show password dialog
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
            }).row();

            connect.buttons.button("@cancel", connect::hide).minWidth(210);
            connect.buttons.button("@ok", () -> {
                try {
                    PlayerConnect.joinRoom(
                        PlayerConnectLink.fromString(room.link()),
                        password[0],
                        () -> {
                            connect.hide();
                            dialog.hide();
                        });
                } catch (Throwable e) {
                    connect.hide();
                    Vars.ui.showException("@message.connect.fail", e);
                }
            }).minWidth(210);

            connect.show();
        } else {
            // Direct join (no password)
            try {
                PlayerConnect.joinRoom(
                    PlayerConnectLink.fromString(room.link()),
                    "",
                    () -> dialog.hide());
            } catch (Throwable e) {
                Vars.ui.showException("@message.connect.fail", e);
            }
        }
    }

    private int columns() {
        return Mathf.clamp((int)((Core.graphics.getWidth() / Scl.scl() * 0.9f) / targetWidth()), 1, 4);
    }

    private float targetWidth() {
        return Math.min(Core.graphics.getWidth() / Scl.scl() * 0.9f, 550f);
    }

    private String getVersionString(String versionString) {
        if (versionString == null || versionString.isEmpty()) {
            return "";
        }

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

    private BuildInfo extract(String combined) {
        BuildInfo info = new BuildInfo();
        if (combined == null || combined.isEmpty()) {
            return info;
        }

        // Pattern: "build 146" or "v146" or just "146"
        Pattern pattern = Pattern.compile("(?:build\\s*)?(\\d+)(?:\\s*(.*))?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(combined.trim());

        if (matcher.find()) {
            try {
                info.build = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                info.build = -1;
            }
            if (matcher.groupCount() > 1 && matcher.group(2) != null) {
                info.modifier = matcher.group(2).trim();
            }
        }

        return info;
    }

    private static class BuildInfo {
        public String type = "custom";
        public int build = -1;
        @SuppressWarnings("unused")
        public int revision = -1;
        @SuppressWarnings("unused")
        public String modifier;
    }
}
