package mindustrytool.features.quickrotate;

import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
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

/**
 * Quick rotate feature: shows an in-world floating overlay next to the
 * selected rotatable building with step-rotate controls.
 */
public class QuickRotateFeature extends Feature {

    public final ConfigGroup config;
    public final ConfigValue<Boolean> autoDismissConfig;
    public final ConfigValue<Boolean> offsetAboveConfig;

    private @Nullable Building currentTarget;
    private @Nullable Table currentMenu;
    private @Nullable QuickRotateSettingsDialog settingsDialog;

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
                closeMenu();
            }
        });
    }

    public void resetToDefaults() {
        autoDismissConfig.reset();
        offsetAboveConfig.reset();
    }

    @Override
    public void onDisable() {
        closeMenu();
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

    public @Nullable Building currentTarget() {
        return currentTarget;
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
                closeMenu();
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
            closeMenu();
        }
    }

    private void update() {
        if (!isEnabled() || Vars.state == null || !Vars.state.isGame() || Vars.player == null) {
            closeMenu();
            return;
        }
        if (currentTarget == null) {
            return;
        }
        if (!currentTarget.isValid() || currentTarget.tile == null
                || currentTarget.tile.build != currentTarget || !isEligible(currentTarget)) {
            closeMenu();
        }
    }

    public void showOverlay(Building building) {
        if (!isEligible(building)) {
            return;
        }
        if (building == currentTarget && currentMenu != null) {
            updateOverlayPosition(building);
            return;
        }
        closeMenu();
        currentTarget = building;

        currentMenu = new QuickRotateOverlayView(this).build();
        currentMenu.visible(() -> Vars.ui != null && Vars.ui.hudfrag != null
                && Vars.ui.hudfrag.shown && Vars.state != null && Vars.state.isGame());
        currentMenu.touchable = Touchable.enabled;
        attachMenuToHud();
        updateOverlayPosition(building);
    }

    public void closeMenu() {
        if (currentMenu != null) {
            currentMenu.remove();
            currentMenu = null;
        }
        currentTarget = null;
    }

    private void attachMenuToHud() {
        if (currentMenu == null) {
            return;
        }

        currentMenu.update(() -> {
            if (currentTarget == null || Vars.player == null || Core.camera == null) {
                closeMenu();
                return;
            }
            updateOverlayPosition(currentTarget);
        });

        if (Vars.ui != null && Vars.ui.hudGroup != null) {
            Table menu = currentMenu;
            Core.app.post(() -> {
                if (currentMenu == menu && Vars.ui.hudGroup != null) {
                    Vars.ui.hudGroup.addChild(menu);
                    menu.pack();
                    if (currentTarget != null) {
                        updateOverlayPosition(currentTarget);
                    }
                }
            });
        }
    }

    private void updateOverlayPosition(Building building) {
        if (Core.camera == null || Core.graphics == null || currentMenu == null) {
            return;
        }
        // Offset in world space before projecting so camera zoom scales it correctly.
        Boolean offsetAbove = offsetAboveConfig.get();
        float anchorY = !Boolean.FALSE.equals(offsetAbove)
                ? building.y + building.block.size * Vars.tilesize * 1.5f
                : building.y;
        Vec2 pos = Core.camera.project(building.x, anchorY);
        float x = pos.x;
        float y = pos.y;
        float w = currentMenu.getWidth() > 0f ? currentMenu.getWidth() : 96f;
        float h = currentMenu.getHeight() > 0f ? currentMenu.getHeight() : 48f;
        x = Mathf.clamp(x, w / 2f + 8f, Core.graphics.getWidth() - w / 2f - 8f);
        y = Mathf.clamp(y, h / 2f + 8f, Core.graphics.getHeight() - h / 2f - 8f);
        currentMenu.setPosition(x, y, Align.center);
    }

    public void rotateClockwise() {
        // Verified against InputHandler.rotateBlock: direction=false decrements
        // rotation, which is visually clockwise on screen.
        rotate(false);
    }

    public void rotateCounterClockwise() {
        // Verified against InputHandler.rotateBlock: direction=true increments
        // rotation, which is visually counter-clockwise on screen.
        rotate(true);
    }

    public void rotate(boolean clockwise) {
        if (currentTarget == null || Vars.player == null) {
            return;
        }
        if (!isEligible(currentTarget)) {
            closeMenu();
            return;
        }
        Call.rotateBlock(Vars.player, currentTarget, clockwise);
    }

    private boolean isAutoDismiss() {
        return !Boolean.FALSE.equals(autoDismissConfig.get());
    }

    private boolean isMenuTouched() {
        if (currentMenu == null || Core.scene == null || Core.input == null) {
            return false;
        }
        Element hit = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
        return hit != null && (hit == currentMenu || hit.isDescendantOf(currentMenu));
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
