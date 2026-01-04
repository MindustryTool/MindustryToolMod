package mindustrytool.features.social.auth;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Dialog;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.Vars;

import arc.scene.ui.layout.Stack;
import mindustry.content.Planets;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.Button;
import arc.scene.style.Drawable;
import arc.scene.actions.Actions;
import arc.math.Interp;

public class ProfileDialog extends Dialog {
    private static final Color CYAN_PRIMARY = Color.valueOf("00d4ff");
    private static final Color CYAN_DARK = Color.valueOf("0099cc");
    private static final Color CYAN_BRIGHT = Color.valueOf("00ffff");

    private SessionData session;

    public ProfileDialog() {
        super("");

        // Full screen transparent background
        this.setFillParent(true);
        this.background(null);

        addCloseButton();
        closeOnBack(); // Enable ESC/Back key
    }

    private enum ProfileView {
        OVERVIEW, COMBAT, SETTINGS
    }

    private ProfileView currentView = ProfileView.OVERVIEW;
    private Table contentArea;

    public void show(SessionData session) {
        this.session = session;
        if (session == null)
            return;

        cont.clear(); // Clear main content table
        cont.margin(0f); // Remove default dialog margins
        build();

        // Intro Animation
        cont.setOrigin(arc.util.Align.center);
        cont.setScale(1f, 0f);
        cont.addAction(Actions.scaleTo(1f, 1f, 1.2f, Interp.pow3Out));

        super.show();
    }

    private void build() {
        Stack stack = new Stack();

        ParallaxContainer parallax = new ParallaxContainer();

        // Main Layout Table
        Table main = new Table();
        // main.background(Styles.black6); // REMOVED: Don't fill screen with card bg

        // Define Layout based on Device Type
        boolean isMobile = Vars.mobile;

        // Add margins to prevent edge touching
        main.margin(isMobile ? 10f : 50f);

        if (isMobile) {
            // === MOBILE LAYOUT (Vertical) ===
            Table mobileContainer = new Table();
            // Custom Dark Background (90% Opacity Black)
            mobileContainer.background(new TextureRegionDrawable(arc.Core.atlas.white()).tint(0f, 0f, 0f, 0.9f));

            // 1. Navigation (Top Tab)
            mobileContainer.add(buildNavBar(true)).growX().height(60f).row();

            // 2. Content Area (Scrollable)
            contentArea = new Table();
            ScrollPane pane = new ScrollPane(contentArea);
            pane.setScrollingDisabled(true, false); // Vertical scroll only
            mobileContainer.add(pane).grow();

            main.add(mobileContainer).grow();
            parallax.shadowTarget = mobileContainer;

        } else {
            // === DESKTOP LAYOUT (Horizontal Split) ===
            // === DESKTOP LAYOUT (Horizontal Split) ===
            Table container = new Table();
            // Custom Dark Background (90% Opacity Black)
            container.background(new TextureRegionDrawable(arc.Core.atlas.white()).tint(0f, 0f, 0f, 0.9f));

            // 1. Navigation (Left Sidebar)
            container.add(buildNavBar(false)).width(200f).growY();

            // 2. Content Area
            contentArea = new Table();
            contentArea.defaults().pad(20f).padBottom(10f);
            container.add(contentArea).grow();

            // Constrain max width for desktop to avoid "too big" feel
            main.add(container).grow().maxWidth(920f).maxHeight(700f);
            parallax.shadowTarget = container;
        }

        // Initialize Content
        rebuildContent();

        // Enable Parallax
        main.setTransform(true);
        parallax.mainContent = main;
        parallax.add(main).grow(); // Full size

        stack.add(parallax);
        stack.add(new GlitchOverlay());

        // Back Button (Always bottom right or absolute)
        Table uiLayer = new Table();
        uiLayer.setFillParent(true);
        uiLayer.bottom(); // Centered bottom
        uiLayer.add(new HologramButton("BACK", this::hide)).size(180f, 60f).padBottom(2f);
        stack.add(uiLayer);

        cont.add(stack).grow();
    }

