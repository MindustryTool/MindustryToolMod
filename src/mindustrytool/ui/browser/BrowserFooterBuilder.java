package mindustrytool.ui.browser;

import arc.scene.ui.layout.Table;
import mindustrytool.data.api.PagingRequest;
import mindustrytool.service.content.ContentType;
import mindustrytool.core.config.Config;
import mindustrytool.ui.common.PaginationFooter;

public class BrowserFooterBuilder {
    public static <T> void build(Table parent, ContentType type, PagingRequest<T> request, arc.func.Cons<arc.struct.Seq<T>> handler) {
        String url = type == ContentType.MAP ? Config.UPLOAD_MAP_URL : Config.UPLOAD_SCHEMATIC_URL;
        PaginationFooter.render(parent, request, handler, url);
    }
}
