package mindustrytool.ui.server;

import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.func.Cons;
import mindustrytool.core.model.ServerData;
import mindustrytool.data.api.PagingRequest;
import mindustrytool.ui.common.PaginationFooter;

public class ServerFooter {
    public static void render(Table parent, PagingRequest<ServerData> request, Cons<Seq<ServerData>> handler) {
        PaginationFooter.render(parent, request, handler, null);
    }
}
