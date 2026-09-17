package mindustrytool.features.joystick;

import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.input.InputHandler;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.overlay.SolimDialog;

/**
 * On-screen virtual joystick that decouples unit movement from camera panning.
 * Replaces {@code Vars.control.input} with joystick-aware input handlers while enabled.
 */
public class JoystickFeature extends Feature {

    public static final float DEFAULT_POS_X = 25f;
    public static final float DEFAULT_POS_Y = 140f;

    /** Normalized movement vector (-1..1 per axis) driven by the joystick knob; zero when idle. */
    public final Vec2 moveVector = new Vec2();

    /** Pointer ID currently touching the joystick knob, or -1 when idle. */
    public int activePointer = -1;

    public final ConfigGroup config;
    public final ConfigValue<Float> sizeConfig;
    public final ConfigValue<Float> opacityConfig;
    public final ConfigValue<Boolean> showHandleConfig;
    public final ConfigValue<Float> posXConfig;
    public final ConfigValue<Float> posYConfig;

    private @Nullable InputHandler originalInput;
    private @Nullable JoystickHudView hudView;
    private @Nullable JoystickSettingsDialog settingsDialog;

    public JoystickFeature() {
        super(FeatureMetadata.builder()
                .id("joystick")
                .icon(FileIcon.of("grid-2x2.png"))
                .order(21)
                .enabledByDefault(false)
                .build());

        config = configGroup();
        sizeConfig = config.floatValue("size", 1f);
        opacityConfig = config.floatValue("opacity", 0.8f);
        showHandleConfig = config.boolValue("showHandle", true);
        posXConfig = config.floatValue("posX", DEFAULT_POS_X);
        posYConfig = config.floatValue("posY", DEFAULT_POS_Y);

        Events.run(WorldLoadEvent.class, () -> Core.app.post(() -> {
            if (isEnabled()) {
                swapInput();
            }
        }));
    }

    @Override
    public void onEnable() {
        swapInput();
        mountHud();
    }

    @Override
    public void onDisable() {
        restoreInput();
        unmountHud();
        moveVector.setZero();
        activePointer = -1;
    }

    public boolean isKnobHeld() {
        return activePointer != -1;
    }

    public void cancelPanDelay() {
        if (Vars.control != null && Vars.control.input instanceof JoystickMobileInput) {
            ((JoystickMobileInput) Vars.control.input).cancelPanDelay();
        }
    }

    /** Replaces the active input handler with a joystick-aware one, remembering the original. */
    private void swapInput() {
        InputHandler current = Vars.control.input;
        if (current instanceof JoystickMobileInput || current instanceof JoystickDesktopInput) {
            return;
        }
        originalInput = current;
        InputHandler replacement = Vars.mobile ? new JoystickMobileInput(this) : new JoystickDesktopInput(this);
        Vars.control.setInput(replacement);
    }

    /** Restores the original input handler captured before the swap. */
    private void restoreInput() {
        if (originalInput != null
                && (Vars.control.input instanceof JoystickMobileInput || Vars.control.input instanceof JoystickDesktopInput)) {
            Vars.control.setInput(originalInput);
        }
        originalInput = null;
    }

    private void mountHud() {
        if (hudView != null) {
            return;
        }
        hudView = new JoystickHudView(this);
        Element el = hudView.element();
        el.name = "joystick-hud";
        el.visible(() -> Vars.ui != null && Vars.ui.hudfrag != null && Vars.ui.hudfrag.shown
                && Vars.state != null && Vars.state.isGame());

        Core.app.post(() -> {
            if (hudView != null && Vars.ui != null && Vars.ui.hudGroup != null) {
                Vars.ui.hudGroup.addChild(el);
            }
        });
    }

    private void unmountHud() {
        if (hudView == null) {
            return;
        }
        JoystickHudView view = hudView;
        hudView = null;
        Core.app.post(() -> {
            view.element().remove();
            view.dispose();
        });
    }

    /** Restores the joystick position back to the default bottom-left offset. */
    public void resetPosition() {
        posXConfig.reset();
        posYConfig.reset();
        if (hudView != null) {
            Core.app.post(hudView::keepInScreen);
        }
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new JoystickSettingsDialog(this);
            }
            return settingsDialog;
        };
    }
}
