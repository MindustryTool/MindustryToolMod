package mindustrytool.features.browser.common;

import static solim.UI.*;

import arc.graphics.Color;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.TextButton.TextButtonStyle;
import mindustry.ui.Fonts;
import solim.graphics.RoundedDrawable;

/**
 * Dedicated button styles for browser UI featuring continuous rounded borders
 * with channel-list blue/indigo color palette for all state variants.
 * Does not mutate global Arc styles.
 */
public final class WebStyles {

    public static final Color CHANNEL_BLUE = new Color(0.45f, 0.35f, 0.9f, 0.8f);
    public static final Color CHANNEL_BLUE_OVER = new Color(0.55f, 0.45f, 1.0f, 0.9f);
    public static final Color CHANNEL_BLUE_DOWN = new Color(0.40f, 0.30f, 0.85f, 1.0f);
    public static final Color CHANNEL_BLUE_BG = new Color(0.45f, 0.35f, 0.9f, 0.15f);
    public static final Color CHANNEL_BLUE_BG_OVER = new Color(0.45f, 0.35f, 0.9f, 0.35f);
    public static final Color CHANNEL_BLUE_BG_DOWN = new Color(0.45f, 0.35f, 0.9f, 0.60f);

    public static final Color DISABLED_BORDER = new Color(0.3f, 0.3f, 0.3f, 0.5f);
    public static final Color DISABLED_BG = new Color(0.1f, 0.1f, 0.1f, 0.3f);

    public static final ButtonStyle webButton;
    public static final TextButtonStyle webTextButton;

    static {
        int radius = unit(2);
        float stroke = 1.5f;

        webButton = new ButtonStyle();
        webButton.up = new RoundedDrawable(radius, CHANNEL_BLUE_BG, stroke, CHANNEL_BLUE);
        webButton.over = new RoundedDrawable(radius, CHANNEL_BLUE_BG_OVER, stroke, CHANNEL_BLUE_OVER);
        webButton.down = new RoundedDrawable(radius, CHANNEL_BLUE_BG_DOWN, stroke, CHANNEL_BLUE_DOWN);
        webButton.disabled = new RoundedDrawable(radius, DISABLED_BG, 1.0f, DISABLED_BORDER);

        webTextButton = new TextButtonStyle();
        webTextButton.up = new RoundedDrawable(radius, CHANNEL_BLUE_BG, stroke, CHANNEL_BLUE);
        webTextButton.over = new RoundedDrawable(radius, CHANNEL_BLUE_BG_OVER, stroke, CHANNEL_BLUE_OVER);
        webTextButton.down = new RoundedDrawable(radius, CHANNEL_BLUE_BG_DOWN, stroke, CHANNEL_BLUE_DOWN);
        webTextButton.disabled = new RoundedDrawable(radius, DISABLED_BG, 1.0f, DISABLED_BORDER);
        webTextButton.font = Fonts.def;
        webTextButton.fontColor = Color.white;
        webTextButton.overFontColor = Color.white;
        webTextButton.downFontColor = Color.lightGray;
        webTextButton.disabledFontColor = Color.gray;
    }

    private WebStyles() {
    }
}