    private void rebuildContent() {
        contentArea.clear();

        switch (currentView) {
            case OVERVIEW:
                buildOverview();
                break;
            case COMBAT:
                Label combatLabel = new Label("COMBAT STATS [WIP]");
                combatLabel.setColor(Color.gray);
                combatLabel.setFontScale(1.5f);
                contentArea.add(combatLabel);
                break;
            case SETTINGS:
                Label settingsLabel = new Label("PROFILE SETTINGS [WIP]");
                settingsLabel.setColor(Color.gray);
                settingsLabel.setFontScale(1.5f);
                contentArea.add(settingsLabel);
                break;
        }
    }

    private Table buildNavBar(boolean horizontal) {
        Table nav = new Table();
        nav.background(Styles.black8);

        // Set default alignment for all children
        nav.defaults().left().growX();

        if (!horizontal) {
            // Desktop Sidebar Header
            Label menuLabel = new Label("MENU");
            menuLabel.setColor(CYAN_DARK);
            menuLabel.setFontScale(0.8f);
            nav.add(menuLabel).padLeft(10f).padTop(10f).padBottom(5f).row();
            nav.image().color(CYAN_DARK).height(2f).padBottom(10f).row();
        }

        // Navigation Buttons
        addNavButton(nav, "OVERVIEW", Icon.home, ProfileView.OVERVIEW, horizontal);
        if (!horizontal)
            nav.row();

        if (!horizontal) {
            nav.add().growY().row(); // Spacer pushes profile to bottom

            // Tech Divider before Profile
            nav.table(t -> {
                t.image().color(CYAN_DARK).height(2f).growX().padRight(5f);
                t.image().color(CYAN_BRIGHT).height(2f).width(20f);
                t.image().color(CYAN_DARK).height(2f).growX().padLeft(5f);
            }).growX().height(2f).pad(10f).padBottom(20f).row();

            // Profile Footer - Avatar + Name Badge
            Table profile = new Table();
            profile.center(); // Center the content

            TextureRegion avatarReg = null;
            if (session.imageUrl != null && ImageCache.has(session.imageUrl)) {
                avatarReg = new TextureRegion(ImageCache.get(session.imageUrl));
            }

            float commonHeight = 50f;

            // Unified Profile Card - Single Hologram Frame
            Table profileCard = new Table() {
                @Override
                public void draw() {
                    float w = getWidth();
                    float h = getHeight();

                    // Border / Frame
                    Draw.color(CYAN_PRIMARY);
                    Lines.stroke(2f);

                    float len = 10f;
                    Lines.line(x, y + h, x + len, y + h);
                    Lines.line(x, y + h, x, y + h - len);
                    Lines.line(x + w, y + h, x + w - len, y + h);
                    Lines.line(x + w, y + h, x + w, y + h - len);
                    Lines.line(x, y, x + len, y);
                    Lines.line(x, y, x, y + len);
                    Lines.line(x + w, y, x + w - len, y);
                    Lines.line(x + w, y, x + w, y + len);

                    Draw.reset();
                    super.draw();
                }
            };

            Image avatar = new Image(avatarReg != null ? new TextureRegionDrawable(avatarReg) : Icon.players);
            profileCard.add(avatar).size(32f).padLeft(10f).padRight(10f); // Avatar on left

            Label name = new Label(session.name());
            if (session.name().length() > 12) {
                name.setFontScale(0.7f);
            } else {
                name.setFontScale(0.9f);
            }
            name.setColor(CYAN_BRIGHT);
            name.setEllipsis(true);

            profileCard.add(name).growX().left().padRight(10f); // Name on right

            profile.add(profileCard).height(commonHeight).growX();

            nav.add(profile).pad(10f).padBottom(10f).growX().row();
        }

        return nav;
    }

