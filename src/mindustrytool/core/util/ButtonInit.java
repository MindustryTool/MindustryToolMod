package mindustrytool.core.util;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.fragments.MenuFragment.MenuButton;
import mindustrytool.Main;
import mindustrytool.ui.browser.JoinDialogInjector;
import mindustrytool.ui.dialog.LoginDialog;

public class ButtonInit {
    private static LoginDialog loginDialog;
    private static TextureRegionDrawable toolIcon;

    public static void add() {
        loginDialog = new LoginDialog();
        
        // Load and resize custom icon to match menu icon size (48x48)
        Pixmap original = new Pixmap(Vars.mods.getMod(Main.class).root.child("icon.png"));
        int targetSize = 36;
        Pixmap scaled = new Pixmap(targetSize, targetSize);
        scaled.draw(original, 0, 0, original.width, original.height, 0, 0, targetSize, targetSize, true);
        original.dispose();
        
        Texture tex = new Texture(scaled);
        tex.setFilter(Texture.TextureFilter.linear);
        scaled.dispose();
        
        toolIcon = new TextureRegionDrawable(new TextureRegion(tex));
        
        Vars.ui.schematics.buttons.button("Browse", Icon.menu, () -> {
            Vars.ui.schematics.hide();
            Main.schematicDialog.show();
        });
        
        String map = Core.bundle.format("message.map-browser.title");
        
        if (Vars.mobile) {
            Vars.ui.menufrag.addButton(map, Icon.map, Main.mapDialog::show);
        } else {
            Vars.ui.menufrag.addButton(new MenuButton("Tools", toolIcon, () -> {},
                new MenuButton(map, Icon.map, Main.mapDialog::show)));
        }
        
        // Inject Player Connect section into JoinDialog
        JoinDialogInjector.inject();
        
        // Create Account button table on main menu stage
        AccountButtonTable.create(loginDialog);
    }
}
