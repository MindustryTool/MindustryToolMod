package mindustrytool.core.util;

import arc.Core;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.fragments.MenuFragment.MenuButton;
import mindustrytool.Main;
import mindustrytool.ui.browser.JoinDialogInjector;
import mindustrytool.ui.dialog.LoginDialog;

public class ButtonInit {
    private static LoginDialog loginDialog;

    public static void add() {
        loginDialog = new LoginDialog();
        
        Vars.ui.schematics.buttons.button("Browse", Icon.menu, () -> {
            Vars.ui.schematics.hide();
            Main.schematicDialog.show();
        });
        
        String map = Core.bundle.format("message.map-browser.title");
        
        if (Vars.mobile) {
            Vars.ui.menufrag.addButton(map, Icon.map, Main.mapDialog::show);
        } else {
            Vars.ui.menufrag.addButton(new MenuButton("Tools", Icon.wrench, () -> {},
                new MenuButton(map, Icon.map, Main.mapDialog::show)));
        }
        
        // Inject Player Connect section into JoinDialog
        JoinDialogInjector.inject();
        
        // Create Account button table on main menu stage
        AccountButtonTable.create(loginDialog);
    }
}
