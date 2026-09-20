package solim.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.graphics.Color;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.TextField.TextFieldStyle;
import solim.input.SolimTextField;
import solim.reactive.Signal;
import solim.test.SolimEnv;

class InputStyleTest extends SolimEnv {

    @BeforeEach
    void setUp() {
        newScene();
    }

    private static int rgba(Color color) {
        return Color.rgba8888(color.r, color.g, color.b, color.a);
    }

    private static TextureRegionDrawable stubDrawable() {
        return new TextureRegionDrawable(new TextureRegion());
    }

    private static TextFieldStyle fullBaseStyle() {
        TextFieldStyle base = new TextFieldStyle();
        base.background = stubDrawable();
        base.focusedBackground = stubDrawable();
        base.disabledBackground = stubDrawable();
        base.invalidBackground = stubDrawable();
        base.cursor = stubDrawable();
        base.selection = stubDrawable();
        base.font = new Font(new Font.FontData(), new TextureRegion(), false);
        base.fontColor = new Color(1f, 1f, 1f, 1f);
        base.focusedFontColor = new Color(1f, 1f, 1f, 1f);
        base.disabledFontColor = new Color(0.5f, 0.5f, 0.5f, 1f);
        base.messageFont = new Font(new Font.FontData(), new TextureRegion(), false);
        base.messageFontColor = new Color(0.5f, 0.5f, 0.5f, 1f);
        return base;
    }

    @Test
    void partialPresetInheritsRest() {
        TextFieldStyle base = fullBaseStyle();
        TextureRegionDrawable blank = stubDrawable();
        InputStyle preset = InputStyle.builder()
                .background(blank)
                .focusedBackground(blank)
                .disabledBackground(blank)
                .invalidBackground(blank)
                .build();

        TextFieldStyle copy = preset.appliedTo(base);

        assertSame(blank, copy.background);
        assertSame(blank, copy.focusedBackground);
        assertSame(blank, copy.disabledBackground);
        assertSame(blank, copy.invalidBackground);
        assertSame(base.font, copy.font);
        assertEquals(rgba(base.fontColor), rgba(copy.fontColor));
        assertEquals(rgba(base.focusedFontColor), rgba(copy.focusedFontColor));
        assertEquals(rgba(base.disabledFontColor), rgba(copy.disabledFontColor));
        assertSame(base.messageFont, copy.messageFont);
        assertEquals(rgba(base.messageFontColor), rgba(copy.messageFontColor));
        assertSame(base.cursor, copy.cursor);
        assertSame(base.selection, copy.selection);
    }

    @Test
    void emptyPresetFallsBackEntirely() {
        TextFieldStyle base = fullBaseStyle();
        TextFieldStyle copy = InputStyle.builder().build().appliedTo(base);

        assertSame(base.background, copy.background);
        assertSame(base.focusedBackground, copy.focusedBackground);
        assertSame(base.disabledBackground, copy.disabledBackground);
        assertSame(base.invalidBackground, copy.invalidBackground);
        assertSame(base.cursor, copy.cursor);
        assertSame(base.selection, copy.selection);
        assertSame(base.font, copy.font);
        assertEquals(rgba(base.fontColor), rgba(copy.fontColor));
        assertEquals(rgba(base.focusedFontColor), rgba(copy.focusedFontColor));
        assertEquals(rgba(base.disabledFontColor), rgba(copy.disabledFontColor));
        assertSame(base.messageFont, copy.messageFont);
        assertEquals(rgba(base.messageFontColor), rgba(copy.messageFontColor));
    }

    @Test
    void stylingOneFieldLeavesSiblingUntouched() {
        SolimTextField first = new SolimTextField(Signal.of(""));
        SolimTextField second = new SolimTextField(Signal.of(""));
        try {
            TextFieldStyle base = first.field().getStyle();
            assertSame(base, second.field().getStyle());

            Drawable baseBackground = base.background;
            TextureRegionDrawable blank = stubDrawable();
            InputStyle preset = InputStyle.builder()
                    .background(blank)
                    .focusedBackground(blank)
                    .disabledBackground(blank)
                    .invalidBackground(blank)
                    .build();

            first.style(preset);

            assertNotSame(base, first.field().getStyle());
            assertSame(blank, first.field().getStyle().background);
            assertSame(blank, first.field().getStyle().focusedBackground);
            assertSame(blank, first.field().getStyle().disabledBackground);
            assertSame(blank, first.field().getStyle().invalidBackground);
            assertSame(base, second.field().getStyle());
            assertSame(baseBackground, base.background);
        } finally {
            first.dispose();
            second.dispose();
        }
    }

    @Test
    void presetSharedAcrossFields() {
        SolimTextField first = new SolimTextField(Signal.of(""));
        SolimTextField second = new SolimTextField(Signal.of(""));
        try {
            TextureRegionDrawable blank = stubDrawable();
            InputStyle preset = InputStyle.builder()
                    .background(blank)
                    .focusedBackground(blank)
                    .disabledBackground(blank)
                    .invalidBackground(blank)
                    .build();

            first.style(preset);
            second.style(preset);

            assertSame(blank, first.field().getStyle().background);
            assertSame(blank, second.field().getStyle().background);
            assertNotSame(first.field().getStyle(), second.field().getStyle());
        } finally {
            first.dispose();
            second.dispose();
        }
    }
}
