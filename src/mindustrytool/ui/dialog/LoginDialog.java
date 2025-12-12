package mindustrytool.ui.dialog;

import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Main;
import mindustrytool.service.auth.AuthService;
import mindustrytool.ui.component.NetworkImage;
import mindustrytool.ui.image.ImageCache;

public class LoginDialog extends BaseDialog {
    private boolean isLoggingIn;

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
        cont.row();
        cont.button("Clear Cache", Icon.trash, this::clearAllCache).size(200f, 50f).pad(10f).padTop(30f);
    }
    
    private void clearAllCache() {
        ImageCache.clear();
        NetworkImage.clearCache();
        Main.imageDir.deleteDirectory();
        Main.mapsDir.deleteDirectory();
        Main.schematicDir.deleteDirectory();
        Main.imageDir.mkdirs();
        Main.mapsDir.mkdirs();
        Main.schematicDir.mkdirs();
        Vars.ui.showInfoFade("@message.cache-cleared");
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
