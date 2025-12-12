package mindustrytool.ui.browser;

import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.ui.dialogs.BaseDialog;
import arc.util.Reflect;
import mindustry.gen.Icon;

public class PausedMenuInjector {
    public static void inject(BaseDialog roomDialog) {
        Vars.ui.paused.shown(() -> {
            Table root = Vars.ui.paused.cont;
            @SuppressWarnings("rawtypes") arc.struct.Seq<arc.scene.ui.layout.Cell> cells = root.getCells();
            if (Vars.mobile) root.row().buttonRow("@message.manage-room.title", Icon.planet, roomDialog::show).disabled(b -> !Vars.net.server()).row();
            else if (Reflect.<Integer>get(cells.get(cells.size - 2), "colspan") == 2)
                root.row().button("@message.manage-room.title", Icon.planet, roomDialog::show).colspan(2).width(450f).disabled(b -> !Vars.net.server()).row();
            else root.row().button("@message.manage-room.title", Icon.planet, roomDialog::show).disabled(b -> !Vars.net.server()).row();
            cells.swap(cells.size - 1, cells.size - 2);
        });
    }
}
