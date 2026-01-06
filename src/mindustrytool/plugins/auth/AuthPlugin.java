package mindustrytool.plugins.auth;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.Plugin;

/**
 * Plugin: Authentication
 * Provides login/logout functionality with account button UI.
 * Completely self-contained - no external dependencies.
 */
public class AuthPlugin implements Plugin {
    public static LoginDialog loginDialog;
    private static Table accountTable;
    private static ImageButton accountButton;
    private static SessionData session;
    private static Label nameLabel, creditLabel, roleLabel;

    @Override
    public String getName() {
        return "Auth";
    }

    @Override
    public int getPriority() {
        return 80;
    }

    @Override
    public void init() {
        loginDialog = new LoginDialog();
        Events.on(ClientLoadEvent.class, e -> createAccountButton());
    }

    private static void createAccountButton() {
        accountTable = new Table(Styles.black5) {
            @Override
            public void act(float delta) {
                super.act(delta);
                updateButtonState();
                updateSize();
            }
        };
        accountTable.setTransform(true);
        accountTable.margin(5);

        accountButton = accountTable.button(Icon.ok, Styles.emptyi, 32f, () -> loginDialog.show()).get();

        accountTable.table(info -> {
            nameLabel = info.add("Account").padLeft(5).left().get();
            info.row();
            creditLabel = info.add("").padLeft(5).left().color(new Color(0.8f, 0.8f, 0.2f, 1f)).get();
            info.row();
            roleLabel = info.add("").padLeft(5).left().get();
        }).left().growX();

        AuthService.onLoginStateChanged(AuthPlugin::fetchSession);
        accountTable.addListener(new DragHandler(accountTable));

        updateSize();
        accountTable.setPosition(Core.graphics.getWidth() - accountTable.getWidth() - 10, 10);
        Vars.ui.menuGroup.addChild(accountTable);

        if (AuthService.isLoggedIn())
            fetchSession();
    }

    private static void fetchSession() {
        if (!AuthService.isLoggedIn()) {
            session = null;
            updateLabels();
            return;
        }
        Api.getSession(s -> {
            session = s;
            updateLabels();
        }, e -> {
            session = null;
            updateLabels();
        });
    }

    private static void updateLabels() {
        if (nameLabel == null || creditLabel == null)
            return;
        if (session != null && AuthService.isLoggedIn()) {
            nameLabel.setText(session.name() != null ? session.name() : "User");
            creditLabel.setText("Credit: " + session.credit());
            if (roleLabel != null && session.topRole() != null) {
                String c = session.topRole().color != null ? session.topRole().color.replace("#", "") : "ffffff";
                roleLabel.setText("[#" + c + "]" + session.topRole().id);
            } else if (roleLabel != null)
                roleLabel.setText("");
        } else {
            nameLabel.setText("Account");
            creditLabel.setText("");
            if (roleLabel != null)
                roleLabel.setText("");
        }
    }

    private static void updateSize() {
        if (accountTable == null)
            return;
        float w = Core.graphics.getWidth(), h = Core.graphics.getHeight();
        accountTable.setSize(h < w ? w / 5 : w * 4 / 5, h < w ? h / 5 : h / 10);
    }

    private static void updateButtonState() {
        if (accountButton != null)
            accountButton.getStyle().imageUp = AuthService.isLoggedIn() ? Icon.ok : Icon.play;
    }

    /** Drag handler for account button */
    private static class DragHandler extends InputListener {
        private final Table target;
        private boolean isDragging = false;
        private float dragStartX, dragStartY;

        DragHandler(Table target) {
            this.target = target;
        }

        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
            if (button == KeyCode.mouseLeft) {
                isDragging = true;
                dragStartX = x;
                dragStartY = y;
                return true;
            }
            return false;
        }

        @Override
        public void touchDragged(InputEvent event, float x, float y, int pointer) {
            if (isDragging)
                target.setPosition(event.stageX - dragStartX, event.stageY - dragStartY);
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
            isDragging = false;
        }
    }
}
