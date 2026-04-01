package mindustrytool.features.auth;

import arc.Core;
import mindustry.Vars;
import mindustry.ui.dialogs.BaseDialog;

public class AuthLoginDialog extends BaseDialog {
    public AuthLoginDialog(AuthService authService) {
        super("@login");
        name = "loginDialog";

        buttons.button("@cancel", () -> {
            authService.cancelLogin();
            hide();
        }).width(230);
    }

    void showLoading() {
        cont.clear();
        cont.add("@generate-loading-link");
    }

    void showLoginUrl(String loginUrl) {
        cont.clear();
        cont.button(loginUrl, () -> {
            Core.app.setClipboardText(loginUrl);
            Vars.ui.showInfoFade("@copied");
        }).margin(40).growX().wrapLabel(true).fontScale(0.5f);
    }
}
