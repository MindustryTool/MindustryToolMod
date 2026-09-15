package solim.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.graphics.Color;
import arc.scene.ui.Button.ButtonStyle;
import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.graphics.RoundedDrawable;
import solim.input.Button;
import solim.runtime.SignalDispatcher;
import solim.signal.Signal;

class ReactiveStyleTest {

    private static int rgba(Color color) {
        return Color.rgba8888(color.r, color.g, color.b, color.a);
    }

    @BeforeAll
    static void initArc() {
        if (Core.app == null) {
            Core.app = new MockApplication();
        }
        if (Core.graphics == null) {
            Core.graphics = new MockGraphics();
        }
    }

    @BeforeEach
    void reset() {
        StyleCache.clear();
        SignalDispatcher.resetForTests();
    }

    @Test
    void colorSignalUpdatesDrawableWithoutRebuild() {
        Signal<Color> colorSignal = Signal.of(Color.red);
        Button button = new Button();
        button.style(s -> s.rounded(8).up(u -> u.background(colorSignal)));

        ButtonStyle initial = button.button().getStyle();
        assertNotNull(initial.up);
        RoundedDrawable up = (RoundedDrawable) initial.up;
        assertEquals(rgba(Color.red), rgba(up.getFillColor()));

        colorSignal.set(Color.green);
        SignalDispatcher.flush();

        ButtonStyle updated = button.button().getStyle();
        assertNotNull(updated.up);
        RoundedDrawable updatedUp = (RoundedDrawable) updated.up;
        assertEquals(rgba(Color.green), rgba(updatedUp.getFillColor()));

        button.dispose();
    }

    @Test
    void reactiveBindingsDisposedWithButton() {
        Signal<Color> colorSignal = Signal.of(Color.red);
        Button button = new Button();
        button.style(s -> s.rounded(8).up(u -> u.background(colorSignal)));

        colorSignal.set(Color.green);
        SignalDispatcher.flush();
        RoundedDrawable beforeDispose = (RoundedDrawable) button.button().getStyle().up;
        assertEquals(rgba(Color.green), rgba(beforeDispose.getFillColor()));

        button.dispose();

        colorSignal.set(Color.blue);
        SignalDispatcher.flush();

        RoundedDrawable afterDispose = (RoundedDrawable) button.button().getStyle().up;
        assertEquals(rgba(Color.green), rgba(afterDispose.getFillColor()));
    }

    @Test
    void reactiveLayoutUpdates() {
        Signal<Float> paddingSignal = Signal.of(4f);
        Button button = new Button();
        button.style(s -> s.rounded(8).padding(paddingSignal).up(u -> u.background(Color.blue)));

        SolimButtonStyleBuilder probe = new SolimButtonStyleBuilder()
                .rounded(8)
                .padding(paddingSignal)
                .up(u -> u.background(Color.blue));
        assertTrue(!probe.isStatic());

        paddingSignal.set(12f);
        SignalDispatcher.flush();

        button.dispose();
    }

    @Test
    void staticPaddingAndGapApplied() {
        Button button = new Button();
        button.style(s -> s.rounded(4).padding(8f).gap(4f).up(u -> u.background(Color.red)));

        ButtonStyle style = button.button().getStyle();
        assertNotNull(style.up);
        assertTrue(style.up instanceof RoundedDrawable);

        button.dispose();
    }

    @Test
    void scopedLambdaPreservesChain() {
        Button button = new Button();
        Button same = button.style(s -> s.rounded(4).up(u -> u.background(Color.red))).width(100f);
        assertTrue(same == button);
        assertEquals(100f, button.button().getWidth(), 0.001f);
        button.dispose();
    }
}
