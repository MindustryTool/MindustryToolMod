package mindustrytool.features.display.quickaccess;

import java.util.Optional;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.graphics.Color;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.event.VisibilityListener;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.Button;
import arc.scene.ui.Dialog;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Scaling;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Main;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;

public class QuickAccessHud extends Table implements Feature {
    private Table currentPopup;
    private Feature currentPopupFeature;

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.quick-access-hud.name")
                .description("@feature.quick-access-hud.description")
                .icon(Icon.menu)
                .build();
    }

    @Override
    public void init() {
        // Ensure we are not catching input for the whole screen if we were filling it
        // (we aren't)
        touchable = Touchable.childrenOnly;

        // Initial position
        setPosition(QuickAccessConfig.x(), QuickAccessConfig.y());

        // Build UI
        rebuild();

        addListener(new VisibilityListener() {
            @Override
            public boolean hidden() {
                if (currentPopup != null) {
                    currentPopup.remove();
                    currentPopup = null;
                    currentPopupFeature = null;
                }

                return false;
            }
        });

        Events.on(EventType.ResizeEvent.class, event -> {
            this.rebuild();
        });

        Events.on(EventType.StateChangeEvent.class, event -> {
            if (currentPopup != null) {
                currentPopup.remove();
                currentPopup = null;
                currentPopupFeature = null;
            }
        });
    }

    private void rebuild() {
        clear();

        // Main container table that will be dragged
        Table container = new Table();
        container.background(Styles.black6);
        container.setColor(1f, 1f, 1f, QuickAccessConfig.opacity());
        container.touchable = Touchable.enabled; // Container catches touches

        float scale = QuickAccessConfig.scale();
        float buttonSize = 48f * scale;
        float margin = 8f * scale;

        // 1. Anchor (Draggable only)
        container.button(Icon.move, Styles.clearNonei, () -> {
        })
                .size(buttonSize)
                .margin(margin)
                .get()
                .addListener(new InputListener() {
                    float lastX, lastY;

                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                        lastX = x;
                        lastY = y;
                        return true;
                    }

                    @Override
                    public void touchDragged(InputEvent event, float x, float y, int pointer) {
                        try {
                            moveBy(x - lastX, y - lastY);

                            float sw = Core.graphics.getWidth();
                            float sh = Core.graphics.getHeight();

                            QuickAccessHud.this.x = Mathf.clamp(QuickAccessHud.this.x, 0, sw - 40f);
                            QuickAccessHud.this.y = Mathf.clamp(QuickAccessHud.this.y, 0, sh - 40f);

                            QuickAccessConfig.x(QuickAccessHud.this.x);
                            QuickAccessConfig.y(QuickAccessHud.this.y);

                            closePopup();
                        } catch (Exception e) {
                            Log.err(e);
                        }
                    }
                });

        float sw = Core.graphics.getWidth();
        float sh = Core.graphics.getHeight();

        QuickAccessHud.this.x = Mathf.clamp(QuickAccessHud.this.x, 0, sw - 40f);
        QuickAccessHud.this.y = Mathf.clamp(QuickAccessHud.this.y, 0, sh - 40f);

        QuickAccessConfig.x(QuickAccessHud.this.x);
        QuickAccessConfig.y(QuickAccessHud.this.y);

        // 2. Separator
        Image sep = new Image(Tex.whiteui);
        sep.setColor(mindustry.graphics.Pal.accent);
        container.add(sep).width(2f).fillY();

        // 3. Content (Always visible)
        Table content = new Table();
        populateContent(content);
        container.add(content);

        add(container).pad(0).margin(0);
        pack();
    }

    private void populateContent(Table t) {
        t.background(Styles.black6);

        Seq<Feature> features = FeatureManager.getInstance().getFeatures();
        int i = 0;
        int cols = QuickAccessConfig.cols();
        float scale = QuickAccessConfig.scale();
        float buttonSize = 48f * scale;
        float margin = 8f * scale;

        for (Feature f : features) {
            // Skip this feature itself
            if (f == this)
                continue;

            FeatureMetadata meta = f.getMetadata();
            if (!meta.quickAccess()) {
                continue;
            }

            if (!QuickAccessConfig.isFeatureVisible(meta.name())) {
                continue;
            }

            Button[] btnRef = new Button[1];
            btnRef[0] = t.button(b -> {
                b.image(meta.icon())
                        .size(buttonSize * 0.7f)
                        .scaling(Scaling.fit)
                        .update(l -> l.setColor(FeatureManager.getInstance().isEnabled(f) ? Color.white : Pal.gray));
            }, Styles.clearNonei, () -> {
                showPopupFor(btnRef[0], f);
            })
                    .size(buttonSize)
                    .margin(margin)
                    .tooltip(meta.name())
                    .get();

            if (++i % cols == 0)
                t.row();
        }

        Button[] btnRef = new Button[1];
        btnRef[0] = t.button(b -> {
            b.image(Icon.settings)
                    .size(buttonSize * 0.7f)
                    .scaling(Scaling.fit);
        }, Styles.clearNonei, () -> {
            Main.featureSettingDialog.show(false);
        })
                .size(buttonSize)
                .margin(margin)
                .get();
    }

    // Helper to be used inside populateContent
    private void showPopupFor(Element anchor, Feature f) {
        if (currentPopup != null) {
            boolean isSame = (currentPopupFeature == f);
            closePopup();

            if (isSame) {
                return;
            }
        }

        currentPopupFeature = f;

        Table popup = new Table();

        popup.visible(() -> Vars.ui.hudfrag != null && Vars.ui.hudfrag.shown);
        popup.background(Styles.black6);
        popup.touchable = Touchable.enabled;

        // Options
        popup.check("@enabled", FeatureManager.getInstance().isEnabled(f), b -> {
            FeatureManager.getInstance().setEnabled(f, b);
        }).pad(10).left().row();

        float widthScale = QuickAccessConfig.width();

        f.setting().ifPresent(dialog -> {
            popup.button("@settings", Icon.settings, () -> {
                dialog.show();
                closePopup();
            }).fillX().pad(10).padTop(0).left().minWidth(220 * widthScale);
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

        Vars.ui.hudGroup.addChild(popup);
        popup.name = "quickAccessPopup";
        currentPopup = popup;

        Timer.schedule(() -> popup.toFront(), 5f);
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
            remove();

            name = "quick-access-hud";
            visible(() -> Vars.ui.hudfrag.shown && Vars.state.isGame());
            Core.app.post(() -> Vars.ui.hudGroup.addChild(this));
        }
    }

    @Override
    public void onDisable() {
        closePopup();
        remove();
    }

    @Override
    public Optional<Dialog> setting() {
        var dialog = new BaseDialog("@settings");
        dialog.name = "quickAccessSettingDialog";

        dialog.addCloseButton();
        dialog.closeOnBack();

        Table table = new Table();

        dialog.cont.pane(table)
                .center()
                .maxWidth(800)
                .grow();

        // Settings
        table.add("@settings").style(Styles.outlineLabel).left().pad(5).row();

        // Opacity
        Table opacityTable = new Table();
        opacityTable.left();
        opacityTable.add("@opacity").left().padRight(10);
        Slider opacitySlider = new Slider(0.05f, 1f, 0.05f, false);
        opacitySlider.setValue(QuickAccessConfig.opacity());
        Label opacityLabel = new Label(String.format("%.0f%%", QuickAccessConfig.opacity() * 100));
        opacitySlider.changed(() -> {
            QuickAccessConfig.opacity(opacitySlider.getValue());
            opacityLabel.setText(String.format("%.0f%%", QuickAccessConfig.opacity() * 100));
            QuickAccessHud.this.rebuild();
        });
        opacityTable.add(opacitySlider).width(200f);
        opacityTable.add(opacityLabel).padLeft(10);
        table.add(opacityTable).left().pad(5).row();

        // Scale
        Table scaleTable = new Table();
        scaleTable.left();
        scaleTable.add("@scale").left().padRight(10);
        Slider scaleSlider = new Slider(0.5f, 1.5f, 0.1f, false);
        scaleSlider.setValue(QuickAccessConfig.scale());
        Label scaleLabel = new Label(String.format("%.0f%%", QuickAccessConfig.scale() * 100));
        scaleSlider.changed(() -> {
            QuickAccessConfig.scale(scaleSlider.getValue());
            scaleLabel.setText(String.format("%.0f%%", QuickAccessConfig.scale() * 100));
            QuickAccessHud.this.rebuild();
        });
        scaleTable.add(scaleSlider).width(200f);
        scaleTable.add(scaleLabel).padLeft(10);
        table.add(scaleTable).left().pad(5).row();

        // Width
        Table widthTable = new Table();
        widthTable.left();
        widthTable.add("@width").left().padRight(10);
        Slider widthSlider = new Slider(0.5f, 2.0f, 0.1f, false);
        widthSlider.setValue(QuickAccessConfig.width());
        Label widthLabel = new Label(String.format("%.0f%%", QuickAccessConfig.width() * 100));
        widthSlider.changed(() -> {
            QuickAccessConfig.width(widthSlider.getValue());
            widthLabel.setText(String.format("%.0f%%", QuickAccessConfig.width() * 100));
            QuickAccessHud.this.rebuild();
        });
        widthTable.add(widthSlider).width(200f);
        widthTable.add(widthLabel).padLeft(10);
        table.add(widthTable).left().pad(5).row();

        // Columns
        Table colsTable = new Table();
        colsTable.left();
        colsTable.add("@columns").left().padRight(10);
        Slider colsSlider = new Slider(1, 9, 1, false);
        colsSlider.setValue(QuickAccessConfig.cols());
        Label colsLabel = new Label(String.valueOf(QuickAccessConfig.cols()));
        colsSlider.changed(() -> {
            QuickAccessConfig.cols((int) colsSlider.getValue());
            colsLabel.setText(String.valueOf((int) colsSlider.getValue()));
            QuickAccessHud.this.rebuild();
        });
        colsTable.add(colsSlider).width(200f);
        colsTable.add(colsLabel).padLeft(10);
        table.add(colsTable).left().pad(5).row();

        table.image().color(Color.gray).height(2).growX().pad(5).row();
        table.add("@features").style(Styles.outlineLabel).left().pad(5).row();

        Seq<Feature> features = FeatureManager.getInstance().getFeatures();
        for (Feature f : features) {
            if (f == QuickAccessHud.this) {
                continue;
            }

            FeatureMetadata meta = f.getMetadata();

            if (!meta.quickAccess()) {
                continue;
            }

            table.check(meta.name(), QuickAccessConfig.isFeatureVisible(meta.name()), b -> {
                QuickAccessConfig.setFeatureVisible(meta.name(), b);
                QuickAccessHud.this.rebuild();
            }).fillX().top().left().pad(5).get().left();

            table.row();
        }

        table.button("@reset", () -> {
            QuickAccessConfig.x(0);
            QuickAccessConfig.y(0);
        }).fillX().top().left().pad(5).get().left();

        return Optional.of(dialog);
    }
}
