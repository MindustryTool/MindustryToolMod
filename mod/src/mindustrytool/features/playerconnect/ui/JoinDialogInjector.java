package mindustrytool.features.playerconnect.ui;

import arc.Events;
import arc.func.Cons;
import arc.scene.Element;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Reflect;
import mindustry.Vars;
import mindustry.game.EventType.ResizeEvent;
import mindustry.ui.dialogs.JoinDialog;
import mindustrytool.features.playerconnect.PlayerConnectFeature;

public class JoinDialogInjector {

    private static final String ROOT_NAME = "pc-browser-root";

    private final PlayerConnectFeature feature;
    private final Cons<ResizeEvent> resizeListener;
    private @Nullable RoomBrowserView browserView;

    public JoinDialogInjector(PlayerConnectFeature feature) {
        this.feature = feature;
        this.resizeListener = event -> {
            if (feature.enabled().peek()) {
                performInjection();
            }
        };

        Events.on(ResizeEvent.class, resizeListener);
    }

    public void dispose() {
        Events.remove(ResizeEvent.class, resizeListener);

        if (browserView != null) {
            browserView.element().remove();
            browserView.dispose();
            browserView = null;
        }
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

        Table hosts = Reflect.get(dialog, "hosts");

        if (hosts == null) {
            return;
        }

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
