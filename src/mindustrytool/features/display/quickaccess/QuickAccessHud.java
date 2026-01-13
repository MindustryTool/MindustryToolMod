package mindustrytool.features.display.quickaccess;

import arc.Core;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.graphics.Color;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.ui.Image;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;

public class QuickAccessHud extends Table implements Feature {
    private final QuickAccessConfig config = new QuickAccessConfig();
    private Table currentPopup;
    private Feature currentPopupFeature;

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("Quick Access HUD")
                .description("Quick access bar for toggling features")
                .icon(Iconc.menu)
                .build();
    }

    @Override
    public void init() {
        // Ensure we are not catching input for the whole screen if we were filling it
        // (we aren't)
        touchable = Touchable.childrenOnly;

        // Initial position
        setPosition(config.x(), config.y());

        // Build UI
        rebuild();
    }

    private void rebuild() {
        clear();

        // Main container table that will be dragged
        Table container = new Table();
        container.background(Styles.black6);
        container.touchable = Touchable.enabled; // Container catches touches

        // 1. Anchor (Draggable only)
        container.button(Icon.move, Styles.clearNonei, () -> {
        }).size(40f).get().addListener(new InputListener() {
            float lastX, lastY;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                lastX = x;
                lastY = y;
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                // Move the main table (this)
                moveBy(x - lastX, y - lastY);

                // Clamp to screen
                // Ensure the anchor (40f) remains visible
                float sw = Core.graphics.getWidth();
                float sh = Core.graphics.getHeight();
                QuickAccessHud.this.x = Mathf.clamp(QuickAccessHud.this.x, 0, sw - 40f);
                QuickAccessHud.this.y = Mathf.clamp(QuickAccessHud.this.y, 0, sh - 40f);

                config.x(QuickAccessHud.this.x);
                config.y(QuickAccessHud.this.y);

                // Close popup if moving
                closePopup();
            }
        });

        // 2. Separator
        Image sep = new Image(Tex.whiteui);
        sep.setColor(mindustry.graphics.Pal.accent);
        container.add(sep).width(2f).fillY().pad(0, 4, 0, 4);

        // 3. Content (Always visible)
        Table content = new Table();
        populateContent(content);
        container.add(content);

        add(container);
        pack();
    }

    private void populateContent(Table t) {
        t.background(Styles.black6);

        Seq<Feature> features = FeatureManager.getInstance().getFeatures();
        int i = 0;
        int cols = 5; // 5 buttons per row

        for (Feature f : features) {
            // Skip this feature itself
            if (f == this)
                continue;

            FeatureMetadata meta = f.getMetadata();
            if (!meta.quickAccess())
                continue;

            // Feature Button
            // Using Label for Iconc
            Button[] btnRef = new Button[1];
            btnRef[0] = t.button(b -> {
                b.label(() -> String.valueOf(meta.icon()))
                        .fontScale(1.5f)
                        .update(l -> l
                                .setColor(FeatureManager.getInstance().isEnabled(f) ? Color.white
                                        : mindustry.graphics.Pal.gray));
            }, Styles.clearNonei, () -> {
                showPopupFor(btnRef[0], f);
            }).size(40f).pad(2f).tooltip(meta.name()).get();

            if (++i % cols == 0)
                t.row();
        }
    }

    // Helper to be used inside populateContent
    private void showPopupFor(Element anchor, Feature f) {
        if (currentPopup != null) {
            boolean isSame = (currentPopupFeature == f);
            closePopup();
            // If clicking same feature, toggle off (already closed above)
            if (isSame) {
                return;
            }
        }

        currentPopupFeature = f;

        Table popup = new Table();
        popup.background(Styles.black6);
        popup.touchable = Touchable.enabled;

        // Options
        popup.check("Enabled", FeatureManager.getInstance().isEnabled(f), b -> {
            FeatureManager.getInstance().setEnabled(f, b);
        }).pad(10).left().row();

        f.setting().ifPresent(dialog -> {
            popup.button("Settings", Icon.settings, () -> {
                dialog.show();
                closePopup();
            }).fillX().pad(10).padTop(0).left();
        });

        popup.pack();

        // Position
        Vec2 pos = anchor.localToStageCoordinates(new Vec2(0, 0));
        float screenHeight = Core.graphics.getHeight();

        if (pos.y < screenHeight / 2) {
            // Lower half -> show above
            popup.setPosition(pos.x, pos.y + anchor.getHeight());
        } else {
            // Upper half -> show below
            popup.setPosition(pos.x, pos.y - popup.getHeight());
        }

        // Clamp X to screen
        if (popup.x < 0)
            popup.x = 0;
        if (popup.x + popup.getWidth() > Core.graphics.getWidth())
            popup.x = Core.graphics.getWidth() - popup.getWidth();

        // Clamp Y just in case
        if (popup.y < 0)
            popup.y = 0;
        if (popup.y + popup.getHeight() > Core.graphics.getHeight())
            popup.y = Core.graphics.getHeight() - popup.getHeight();

        Core.scene.add(popup);
        currentPopup = popup;
    }

    private void closePopup() {
        if (currentPopup != null) {
            currentPopup.remove();
            currentPopup = null;
            currentPopupFeature = null;
        }
    }

    @Override
    public void onEnable() {
        if (Vars.ui != null && Vars.ui.hudGroup != null) {
            // Remove existing if any
            if (Vars.ui.hudGroup.find("quick-access-hud") != null) {
                Vars.ui.hudGroup.find("quick-access-hud").remove();
            }
            name = "quick-access-hud";
            Vars.ui.hudGroup.addChild(this);
            // Ensure visible only in game
            visible(() -> Vars.ui.hudfrag.shown && Vars.state.isGame());
        }
    }

    @Override
    public void onDisable() {
        closePopup();
        remove();
    }
}
