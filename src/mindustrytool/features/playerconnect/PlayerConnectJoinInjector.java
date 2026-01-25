package mindustrytool.features.playerconnect;

import arc.Core;
import arc.func.Cons;
import arc.math.Mathf;
import arc.scene.ui.layout.Scl;
import arc.scene.Element;
import arc.scene.ui.Button;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Collapser;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Reflect;
import arc.util.Http.HttpStatusException;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.JoinDialog;
import mindustrytool.services.PlayerConnectService;

import java.util.HashMap;

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

        Cons<Seq<PlayerConnectRoom>> renderRooms = rooms -> {
            playerConnectTable.clear();

            if (rooms == null || rooms.isEmpty()) {
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

            // Map provider addresses -> provider names for grouping
            var addrToName = new HashMap<String, String>();
            for (int i = 0; i < PlayerConnectProviders.online.size; i++) {
                String name = PlayerConnectProviders.online.getKeyAt(i);
                String address = PlayerConnectProviders.online.getValueAt(i);
                String addrHost = address;
                // Handle IPv6 like [::1]:1234
                if (addrHost.startsWith("[") && addrHost.contains("]:")) {
                    addrHost = addrHost.substring(1, addrHost.indexOf("]:"));
                } else if (addrHost.contains(":")) {
                    int idx = addrHost.lastIndexOf(':');
                    addrHost = addrHost.substring(0, idx);
                }
                addrToName.put(addrHost, name);
            }

            for (var room : rooms) {
                var link = PlayerConnectLink.fromString(room.getLink());
                var host = link.host;
                var group = addrToName.getOrDefault(host, host);

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
                        PlayerConnectRenderer.render(container, room, targetWidth());
                        if (++i % cols == 0) {
                            container.row();
                        }
                    }
                }).growX();
                playerConnectTable.row();
            }
        };

        Cons<Throwable> renderError = error -> {
            String message = error.getMessage();

            if (error instanceof HttpStatusException httpStatusException) {
                message = httpStatusException.response.getResultAsString();
            }

            playerConnectTable.clear();
            playerConnectTable.labelWrap(message)
                    .center()
                    .labelAlign(0)
                    .expand()
                    .fill()
                    .pad(10);
        };

        PlayerConnectProviders.refreshOnline(
                () -> playerConnectService.findPlayerConnectRooms(searchTerm)
                        .thenAccept(data -> Core.app.post(() -> renderRooms.get(data)))
                        .exceptionally(error -> {
                            Core.app.post(() -> renderError.get(error));
                            return null;
                        }),
                e -> playerConnectService.findPlayerConnectRooms(searchTerm)
                        .thenAccept(data -> Core.app.post(() -> renderRooms.get(data)))
                        .exceptionally(error -> {
                            Core.app.post(() -> renderError.get(error));
                            return null;
                        }));
    }

    private int columns() {
        return Mathf.clamp((int) ((Core.graphics.getWidth() / Scl.scl() * 0.9f) / targetWidth()), 1, 4);
    }

    float targetWidth() {
        return Math.min(Core.graphics.getWidth() / Scl.scl() * 0.9f, 550f);
    }
}
