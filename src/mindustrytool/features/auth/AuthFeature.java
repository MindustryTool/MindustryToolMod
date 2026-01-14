package mindustrytool.features.auth;

import arc.Core;
import arc.Events;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.ui.Styles;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.auth.dto.UserSession;
import mindustrytool.ui.NetworkImage;

public class AuthFeature implements Feature {

    private Table authWindow;

    @Override
    public FeatureMetadata getMetadata() {
        return new FeatureMetadata.Builder()
                .name("Authentication")
                .description("Login to Mindustry Tool")
                .enabledByDefault(true)
                .icon(Iconc.lock)
                .quickAccess(false)
                .build();
    }

    @Override
    public void init() {
        var wholeViewport = new Table();
        wholeViewport.setFillParent(true);
        wholeViewport.top().right();

        authWindow = wholeViewport.table()
                .get();

        authWindow.top().right();
        authWindow.touchable = Touchable.childrenOnly;

        Vars.ui.menuGroup.addChild(wholeViewport);

        // Restore session
        AuthService.getInstance().refreshTokenIfNeeded(() -> {
            AuthService.getInstance().fetchUserSession(() -> {
                Core.app.post(this::updateAuthWindow);
            }, () -> {
                AuthService.getInstance().logout();
                Core.app.post(this::updateAuthWindow);
            });
        }, () -> {
            AuthService.getInstance().logout();
            Core.app.post(this::updateAuthWindow);
        });

        Events.on(ClientLoadEvent.class, e -> {
            authWindow.toFront();
            updateAuthWindow();
        });

        Events.on(UserSession.class, user -> {
            Core.app.post(this::updateAuthWindow);
        });
    }

    private void updateAuthWindow() {
        if (authWindow == null) {
            return;
        }

        authWindow.clear();

        Table content = new Table();
        content.setBackground(Styles.black6);

        if (AuthService.getInstance().isLoggedIn()) {
            UserSession user = AuthService.getInstance().getCurrentUser();

            if (user != null) {
                if (user.imageUrl() != null) {
                    content.add(new NetworkImage(user.imageUrl())).size(64);
                }

                if (!Vars.mobile) {
                    content.add(user.name()).labelAlign(Align.left).padLeft(8);
                }

                // Make the whole content area clickable to show profile/logout
                content.touchable = Touchable.enabled;
                content.clicked(this::showProfileDialog);
            } else {
                content.add("Loading...");
            }
        } else {
            // If not logged in, just show the button
            content.button("Login", Icon.lock, this::startLogin).size(120, 50);
        }

        authWindow.add(content).top().right().margin(8f);
    }

    private void startLogin() {
        AuthService.getInstance().login(
                () -> {
                    Vars.ui.showInfo("Login successful!");
                    Core.app.post(this::updateAuthWindow);
                },
                () -> Vars.ui.showErrorMessage("Login failed or timed out."));
    }

    private void showProfileDialog() {
        UserSession user = AuthService.getInstance().getCurrentUser();
        Vars.ui.showConfirm("Logout",
                "Logged in as " + (user != null ? user.name() : "Unknown") + "\nDo you want to logout?", () -> {
                    AuthService.getInstance().logout();
                    updateAuthWindow();
                });
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }
}
