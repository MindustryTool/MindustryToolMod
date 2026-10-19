package mindustrytool.features.background;

public final class BackgroundLifecycle {

    public enum Action {
        NONE,
        RELEASE_VIDEO,
        RELOAD
    }

    private BackgroundLifecycle() {
    }

    public static Action next(boolean enabled, boolean menu, boolean hasRenderer, boolean hasVideo) {
        if (!enabled) {
            return Action.NONE;
        }
        if (!menu) {
            return hasVideo ? Action.RELEASE_VIDEO : Action.NONE;
        }
        return hasRenderer ? Action.NONE : Action.RELOAD;
    }
}
