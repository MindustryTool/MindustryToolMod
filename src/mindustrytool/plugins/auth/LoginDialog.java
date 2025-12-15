package mindustrytool.plugins.auth;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class LoginDialog extends BaseDialog {
    private static final Fi imageDir = Vars.dataDirectory.child("mindustry-tool-caches");
    private static final Fi mapsDir = Vars.dataDirectory.child("mindustry-tool-maps");
    private static final Fi schematicDir = Vars.dataDirectory.child("mindustry-tool-schematics");
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
        if (isLoggingIn) { 
            cont.add("@login.waiting").pad(20f).row(); 
            cont.add("@login.browser-hint").color(Color.gray).pad(10f).row(); 
            cont.button("@cancel", Icon.cancel, this::cancelLogin).size(200f, 60f).pad(10f); 
        }
        else if (AuthService.isLoggedIn()) {
            cont.add("@login.logged-in").color(Color.green).pad(20f).row();
            String token = AuthService.getAccessToken();
            if (token != null && !token.isEmpty()) { 
                cont.add("Token:").pad(5f).row(); 
                cont.table(t -> { 
                    t.field(token, s -> {}).disabled(true).width(380f).get(); 
                    t.button(Icon.copy, () -> { Core.app.setClipboardText(token); Vars.ui.showInfoFade("Token copied"); }).size(40f); 
                }).pad(5f).row(); 
            }
            cont.button("@login.logout", Icon.cancel, this::doLogout).size(200f, 60f).pad(10f);
        }
        else { 
            cont.add("@login.not-logged-in").pad(20f).row(); 
            cont.button("@login.button", Icon.play, this::startLogin).size(200f, 60f).pad(10f); 
        }
        cont.row();
        cont.button("Clear Cache", Icon.trash, this::clearAllCache).size(200f, 50f).pad(10f).padTop(30f);
    }
    
    private void clearAllCache() {
        ImageCache.clear();
        imageDir.deleteDirectory(); mapsDir.deleteDirectory(); schematicDir.deleteDirectory();
        imageDir.mkdirs(); mapsDir.mkdirs(); schematicDir.mkdirs();
        Vars.ui.showInfoFade("@message.cache-cleared");
    }

    private void startLogin() {
        isLoggingIn = true; rebuild();
        AuthService.login(s -> { isLoggingIn = false; Vars.ui.showInfoFade(s); rebuild(); }, e -> { isLoggingIn = false; Vars.ui.showErrorMessage(e); rebuild(); });
    }

    private void cancelLogin() { isLoggingIn = false; rebuild(); }
    private void doLogout() { AuthService.logout(); Vars.ui.showInfoFade("@login.logged-out"); rebuild(); }
    public static Table createMenuButton(Runnable onClick) { return new Table(t -> t.button(AuthService.isLoggedIn() ? Icon.ok : Icon.play, Styles.clearNonei, 24f, onClick).size(40f)); }
}
