package mindustrytool.features.playerconnect.ui;

import arc.Events;
import arc.scene.Element;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustrytool.utils.ReflectUtil;
import mindustry.Vars;
import mindustry.game.EventType.ResizeEvent;
import mindustry.ui.dialogs.JoinDialog;
import mindustrytool.features.playerconnect.PlayerConnectFeature;

public class JoinDialogInjector {

    private static final String ROOT_NAME = "pc-browser-root";

    private final PlayerConnectFeature feature;
    private RoomBrowserView browserView;

    public JoinDialogInjector(PlayerConnectFeature feature) {
        this.feature = feature;

        Events.run(ResizeEvent.class, () -> {
            if (feature.enabled().peek()) {
                performInjection();
            }
        });
    }

    public void inject() {
        JoinDialog dialog = Vars.ui.join;

        if (dialog == null) {
            return;
        }

        dialog.shown(this::performInjection);
    }

    private void performInjection() {
        JoinDialog dialog = Vars.ui.join;
        
        if (dialog == null) {
            return;
        }

        Table hosts = ReflectUtil.getOrNull(dialog, "hosts");
        
        if (hosts == null) {
            return;
        }

        // 2. Add RoomBrowserView to hosts table if not present
        if (hosts.find(ROOT_NAME) == null) {
            if (browserView == null) {
                browserView = new RoomBrowserView(feature);
            }

            Element el = browserView.element();
            el.name = ROOT_NAME;

            Seq<Element> existing = new Seq<>(hosts.getChildren());
            hosts.clear();

            hosts.add(el).growX().padTop(6).row();

            for (Element child : existing) {
                if (ROOT_NAME.equals(child.name)) {
                    continue;
                }
                hosts.add(child).growX().padTop(6).row();
            }
        }
    }
}
