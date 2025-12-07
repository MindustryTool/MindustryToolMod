package mindustrytool.presentation.dialog;

import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.domain.service.AuthService;

/**
 * Dialog for user login/logout functionality.
 */
public class LoginDialog extends BaseDialog {
    private boolean isLoggingIn = false;

    public LoginDialog() {
        super("@login");
        addCloseButton();
        AuthService.onLoginStateChanged(this::rebuild);
        shown(this::rebuild);
    }

    private void rebuild() {
        cont.clear();
        cont.defaults().width(400f).pad(10f);

        if (isLoggingIn) {
            showLoggingIn();
        } else if (AuthService.isLoggedIn()) {
            showLoggedIn();
        } else {
            showLoggedOut();
        }
    }

    private void showLoggedOut() {
        cont.add("@login.not-logged-in").pad(20f).row();
        cont.button("@login.button", Icon.play, () -> {
            isLoggingIn = true;
            rebuild();
            AuthService.login(
                success -> {
                    isLoggingIn = false;
                    mindustry.Vars.ui.showInfoFade(success);
                    rebuild();
                },
                error -> {
                    isLoggingIn = false;
                    mindustry.Vars.ui.showErrorMessage(error);
                    rebuild();
                }
            );
        }).size(200f, 60f).pad(10f);
    }

    private void showLoggingIn() {
        cont.add("@login.waiting").pad(20f).row();
        cont.add("@login.browser-hint").color(arc.graphics.Color.gray).pad(10f).row();
        cont.button("@cancel", Icon.cancel, () -> {
            isLoggingIn = false;
            rebuild();
        }).size(200f, 60f).pad(10f);
    }

    private void showLoggedIn() {
        cont.add("@login.logged-in").color(arc.graphics.Color.green).pad(20f).row();
        
        // Show access token
        String token = AuthService.getAccessToken();
        if (token != null && !token.isEmpty()) {
            cont.add("Token:").pad(5f).row();
            cont.table(t -> {
                t.field(token, s -> {}).disabled(true).width(380f).get();
                t.button(Icon.copy, () -> {
                    arc.Core.app.setClipboardText(token);
                    mindustry.Vars.ui.showInfoFade("Token copied to clipboard");
                }).size(40f);
            }).pad(5f).row();
        }
        
        cont.button("@login.logout", Icon.cancel, () -> {
            AuthService.logout();
            mindustry.Vars.ui.showInfoFade("@login.logged-out");
            rebuild();
        }).size(200f, 60f).pad(10f);
    }

    /** Create a compact login button for menus */
    public static Table createMenuButton(Runnable onClick) {
        return new Table(t -> {
            t.button(AuthService.isLoggedIn() ? Icon.ok : Icon.play, Styles.clearNonei, 24f, onClick).size(40f);
        });
    }
}
