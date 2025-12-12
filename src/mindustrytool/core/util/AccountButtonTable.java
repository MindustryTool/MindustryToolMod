package mindustrytool.core.util;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.scene.ui.ImageButton;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.service.auth.AuthService;
import mindustrytool.ui.dialog.LoginDialog;

public class AccountButtonTable {
    private static Table accountTable;
    private static ImageButton accountButton;

    public static void create(LoginDialog loginDialog) {
        accountTable = new Table(Styles.black5) {
            @Override
            public void act(float delta) { super.act(delta); updateButtonState(); updateSize(); }
        };
        accountTable.setTransform(true);
        accountTable.margin(5);
        
        accountButton = accountTable.button(Icon.ok, Styles.emptyi, 32f, () -> loginDialog.show()).get();
        
        Label[] labels = new Label[3];
        accountTable.table(info -> {
            labels[0] = info.add("Account").padLeft(5).left().get();
            info.row();
            labels[1] = info.add("").padLeft(5).left().color(new Color(0.8f, 0.8f, 0.2f, 1f)).get();
            info.row();
            labels[2] = info.add("").padLeft(5).left().get();
        }).left().growX();
        
        SessionManager.init(labels[0], labels[1], labels[2]);
        AuthService.onLoginStateChanged(SessionManager::fetchSession);
        accountTable.addListener(new DragHandler(accountTable));
        
        updateSize();
        accountTable.setPosition(Core.graphics.getWidth() - accountTable.getWidth() - 10, 10);
        Vars.ui.menuGroup.addChild(accountTable);
        
        if (AuthService.isLoggedIn()) SessionManager.fetchSession();
    }

    private static void updateSize() {
        if (accountTable == null) return;
        float w = Core.graphics.getWidth(), h = Core.graphics.getHeight();
        accountTable.setSize(h < w ? w / 5 : w * 4 / 5, h < w ? h / 5 : h / 10);
    }

    private static void updateButtonState() {
        if (accountButton != null) accountButton.getStyle().imageUp = AuthService.isLoggedIn() ? Icon.ok : Icon.play;
    }

    public static Table getTable() { return accountTable; }
}