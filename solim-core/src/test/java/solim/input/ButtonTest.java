package solim.input;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.scene.Element;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.signal.Signal;
import solim.runtime.ParentStack;

class ButtonTest {

	@BeforeAll
	static void checkArcContext() {
		Assumptions.assumeTrue(Core.scene != null, "Arc Core.scene is null; skipping skin-dependent tests");
	}

	@Test
	void buttonWidthAndHeightCustomSize() {
		Button btn = new Button().width(200f).height(80f);

		assertEquals(200f, btn.sizedButton().getPrefWidth(), 0.01f);
		assertEquals(80f, btn.sizedButton().getPrefHeight(), 0.01f);

		btn.dispose();
	}

	@Test
	void buttonChildrenAndClickEvent() {
		boolean[] clicked = {false};
		Button btn = new Button(() -> clicked[0] = true).children(() -> {
			ParentStack.add(new Element());
		});

		assertEquals(1, btn.sizedButton().getChildren().size);

		InputEvent event = new InputEvent();
		btn.sizedButton().getListeners().forEach(listener -> {
			if (listener instanceof ClickListener) {
				((ClickListener) listener).clicked(event, 0f, 0f);
			}
		});

		assertTrue(clicked[0], "Button onClick should execute");
		assertTrue(event.stopped, "Button should stop event propagation");

		btn.dispose();
	}

	@Test
	void buttonReactiveReactivity() {
		Signal<Boolean> enabled = Signal.of(true);
		Signal<Boolean> visible = Signal.of(true);

		Button btn = new Button().enabled(enabled).visible(visible);

		assertFalse(btn.sizedButton().isDisabled());
		assertTrue(btn.sizedButton().visible);

		enabled.set(false);
		assertTrue(btn.sizedButton().isDisabled());

		visible.set(false);
		assertFalse(btn.sizedButton().visible);

		btn.dispose();
	}
}
