package mindustrytool.features.browser.common;

import static solim.UI.*;

import arc.graphics.Color;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.TextButton.TextButtonStyle;
import mindustry.ui.Fonts;
import solim.style.SolimButtonStyle;
import solim.style.SolimButtonStyleBuilder;

/**
 * Design system hub for browser UI with shadcn/ui-inspired semantic colors
 * and shared button variants with built-in padding.
 * Does not mutate global Arc styles.
 */
public final class WebStyles {

    /**
     * Semantic color tokens for browser UI.
     */
    public static final class Colors {
        public static final Color PRIMARY = new Color(0.45f, 0.35f, 0.90f, 1.0f);
        public static final Color PRIMARY_HOVER = new Color(0.55f, 0.45f, 1.0f, 1.0f);
        public static final Color PRIMARY_DOWN = new Color(0.40f, 0.30f, 0.85f, 1.0f);
        public static final Color PRIMARY_FG = new Color(1f, 1f, 1f, 1f);
        public static final Color PRIMARY_BG = new Color(0.45f, 0.35f, 0.90f, 0.15f);
        public static final Color PRIMARY_BG_HOVER = new Color(0.45f, 0.35f, 0.90f, 0.35f);
        public static final Color PRIMARY_BG_DOWN = new Color(0.45f, 0.35f, 0.90f, 0.60f);

        public static final Color SECONDARY = new Color(0.20f, 0.20f, 0.28f, 0.70f);
        public static final Color SECONDARY_HOVER = new Color(0.26f, 0.26f, 0.36f, 0.80f);
        public static final Color SECONDARY_DOWN = new Color(0.15f, 0.15f, 0.21f, 0.90f);
        public static final Color SECONDARY_FG = new Color(0.90f, 0.90f, 0.95f, 1.0f);

        public static final Color GHOST_HOVER = new Color(1f, 1f, 1f, 0.10f);
        public static final Color GHOST_DOWN = new Color(1f, 1f, 1f, 0.18f);
        public static final Color GHOST_FG = new Color(0.75f, 0.75f, 0.82f, 1.0f);

        public static final Color DANGER = new Color(0.85f, 0.25f, 0.25f, 1.0f);
        public static final Color DANGER_HOVER = new Color(0.95f, 0.33f, 0.33f, 1.0f);
        public static final Color DANGER_DOWN = new Color(0.70f, 0.18f, 0.18f, 1.0f);
        public static final Color DANGER_FG = new Color(1f, 1f, 1f, 1f);

        public static final Color BORDER = new Color(0.35f, 0.35f, 0.45f, 0.40f);

        public static final Color DISABLED_BG = new Color(0.1f, 0.1f, 0.1f, 0.3f);
        public static final Color DISABLED_BORDER = new Color(0.3f, 0.3f, 0.3f, 0.5f);
        public static final Color DISABLED_FG = new Color(0.5f, 0.5f, 0.5f, 1f);

        private Colors() {
        }
    }

    private static final SolimButtonStyle PRIMARY_STYLE = new SolimButtonStyleBuilder()
            .rounded(unit(2))
            .padding(unit(2))
            .up(u -> u.background(Colors.PRIMARY))
            .over(o -> o.background(Colors.PRIMARY_HOVER))
            .down(d -> d.background(Colors.PRIMARY_DOWN))
            .disabled(dis -> dis.background(Colors.DISABLED_BG))
            .build();

    private static final SolimButtonStyle SECONDARY_STYLE = new SolimButtonStyleBuilder()
            .rounded(unit(2))
            .padding(unit(2))
            .up(u -> u.background(Colors.SECONDARY))
            .over(o -> o.background(Colors.SECONDARY_HOVER))
            .down(d -> d.background(Colors.SECONDARY_DOWN))
            .disabled(dis -> dis.background(Colors.DISABLED_BG))
            .build();

