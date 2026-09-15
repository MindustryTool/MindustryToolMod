package mindustrytool.services.auth;

import static solim.UI.*;

import arc.Core;
import arc.Events;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.util.Log;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.events.LoginUriEvent;
import mindustrytool.models.response.UserSession;
import solim.core.Component;
import solim.signal.Computed;

public class AuthOverlay {
    private static AuthOverlay instance;

    private AuthLoginDialog loginDialog;
    private final Computed<AuthState> state = new Computed<>(() -> {
        MindustryAuthProvider auth = MindustryAuthProvider.getInstance();
        return new AuthState(
                Boolean.TRUE.equals(auth.sessionLoading().get()),
                auth.sessionError().get(),
                auth.session().get());
    });

    public static class AuthState {
        public final boolean isLoading;
        public final @Nullable Throwable error;
        public final @Nullable UserSession user;

        public AuthState(boolean isLoading, @Nullable Throwable error, @Nullable UserSession user) {
            this.isLoading = isLoading;
            this.error = error;
            this.user = user;
        }
    }

    public static AuthOverlay getInstance() {
        if (instance == null) {
            instance = new AuthOverlay();
        }
        return instance;
    }

    private AuthOverlay() {
    }

    public void init() {
        initUi();
    }

    public void initUi() {
        if (Vars.ui.menuGroup == null) {
            Log.info("AuthOverlay init skipped: menuGroup null");
            return;
        }

        Core.app.post(() -> {
            Element overlayEl = buildOverlay().element();
            overlayEl.name = "authWindow";
            Vars.ui.menuGroup.addChild(overlayEl);
            overlayEl.toFront();
        });

        Events.on(LoginUriEvent.class, e -> {
            if (loginDialog != null) {
                Core.app.post(() -> loginDialog.showLoginUrl(e.loginUrl));
            }
        });
    }

    private Component buildOverlay() {
        return column()
                .fillParent()
                .top()
                .right()
                .touchable(Touchable.childrenOnly)
                .children(() -> {
                    dynamic(state, s -> {
                        if (s.isLoading) {
                            return row()
                                    .top()
                                    .right()
                            .margin(8f)
                            .background(Styles.black6)
                            .padding(unit(2))
                            .children(() -> {
                                text(Core.bundle.get("auth.session.loading"));
                                    });
                        }

                        if (s.error != null) {
                            return row()
                                    .top()
                                    .right()
                            .margin(8f)
                            .background(Styles.black6)
                            .gap(unit(2))
                            .padding(unit(2))
                            .children(() -> {
                                text(Core.bundle.get("auth.session.error"));
                                        String errText = s.error.getLocalizedMessage() != null
                                                ? s.error.getLocalizedMessage()
                                                : "";
                                        if (!errText.isEmpty()) {
                                            text(errText);
                                        }
                                        button(Core.bundle.get("auth.session.retry"), Icon.refresh, this::startLoginUI);
                                    });
                        }

                        if (s.user == null) {
                            return row()
                                    .top()
                                    .right()
                            .margin(8f)
                            .background(Styles.black6)
                            .padding(unit(2))
                            .children(() -> {
                                button(Core.bundle.get("auth.login"), this::startLoginUI);
                                    });
                        }

                        UserSession user = s.user;

                        return card()
                                .top()
                                .right()
                                .children(() -> {
                                    row().padding(unit(2)).background(Styles.black6).gap(unit(2)).center().children(() -> {
                                        if (user.getImageUrl() != null && !user.getImageUrl().isEmpty()) {
                                            networkImage(user.getImageUrl()).size(64f);
                                        }
                                        if (!Vars.mobile && user.getName() != null) {
                                            text(user.getName());
                                        }
                                    });
                                })
                                .onClick(() -> {
                                    Vars.ui.showConfirm(
                                            Core.bundle.get("auth.logout.confirm-title"),
                                            Core.bundle.format("auth.logout.confirm-message", user.getName()),
                                            MindustryAuthProvider.getInstance()::logout);
                                });
                    }).top().right();
                });
    }

    public void startLoginUI() {
        MindustryAuthProvider auth = MindustryAuthProvider.getInstance();

        if (loginDialog == null) {
            loginDialog = new AuthLoginDialog(auth);
        }

        Core.app.post(() -> {
            loginDialog.showLoading();
            loginDialog.show();
        });

        auth.login()
                .thenRun(() -> Core.app.post(() -> {
                    if (loginDialog != null)
                        loginDialog.hide();
                    Vars.ui.showInfo(Core.bundle.get("auth.login.success"));
                }))
                .exceptionally(e -> {
                    Core.app.post(() -> {
                        if (loginDialog != null)
                            loginDialog.hide();
                        Vars.ui.showException(Core.bundle.get("auth.login.failed"), e);
                    });
                    return null;
                });
    }

    public void showLoading() {
        if (loginDialog != null) {
            Core.app.post(() -> loginDialog.showLoading());
        }
    }

    public void showLoginUrl(String loginUrl) {
        if (loginDialog != null) {
            Core.app.post(() -> loginDialog.showLoginUrl(loginUrl));
        }
    }
}
