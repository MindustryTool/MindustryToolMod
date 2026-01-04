package mindustrytool.features.gameplay.controls;

import arc.struct.Seq;
import mindustrytool.Feature;
import mindustrytool.features.content.browser.LazyComponent;

public class TouchFeature implements Feature {

    public static final LazyComponent<TouchHandler> touchComponent = new LazyComponent<>(
            "Touch",
            "On-screen controls for mobile (Joystick/D-Pad).",
            TouchHandler::new,
            false // Default disabled
    );

    static {
        touchComponent.onSettings(() -> new TouchSettingsDialog().show());

        touchComponent.onToggle(enabled -> {
            // Ensure instance is created/loaded if enabling
            if (enabled || touchComponent.isLoaded()) {
                touchComponent.get().setEnabled(enabled);
            }
        });
    }

    public static Seq<LazyComponent<?>> getLazyComponents() {
        return Seq.with(touchComponent);
    }

    @Override
    public String getName() {
        return "Touch Controls";
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public void init() {
        // Initialize state if enabled by default (though we set it to false)
        if (touchComponent.isEnabled()) {
            touchComponent.get().setEnabled(true);
        }
    }
}