    private static final SolimButtonStyle OUTLINE_STYLE = new SolimButtonStyleBuilder()
            .rounded(unit(2))
            .border(1.5f, Colors.PRIMARY)
            .padding(unit(2))
            .up(u -> u.background(Colors.PRIMARY_BG))
            .over(o -> o.background(Colors.PRIMARY_BG_HOVER).border(1.5f, Colors.PRIMARY_HOVER))
            .down(d -> d.background(Colors.PRIMARY_BG_DOWN).border(1.5f, Colors.PRIMARY_DOWN))
            .disabled(dis -> dis.background(Colors.DISABLED_BG).border(1.0f, Colors.DISABLED_BORDER))
            .build();

    private static final SolimButtonStyle GHOST_STYLE = new SolimButtonStyleBuilder()
            .rounded(unit(2))
            .padding(unit(2))
            .up(u -> u.background(Color.clear))
            .over(o -> o.background(Colors.GHOST_HOVER))
            .down(d -> d.background(Colors.GHOST_DOWN))
            .disabled(dis -> dis.background(Colors.DISABLED_BG))
            .build();

    private static final SolimButtonStyle DANGER_STYLE = new SolimButtonStyleBuilder()
            .rounded(unit(2))
            .padding(unit(2))
            .up(u -> u.background(Colors.DANGER))
            .over(o -> o.background(Colors.DANGER_HOVER))
            .down(d -> d.background(Colors.DANGER_DOWN))
            .disabled(dis -> dis.background(Colors.DISABLED_BG))
            .build();

    private static final SolimButtonStyle PRIMARY_TEXT_STYLE = textOf(
            PRIMARY_STYLE, Colors.PRIMARY_FG, Colors.PRIMARY_FG, Color.lightGray, Colors.DISABLED_FG);

    private static final SolimButtonStyle SECONDARY_TEXT_STYLE = textOf(
            SECONDARY_STYLE, Colors.SECONDARY_FG, Colors.PRIMARY_FG, Color.lightGray, Colors.DISABLED_FG);

    private static final SolimButtonStyle OUTLINE_TEXT_STYLE = textOf(
            OUTLINE_STYLE, Colors.PRIMARY_FG, Colors.PRIMARY_FG, Color.lightGray, Colors.DISABLED_FG);

    private static final SolimButtonStyle GHOST_TEXT_STYLE = textOf(
            GHOST_STYLE, Colors.GHOST_FG, Colors.PRIMARY_FG, Color.lightGray, Colors.DISABLED_FG);

    private static final SolimButtonStyle DANGER_TEXT_STYLE = textOf(
            DANGER_STYLE, Colors.DANGER_FG, Colors.DANGER_FG, Color.lightGray, Colors.DISABLED_FG);

    public static SolimButtonStyle primary() {
        return PRIMARY_STYLE;
    }

    public static SolimButtonStyle primaryText() {
        return PRIMARY_TEXT_STYLE;
    }

    public static SolimButtonStyle secondary() {
        return SECONDARY_STYLE;
    }

    public static SolimButtonStyle secondaryText() {
        return SECONDARY_TEXT_STYLE;
    }

    public static SolimButtonStyle outline() {
        return OUTLINE_STYLE;
    }

    public static SolimButtonStyle outlineText() {
        return OUTLINE_TEXT_STYLE;
    }

    public static SolimButtonStyle ghost() {
        return GHOST_STYLE;
    }

    public static SolimButtonStyle ghostText() {
        return GHOST_TEXT_STYLE;
    }

    public static SolimButtonStyle danger() {
        return DANGER_STYLE;
    }

    public static SolimButtonStyle dangerText() {
        return DANGER_TEXT_STYLE;
    }

    private static SolimButtonStyle textOf(
            SolimButtonStyle base, Color fontColor, Color overFontColor, Color downFontColor, Color disabledFontColor) {
        ButtonStyle s = base.style();
        TextButtonStyle ts = new TextButtonStyle();
        ts.up = s.up;
        ts.over = s.over;
        ts.down = s.down;
        ts.disabled = s.disabled;
        ts.checked = s.checked;
        ts.font = Fonts.def;
        ts.fontColor = fontColor.cpy();
        ts.overFontColor = overFontColor.cpy();
        ts.downFontColor = downFontColor.cpy();
        ts.disabledFontColor = disabledFontColor.cpy();
        return new SolimButtonStyle(ts, base.padding(), base.margin(), base.gap(), Fonts.def);
    }

    private WebStyles() {
    }
}
