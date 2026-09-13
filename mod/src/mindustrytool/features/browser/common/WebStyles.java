package mindustrytool.features.browser.common;

import static solim.UI.*;

import arc.graphics.Color;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.TextButton.TextButtonStyle;
import mindustry.ui.Fonts;
import solim.style.SolimButtonStyleBuilder;

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

        SolimButtonStyleBuilder builder = new SolimButtonStyleBuilder()
                .rounded(radius)
                .border(stroke, CHANNEL_BLUE)
                .up(u -> u.background(CHANNEL_BLUE_BG))
                .over(o -> o.background(CHANNEL_BLUE_BG_OVER).border(stroke, CHANNEL_BLUE_OVER))
                .down(d -> d.background(CHANNEL_BLUE_BG_DOWN).border(stroke, CHANNEL_BLUE_DOWN))
                .disabled(dis -> dis.background(DISABLED_BG).border(1.0f, DISABLED_BORDER));

        webButton = builder.build().style();

        webTextButton = new TextButtonStyle();
        webTextButton.up = webButton.up;
        webTextButton.over = webButton.over;
        webTextButton.down = webButton.down;
        webTextButton.disabled = webButton.disabled;
        webTextButton.font = Fonts.def;
        webTextButton.fontColor = Color.white;
        webTextButton.overFontColor = Color.white;
        webTextButton.downFontColor = Color.lightGray;
        webTextButton.disabledFontColor = Color.gray;
    }

    public static ButtonStyle button() {
        return webButton;
    }

    public static ButtonStyle webButton() {
        return webButton;
    }

    public static TextButtonStyle webTextButton() {
        return webTextButton;
    }

    private WebStyles() {
    }
}
