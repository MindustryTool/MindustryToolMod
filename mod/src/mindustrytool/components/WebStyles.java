package mindustrytool.components;

import static solim.UI.*;

import arc.graphics.Color;
import arc.scene.style.Drawable;
import arc.scene.ui.Button.ButtonStyle;
import arc.util.Nullable;
import arc.scene.ui.TextButton.TextButtonStyle;
import mindustry.gen.Tex;
import mindustry.ui.Fonts;
import solim.style.InputStyle;
import solim.style.SolimButtonStyle;
import solim.style.SolimButtonStyleBuilder;

/**
 * Design system hub for browser UI with shadcn/ui-inspired semantic colors
 * and shared button variants with built-in padding.
 * Does not mutate global Arc styles.
 */
public class WebStyles {

    /**
     * Semantic color tokens for browser UI, derived from the shadcn/ui dark
     * theme (oklch converted to sRGB).
     */
    public static final class Colors {
        public static final Color PRIMARY = new Color(0.420f, 0.310f, 0.890f, 1.0f);
        public static final Color PRIMARY_HOVER = new Color(0.490f, 0.390f, 0.960f, 1.0f);
        public static final Color PRIMARY_DOWN = new Color(0.350f, 0.250f, 0.780f, 1.0f);
        public static final Color PRIMARY_FG = new Color(0.980f, 0.980f, 1.000f, 1.0f);
        public static final Color PRIMARY_BG = new Color(0.420f, 0.310f, 0.890f, 0.10f);
        public static final Color PRIMARY_BG_HOVER = new Color(0.420f, 0.310f, 0.890f, 0.16f);
        public static final Color PRIMARY_BG_DOWN = new Color(0.420f, 0.310f, 0.890f, 0.24f);

        public static final Color SECONDARY = new Color(0.145f, 0.145f, 0.165f, 1.0f);
        public static final Color SECONDARY_HOVER = new Color(0.190f, 0.190f, 0.215f, 1.0f);
        public static final Color SECONDARY_DOWN = new Color(0.105f, 0.105f, 0.120f, 1.0f);
        public static final Color SECONDARY_FG = new Color(0.925f, 0.925f, 0.945f, 1.0f);

        public static final Color GHOST_HOVER = new Color(1.0f, 1.0f, 1.0f, 0.08f);
        public static final Color GHOST_DOWN = new Color(1.0f, 1.0f, 1.0f, 0.14f);
        public static final Color GHOST_FG = new Color(0.630f, 0.630f, 0.660f, 1.0f);

        public static final Color DANGER = new Color(0.900f, 0.250f, 0.270f, 1.0f);
        public static final Color DANGER_HOVER = new Color(0.960f, 0.310f, 0.330f, 1.0f);
        public static final Color DANGER_DOWN = new Color(0.740f, 0.180f, 0.200f, 1.0f);
        public static final Color DANGER_FG = new Color(1.0f, 1.0f, 1.0f, 1.0f);

        public static final Color BORDER = new Color(1.0f, 1.0f, 1.0f, 0.10f);
        public static final Color BORDER_INPUT = new Color(1.0f, 1.0f, 1.0f, 0.15f);

        public static final Color DISABLED_BG = new Color(0.145f, 0.145f, 0.165f, 0.45f);
        public static final Color DISABLED_BORDER = new Color(0.300f, 0.300f, 0.320f, 0.40f);
        public static final Color DISABLED_FG = new Color(0.400f, 0.400f, 0.420f, 1.0f);

        public static final Color SECTION_BG = new Color(0.075f, 0.075f, 0.085f, 1.0f);
        public static final Color SECTION_BORDER = new Color(0.180f, 0.180f, 0.200f, 1.0f);

        public static final Color CHIP_BG = new Color(0.115f, 0.115f, 0.130f, 1.0f);
        public static final Color CHIP_BORDER = new Color(0.190f, 0.190f, 0.210f, 1.0f);
        public static final Color CHIP_HOVER_BG = new Color(0.170f, 0.170f, 0.190f, 1.0f);
        public static final Color CHIP_DOWN_BG = new Color(0.220f, 0.220f, 0.245f, 1.0f);
        public static final Color CHIP_FG = new Color(0.630f, 0.630f, 0.660f, 1.0f);
        public static final Color CHIP_CHECKED_BG = new Color(0.260f, 0.220f, 0.420f, 1.0f);
        public static final Color CHIP_CHECKED_BORDER = new Color(0.520f, 0.440f, 0.760f, 1.0f);
        public static final Color CHIP_CHECKED_FG = new Color(0.940f, 0.920f, 1.0f, 1.0f);

        public static final Color CLEAR_BG = new Color(0.115f, 0.115f, 0.130f, 1.0f);
        public static final Color CLEAR_BORDER = new Color(0.190f, 0.190f, 0.210f, 1.0f);
        public static final Color CLEAR_FG = new Color(0.680f, 0.680f, 0.710f, 1.0f);
        public static final Color CLEAR_HOVER_BG = new Color(0.170f, 0.170f, 0.190f, 1.0f);
        public static final Color CLEAR_DOWN_BG = new Color(0.090f, 0.090f, 0.105f, 1.0f);

