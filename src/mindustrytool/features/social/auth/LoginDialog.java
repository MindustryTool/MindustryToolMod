package mindustrytool.features.social.auth;

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

    public LoginDialog() {
        super("@login");
        addCloseButton();

        arc.Events.on(mindustrytool.events.LoginStateChangeEvent.class, e -> rebuild());
        shown(this::rebuild);
    }

    private void rebuild() {
        cont.clear();
        cont.defaults().width(400f).pad(10f);

        // Header
        cont.table(t -> {
            t.add("@login").color(Color.yellow).get().setFontScale(1.3f);
            t.row();
            t.image().color(Color.yellow).height(3f).width(200f).padBottom(10f); // Underline effect
        }).row();

        if (AuthService.isLoggedIn()) {
            SessionData session = AuthFeature.getSession();

            // Profile Card
            cont.table(Styles.black6, t -> {
                t.defaults().pad(5f);
                t.add("@login.logged-in").color(Color.green).padBottom(10f).row();

                String name = (session != null && session.name() != null) ? session.name() : "User";
                t.add(name).color(Color.white).get().setFontScale(1.2f);
                t.row();

                if (session != null) {
                    if (session.topRole() != null) {
                        String c = session.topRole().color != null ? session.topRole().color.replace("#", "")
                                : "ffffff";
                        t.add(session.topRole().id).color(Color.valueOf(c)).row();
                    }
                    t.table(info -> {
                        info.image(Icon.star).color(Color.gold).size(16f).padRight(5f);
                        info.add("Credit: " + session.credit()).color(Color.gold);
                    }).padTop(5f).row();
                }
            }).width(420f).pad(10f).row();

            // Token Section - REMOVED as requested for safety

            cont.button("@login.logout", Icon.cancel, this::doLogout).size(200f, 60f).pad(10f);
        } else {
            // Not logged in - This state should mostly be invisible now as we don't show
            // dialog if not logged in.
            // But if somehow shown, show a simple close button or nothing.
            cont.add("Please login via the main menu.").color(Color.gray).pad(20f).row();
        }

        cont.row();
        cont.image().color(Color.darkGray).height(2f).fillX().pad(20f).row();
        cont.button("Clear Cache", Icon.trash, this::clearAllCache).size(200f, 50f).pad(10f);
    }

    private void clearAllCache() {
        ImageCache.clear();
        imageDir.deleteDirectory();
        mapsDir.deleteDirectory();
        schematicDir.deleteDirectory();
        imageDir.mkdirs();
        mapsDir.mkdirs();
        schematicDir.mkdirs();
        Vars.ui.showInfoFade("@mdt.message.cache-cleared");
    }

    void startLogin() {
        AuthService.login(s -> {
            Vars.ui.showInfoFade(s);
            rebuild();
        }, e -> {
            Vars.ui.showErrorMessage(e);
            rebuild();
        });
    }

    private void doLogout() {
        AuthService.logout();
        Vars.ui.showInfoFade("@login.logged-out");
        hide(); // Close dialog immediately
    }

    public static Table createMenuButton(Runnable onClick) {
        return new Table(t -> t.button(AuthService.isLoggedIn() ? Icon.ok : Icon.play, Styles.clearNonei, 24f, onClick)
                .size(40f));
    }
}
