package mindustrytool.features.browser.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Scene;
import mindustry.ui.Fonts;
import mindustrytool.components.WebStyles;
import mindustrytool.test.MindustryTestEnv;
import solim.graphics.RoundedDrawable;
import solim.style.SolimButtonStyle;

class WebStylesTest extends MindustryTestEnv {

    private static Font testFont;

    private static int rgba(Color color) {
        return Color.rgba8888(color.r, color.g, color.b, color.a);
    }

    @BeforeAll
    static void setUp() {
        if (Core.scene == null) {
            Core.scene = new Scene();
        }
        if (testFont == null) {
            Font.FontData fontData = new Font.FontData() {
                @Override
                public boolean hasGlyph(char ch) {
                    return true;
                }

                @Override
                public Font.Glyph getGlyph(char ch) {
                    Font.Glyph g = super.getGlyph(ch);
                    if (g == null) {
                        g = new Font.Glyph();
                        g.id = ch;
                        g.width = 8;
                        g.height = 12;
                        g.xadvance = 8;
                        setGlyph(ch, g);
                    }
                    return g;
                }
            };
            fontData.lineHeight = 18f;
            fontData.capHeight = 14f;
            fontData.ascent = 14f;
            fontData.descent = -4f;
            fontData.down = -18f;
            testFont = new Font(fontData, new TextureRegion(), false);
        }
        Fonts.def = testFont;
    }

    @Test
    void variantsReturnCachedSingletonsWithPadding() {
        assertSame(WebStyles.primary(), WebStyles.primary());
        assertSame(WebStyles.secondary(), WebStyles.secondary());
        assertSame(WebStyles.outline(), WebStyles.outline());
        assertSame(WebStyles.ghost(), WebStyles.ghost());
        assertSame(WebStyles.danger(), WebStyles.danger());

        assertSame(WebStyles.primaryText(), WebStyles.primaryText());
        assertSame(WebStyles.secondaryText(), WebStyles.secondaryText());
        assertSame(WebStyles.outlineText(), WebStyles.outlineText());
        assertSame(WebStyles.ghostText(), WebStyles.ghostText());
        assertSame(WebStyles.dangerText(), WebStyles.dangerText());

        float expectedPadding = 8f;
        assertEquals(expectedPadding, WebStyles.primary().padding().floatValue(), 0.001f);
        assertEquals(expectedPadding, WebStyles.secondary().padding().floatValue(), 0.001f);
        assertEquals(expectedPadding, WebStyles.outline().padding().floatValue(), 0.001f);
        assertEquals(expectedPadding, WebStyles.ghost().padding().floatValue(), 0.001f);
        assertEquals(expectedPadding, WebStyles.danger().padding().floatValue(), 0.001f);
        assertEquals(expectedPadding, WebStyles.primaryText().padding().floatValue(), 0.001f);
        assertEquals(expectedPadding, WebStyles.secondaryText().padding().floatValue(), 0.001f);
        assertEquals(expectedPadding, WebStyles.outlineText().padding().floatValue(), 0.001f);
        assertEquals(expectedPadding, WebStyles.ghostText().padding().floatValue(), 0.001f);
        assertEquals(expectedPadding, WebStyles.dangerText().padding().floatValue(), 0.001f);
    }

    @Test
    void primaryVariantColors() {
        SolimButtonStyle style = WebStyles.primary();
        RoundedDrawable up = (RoundedDrawable) style.style().up;
        RoundedDrawable over = (RoundedDrawable) style.style().over;
        RoundedDrawable down = (RoundedDrawable) style.style().down;
        assertEquals(rgba(WebStyles.Colors.PRIMARY), rgba(up.getFillColor()));
        assertEquals(rgba(WebStyles.Colors.PRIMARY_HOVER), rgba(over.getFillColor()));
        assertEquals(rgba(WebStyles.Colors.PRIMARY_DOWN), rgba(down.getFillColor()));
        assertEquals(8, up.getRadius());
    }

    @Test
    void secondaryVariantColors() {
        SolimButtonStyle style = WebStyles.secondary();
        RoundedDrawable up = (RoundedDrawable) style.style().up;
        RoundedDrawable over = (RoundedDrawable) style.style().over;
        RoundedDrawable down = (RoundedDrawable) style.style().down;
        assertEquals(rgba(WebStyles.Colors.SECONDARY), rgba(up.getFillColor()));
        assertEquals(rgba(WebStyles.Colors.SECONDARY_HOVER), rgba(over.getFillColor()));
        assertEquals(rgba(WebStyles.Colors.SECONDARY_DOWN), rgba(down.getFillColor()));
    }

