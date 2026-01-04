package mindustrytool.features.content.browser;

import arc.Core;
import arc.scene.ui.layout.Table;
import mindustry.gen.*;
import mindustry.ui.*;
import arc.struct.Seq;
import arc.func.Cons;
import static mindustry.Vars.ui;

/** Unified pagination footer for browser dialogs. */
public class PaginationFooter {
    public static <T> void build(Table f, PagingRequest<T> request, Cons<Seq<T>> handler, String uploadUrl) {
        f.clear();
        f.defaults().margin(4).pad(4).width(100).height(40);
        f.button(Icon.left, () -> request.previousPage(handler))
                .disabled(request.isLoading() || request.getPage() == 0 || request.isError());
        f.table(Tex.buttonDisabled, t -> t.labelWrap(String.valueOf(request.getPage() + 1)).width(50)
                .style(Styles.defaultLabel).labelAlign(0).center().fill()).width(70);
        f.button(Icon.edit, () -> ui.showTextInput("@select-page", "", "", input -> {
            try {
                request.setPage(Integer.parseInt(input));
                request.getPage(handler);
            } catch (Exception e) {
                ui.showInfo("Invalid input");
            }
        })).disabled(request.isLoading() || !request.hasMore() || request.isError());
        f.button(Icon.right, () -> request.nextPage(handler))
                .disabled(request.isLoading() || !request.hasMore() || request.isError());
        if (uploadUrl != null)
            f.button("@upload", () -> Core.app.openURI(uploadUrl)).disabled(request.isLoading());
    }
}
