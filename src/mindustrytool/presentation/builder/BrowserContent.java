package mindustrytool.presentation.builder;

import arc.Core;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.Seq;
import mindustry.ui.Styles;
import mindustrytool.core.model.ContentData;
import mindustrytool.data.api.PagingRequest;
import mindustrytool.domain.service.*;
import mindustrytool.presentation.component.ContentPreview;
import mindustry.ui.dialogs.BaseDialog;

public class BrowserContent {
    public static <T extends ContentData> Cell<Table> render(Table parent, PagingRequest<T> request, 
            Seq<T> dataList, ContentType type, BaseDialog infoDialog, ContentClickHandler<T> onClick) {
        return parent.table(c -> {
            if (request.isLoading()) { 
                c.labelWrap(Core.bundle.get("message.loading")).center().labelAlign(0).expand().fill(); 
                return; 
            }
            if (request.isError()) { 
                c.button("Error: " + request.getError() + " - Reload?", Styles.nonet, () -> {})
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
                    if (data == null) continue;
                    ContentPreview.Type pType = type == ContentType.MAP 
                        ? ContentPreview.Type.MAP : ContentPreview.Type.SCHEMATIC;
                    Button btn = new ContentPreview(pType, data, () -> onClick.onClick(data)).create(p, infoDialog);
                    sum += btn.getPrefWidth();
                    if (sum >= Core.graphics.getWidth() * 0.8) { p.row(); sum = 0; }
                }
                p.top();
            }).scrollY(true).expand().fill();
        }).margin(0).expand().fill().top();
    }

    public interface ContentClickHandler<T> {
        void onClick(T data);
    }
}
