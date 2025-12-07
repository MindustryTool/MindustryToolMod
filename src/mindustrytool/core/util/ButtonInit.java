package mindustrytool.core.util;

import arc.Core;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.fragments.MenuFragment.MenuButton;
import mindustrytool.Main;

public class ButtonInit {
    public static void add() {
        Vars.ui.schematics.buttons.button("Browse", Icon.menu, () -> {
            Vars.ui.schematics.hide();
            Main.schematicDialog.show();
        });
        String map = Core.bundle.format("message.map-browser.title");
        String pc = Core.bundle.format("message.player-connect.title");
        if (Vars.mobile) {
            Vars.ui.menufrag.addButton(map, Icon.map, Main.mapDialog::show);
            Vars.ui.menufrag.addButton(pc, Icon.menu, Main.playerConnectRoomsDialog::show);
        } else {
            Vars.ui.menufrag.addButton(new MenuButton("Tools", Icon.wrench, () -> {},
                new MenuButton(map, Icon.map, Main.mapDialog::show),
                new MenuButton(pc, Icon.menu, Main.playerConnectRoomsDialog::show)));
        }
    }
}
