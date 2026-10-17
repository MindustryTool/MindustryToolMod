package solim.input;

import static org.junit.jupiter.api.Assertions.*;
import static solim.modifier.Values.height;
import static solim.modifier.Values.position;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.width;
import static solim.modifier.Values.x;
import static solim.modifier.Values.y;

import arc.Core;
import arc.scene.Element;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.runtime.AttachmentStack;
import solim.test.SolimEnv;

class ButtonComponentTest extends SolimEnv {

    @BeforeAll
    static void checkArcContext() {
        Assumptions.assumeTrue(Core.scene != null, "Arc Core.scene is null; skipping skin-dependent tests");
    }

    @Test
    void createsWithDefaultName() {
        Button btn = new Button();
        assertEquals("solim-button-sizedButton", btn.button().name);
    }

    @Test
    void onClickExecutesCallback() {
        boolean[] clicked = { false };
        Button btn = new Button(() -> clicked[0] = true);

        InputEvent event = new InputEvent();
        btn.button().getListeners().forEach(listener -> {
            if (listener instanceof ClickListener) {
                ((ClickListener) listener).clicked(event, 0f, 0f);
            }
        });

        assertTrue(clicked[0]);
        btn.dispose();
    }

    @Test
    void onClickStopsEventPropagation() {
        Button btn = new Button(() -> {
        });
        InputEvent event = new InputEvent();

        btn.button().getListeners().forEach(listener -> {
            if (listener instanceof ClickListener) {
                ((ClickListener) listener).clicked(event, 0f, 0f);
            }
        });

        assertTrue(event.stopped);
        btn.dispose();
    }

    @Test
    void reactiveEnabledTogglesDisabledState() {
        Signal<Boolean> enabled = Signal.of(true);
        Button btn = new Button().enabled(enabled);

        assertFalse(btn.button().isDisabled());

        enabled.set(false);
        assertTrue(btn.button().isDisabled());

        enabled.set(true);
        assertFalse(btn.button().isDisabled());
        btn.dispose();
    }

    @Test
    void reactiveVisibleUpdatesVisibility() {
        Signal<Boolean> visible = Signal.of(true);
        Button btn = new Button().visible(visible);

        assertTrue(btn.button().visible);

        visible.set(false);
        assertFalse(btn.button().visible);

        visible.set(true);
        assertTrue(btn.button().visible);
        btn.dispose();
    }

    @Test
    void reactiveCheckedUpdatesCheckedState() {
        Signal<Boolean> checked = Signal.of(false);
        Button btn = new Button().checked(checked);

        assertFalse(btn.button().isChecked());

        checked.set(true);
        assertTrue(btn.button().isChecked());

        checked.set(false);
        assertFalse(btn.button().isChecked());
        btn.dispose();
    }

    private static void fireClick(Button btn) {
        InputEvent event = new InputEvent();
        btn.button().getListeners().forEach(listener -> {
            if (listener instanceof ClickListener) {
                ((ClickListener) listener).clicked(event, 0f, 0f);
            }
        });
    }

    @Test
    void checkedBindingWinsOverArcClickAutoToggle() {

        Signal<Boolean> checked = Signal.of(true);
        Button btn = new Button().checked(checked);

        assertTrue(btn.button().isChecked());

        fireClick(btn);
        assertTrue(btn.button().isChecked(), "reactive binding must win over Arc's auto-toggle");

        checked.set(false);
        assertFalse(btn.button().isChecked());

        fireClick(btn);
        assertFalse(btn.button().isChecked(), "reactive binding must win over Arc's auto-toggle");
        btn.dispose();
    }

    @Test
    void constantCheckedReadableIsNotReassertedOnClick() {

        Button btn = new Button().checked(Readable.of(false));

        assertFalse(btn.button().isChecked());

        fireClick(btn);
        assertTrue(btn.button().isChecked(), "constant readables must not be pinned by the binding");
        btn.dispose();
    }

    @Test
    void checkedBindingTracksSignalAcrossClicks() {

        Signal<Boolean> checked = Signal.of(false);
        Button btn = new Button(() -> checked.set(!checked.peek())).checked(checked);

        fireClick(btn);
        assertTrue(btn.button().isChecked(), "binding must reflect the signal after the click");

        fireClick(btn);
        assertFalse(btn.button().isChecked(), "binding must reflect the signal after the click");
        btn.dispose();
    }

    @Test
    void widthSetsPrefWidth() {
        Button btn = new Button().props(width(200f), prefWidth(200f));
        assertEquals(200f, btn.button().getPrefWidth(), 0.01f);
        btn.dispose();
    }

    @Test
    void heightSetsPrefHeight() {
        Button btn = new Button().props(height(80f), prefHeight(80f));
        assertEquals(80f, btn.button().getPrefHeight(), 0.01f);
        btn.dispose();
    }

    @Test
    void sizeSetsBothDimensions() {
        Button btn = new Button().props(size(100f), prefWidth(100f), prefHeight(100f));
        assertEquals(100f, btn.button().getPrefWidth(), 0.01f);
        assertEquals(100f, btn.button().getPrefHeight(), 0.01f);
        btn.dispose();
    }

    @Test
    void positionSetsCoordinates() {
        Button btn = new Button();

        btn.props(x(10f));
        assertEquals(10f, btn.button().x, 0.01f);

        btn.props(y(20f));
        assertEquals(20f, btn.button().y, 0.01f);

        btn.props(position(30f, 40f));
        assertEquals(30f, btn.button().x, 0.01f);
        assertEquals(40f, btn.button().y, 0.01f);
        btn.dispose();
    }

    @Test
    void nameModifierUpdatesButtonName() {
        Button btn = new Button();
        btn.name("my-button");
        assertEquals("my-button", btn.button().name);
        btn.dispose();
    }

    @Test
    void childrenAddsToButton() {
        Button btn = new Button().children(() -> {
            AttachmentStack.add(new Element());
        });
        assertEquals(1, btn.button().getChildren().size);
        btn.dispose();
    }

    @Test
    void elementIsSameAsSizedButton() {
        Button btn = new Button();
        assertSame(btn.button(), btn.element());
        btn.dispose();
    }
}