    @Test
    void outlineVariantUsesPrimaryWash() {
        SolimButtonStyle style = WebStyles.outline();
        RoundedDrawable up = (RoundedDrawable) style.style().up;
        RoundedDrawable over = (RoundedDrawable) style.style().over;
        RoundedDrawable down = (RoundedDrawable) style.style().down;
        assertEquals(rgba(Color.clear), rgba(up.getFillColor()));
        assertEquals(rgba(WebStyles.Colors.PRIMARY_BG_HOVER), rgba(over.getFillColor()));
        assertEquals(rgba(WebStyles.Colors.PRIMARY_BG), rgba(down.getFillColor()));
        assertEquals(1.5f, up.getStroke(), 0.001f);
        assertEquals(rgba(WebStyles.Colors.BORDER_INPUT), rgba(up.getBorderColor()));
        assertEquals(rgba(WebStyles.Colors.BORDER_INPUT), rgba(over.getBorderColor()));
        assertEquals(rgba(WebStyles.Colors.BORDER_INPUT), rgba(down.getBorderColor()));
        assertEquals(8, up.getRadius());
    }

    @Test
    void ghostVariantColors() {
        SolimButtonStyle style = WebStyles.ghost();
        RoundedDrawable over = (RoundedDrawable) style.style().over;
        RoundedDrawable down = (RoundedDrawable) style.style().down;
        assertEquals(rgba(WebStyles.Colors.GHOST_HOVER), rgba(over.getFillColor()));
        assertEquals(rgba(WebStyles.Colors.GHOST_DOWN), rgba(down.getFillColor()));
    }

    @Test
    void dangerVariantColors() {
        SolimButtonStyle style = WebStyles.danger();
        RoundedDrawable up = (RoundedDrawable) style.style().up;
        RoundedDrawable over = (RoundedDrawable) style.style().over;
        RoundedDrawable down = (RoundedDrawable) style.style().down;
        assertEquals(rgba(WebStyles.Colors.DANGER), rgba(up.getFillColor()));
        assertEquals(rgba(WebStyles.Colors.DANGER_HOVER), rgba(over.getFillColor()));
        assertEquals(rgba(WebStyles.Colors.DANGER_DOWN), rgba(down.getFillColor()));
    }

    @Test
    void actionChipHasNoCheckedStateAndMatchesFilterChipAppearance() {
        assertSame(WebStyles.actionChip(), WebStyles.actionChip());
        SolimButtonStyle action = WebStyles.actionChip();
        SolimButtonStyle filter = WebStyles.filterChip();

        // Arc's Button renders style.checked whenever isChecked is true, and Arc toggles
        // isChecked on every click. A null checked drawable makes that toggle inert so an
        // action chip can never latch into a selected appearance.
        assertNull(action.style().checked);

        // Everything Arc can actually draw must match filterChip so action chips are
        // visually indistinguishable from toggle chips apart from the selected state.
        RoundedDrawable actionUp = (RoundedDrawable) action.style().up;
        RoundedDrawable filterUp = (RoundedDrawable) filter.style().up;
        assertEquals(rgba(filterUp.getFillColor()), rgba(actionUp.getFillColor()));
        assertEquals(rgba(filterUp.getBorderColor()), rgba(actionUp.getBorderColor()));
        assertEquals(filterUp.getStroke(), actionUp.getStroke(), 0.001f);
        assertEquals(filterUp.getRadius(), actionUp.getRadius());

        RoundedDrawable actionOver = (RoundedDrawable) action.style().over;
        RoundedDrawable filterOver = (RoundedDrawable) filter.style().over;
        assertEquals(rgba(filterOver.getFillColor()), rgba(actionOver.getFillColor()));
        assertEquals(rgba(filterOver.getBorderColor()), rgba(actionOver.getBorderColor()));

        RoundedDrawable actionDown = (RoundedDrawable) action.style().down;
        RoundedDrawable filterDown = (RoundedDrawable) filter.style().down;
        assertEquals(rgba(filterDown.getFillColor()), rgba(actionDown.getFillColor()));
        assertEquals(rgba(filterDown.getBorderColor()), rgba(actionDown.getBorderColor()));

        // The toggle counterpart must still be able to show a selected state.
        assertNotNull(filter.style().checked);
    }

