package mindustrytool.ui.dock;

import arc.util.Nullable;
import solim.config.ConfigGroup;
import solim.config.ContextualConfigValue;
import solim.overlay.DockAnchor;
import solim.overlay.DockAnchorStore;
import solim.overlay.DockSide;
import solim.reactive.Signal;
import solim.reactive.Signals;

public final class DockPositionConfig implements DockAnchorStore {

    private static final String SEPARATOR = "|";
    private static final String EMPTY = "";

    private final ContextualConfigValue<Float, Boolean> xConfig;
    private final ContextualConfigValue<Float, Boolean> yConfig;
    private final ContextualConfigValue<String, Boolean> dockConfig;

    public DockPositionConfig(ConfigGroup group, ContextualConfigValue<Float, Boolean> xConfig,
            ContextualConfigValue<Float, Boolean> yConfig) {
        this.xConfig = xConfig;
        this.yConfig = yConfig;
        this.dockConfig = group.stringValueKeyed(
                "dock",
                Signals.isPortrait(),
                portrait -> portrait ? "portrait" : "landscape",
                EMPTY);
    }

    public Signal<Float> xSignal() {
        return xConfig.signal();
    }

    public Signal<Float> ySignal() {
        return yConfig.signal();
    }

    public float x(float fallback) {
        Float value = xConfig.get();
        return value != null ? value : fallback;
    }

    public float y(float fallback) {
        Float value = yConfig.get();
        return value != null ? value : fallback;
    }

    public boolean isDocked() {
        return load() != null;
    }

    public void resetTo(float x, float y) {
        dockConfig.reset();
        xConfig.set(x);
        yConfig.set(y);
    }

    @Override
    public @Nullable DockAnchor load() {
        return decode(dockConfig.get());
    }

    @Override
    public void save(@Nullable DockAnchor anchor) {
        dockConfig.set(anchor != null ? encode(anchor) : EMPTY);
    }

    private static String encode(DockAnchor anchor) {
        return anchor.targetId()
                + SEPARATOR + anchor.side().name()
                + SEPARATOR + anchor.offset()
                + SEPARATOR + anchor.x()
                + SEPARATOR + anchor.y();
    }

    private static @Nullable DockAnchor decode(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 5) {
            return null;
        }
        try {
            return new DockAnchor(
                    parts[0],
                    DockSide.valueOf(parts[1]),
                    Float.parseFloat(parts[2]),
                    Float.parseFloat(parts[3]),
                    Float.parseFloat(parts[4]));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
