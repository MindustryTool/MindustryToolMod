package mindustrytool.features.auth;

import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.util.Align;
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
        wholeViewport.setFillParent(true);
        wholeViewport.top().right();

        authWindow = wholeViewport.table().get();

        authWindow.top().right();
        authWindow.touchable = Touchable.childrenOnly;

        Vars.ui.menuGroup.addChild(wholeViewport);

        Table content = new Table();
        content.setBackground(Styles.black6);

        authWindow.add(content).top().right().margin(8f);
        authWindow.toFront();

        AuthService.getInstance().sessionStore.subscribe((user, state, error) -> {
            if (state == LoadState.LOADING || state == LoadState.IDLE) {
                content.clear();
                content.add("@loading").labelAlign(Align.left).padLeft(8);
            } else if (error != null) {
                content.clear();
                content.add("@error").labelAlign(Align.left).padLeft(8);
                content.add(error.getLocalizedMessage()).labelAlign(Align.left).padLeft(8).row();
                content.button("@retry", Icon.refresh, this::startLogin);
            } else if (user == null) {
                content.clear();
                content.button("@login", () -> startLogin());
            } else if (user != null) {
                content.clear();

                if (user.imageUrl() != null) {
                    content.add(new NetworkImage(user.imageUrl())).size(64);
                }

                if (!Vars.mobile) {
                    content.add(user.name()).labelAlign(Align.left).padLeft(8);
                }

                content.touchable = Touchable.enabled;
                content.clicked(() -> {
                    Vars.ui.showConfirm("Logout", "Logged in as " + user.name() + "\nDo you want to logout?",
                            () -> AuthService.getInstance().logout());
                });
            }
        });

        Timer.schedule(() -> AuthService.getInstance()
                .refreshTokenIfNeeded()
                .thenCompose((_void) -> AuthService.getInstance().sessionStore.fetch()), 0, 60 * 5);
    }

    private void startLogin() {
        AuthService.getInstance().login()
                .thenRun(() -> Vars.ui.showInfo("Login successful!"))
                .exceptionally(e -> {
                    Vars.ui.showException("Login failed or timed out.", e);
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
