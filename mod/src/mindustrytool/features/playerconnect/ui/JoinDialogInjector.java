package mindustrytool.features.playerconnect.ui;

import arc.Core;
import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Reflect;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.JoinDialog;
import mindustrytool.features.playerconnect.PlayerConnectFeature;

public class JoinDialogInjector {

    private static final String ROOT_NAME = "pc-browser-root";
    private static final String BUTTON_NAME = "pc-join-link-btn";

    private final PlayerConnectFeature feature;
    private RoomBrowserView browserView;

    public JoinDialogInjector(PlayerConnectFeature feature) {
        this.feature = feature;
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

        // 1. Add "Join via Link" button to dialog bottom buttons if not present
        if (dialog.buttons != null && dialog.buttons.find(BUTTON_NAME) == null) {
            dialog.buttons.button(Core.bundle.get("feature.player-connect.join-link-title", "Join via Link"), Icon.add, () -> {
                new JoinRoomDialog().show();
            }).name(BUTTON_NAME).size(210f, 64f);

            @SuppressWarnings("rawtypes")
            Seq<Cell> cells = dialog.buttons.getCells();
            if (cells.size >= 3) {
                cells.swap(cells.size - 1, cells.size - 3);
            }
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
