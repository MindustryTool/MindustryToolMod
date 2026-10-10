package mindustrytool.features.quickrotate;

import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.util.Align;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.core.GameState.State;
import mindustry.game.EventType.StateChangeEvent;
import mindustry.game.EventType.TapEvent;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Icon;
import mindustry.input.DesktopInput;
import mindustry.input.InputHandler;
import mindustry.input.MobileInput;
import mindustry.input.PlaceMode;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.overlay.SolimDialog;
import solim.reactive.Signal;

/**
 * Quick rotate feature: shows an in-world floating overlay next to the
 * selected rotatable building with step-rotate controls.
 */
public class QuickRotateFeature extends Feature {

    public final ConfigGroup config;
    public final ConfigValue<Boolean> autoDismissConfig;
    public final ConfigValue<Boolean> offsetAboveConfig;

    private final Signal<Building> target = Signal.of(null);
    private final Signal<Float> xSignal = Signal.of(0f);
    private final Signal<Float> ySignal = Signal.of(0f);

    private @Nullable QuickRotateSettingsDialog settingsDialog;
    private @Nullable QuickRotateOverlayView overlayView;

    public QuickRotateFeature() {
        super(FeatureMetadata.builder()
                .id("quick-rotate")
                .icon(Icon.refresh)
                .order(13)
                .quickAccess(true)
                .build());

        config = configGroup();
        autoDismissConfig = config.boolValue("auto-dismiss", true);
        offsetAboveConfig = config.boolValue("offset-above", true);

        Events.on(TapEvent.class, this::handleTap);
        Events.run(Trigger.update, this::update);
        Events.on(StateChangeEvent.class, e -> {
            if (e.to == State.menu) {
                hideOverlay();
            }
        });
    }

    public void resetToDefaults() {
        autoDismissConfig.reset();
        offsetAboveConfig.reset();
    }

    @Override
    public void onDisable() {
        hideOverlay();
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new QuickRotateSettingsDialog(this);
            }
            return settingsDialog;
        };
    }

    public Signal<Building> target() {
        return target;
    }

    public @Nullable Building currentTarget() {
        return target.peek();
    }

    public Signal<Float> xSignal() {
        return xSignal;
    }

    public Signal<Float> ySignal() {
        return ySignal;
    }

    public boolean isEligible(@Nullable Building building) {
        if (building == null || building.block == null) {
            return false;
        }
        if (!building.block.rotate) {
            return false;
        }
        if (Vars.player == null || building.team != Vars.player.team()) {
            return false;
        }
        return building.interactable(Vars.player.team());
    }

    private void handleTap(TapEvent e) {
        if (!isEnabled() || Vars.state == null || !Vars.state.isGame() || Vars.player == null) {
            return;
        }
        if (isPlacingInput()) {
            return;
        }
        if (e == null || e.tile == null) {
            if (isAutoDismiss()) {
                hideOverlay();
            }
            return;
        }
        if (isMenuTouched()) {
            return;
        }
        Building building = e.tile.build;
        if (isEligible(building)) {
            showOverlay(building);
        } else if (isAutoDismiss()) {
            hideOverlay();
        }
    }

    private void update() {
        if (!isEnabled() || Vars.state == null || !Vars.state.isGame() || Vars.player == null) {
            hideOverlay();
            return;
        }
        Building current = target.peek();
        if (current == null) {
            return;
        }
        if (!current.isValid() || current.tile == null || current.tile.build != current
                || !isEligible(current)) {
            hideOverlay();
            return;
        }
        updateOverlayPosition(current);
    }

    public void showOverlay(Building building) {
        if (!isEligible(building)) {
            return;
        }
        target.set(building);
        updateOverlayPosition(building);
        if (overlayView != null) {
            return;
        }
        overlayView = new QuickRotateOverlayView(this);
        Element element = overlayView.element();
        element.name = "quick-rotate-overlay";
        Core.app.post(() -> {
            if (overlayView != null && Vars.ui != null && Vars.ui.hudGroup != null) {
                Vars.ui.hudGroup.addChild(element);
            }
        });
    }

    public void hideOverlay() {
        target.set(null);
        if (overlayView != null) {
            QuickRotateOverlayView view = overlayView;
            overlayView = null;
            Core.app.post(() -> {
                view.element().remove();
                view.dispose();
            });
        }
    }

    private void updateOverlayPosition(Building building) {
        if (Core.camera == null || Core.graphics == null) {
            return;
        }
        Vec2 pos = Core.camera.project(building.x, building.y);
        float x = pos.x;
        float y = pos.y;
        Boolean offsetAbove = offsetAboveConfig.get();
        if (!Boolean.FALSE.equals(offsetAbove)) {
            y += building.block.size * Vars.tilesize * 1.5f;
        }
        Element element = overlayView != null ? overlayView.element() : null;
        float w = element != null && element.getWidth() > 0f ? element.getWidth() : 96f;
        float h = element != null && element.getHeight() > 0f ? element.getHeight() : 48f;
        x = Mathf.clamp(x, w / 2f + 8f, Core.graphics.getWidth() - w / 2f - 8f);
        y = Mathf.clamp(y, h / 2f + 8f, Core.graphics.getHeight() - h / 2f - 8f);
        xSignal.set(x);
        ySignal.set(y);
        if (element != null) {
            element.setPosition(x, y, Align.center);
        }
    }

    public void rotateClockwise() {
        rotate(true);
    }

    public void rotateCounterClockwise() {
        rotate(false);
    }

    public void rotate(boolean clockwise) {
        Building building = target.peek();
        if (building == null || Vars.player == null) {
            return;
        }
        if (!isEligible(building)) {
            hideOverlay();
            return;
        }
        Call.rotateBlock(Vars.player, building, clockwise);
    }

    private boolean isAutoDismiss() {
        return !Boolean.FALSE.equals(autoDismissConfig.get());
    }

    private boolean isMenuTouched() {
        if (overlayView == null || Core.scene == null || Core.input == null) {
            return false;
        }
        Element root = overlayView.element();
        Element hit = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
        return hit != null && (hit == root || hit.isDescendantOf(root));
    }

    private boolean isPlacingInput() {
        if (Vars.control == null || Vars.control.input == null) {
            return false;
        }
        InputHandler input = Vars.control.input;
        if (input.isPlacing() || input.isUsingSchematic()) {
            return true;
        }
        if (input instanceof MobileInput) {
            MobileInput mobile = (MobileInput) input;
            return mobile.lineMode || mobile.selecting || mobile.schematicMode || mobile.mode != PlaceMode.none;
        }
        if (input instanceof DesktopInput) {
            return ((DesktopInput) input).mode != PlaceMode.none;
        }
        return false;
    }
}
