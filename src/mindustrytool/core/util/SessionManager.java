package mindustrytool.core.util;

import arc.scene.ui.Label;
import mindustrytool.core.model.SessionData;
import mindustrytool.data.api.Api;
import mindustrytool.service.auth.AuthService;

/** Manages session state and updates account labels. */
public class SessionManager {
    private static SessionData session;
    private static Label nameLabel;
    private static Label creditLabel;
    private static Label roleLabel;

    public static void init(Label name, Label credit, Label role) { nameLabel = name; creditLabel = credit; roleLabel = role; }

    public static void fetchSession() {
        if (!AuthService.isLoggedIn()) { session = null; updateLabels(); return; }
        Api.getSession(s -> { session = s; updateLabels(); }, e -> { session = null; updateLabels(); });
    }

    private static void updateLabels() {
        if (nameLabel == null || creditLabel == null) return;
        if (session != null && AuthService.isLoggedIn()) {
            nameLabel.setText(session.name() != null ? session.name() : "User");
            creditLabel.setText("Credit: " + session.credit());
            if (roleLabel != null && session.topRole() != null) {
                String c = session.topRole().color != null ? session.topRole().color.replace("#", "") : "ffffff";
                roleLabel.setText("[#" + c + "]" + session.topRole().id);
            } else if (roleLabel != null) roleLabel.setText("");
        } else {
            nameLabel.setText("Account");
            creditLabel.setText("");
            if (roleLabel != null) roleLabel.setText("");
        }
    }
}
