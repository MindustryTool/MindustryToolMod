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
            float availableWidth = Core.graphics.getWidth();
            // Estimate card width (including padding used in ContentPreviewFactory)
            float itemWidth = Scl.scl(cardWidth) + Scl.scl(12);

            // Calculate columns: (Screen - DialogMargin) / ItemWidth
            // Using 60 as safety margin for dialog borders
            int cols = Math.max(1, (int) ((availableWidth - Scl.scl(60)) / itemWidth));

            int i = 0;
            for (T data : dataList) {
                if (data == null)
                    continue;

                ContentPreviewFactory.create(p, data, type, infoDialog, hide, cardWidth);

                if (++i % cols == 0) {
                    p.row();
                }
            }
            p.top();
        }).scrollY(true).expand().fill();
    }
}