        public static final Color SECONDARY_BG = new Color(0.100f, 0.100f, 0.115f, 1.0f);

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
            .border(1.5f, Colors.BORDER_INPUT)
            .padding(unit(2))
            .up(u -> u.background(Color.clear))
            .over(o -> o.background(Colors.PRIMARY_BG_HOVER))
            .down(d -> d.background(Colors.PRIMARY_BG))
            .disabled(dis -> dis.background(Colors.DISABLED_BG).border(1.0f, Colors.DISABLED_BORDER))
            .build();

    private static final SolimButtonStyle GHOST_STYLE = new SolimButtonStyleBuilder()
            .rounded(unit(2))
            .padding(unit(2))
            .up(u -> u.background(Color.clear))
            .over(o -> o.background(Colors.GHOST_HOVER))
            .down(d -> d.background(Colors.GHOST_DOWN))
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


    private static final SolimButtonStyle CARD_ACTION_STYLE = new SolimButtonStyleBuilder()
            .rounded(unit(2))
            .border(1.0f, Colors.BORDER_INPUT)
            .padding(unit(1.5f))
            .up(u -> u.background(Colors.SECONDARY))
            .over(o -> o.background(Colors.SECONDARY_HOVER))
            .down(d -> d.background(Colors.SECONDARY_DOWN))
            .disabled(dis -> dis.background(Colors.DISABLED_BG).border(1.0f, Colors.DISABLED_BORDER))
            .build();

    private static final SolimButtonStyle CARD_ACTION_TEXT_STYLE = textOf(
            CARD_ACTION_STYLE, Colors.SECONDARY_FG, Colors.PRIMARY_FG, Color.lightGray, Colors.DISABLED_FG);

    private static final SolimButtonStyle PREVIEW_CARD_STYLE = new SolimButtonStyleBuilder()
            .rounded(unit(2))
            .border(1.0f, Colors.BORDER_INPUT)
            .up(u -> u.background(Colors.SECONDARY))
            .over(o -> o.background(Colors.SECONDARY_HOVER).border(1.0f, Colors.BORDER_INPUT))
            .down(d -> d.background(Colors.SECONDARY_DOWN))
            .build();

    private static final SolimButtonStyle SECTION_PANEL_STYLE = new SolimButtonStyleBuilder()
            .rounded(unit(2))
            .border(1.0f, Colors.SECTION_BORDER)
            .padding(unit(1))
            .up(u -> u.background(Colors.SECTION_BG))
            .over(o -> o.background(Colors.SECTION_BG))
            .down(d -> d.background(Colors.SECTION_BG))
            .build();

    private static final SolimButtonStyle FILTER_CHIP_STYLE = new SolimButtonStyleBuilder()
            .rounded(unit(2))
            .border(1.5f, Colors.CHIP_BORDER)
            .padding(unit(1))
            .up(u -> u.background(Color.clear))
            .over(o -> o.background(Colors.CHIP_HOVER_BG))
            .down(d -> d.background(Colors.CHIP_DOWN_BG))
            .checked(c -> c.background(Colors.CHIP_CHECKED_BG).border(1.5f, Colors.CHIP_CHECKED_BORDER))
            .build();

    private static final SolimButtonStyle CLEAR_FILTERS_STYLE = new SolimButtonStyleBuilder()
            .rounded(unit(2))
            .border(1.0f, Colors.CLEAR_BORDER)
            .padding(unit(1.5f))
            .up(u -> u.background(Colors.CLEAR_BG))
            .over(o -> o.background(Colors.CLEAR_HOVER_BG))
            .down(d -> d.background(Colors.CLEAR_DOWN_BG))
            .build();

    private static final SolimButtonStyle FILTER_CHIP_TEXT_STYLE = textOf(
            FILTER_CHIP_STYLE, Colors.CHIP_FG, Colors.CHIP_CHECKED_FG, Color.lightGray, Colors.DISABLED_FG);

    private static final SolimButtonStyle CLEAR_FILTERS_TEXT_STYLE = textOf(
            CLEAR_FILTERS_STYLE, Colors.CLEAR_FG, Colors.CHIP_CHECKED_FG, Color.lightGray, Colors.DISABLED_FG);

    private static final InputStyle CLEAR_INPUT = InputStyle.builder()
            .background(Tex.clear)
            .focusedBackground(Tex.clear)
            .disabledBackground(Tex.clear)
            .invalidBackground(Tex.clear)
            .build();

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

    public static SolimButtonStyle cardAction() {
        return CARD_ACTION_STYLE;
    }

    public static SolimButtonStyle cardActionText() {
        return CARD_ACTION_TEXT_STYLE;
    }

    public static SolimButtonStyle previewCard() {
        return PREVIEW_CARD_STYLE;
    }

    public static @Nullable Drawable previewCardBackground() {
        return PREVIEW_CARD_STYLE.style() != null ? PREVIEW_CARD_STYLE.style().up : null;
    }

    public static InputStyle clearInput() {
        return CLEAR_INPUT;
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

    public static SolimButtonStyle sectionPanel() {
        return SECTION_PANEL_STYLE;
    }

    public static SolimButtonStyle filterChip() {
        return FILTER_CHIP_STYLE;
    }

    public static SolimButtonStyle filterChipText() {
        return FILTER_CHIP_TEXT_STYLE;
    }

    public static SolimButtonStyle clearFilters() {
        return CLEAR_FILTERS_STYLE;
    }

    public static SolimButtonStyle clearFiltersText() {
        return CLEAR_FILTERS_TEXT_STYLE;
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
