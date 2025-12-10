package mindustrytool.ui.dialog;

import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.service.auth.AuthService;

/** Reusable login state view components. */
public final class LoginStateViews {
    private LoginStateViews() {}

    public static void showLoggedOut(Table cont, Runnable onLogin) {
        cont.add("@login.not-logged-in").pad(20f).row();
        cont.button("@login.button", Icon.play, onLogin).size(200f, 60f).pad(10f);
    }

    public static void showLoggingIn(Table cont, Runnable onCancel) {
        cont.add("@login.waiting").pad(20f).row();
        cont.add("@login.browser-hint").color(Color.gray).pad(10f).row();
        cont.button("@cancel", Icon.cancel, onCancel).size(200f, 60f).pad(10f);
    }

    public static void showLoggedIn(Table cont, Runnable onLogout) {
        cont.add("@login.logged-in").color(Color.green).pad(20f).row();
        String token = AuthService.getAccessToken();
        if (token != null && !token.isEmpty()) {
            cont.add("Token:").pad(5f).row();
            cont.table(t -> {
                t.field(token, s -> {}).disabled(true).width(380f).get();
                t.button(Icon.copy, () -> {
                    arc.Core.app.setClipboardText(token);
                    mindustry.Vars.ui.showInfoFade("Token copied");
                }).size(40f);
            }).pad(5f).row();
        }
        cont.button("@login.logout", Icon.cancel, onLogout).size(200f, 60f).pad(10f);
    }

    public static Table createMenuButton(Runnable onClick) {
        return new Table(t -> t.button(AuthService.isLoggedIn() ? Icon.ok : Icon.play, Styles.clearNonei, 24f, onClick).size(40f));
    }
}
