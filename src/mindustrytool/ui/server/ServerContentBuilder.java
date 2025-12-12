package mindustrytool.ui.server;

import arc.Core;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import mindustrytool.core.model.ServerData;
import mindustrytool.data.api.PagingRequest;
import mindustrytool.ui.component.ServerCard;

public class ServerContentBuilder {
    public static void build(Table parent, PagingRequest<ServerData> request, Seq<ServerData> data, arc.func.Cons<Seq<ServerData>> handler) {
        parent.table(c -> {
            if (request.isLoading()) { c.labelWrap(Core.bundle.format("message.loading")).center().labelAlign(0).expand().fill(); return; }
            if (request.isError()) { c.button("Error: " + request.getError() + " - Reload?", mindustry.ui.Styles.nonet, () -> request.getPage(handler)).center().labelAlign(0).expand().fill(); return; }
            if (data.size == 0) { c.pane(p -> p.add("message.no-result")); return; }
            c.pane(p -> {
                int cols = Math.max(1, (int)(Core.graphics.getWidth() / 800f)), i = 0;
                for (ServerData d : data) { try { ServerCard.render(p, d); if (++i % cols == 0) p.row(); } catch (Exception e) { Log.err("Error rendering server card", e); } }
                p.top();
            }).scrollY(true).expand().fill();
        }).expand().fill().top();
    }
}
