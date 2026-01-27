package mindustrytool.features.auth;

import arc.Core;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Log;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.services.ReactiveStore.LoadState;
import mindustrytool.ui.NetworkImage;

public class AuthFeature implements Feature {

    private Table authWindow;

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.authentication.name")
                .description("@feature.authentication.description")
                .enabledByDefault(true)
                .icon(Icon.lock)
                .quickAccess(false)
                .build();
    }

    @Override
    public void init() {
        var wholeViewport = new Table();
        wholeViewport.name = "authWindow";
        wholeViewport.setFillParent(true);
        wholeViewport.top().right();

        authWindow = wholeViewport.table().get();

        authWindow.top().right();
        authWindow.touchable = Touchable.childrenOnly;

        Core.app.post(() -> Vars.ui.menuGroup.addChild(wholeViewport));

        Table content = new Table();
        content.setBackground(Styles.black6);

        authWindow.add(content).top().right().margin(8f);
        authWindow.toFront();

        AuthService.getInstance().sessionStore.subscribe((user, state, error) -> {
            if (state == LoadState.LOADING) {
                content.clear();
                content.add("@loading").wrapLabel(false).labelAlign(Align.left).padLeft(8);
            } else if (error != null) {
                content.clear();
                content.add("@error").labelAlign(Align.left).padLeft(8);
                content.add(error.getLocalizedMessage()).labelAlign(Align.left).padLeft(8).row();
                content.button("@retry", Icon.refresh, this::startLogin);

                Log.err("Failed to login", error);
            } else if (user == null) {
                content.clear();
                content.button("@login", this::startLogin).wrapLabel(false);
            } else if (user != null) {
                content.clear();

                if (user.getImageUrl() != null) {
                    content.add(new NetworkImage(user.getImageUrl())).size(64);
                }

                if (!Vars.mobile) {
                    content.add(user.getName()).labelAlign(Align.left).padLeft(8);
                }

                content.touchable = Touchable.enabled;
                content.clicked(() -> {
                    Vars.ui.showConfirm("Logout", "Logged in as " + user.getName() + "\nDo you want to logout?",
                            () -> AuthService.getInstance().logout());
                });
            }

            content.pack();
        });

        AuthService.getInstance()
                .refreshTokenIfNeeded()
                .thenCompose((_void) -> AuthService.getInstance().sessionStore.fetch());

        Timer.schedule(() -> {
            if (AuthService.getInstance().isLoggedIn()) {
                AuthService.getInstance()
                        .refreshTokenIfNeeded()
                        .thenCompose((_void) -> AuthService.getInstance().sessionStore.fetch());
            }
        }, 0, 60 * 5);
    }

    private void startLogin() {
        AuthService.getInstance().login()
                .thenRun(() -> Core.app.post(() -> Vars.ui.showInfo("Login successful!")))
                .exceptionally(e -> {
                    Core.app.post(() -> Vars.ui.showException("Login failed or timed out.", e));
                    return null;
                });
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }
}
