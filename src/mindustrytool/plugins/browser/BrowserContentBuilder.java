package mindustrytool.plugins.browser;

import arc.Core;

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
            // Estimate card width (Strict cardWidth + 8px padding from
            // ContentPreview.create .pad(4))
            float itemWidth = Scl.scl(cardWidth + 8f);

            // Calculate columns: (Screen - DialogMargin) / ItemWidth
            // Adjusted safety margin to 80 to account for vertical scrollbar (~20px) +
            // dialog pads
            int cols = Math.max(1, (int) ((availableWidth - Scl.scl(80)) / itemWidth));

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
        }).scrollY(true).scrollX(false).expand().fill();
    }
}
