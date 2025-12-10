package mindustrytool.ui.server;

import arc.Core;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustrytool.core.config.Config;
import mindustrytool.data.api.PagingRequest;
import mindustrytool.core.model.ServerData;
import arc.struct.Seq;
import arc.func.Cons;

public class ServerFooter {
    public static void render(Table parent, PagingRequest<ServerData> request, Cons<Seq<ServerData>> handler) {
        parent.table(f -> {
            f.button(Icon.left, () -> request.previousPage(handler)).margin(4).pad(4).width(100).height(40)
                    .disabled(request.isLoading() || request.getPage() == 0 || request.isError());

            f.table(Tex.buttonDisabled, t -> t.labelWrap(String.valueOf(request.getPage() + 1))
                    .width(50).style(Styles.defaultLabel).labelAlign(0).center().fill()).pad(4).height(40);

            f.button(Icon.edit, () -> Vars.ui.showTextInput("@select-page", "", "", input -> {
                try { request.setPage(Integer.parseInt(input)); request.getPage(handler); }
                catch (Exception e) { Vars.ui.showInfo("Invalid input"); }
            })).margin(4).pad(4).width(100).height(40).disabled(request.isLoading() || !request.hasMore() || request.isError());

            f.button(Icon.right, () -> request.nextPage(handler)).margin(4).pad(4).width(100).height(40)
                    .disabled(request.isLoading() || !request.hasMore() || request.isError());

            f.button("@upload", () -> Core.app.openURI(Config.UPLOAD_SCHEMATIC_URL)).margin(4).pad(4).width(100).height(40)
                    .disabled(request.isLoading() || !request.hasMore() || request.isError());

            f.bottom();
        }).expandX().fillX();
    }
}
