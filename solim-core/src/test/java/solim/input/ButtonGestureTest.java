package solim.input;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.event.InputEvent;
import arc.scene.ui.Button.ButtonStyle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.signal.Signal;
import solim.ui.Ui;
import arc.input.KeyCode;
import arc.scene.event.ClickListener;
import arc.scene.event.EventListener;
import solim.layout.Row;
import solim.runtime.SignalDispatcher;

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
		for (EventListener l : btn.element().getListeners()) {
			if (l instanceof ClickListener) {
				((ClickListener) l).clicked(event, 0f, 0f);
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

		ClickListener cl = null;
		for (EventListener l : btn.element().getListeners()) {
			if (l instanceof ClickListener) {
				cl = (ClickListener) l;
				break;
			}
		}
		assertNotNull(cl);

		InputEvent event = new InputEvent();
		event.listenerActor = btn.sizedButton();
		event.targetActor = btn.sizedButton();
		cl.touchDown(event, 0f, 0f, 0, KeyCode.mouseLeft);
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
		Button btn = Ui.button();
		btn.opacity(0.4f);
		assertEquals(0.4f, btn.element().color.a, 0.001f);

		Signal<Float> op = Signal.of(0.8f);
		btn.opacity(op);
		assertEquals(0.8f, btn.element().color.a, 0.001f);

		op.set(0.25f);
		SignalDispatcher.flush();
		assertEquals(0.25f, btn.element().color.a, 0.001f);
	}

	@Test
	void layoutModifierOpacity() {
		Row r = Ui.row().opacity(0.5f);
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
		SignalDispatcher.flush();
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
		SignalDispatcher.flush();
		assertSame(s2, btn.button().getStyle());

		btn.dispose();
	}
}
