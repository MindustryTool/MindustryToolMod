package mindustrytool.features.godmode;

import arc.Core;
import mindustry.Vars;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.ui.dialogs.BaseDialog;

public class GodModeCoreSelectionDialog extends BaseDialog {
    public GodModeCoreSelectionDialog(GodModeDialogs.CorePlaceConsumer onPlace) {
        super("Select Core");
        addCloseButton();

        cont.table(t -> {
            t.pane(p -> {
                int i = 0;
                int size = 200;
                int cols = Math.max(1, (int) (Math.min(Core.graphics.getWidth(), 800) / (size + 10)));

                for (Block block : Vars.content.blocks()) {
                    if (!(block instanceof CoreBlock)) {
                        continue;
                    }
                    if (block.isHidden()) {
                        continue;
                    }

                    p.button(b -> {
                        b.image(block.uiIcon).size(32).padRight(5);
                        b.add(block.localizedName).growX().left();
                    }, () -> {
                        remove();
                        new GodModeCoreConfigDialog(block, onPlace).show();
                    }).size(size, 50).pad(5);

                    if (++i % cols == 0) {
                        p.row();
                    }
                }
            }).grow().maxWidth(800);
        }).grow();
    }
}
