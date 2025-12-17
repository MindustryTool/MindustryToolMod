package mindustrytool.plugins.browser;

import arc.Core;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.Seq;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class BrowserContentBuilder {
    public static <T extends ContentData> void build(Table c, PagingRequest<T> request,
            Seq<T> dataList, ContentType type, BaseDialog infoDialog, Runnable hide, arc.func.Cons<Seq<T>> handler) {

        // Read card width from settings
        int cardWidth = BrowserSettingsDialog.getCardWidth(type);

        c.clear();
        if (request.isLoading()) {
            c.labelWrap(Core.bundle.get("message.loading")).center().labelAlign(0).expand().fill();
            return;
        }
        if (request.isError()) {
            c.button("Error: " + request.getError() + " - Reload?", Styles.nonet, () -> request.getPage(handler))
                    .center().labelAlign(0).expand().fill();
            return;
        }
        if (dataList.size == 0) {
            c.pane(p -> p.add("message.no-result"));
            return;
        }
        c.pane(p -> {
            float sum = 0;
            for (T data : dataList) {
                if (data == null)
                    continue;
                Button btn = ContentPreviewFactory.create(p, data, type, infoDialog, hide, cardWidth);
                sum += btn.getPrefWidth();
                if (sum >= Core.graphics.getWidth() * 0.8) {
                    p.row();
                    sum = 0;
                }
            }
            p.top();
        }).scrollY(true).expand().fill();
    }
}
