package mindustrytool.features.autoplay.tasks;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import mindustry.Vars;
import solim.core.BaseComponent;

public class TransportFloatingOverlay extends BaseComponent {

    private final TransportTask task;
    private @Nullable Table root;

    public TransportFloatingOverlay(TransportTask task) {
        this.task = task;
    }

    @Override
    protected Element build() {
        Table table = new Table();
        this.root = table;
        table.name = "transport-floating-overlay";
        table.setFillParent(true);
        table.touchable = Touchable.childrenOnly;

        table.visible(() -> Vars.ui != null && Vars.ui.hudfrag != null && Vars.ui.hudfrag.shown
                && Vars.state != null && Vars.state.isGame());

        reactiveGrid(task.nodes())
                .key(node -> node.pos)
                .children(node -> new TransportNodeView(task, node));

        return table;
    }

    public void attach() {
        Element el = element();
        Core.app.post(() -> {
            if (Vars.ui != null && Vars.ui.hudGroup != null && el.parent == null) {
                Vars.ui.hudGroup.addChild(el);
            }
        });
    }

    public void detach() {
        Element el = element();
        Core.app.post(() -> {
            el.remove();
            dispose();
        });
    }
}
