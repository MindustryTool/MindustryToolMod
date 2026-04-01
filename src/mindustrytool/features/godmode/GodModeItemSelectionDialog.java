package mindustrytool.features.godmode;

import arc.Core;
import mindustry.Vars;
import mindustry.type.Item;
import mindustry.ui.dialogs.BaseDialog;

public class GodModeItemSelectionDialog extends BaseDialog {
    public GodModeItemSelectionDialog(GodModeDialogs.ItemAddConsumer onAdd) {
        super("Select Item");
        addCloseButton();

        cont.table(t -> {
            t.pane(p -> {
                int i = 0;
                int size = 200;
                int cols = Math.max(1, (int) (Math.min(Core.graphics.getWidth(), 800) / (size + 10)));

                for (Item item : Vars.content.items()) {
                    p.button(b -> {
                        b.image(item.uiIcon).size(32).padRight(5);
                        b.add(item.localizedName).growX().left();
                    }, () -> {
                        remove();
                        new GodModeItemConfigDialog(item, onAdd).show();
                    }).size(size, 50).pad(5);

                    if (++i % cols == 0) {
                        p.row();
                    }
                }
            }).grow().maxWidth(800);
        }).grow();
    }
}
