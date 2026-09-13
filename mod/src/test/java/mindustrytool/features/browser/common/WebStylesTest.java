package mindustrytool.features.browser.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.TextureRegion;
import arc.mock.MockApplication;
import arc.mock.MockGL20;
import arc.mock.MockGraphics;
import arc.mock.MockSettings;
import arc.scene.Scene;
import arc.scene.ui.TextButton.TextButtonStyle;
import java.lang.reflect.Field;
import mindustry.ui.Fonts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.graphics.RoundedDrawable;
import solim.style.SolimButtonStyle;
import mindustrytool.components.WebStyles;

class WebStylesTest {

    private static Font testFont;

    private static int rgba(Color color) {
        return Color.rgba8888(color.r, color.g, color.b, color.a);
    }

    @BeforeAll
    static void setUp() {
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        Core.settings = new MockSettings();
        if (Core.gl == null) {
            Core.gl = new MockGL20();
            Core.gl20 = (MockGL20) Core.gl;
        }
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
        assertEquals(rgba(WebStyles.Colors.PRIMARY_BG), rgba(up.getFillColor()));
        assertEquals(rgba(WebStyles.Colors.PRIMARY_BG_HOVER), rgba(over.getFillColor()));
        assertEquals(rgba(WebStyles.Colors.PRIMARY_BG_DOWN), rgba(down.getFillColor()));
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
    void textCounterpartsContainFontAndDrawables() {
        SolimButtonStyle[] texts = new SolimButtonStyle[]{
            WebStyles.primaryText(),
            WebStyles.secondaryText(),
            WebStyles.outlineText(),
            WebStyles.ghostText(),
            WebStyles.dangerText()
        };
        for (SolimButtonStyle text : texts) {
            assertTrue(text.style() instanceof TextButtonStyle);
            TextButtonStyle ts = (TextButtonStyle) text.style();
            assertNotNull(ts.up);
            assertNotNull(ts.over);
            assertNotNull(ts.down);
            assertEquals(Fonts.def, ts.font);
            assertNotNull(ts.fontColor);
            assertNotNull(ts.overFontColor);
            assertNotNull(ts.downFontColor);
            assertNotNull(ts.disabledFontColor);
        }

        TextButtonStyle outlineText = (TextButtonStyle) WebStyles.outlineText().style();
        RoundedDrawable up = (RoundedDrawable) outlineText.up;
        assertEquals(rgba(WebStyles.Colors.PRIMARY_BG), rgba(up.getFillColor()));
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
