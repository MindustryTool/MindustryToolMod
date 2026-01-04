package mindustrytool.features.social.auth;

import arc.Core;
import arc.Events;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;

import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.util.Http;
import arc.util.Log;

import mindustrytool.Feature;

/**
 * Feature: Authentication
 * Provides login/logout functionality with account button UI.
 * Completely self-contained - no external dependencies.
 */
public class AuthFeature implements Feature {
    public static LoginDialog loginDialog;
    public static ProfileDialog profileDialog;
    private static Table accountTable;
    private static SessionData session;

    public static SessionData getSession() {
        return session;
    }

    @Override
    public String getName() {
        return "Auth";
    }

    @Override
    public int getPriority() {
        return 80;
    }

    @Override
    public void init() {
        loginDialog = new LoginDialog();
        profileDialog = new ProfileDialog();
        Events.on(ClientLoadEvent.class, e -> createAccountButton());
    }

    private static void createAccountButton() {
        // Init table
        accountTable = new Table();
        Vars.ui.menuGroup.addChild(accountTable);

        // Update position loop
        accountTable.update(() -> {
            accountTable.setPosition(Core.graphics.getWidth() / 2f - accountTable.getWidth() / 2f, 10f);
        });

        if (AuthService.isLoggedIn()) {
            fetchSession();
        } else {
            rebuildUI();
        }

        // Event Bus Listener
        Events.on(mindustrytool.events.LoginStateChangeEvent.class, e -> {
            if (e.isLoggedIn) {
                fetchSession();
            } else {
                rebuildUI();
            }
        });
    }

    private static void rebuildUI() {
        if (accountTable == null)
            return;
        accountTable.clear();

        if (!AuthService.isLoggedIn()) {
            // Not logged in: Show Hologram Login Button
            HologramButton loginBtn = new HologramButton("LOGIN SYSTEM", () -> {
                loginDialog.startLogin();
                Vars.ui.showInfoFade("Initialize Neural Link...");
            });
            accountTable.add(loginBtn).size(180f, 60f);
        } else {
            // Logged in: Use Hologram Profile Widget
            accountTable.background(null);

            String name = (session != null && session.name() != null) ? session.name() : "User";
            TextureRegion avatarRegion = null;

            if (session != null && session.imageUrl != null) {
                if (ImageCache.has(session.imageUrl)) {
                    avatarRegion = new TextureRegion(ImageCache.get(session.imageUrl));
                } else {
                    // Fetch avatar async
                    Http.get(session.imageUrl)
                            .error(e -> Log.err("Failed to fetch avatar", e))
                            .submit(r -> {
                                try {
                                    byte[] bytes = r.getResult();
                                    Core.app.post(() -> {
                                        Texture tex = new Texture(new Pixmap(bytes, 0, bytes.length));
                                        ImageCache.put(session.imageUrl, tex);
                                        rebuildUI();
                                    });
                                } catch (Exception e) {
                                    Log.err("Error creating texture", e);
                                }
                            });
                }
            }

            HologramProfileWidget profileWidget = new HologramProfileWidget();
            profileWidget.build(avatarRegion, name, () -> profileDialog.show(session));

            accountTable.add(profileWidget);
        }

        // Ensure size is updated for centering logic
        accountTable.pack();
    }

    private static void fetchSession() {
        if (!AuthService.isLoggedIn()) {
            session = null;
            rebuildUI();
            return;
        }
        Api.getSession(s -> {
            session = s;
            rebuildUI();
        }, e -> {
            session = null;
            rebuildUI(); // Or show error?
        });
    }
}
