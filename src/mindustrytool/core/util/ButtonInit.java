package mindustrytool.core.util;

import arc.Core;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.fragments.MenuFragment.MenuButton;
import mindustrytool.Main;
import mindustrytool.domain.service.AuthService;
import mindustrytool.presentation.dialog.LoginDialog;

public class ButtonInit {
    private static LoginDialog loginDialog;

    public static void add() {
        loginDialog = new LoginDialog();
        
        Vars.ui.schematics.buttons.button("Browse", Icon.menu, () -> {
            Vars.ui.schematics.hide();
            Main.schematicDialog.show();
        });
        
        String map = Core.bundle.format("message.map-browser.title");
        String pc = Core.bundle.format("message.player-connect.title");
        String login = Core.bundle.format("login");
        
        if (Vars.mobile) {
            Vars.ui.menufrag.addButton(map, Icon.map, Main.mapDialog::show);
            Vars.ui.menufrag.addButton(pc, Icon.menu, Main.playerConnectRoomsDialog::show);
            Vars.ui.menufrag.addButton(login, AuthService.isLoggedIn() ? Icon.ok : Icon.play, loginDialog::show);
        } else {
            Vars.ui.menufrag.addButton(new MenuButton("Tools", Icon.wrench, () -> {},
                new MenuButton(map, Icon.map, Main.mapDialog::show),
                new MenuButton(pc, Icon.menu, Main.playerConnectRoomsDialog::show),
                new MenuButton(login, AuthService.isLoggedIn() ? Icon.ok : Icon.play, loginDialog::show)));
        }
    }
}
