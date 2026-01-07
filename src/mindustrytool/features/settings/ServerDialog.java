package mindustrytool.features.settings;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Log;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Config;
import mindustrytool.dto.ServerData;
import mindustrytool.dto.PagingRequest;

public class ServerDialog extends BaseDialog {

    private Seq<ServerData> serversData = new Seq<>();
    private PagingRequest<ServerData> request;

    public ServerDialog() {
        super("Server Browser");

        request = new PagingRequest<>(ServerData.class, Config.API_URL + "servers");

        setItemPerPage();

        onResize(() -> {
            setItemPerPage();
            ServerBrowser();
        });
        request.getPage(this::handleServerResult);
        shown(this::ServerBrowser);
    }

    private void setItemPerPage() {
        request.setItemPerPage(20);
    }

    private void ServerBrowser() {
        clear();

        try {
            addCloseButton();
            row();
            table(searchBar -> {
                searchBar.button("@back", Icon.leftSmall, this::hide)//
                        .width(150).padLeft(2).padRight(2).left();
            }).left();
            row();
            ServerContainer();
            row();
            Footer();
        } catch (Exception ex) {
            clear();
            addCloseButton();
            table(container -> Error(container, Core.bundle.format("message.error") + "\n Error: " + ex.getMessage()));
            Log.err(ex);
        }
    }

    public void loadingWrapper(Runnable action) {
        Core.app.post(() -> {
            if (request.isLoading())
                Vars.ui.showInfoFade("Loading");
            else
                action.run();
        });
    }

    private Cell<TextButton> Error(Table parent, String message) {
        Cell<TextButton> error = parent.button(message, Styles.nonet, () -> request.getPage(this::handleServerResult));

        return error.center().labelAlign(0).expand().fill();
    }

    private Cell<Label> Loading(Table parent) {
        return parent.labelWrap(Core.bundle.format("message.loading")).center().labelAlign(0).expand().fill();
    }

    private Cell<ScrollPane> ServerScrollContainer(Table parent) {
        if (serversData.size == 0)
            return parent.pane(container -> container.add("message.no-result"));

        return parent.pane(container -> {
            var cols = (int) Math.max(1, Core.graphics.getWidth() / 800);

            int i = 0;
            for (ServerData serverData : serversData) {
                try {
                    ServerCard(container, serverData);

                    if (++i % cols == 0) {
                        container.row();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            container.top();
        }).scrollY(true).expand().fill();

    }

    private void ServerCard(Table container, ServerData data) {
        container.top().left();
        container.background(null);

        Color color = Pal.gray;

        var canConnect = data.mapName() != null && data.address() != null;

        container.button(t -> {
            t.top().left();
            t.setColor(color);
            t.add(data.name()).left().labelAlign(Align.left);
            Draw.reset();
            t.row();

            if (!data.description().isEmpty()) {
                int count = 0;
                StringBuilder result = new StringBuilder(data.description().length());
                for (int i = 0; i < data.description().length(); i++) {
                    char c = data.description().charAt(i);
                    if (c == '\n') {
                        count++;
                        if (count < 3)
                            result.append(c);
                    } else {
                        result.append(c);
                    }
                }
                t.add("[gray]" + result).left().wrap();
                t.row();
            }

            t.add(Core.bundle.format("players", data.players())).left().labelAlign(Align.left);
            t.row();

            if (data.mapName() != null && !data.mapName().isEmpty()) {
                t.add("Map: " + data.mapName()).left().labelAlign(Align.left);
                t.row();

                if (data.address() != null && !data.address().isEmpty()) {
                    t.add("Address: " + data.address() + ":" + data.port()).left().labelAlign(Align.left);
                    t.row();
                }
            }

            if (data.gamemode() != null && !data.gamemode().isEmpty()) {
                t.add("Gamemode: " + data.gamemode()).left().labelAlign(Align.left);
                t.row();
            }

            if (data.mode() != null && !data.mode().isEmpty()) {
                t.add("Mode: " + data.mode()).left().labelAlign(Align.left);
                t.row();
            }

            if (data.mods() != null && !data.mods().isEmpty()) {
                t.add("Mods: " + Strings.join(", ", data.mods())).left().labelAlign(Align.left);
                t.row();
            }

        }, Styles.emptyi, () -> {
            if (canConnect) {
                Vars.ui.join.connect(data.address(), data.port());
            } else {
                Vars.ui.showInfoFade("Cannot connect to this server.");
            }
        })//
                .growY()//
                .growX()//
                .left()//
                .disabled(!canConnect)//
                .bottom()//
                .pad(8);
    }

    private void Footer() {
        table(footer -> {
            footer.button(Icon.left, () -> request.previousPage(this::handleServerResult)).margin(4).pad(4).width(100)
                    .disabled(request.isLoading() || request.getPage() == 0 || request.isError()).height(40);

            footer.table(Tex.buttonDisabled, table -> {
                table.labelWrap(String.valueOf(request.getPage() + 1)).width(50).style(Styles.defaultLabel)
                        .labelAlign(0).center().fill();
            }).pad(4).height(40);

            footer.button(Icon.edit, () -> {
                Vars.ui.showTextInput("@select-page", "", "", input -> {
                    try {
                        request.setPage(Integer.parseInt(input));
                        shown(this::ServerBrowser);
                    } catch (Exception e) {
                        Vars.ui.showInfo("Invalid input");
                    }
                });
            })//
                    .margin(4)//
                    .pad(4)//
                    .width(100)//
                    .disabled(request.isLoading() || request.hasMore() == false || request.isError()).height(40);

            footer.button(Icon.right, () -> request.nextPage(this::handleServerResult))//
                    .margin(4)//
                    .pad(4)//
                    .width(100)//
                    .disabled(request.isLoading() || request.hasMore() == false || request.isError()).height(40);

            footer.button("@upload", () -> Core.app.openURI(Config.UPLOAD_SCHEMATIC_URL)).margin(4).pad(4).width(100)
                    .disabled(request.isLoading() || request.hasMore() == false || request.isError()).height(40);

            footer.bottom();
        }).expandX().fillX();
    }

    private Cell<Table> ServerContainer() {
        return table(container -> {
            if (request.isLoading()) {
                Loading(container);
                return;
            }

            if (request.isError()) {
                Error(container, String.format("There is an error, reload? (%s)", request.getError()));
                return;
            }

            ServerScrollContainer(container);
        }).expand().fill().top();
    }

    private void handleServerResult(Seq<ServerData> servers) {
        if (servers != null)
            this.serversData = servers;
        else
            this.serversData.clear();

        ServerBrowser();
    }
}
