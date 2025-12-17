package mindustrytool.plugins.browser;

import mindustry.Vars;

public final class FilterConfig {
    public final float scale;
    public static final int CARD_GAP = 4;

    public FilterConfig() {
        scale = Vars.mobile ? 0.8f : 1f;
    }
}
