package mindustrytool.presentation.builder;

import arc.Core;
import mindustry.Vars;

public final class FilterConfig {
    public final float scale;
    public final int cardSize;
    public final int cols;
    public static final int CARD_GAP = 4;

    public FilterConfig() {
        scale = Vars.mobile ? 0.8f : 1f;
        cardSize = (int) (300 * scale);
        cols = (int) Math.max(Math.floor(Core.graphics.getWidth() / (cardSize + CARD_GAP)), 1);
    }
}
