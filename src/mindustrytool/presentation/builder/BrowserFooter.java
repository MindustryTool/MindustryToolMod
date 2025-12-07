package mindustrytool.presentation.builder;

import arc.Core;
import arc.scene.ui.layout.Table;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustrytool.core.config.Config;
import mindustrytool.data.api.PagingRequest;
import mindustrytool.domain.service.*;
import arc.struct.Seq;
import arc.func.Cons;

public class BrowserFooter {
    public static <T> void render(Table parent, ContentType type, PagingRequest<T> request, Cons<Seq<T>> handler) {
        String uploadUrl = type == ContentType.MAP ? Config.UPLOAD_MAP_URL : Config.UPLOAD_SCHEMATIC_URL;
        parent.table(f -> {
            f.defaults().margin(4).pad(4).width(100).height(40);
            f.button(Icon.left, () -> request.previousPage(handler))
                .disabled(request.isLoading() || request.getPage() == 0 || request.isError());
            f.table(Tex.buttonDisabled, t -> t.labelWrap(String.valueOf(request.getPage() + 1))
                .width(50).style(Styles.defaultLabel).labelAlign(0).center().fill()).width(70);
            f.button(Icon.edit, () -> mindustry.Vars.ui.showTextInput("@select-page", "", "", input -> {
                try { request.setPage(Integer.parseInt(input)); request.getPage(handler); }
                catch (Exception e) { mindustry.Vars.ui.showInfo("Invalid input"); }
            })).disabled(request.isLoading() || !request.hasMore() || request.isError());
            f.button(Icon.right, () -> request.nextPage(handler))
                .disabled(request.isLoading() || !request.hasMore() || request.isError());
            f.button("@upload", () -> Core.app.openURI(uploadUrl)).disabled(request.isLoading());
            f.bottom();
        }).expandX().fillX();
    }
}
