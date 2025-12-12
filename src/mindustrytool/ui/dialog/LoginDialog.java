package mindustrytool.ui.dialog;

import arc.scene.ui.layout.Table;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.service.auth.AuthService;

/** Dialog for user login/logout functionality. */
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
        if (isLoggingIn) LoginStateViews.showLoggingIn(cont, this::cancelLogin);
        else if (AuthService.isLoggedIn()) LoginStateViews.showLoggedIn(cont, this::doLogout);
        else LoginStateViews.showLoggedOut(cont, this::startLogin);
    }

    private void startLogin() {
        isLoggingIn = true;
        rebuild();
        AuthService.login(
            s -> { isLoggingIn = false; mindustry.Vars.ui.showInfoFade(s); rebuild(); },
            e -> { isLoggingIn = false; mindustry.Vars.ui.showErrorMessage(e); rebuild(); });
    }

    private void cancelLogin() { isLoggingIn = false; rebuild(); }

    private void doLogout() {
        AuthService.logout();
        mindustry.Vars.ui.showInfoFade("@login.logged-out");
        rebuild();
    }

    public static Table createMenuButton(Runnable onClick) {
        return LoginStateViews.createMenuButton(onClick);
    }
}
