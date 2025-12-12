package mindustrytool.ui.dialog;

import arc.Core;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.core.config.Config;
import mindustrytool.core.model.ServerData;
import mindustrytool.ui.server.ServerContentBuilder;
import mindustrytool.ui.server.ServerFooter;
import mindustrytool.data.api.PagingRequest;

public class ServerDialog extends BaseDialog {
    private Seq<ServerData> serversData = new Seq<>();
    private final PagingRequest<ServerData> request;

    public ServerDialog() {
        super("Server Browser");
        request = new PagingRequest<>(ServerData.class, Config.API_URL + "servers");
        request.setItemPerPage(20);
        onResize(this::rebuild);
        request.getPage(this::handleResult);
        shown(this::rebuild);
    }

    private void rebuild() {
        clear();
        try {
            addCloseButton();
            row();
            table(sb -> sb.button("@back", Icon.leftSmall, this::hide).width(150).padLeft(2).padRight(2).left()).left();
            row();
            ServerContentBuilder.build(this, request, serversData, this::handleResult);
            row();
            ServerFooter.render(this, request, this::handleResult);
        } catch (Exception ex) {
            clear(); addCloseButton();
            table(c -> c.button(Core.bundle.format("message.error") + "\n" + ex.getMessage(), Styles.nonet, () -> request.getPage(this::handleResult)).center().labelAlign(0).expand().fill());
            Log.err(ex);
        }
    }

    private void handleResult(Seq<ServerData> servers) { serversData = servers != null ? servers : new Seq<>(); rebuild(); }
}