    private void addNavButton(Table table, String text, Drawable icon, ProfileView view, boolean horizontal) {
        Button btn = new Button(Styles.cleart);
        boolean active = currentView == view;

        btn.clearChildren();

        Color iconColor = active ? CYAN_BRIGHT : Color.gray;

        if (horizontal) {
            // Mobile Tab Style (Icon only or Icon+Text compact)
            btn.add(new Image(icon).setScaling(arc.util.Scaling.fit)).size(24f).padRight(5f).get().setColor(iconColor);

            if (active) {
                Label l = new Label(text);
                l.setColor(CYAN_BRIGHT);
                l.setFontScale(0.9f);
                btn.add(l);
            }
        } else {
            // Desktop Sidebar Style (Full width)
            btn.left();

            // Custom Background drawing for Active State
            if (active) {
                // We use a custom Drawable that renders the glow
                btn.setBackground(new TextureRegionDrawable(arc.Core.atlas.white()) {
                    @Override
                    public void draw(float x, float y, float width, float height) {
                        // Glow Background
                        Draw.color(CYAN_PRIMARY, 0.1f);
                        Fill.rect(x + width / 2f, y + height / 2f, width, height);

                        // Left Active Indicator Bar
                        Draw.color(CYAN_BRIGHT);
                        Fill.rect(x + 2f, y + height / 2f, 4f, height);
                        Draw.reset();
                    }
                });
            }

            btn.add(new Image(icon).setScaling(arc.util.Scaling.fit)).size(24f).padLeft(15f).padRight(10f).get()
                    .setColor(iconColor);

            Label l = new Label(text);
            l.setColor(active ? CYAN_BRIGHT : Color.gray);
            l.setFontScale(1f);
            btn.add(l).growX().left();
        }

        btn.clicked(() -> {
            if (currentView != view) {
                currentView = view;
                rebuildContent();
            }
        });

        table.add(btn).grow().height(horizontal ? 60f : 50f).padBottom(horizontal ? 0f : 10f);
    }

    private void buildOverview() {
        boolean isMobile = Vars.mobile;

        Table main = new Table();

        // === BODY CONTENT ===
        if (isMobile) {
            // Mobile: Stack Vertical
            main.add(new SecurityClearanceWidget(session)).growX().height(140f).padBottom(10f).row();
            main.add(new CreditBalanceWidget(session)).growX().height(120f).padBottom(10f).row();
            main.add(new CampaignHologramWidget()).growX().height(250f).padBottom(10f).row();

        } else {
            // Desktop: 2-Column Grid
            Table grid = new Table() {
                @Override
                public void draw() {
                    super.draw();
                    // Tech Lines in the gap (Approximate positions)
                    Draw.color(CYAN_DARK);
                    Lines.stroke(2f);

                    float midX = x + 325f; // Gap center (320 + 5)
                    // Top connection
                    Lines.square(midX, y + height - 70f, 2f, 45f);
                    Lines.line(midX - 10f, y + height - 70f, midX + 10f, y + height - 70f);

                    // Bottom connection
                    Lines.square(midX, y + 70f, 2f, 45f);
                    Lines.line(midX - 10f, y + 70f, midX + 10f, y + 70f);

                    Draw.reset();
                }
            };

            // Left Col
            // Left Col
            // Left Col
            Table leftCol = new Table();
            // leftCol.top(); // REMOVED: Restore centering
            leftCol.defaults().padBottom(10f).growX();
            leftCol.add(new SecurityClearanceWidget(session)).height(140f).row();
            leftCol.add(new CreditBalanceWidget(session)).height(140f).padBottom(0f);

            // Removed .top() and .growY() -> Centers vertically
            grid.add(leftCol).width(320f).padRight(10f);

            // Right Col
            // Removed .top() and changed .grow() to .growX() -> Centers vertically
            grid.add(new CampaignHologramWidget()).growX().height(290f);

            main.add(grid).grow().row();
        }

        // Bottom: Barcode with Scanning Frame
        Table idFrame = new Table() {

            @Override
            public void draw() {
                super.draw();
                Draw.color(CYAN_DARK);
                Lines.stroke(2f);
                // Corner Brackets for ID
                float len = 15f;
                Lines.line(x, y + height, x + len, y + height); // TL
                Lines.line(x, y + height, x, y + height - len);

                Lines.line(x + width, y + height, x + width - len, y + height); // TR
                Lines.line(x + width, y + height, x + width, y + height - len);

                Lines.line(x, y, x + len, y); // BL
                Lines.line(x, y, x, y + len);

                Lines.line(x + width, y, x + width - len, y); // BR
                Lines.line(x + width, y, x + width, y + len);
                Draw.reset();
            }
        };
        idFrame.add(new ConnectionIdWidget(session)).grow();

        main.add(idFrame).growX().height(isMobile ? 60f : 80f).padTop(10f);

        contentArea.add(main).grow();
    }

