package solim.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.graphics.Color;
import arc.scene.ui.Button.ButtonStyle;
import solim.graphics.RoundedDrawable;
import solim.test.SolimEnv;

class SolimButtonStyleBuilderTest extends SolimEnv {

    private static int rgba(Color color) {
        return Color.rgba8888(color.r, color.g, color.b, color.a);
    }


    @BeforeEach
    void clearCache() {
        StyleCache.clear();
    }

    @Test
    void rootInheritanceAndStateOverride() {
        SolimButtonStyleBuilder builder = new SolimButtonStyleBuilder()
                .rounded(8)
                .border(1.5f, Color.white)
                .up(u -> u.background(Color.blue));

        SolimButtonStyle resolved = builder.build();
        ButtonStyle style = resolved.style();

        assertNotNull(style.up);
        assertTrue(style.up instanceof RoundedDrawable);
        RoundedDrawable up = (RoundedDrawable) style.up;
        assertEquals(8, up.getRadius());
        assertEquals(1.5f, up.getStroke(), 0.001f);
        assertEquals(rgba(Color.blue), rgba(up.getFillColor()));
        assertEquals(rgba(Color.white), rgba(up.getBorderColor()));

        assertNotNull(style.over);
        assertNotNull(style.down);
        RoundedDrawable over = (RoundedDrawable) style.over;
        RoundedDrawable down = (RoundedDrawable) style.down;
        assertEquals(8, over.getRadius());
        assertEquals(1.5f, over.getStroke(), 0.001f);
        assertEquals(8, down.getRadius());
        assertEquals(1.5f, down.getStroke(), 0.001f);
    }

    @Test
    void automaticHoverAndPressedTints() {
        Color base = new Color(0.45f, 0.35f, 0.9f, 0.8f);
        SolimButtonStyleBuilder builder = new SolimButtonStyleBuilder()
                .rounded(8)
                .up(u -> u.background(base));

        ButtonStyle style = builder.build().style();
        RoundedDrawable up = (RoundedDrawable) style.up;
        RoundedDrawable over = (RoundedDrawable) style.over;
        RoundedDrawable down = (RoundedDrawable) style.down;

        assertNotNull(over);
        assertNotNull(down);
        Color expectedOver = base.cpy().mul(1.15f);
        Color expectedDown = base.cpy().mul(0.85f);
        assertEquals(rgba(expectedOver), rgba(over.getFillColor()));
        assertEquals(rgba(expectedDown), rgba(down.getFillColor()));
        assertTrue(rgba(up.getFillColor()) != rgba(over.getFillColor()));
    }

    @Test
    void stateOverridesRoot() {
        SolimButtonStyleBuilder builder = new SolimButtonStyleBuilder()
                .rounded(8)
                .border(1.5f, Color.white)
                .up(u -> u.background(Color.blue).rounded(4));

        ButtonStyle style = builder.build().style();
        RoundedDrawable up = (RoundedDrawable) style.up;
        RoundedDrawable over = (RoundedDrawable) style.over;

        assertEquals(4, up.getRadius());
        assertEquals(8, over.getRadius());
    }

    @Test
    void fromButtonStyleComposition() {
        SolimButtonStyleBuilder base = new SolimButtonStyleBuilder()
                .rounded(8)
                .border(1.5f, Color.white)
                .up(u -> u.background(Color.blue));
        ButtonStyle baseStyle = base.build().style();

        SolimButtonStyleBuilder derived = new SolimButtonStyleBuilder()
                .from(baseStyle)
                .border(Color.scarlet);

        ButtonStyle style = derived.build().style();
        RoundedDrawable up = (RoundedDrawable) style.up;
        RoundedDrawable over = (RoundedDrawable) style.over;
        RoundedDrawable down = (RoundedDrawable) style.down;

        assertEquals(rgba(Color.scarlet), rgba(up.getBorderColor()));
        assertEquals(rgba(Color.scarlet), rgba(over.getBorderColor()));
        assertEquals(rgba(Color.scarlet), rgba(down.getBorderColor()));
        assertEquals(rgba(Color.blue), rgba(up.getFillColor()));
    }

    @Test
    void fromBuilderComposition() {
        SolimButtonStyleBuilder base = new SolimButtonStyleBuilder()
                .rounded(8)
                .border(1.5f, Color.white)
                .padding(6f)
                .up(u -> u.background(Color.blue));

        SolimButtonStyleBuilder derived = new SolimButtonStyleBuilder()
                .from(base)
                .gap(4f);

        SolimButtonStyle resolved = derived.build();
        assertEquals(6f, resolved.padding().floatValue(), 0.001f);
        assertEquals(4f, resolved.gap().floatValue(), 0.001f);
        RoundedDrawable up = (RoundedDrawable) resolved.style().up;
        assertEquals(rgba(Color.blue), rgba(up.getFillColor()));
    }

    @Test
    void layoutPropertiesStored() {
        SolimButtonStyle resolved = new SolimButtonStyleBuilder()
                .rounded(4)
                .padding(8f)
                .margin(6f)
                .gap(4f)
                .up(u -> u.background(Color.red))
                .build();

        assertEquals(8f, resolved.padding().floatValue(), 0.001f);
        assertEquals(6f, resolved.margin().floatValue(), 0.001f);
        assertEquals(4f, resolved.gap().floatValue(), 0.001f);
    }

    @Test
    void disabledStateExplicit() {
        Color disabledBg = new Color(0.1f, 0.1f, 0.1f, 0.3f);
        ButtonStyle style = new SolimButtonStyleBuilder()
                .rounded(8)
                .up(u -> u.background(Color.blue))
                .disabled(d -> d.background(disabledBg))
                .build().style();

        assertNotNull(style.disabled);
        RoundedDrawable disabled = (RoundedDrawable) style.disabled;
        assertEquals(rgba(disabledBg), rgba(disabled.getFillColor()));
    }

    @Test
    void noDisabledLeavesNull() {
        ButtonStyle style = new SolimButtonStyleBuilder()
                .rounded(8)
                .up(u -> u.background(Color.blue))
                .build().style();

        assertNull(style.disabled);
        assertNull(style.checked);
    }
}