    @Test
    void actionCardTextHasNoCheckedStateButMatchesCardActionTextAppearance() {
        assertSame(WebStyles.actionCardText(), WebStyles.actionCardText());
        SolimButtonStyle action = WebStyles.actionCardText();
        SolimButtonStyle card = WebStyles.cardActionText();

        assertNull(action.style().checked);

        RoundedDrawable actionUp = (RoundedDrawable) action.style().up;
        RoundedDrawable cardUp = (RoundedDrawable) card.style().up;
        assertEquals(rgba(cardUp.getFillColor()), rgba(actionUp.getFillColor()));
        assertEquals(rgba(cardUp.getBorderColor()), rgba(actionUp.getBorderColor()));
        assertEquals(cardUp.getStroke(), actionUp.getStroke(), 0.001f);
        assertEquals(cardUp.getRadius(), actionUp.getRadius());

        RoundedDrawable actionOver = (RoundedDrawable) action.style().over;
        RoundedDrawable cardOver = (RoundedDrawable) card.style().over;
        assertEquals(rgba(cardOver.getFillColor()), rgba(actionOver.getFillColor()));

        RoundedDrawable actionDown = (RoundedDrawable) action.style().down;
        RoundedDrawable cardDown = (RoundedDrawable) card.style().down;
        assertEquals(rgba(cardDown.getFillColor()), rgba(actionDown.getFillColor()));

        // cardAction is a real toggle style (GodMode player selection) and must keep one.
        assertNotNull(card.style().checked);
    }

    @Test
    void cardActionCheckedStateIsVisibleRatherThanTransparent() {
        RoundedDrawable checked = (RoundedDrawable) WebStyles.cardAction().style().checked;
        assertNotNull(checked, "cardAction is used as a toggle and must declare a checked visual");
        // A transparent checked fill made the GodMode player-selection toggle look
        // unselected; the selected state must actually differ from the up state's fill.
        RoundedDrawable up = (RoundedDrawable) WebStyles.cardAction().style().up;
        assertTrue(checked.getFillColor().a > 0.001f, "checked fill must be visible, not transparent");
        assertTrue(rgba(checked.getFillColor()) != rgba(up.getFillColor()),
                "checked fill must be distinguishable from the up fill");
    }

    @Test
    void nonToggleStylesResolveToNoCheckedState() {
        // These styles declare no checked state, so Arc's per-click auto-toggle must not
        // be able to repaint them. A phantom (transparent) checked drawable additionally
        // suppresses the hover highlight, because Arc checks `checked` before `over`.
        assertNull(WebStyles.outline().style().checked);
        assertNull(WebStyles.sectionPanel().style().checked);
        assertNull(WebStyles.clearFilters().style().checked);
        assertNull(WebStyles.previewCard().style().checked);
        assertNull(WebStyles.primary().style().checked);
        assertNull(WebStyles.secondary().style().checked);
        assertNull(WebStyles.ghost().style().checked);
        assertNull(WebStyles.danger().style().checked);
    }

    @Test
    void toggleStylesKeepAnExplicitCheckedState() {
        assertNotNull(WebStyles.filterChip().style().checked);
        assertNotNull(WebStyles.filterChipText().style().checked);
        assertNotNull(WebStyles.cardAction().style().checked);
        assertNotNull(WebStyles.cardActionText().style().checked);
        assertNotNull(WebStyles.tab().style().checked);
    }

    @Test
    void clearInputReturnsCachedSingleton() {
        assertSame(WebStyles.clearInput(), WebStyles.clearInput());
        // Slot blankness is covered in solim-core InputStyleTest with stub
        // drawables: Tex.clear is null until the game loads assets headless.
    }

    @Test
    void noLegacyAliasesExist() {
        for (Field field : WebStyles.class.getDeclaredFields()) {
            String name = field.getName();
            assertTrue(!name.equals("webButton"), "Legacy webButton should not exist");
            assertTrue(!name.equals("webTextButton"), "Legacy webTextButton should not exist");
            assertTrue(!name.startsWith("CHANNEL_BLUE"), "Legacy CHANNEL_BLUE should not exist");
        }
    }
}