    // ==========================================
    // WIDGET IMPLEMENTATIONS
    // ==========================================

    private class SecurityClearanceWidget extends Table {
        public SecurityClearanceWidget(SessionData s) {
            background(Styles.grayPanel);

            SessionData.Role role = s.topRole();
            String roleName = role != null ? role.id.toUpperCase().replace("-", " ") : "UNKNOWN";
            Color roleColor = role != null && role.color != null ? Color.valueOf(role.color) : Color.white;
            int level = role != null ? role.level : 0;

            add(new Label("SECURITY CLEARANCE")).color(Color.gray).fontScale(0.75f).top().left().pad(5f).row();

            Table content = new Table();
            Label roleLabel = new Label(roleName);
            roleLabel.setFontScale(1.2f);
            roleLabel.setColor(roleColor);

            content.add(roleLabel).row();

            // Level Bar
            Table levelBar = new Table() {
                @Override
                public void draw() {
                    super.draw();
                    float h = height;
                    float w = width;
                    float progress = Mathf.clamp(level / 100f); // Assuming 100 is max for visual

                    Lines.stroke(2f);
                    Draw.color(Color.gray);
                    Lines.rect(x, y, w, h);

                    Draw.color(roleColor);
                    Fill.rect(x + (w * progress) / 2f, y + h / 2f, w * progress, h - 4f);
                    Draw.reset();
                }
            };
            content.add(levelBar).size(200f, 10f).padTop(10f).row();

            content.add(new Label("LEVEL " + level)).fontScale(0.8f).color(Color.lightGray).padTop(5f);

            add(content).expand();

            // "Stamp" effect
            row();
            Label stamp = new Label("[ AUTHORIZED ]") {
                @Override
                public void draw() {
                    float opacity = 0.5f + Mathf.absin(Time.time, 10f, 0.5f);
                    this.setColor(CYAN_BRIGHT.r, CYAN_BRIGHT.g, CYAN_BRIGHT.b, opacity);
                    super.draw();
                }
            };
            stamp.setFontScale(0.6f);
            add(stamp).bottom().right().pad(5f);
        }
    }

    private class CreditBalanceWidget extends Table {
        public CreditBalanceWidget(SessionData s) {
            background(Styles.grayPanel);

            add(new Label("CREDIT BALANCE")).color(Color.gray).fontScale(0.75f).top().left().pad(5f).row();

            int targetCredits = s.credit();

            Label creditLabel = new Label("0 C") {
                float currentVal = 0;

                @Override
                public void act(float delta) {
                    super.act(delta);
                    if (currentVal < targetCredits) {
                        currentVal = Mathf.lerp(currentVal, targetCredits, 0.1f);
                        if (Math.abs(targetCredits - currentVal) < 1)
                            currentVal = targetCredits;
                        setText((int) currentVal + " C");
                    }
                }
            };
            creditLabel.setFontScale(2f);
            creditLabel.setColor(Color.green);

            add(creditLabel).expand();

            // Decorative hex line
            row();
            Table decor = new Table() {
                @Override
                public void draw() {
                    super.draw();
                    Draw.color(Color.green, 0.3f);
                    Lines.stroke(1f);
                    float drawY = y + height / 2f;
                    for (float i = x; i < x + width; i += 10f) {
                        Lines.line(i, drawY, i + 5f, drawY);
                    }
                    Draw.reset();
                }
            };
            add(decor).height(10f).growX().pad(5f);
        }
    }

