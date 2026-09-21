package solim.input;

import static org.junit.jupiter.api.Assertions.*;

import arc.scene.event.InputEvent;
import arc.scene.ui.Button.ButtonStyle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.reactive.Signal;
import arc.input.KeyCode;
import arc.scene.event.ClickListener;
import arc.scene.event.EventListener;
import solim.layout.Row;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class ButtonGestureTest extends SolimEnv {

	@BeforeAll
	static void init() {
	}

	@Test
	void shortClickTriggersOnClick() {
		boolean[] clicked = {false};
		boolean[] longClicked = {false};

		Button btn = new Button(() -> clicked[0] = true)
				.onLongClick(100L, () -> longClicked[0] = true);

		InputEvent event = new InputEvent();
		event.listenerActor = btn.button();
		event.targetActor = btn.button();
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

		Button btn = new Button(() -> clicked[0] = true)
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
		event.listenerActor = btn.button();
		event.targetActor = btn.button();
		cl.touchDown(event, 0f, 0f, 0, KeyCode.mouseLeft);
		assertTrue(btn.button().isPressed());

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
		Button btn = new Button();
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
		Row r = new Row().opacity(0.5f);
		assertEquals(0.5f, r.element().color.a, 0.001f);

		Signal<Float> op = Signal.of(0.9f);
		r.opacity(op);
		assertEquals(0.9f, r.element().color.a, 0.001f);
	}

	@Test
	void reactiveButtonSizeAndMargin() {
		Signal<Float> size = Signal.of(48f);
		Signal<Float> margin = Signal.of(8f);

		Button btn = new Button().size(size).margin(margin);

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

		Button btn = new Button().style(styleSignal);
		assertSame(s1, btn.button().getStyle());

		styleSignal.set(s2);
		SignalDispatcher.flush();
		assertSame(s2, btn.button().getStyle());

		btn.dispose();
	}
}
