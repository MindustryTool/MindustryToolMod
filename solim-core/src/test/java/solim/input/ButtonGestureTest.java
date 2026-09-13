package solim.input;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.ui.Button.ButtonStyle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.modifier.ElementConfig;
import solim.signal.Signal;
import solim.ui.Ui;

class ButtonGestureTest {

	@BeforeAll
	static void init() {
		if (Core.app == null) Core.app = new MockApplication();
		if (Core.graphics == null) Core.graphics = new MockGraphics();
	}

	@Test
	void shortClickTriggersOnClick() {
		boolean[] clicked = {false};
		boolean[] longClicked = {false};

		Button btn = Ui.button(() -> clicked[0] = true)
				.onLongClick(100L, () -> longClicked[0] = true);

		InputEvent event = new InputEvent();
		event.listenerActor = btn.sizedButton();
		event.targetActor = btn.sizedButton();
		for (arc.scene.event.EventListener l : btn.element().getListeners()) {
			if (l instanceof arc.scene.event.ClickListener) {
				((arc.scene.event.ClickListener) l).clicked(event, 0f, 0f);
			}
		}

		assertTrue(clicked[0], "Short click should trigger onClick");
		assertFalse(longClicked[0], "Short click should not trigger onLongClick");
	}

	@Test
	void longClickTriggersAndSuppressesOnClick() throws InterruptedException {
		boolean[] clicked = {false};
		boolean[] longClicked = {false};

		Button btn = Ui.button(() -> clicked[0] = true)
				.onLongClick(50L, () -> longClicked[0] = true);

		arc.scene.event.ClickListener cl = null;
		for (arc.scene.event.EventListener l : btn.element().getListeners()) {
			if (l instanceof arc.scene.event.ClickListener) {
				cl = (arc.scene.event.ClickListener) l;
				break;
			}
		}
		assertNotNull(cl);

		InputEvent event = new InputEvent();
		event.listenerActor = btn.sizedButton();
		event.targetActor = btn.sizedButton();
		cl.touchDown(event, 0f, 0f, 0, arc.input.KeyCode.mouseLeft);
		assertTrue(btn.sizedButton().isPressed());

		// Initial update tick records pressTime
		btn.element().act(0.01f);

		// Wait past long click threshold
		Thread.sleep(70);

		// Update tick triggers onLongClick
		btn.element().act(0.01f);

		assertTrue(longClicked[0], "Holding button should trigger onLongClick");

		// Click event after long click
		cl.clicked(event, 0f, 0f);

		assertFalse(clicked[0], "OnClick should be suppressed when long click was triggered");
	}

	@Test
	void opacityModifierStaticAndReactive() {
		Element el = new Element();
		ElementConfig.opacity(el, 0.4f);
		assertEquals(0.4f, el.color.a, 0.001f);

		Signal<Float> op = Signal.of(0.8f);
		ElementConfig.opacity(el, op);
		assertEquals(0.8f, el.color.a, 0.001f);

		op.set(0.25f);
		solim.runtime.SignalDispatcher.flush();
		assertEquals(0.25f, el.color.a, 0.001f);
	}

	@Test
	void layoutModifierOpacity() {
		solim.layout.Row r = Ui.row().opacity(0.5f);
		assertEquals(0.5f, r.element().color.a, 0.001f);

		Signal<Float> op = Signal.of(0.9f);
		r.opacity(op);
		assertEquals(0.9f, r.element().color.a, 0.001f);
	}

	@Test
	void reactiveButtonSizeAndMargin() {
		Signal<Float> size = Signal.of(48f);
		Signal<Float> margin = Signal.of(8f);

		Button btn = Ui.button().size(size).margin(margin);

		assertEquals(48f, btn.button().getWidth(), 0.01f);
		assertEquals(48f, btn.button().getHeight(), 0.01f);

		size.set(64f);
		solim.runtime.SignalDispatcher.flush();
		assertEquals(64f, btn.button().getWidth(), 0.01f);
		assertEquals(64f, btn.button().getHeight(), 0.01f);
	}

	@Test
	void reactiveButtonStyleUpdatesStyle() {
		ButtonStyle s1 = new ButtonStyle();
		ButtonStyle s2 = new ButtonStyle();
		Signal<ButtonStyle> styleSignal = Signal.of(s1);

		Button btn = Ui.button().style(styleSignal);
		assertSame(s1, btn.button().getStyle());

		styleSignal.set(s2);
		solim.runtime.SignalDispatcher.flush();
		assertSame(s2, btn.button().getStyle());

		btn.dispose();
	}
}