    private class ConnectionIdWidget extends Table {
        public ConnectionIdWidget(SessionData s) {
            background(Styles.grayPanel);

            add(new Label("NEURAL ID VERIFICATION")).color(Color.gray).fontScale(0.75f).top().left().pad(5f).row();

            // Barcode visual simulation
            Table barcode = new Table() {
                @Override
                public void draw() {
                    super.draw();
                    Draw.color(Color.white, 0.5f);
                    float lx = x + 10f;
                    float ly = y + 10f;
                    float lh = height - 20f;

                    long seed = s.id.hashCode() + (long) (Time.time / 20f);
                    java.util.Random r = new java.util.Random(seed);

                    while (lx < x + width - 10f) {
                        float w = 1f + r.nextFloat() * 4f;
                        if (lx + w > x + width - 10f)
                            break;
                        Fill.rect(lx + w / 2f, ly + lh / 2f, w, lh);
                        lx += w + 2f + r.nextFloat() * 3f;
                    }

                    // Scanning laser
                    float scanX = x + 10f + ((Time.time * 2f) % (width - 20f));
                    Draw.color(Color.red, 0.8f);
                    Lines.stroke(2f);
                    Lines.line(scanX, ly, scanX, ly + lh);
                    Draw.color(Color.red, 0.3f);
                    Fill.rect(scanX, ly + lh / 2f, 4f, lh);

                    Draw.reset();
                }
            };

            add(barcode).grow();
        }
    }

    // ==========================================
    // PARALLAX CONTAINER
    // ==========================================
    private class ParallaxContainer extends Table {
        public Table mainContent; // The window being moved
        public Table shadowTarget; // The specific card to cast shadow for

        public ParallaxContainer() {
            this.setFillParent(true);
        }

        @Override
        public void act(float delta) {
            super.act(delta);

            if (mainContent != null) {
                // Calculate mouse offset from center
                float mx = Core.input.mouse().x - Core.graphics.getWidth() / 2f;
                float my = Core.input.mouse().y - Core.graphics.getHeight() / 2f;

                // Apply "Magnet" effect to main content
                // Limit the movement so it doesn't go off screen
                float maxOffset = 30f;
                float tx = Mathf.clamp(mx * 0.05f, -maxOffset, maxOffset);
                float ty = Mathf.clamp(my * 0.05f, -maxOffset, maxOffset);

                mainContent.setTranslation(tx, ty);
            }
        }

        @Override
        public void draw() {
            // REMOVED: Full screen fill blocking the game
            // Draw.color(Color.black, 0.9f);
            // Fill.rect(x + width / 2f, y + height / 2f, width, height);

            // 1. Draw Background (Hex Grid)
            drawHexGrid();

            // 2. Draw Dynamic Shadow
            drawShadow();

            // 3. Draw Children (Main Content)
            super.draw();
        }

        private void drawHexGrid() {
            Draw.color(CYAN_DARK, 0.4f); // Increased from 0.15f to 0.4f
            Lines.stroke(1f);

            float mx = Core.input.mouse().x - Core.graphics.getWidth() / 2f;
            float my = Core.input.mouse().y - Core.graphics.getHeight() / 2f;

            // Parallax the background inversely and slower
            float offX = -mx * 0.02f;
            float offY = -my * 0.02f;

            // Simple hex grid pattern
            float size = 60f;
            float time = Time.time * 0.5f;

            // Optimized grid: full screen loop
            for (float x = 0; x < Core.graphics.getWidth(); x += size * 1.5f) {
                for (float y = 0; y < Core.graphics.getHeight(); y += size * 1.732f) { // sqrt(3)
                    float drawX = x + offX + (y % (size * 3.464f) == 0 ? 0 : size * 0.75f);
                    float drawY = y + offY;

                    // Add some wave movement
                    drawY += Mathf.sin(x / 10f + time / 20f) * 5f;

                    drawHex(drawX, drawY, size / 2f);
                }
            }
            Draw.reset();
        }

        private void drawHex(float x, float y, float r) {
            Lines.poly(x, y, 6, r);
        }

        private void drawShadow() {
            if (shadowTarget == null)
                return;

            // Get shadowTarget's position in ParallaxContainer's coordinate system
            arc.math.geom.Vec2 pos = shadowTarget.localToAscendantCoordinates(this, new arc.math.geom.Vec2(0, 0));

            float mx = Core.input.mouse().x - Core.graphics.getWidth() / 2f;
            float my = Core.input.mouse().y - Core.graphics.getHeight() / 2f;

            // Shadow moves opposite to content ("Light source" feel)
            // Use target's real position + light offset
            float sx = pos.x + 20f - mx * 0.1f;
            float sy = pos.y - 20f - my * 0.1f;
            float w = shadowTarget.getWidth();
            float h = shadowTarget.getHeight();

            Draw.color(Color.black, 0.5f);
            Fill.rect(sx + w / 2f, sy + h / 2f, w, h);
            Draw.reset();
        }
    }

    // ==========================================
    // GLITCH OVERLAY
    // ==========================================
    private class GlitchOverlay extends Table {
        private float life = 1.5f; // Duration matches animation

        public GlitchOverlay() {
            this.setFillParent(true);
            this.touchable = arc.scene.event.Touchable.disabled;
        }

        @Override
        public void draw() {
            super.draw();
            if (life > 0) {
                life -= Time.delta / 60f;

                // Random digital artifacts
                Draw.color(CYAN_BRIGHT, life * 0.8f); // Fade out
                for (int i = 0; i < 4; i++) {
                    if (Mathf.chance(0.4)) {
                        float w = Mathf.random(50f, 400f);
                        float h = Mathf.random(2f, 15f);
                        float x = this.x + Mathf.random(width);
                        float y = this.y + Mathf.random(height);
                        Fill.rect(x, y, w, h);
                    }
                }

                // Random vertical scan line
                if (Mathf.chance(0.3)) {
                    Draw.color(Color.white, life * 0.4f);
                    float x = this.x + Mathf.random(width);
                    Fill.rect(x, this.y + height / 2f, 2f, height);
                }

                Draw.reset();
            } else {
                this.remove();
            }
        }
    }

    // ==========================================
    // CAMPAIGN HOLOGRAM WIDGET
    // ==========================================
    private class CampaignHologramWidget extends Table {
        public CampaignHologramWidget() {
            background(Styles.grayPanel);

            add(new Label("CAMPAIGN NEXUS")).color(Color.gray).fontScale(0.75f).top().left().pad(5f).row();

            // Stack for Planet + HUD
            Stack stack = new Stack();

            // 1. Planet Layer
            Table planetLayer = new Table() {
                @Override
                public void draw() {
                    super.draw();
                    float w = getWidth();
                    float h = getHeight();
                    float cx = x + w / 2f;
                    float cy = y + h / 2f;
                    float rad = Math.min(w, h) * 0.4f;

                    Draw.flush();

                    // Native Planet Asset
                    mindustry.type.Planet current = Planets.serpulo;
                    if (current != null) {
                        TextureRegion icon = current.uiIcon;

                        // Rotate the planet
                        float rotation = Time.time * 1.5f;

                        // 1. Atmosphere Glow
                        Draw.color(CYAN_PRIMARY, 0.3f);
                        Fill.circle(cx, cy, rad * 1.15f);

                        // 2. Planet Texture (Native)
                        Draw.color();
                        Draw.rect(icon, cx, cy, rad * 2f, rad * 2f, rotation);

                        // 3. Hologram Grid Overlay
                        Draw.color(CYAN_DARK, 0.3f);
                        Lines.stroke(1.5f);
                        Lines.circle(cx, cy, rad * 1.1f);

                        // 4. Scanning Beam
                        float scanY = y + h / 2f + Mathf.sin(Time.time, 50f, rad);
                        Draw.color(CYAN_BRIGHT, 0.7f);
                        Lines.line(cx - rad * 1.3f, scanY, cx + rad * 1.3f, scanY);
                    }

                    Draw.reset();
                }
            };
            stack.add(planetLayer);

            // 2. Data HUD Layer
            Table hudLayer = new Table();
            hudLayer.top().right().margin(10f); // FIXED margin for Arc Table

            // Stats Block
            hudLayer.table(t -> {
                t.defaults().right();
                t.add(new Label("SECTORS SECURED // 12")).color(CYAN_BRIGHT).fontScale(0.8f).row();
                t.add(new Label("GLOBAL THREAT // LOW")).color(Color.green).fontScale(0.8f).row();
                t.add(new Label("PROD.RATE // +450/s")).color(Color.yellow).fontScale(0.8f).row();
            }).top().right();

            stack.add(hudLayer);

            add(stack).grow();
        }
    }
}
