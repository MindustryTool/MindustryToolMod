package solim.ui;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.ui.Button;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import solim.signal.Computed;
import solim.signal.Effect;
import solim.signal.Signal;
import solim.runtime.SignalDispatcher;

class BindingTest {

	static class TestElement {
		String text = "";
		boolean enabled = true;
		boolean visible = true;
		Color color = Color.white;
		float width = 0f;

		void setText(String t) {
			this.text = t;
		}

		void setEnabled(boolean e) {
			this.enabled = e;
		}

		void setVisible(boolean v) {
			this.visible = v;
		}

		void setColor(Color c) {
			this.color = c;
		}

		void setWidth(float w) {
			this.width = w;
		}
	}

	@Test
	void staticTextNoBindingNeeded() {
		TestElement t = new TestElement();
		t.setText("Hello");
		assertEquals("Hello", t.text);
	}

	@Test
	void reactiveTextImmediateApply() {
		Signal<String> s = Signal.of("Alice");
		TestElement target = new TestElement();
		Effect b = Binding.of((Consumer<String>) t -> target.setText(t), s);
		assertEquals("Alice", target.text, "Immediate apply on create");
		s.set("Bob");
		SignalDispatcher.flush();
		assertEquals("Bob", target.text, "Update on signal change");
		b.dispose();
		s.set("Charlie");
		SignalDispatcher.flush();
		assertEquals("Bob", target.text, "Disposed binding does not update");
	}

	@Test
	void reactiveTextViaComputed() {
		Signal<Integer> count = Signal.of(1);
		Computed<String> text = count.map(v -> "Count: " + v);
		TestElement target = new TestElement();
		Effect b = Binding.of((Consumer<String>) t -> target.setText(t), text);
		assertEquals("Count: 1", target.text);
		count.set(2);
		SignalDispatcher.flush();
		assertEquals("Count: 2", target.text);
		b.dispose();
	}

	@Test
	void noRebuildOnChange() {
		Signal<String> s = Signal.of("a");
		TestElement target = new TestElement();
		Effect b = Binding.bind(s, t -> target.setText(t));
		TestElement ref = target;
		s.set("b");
		SignalDispatcher.flush();
		assertSame(ref, target, "Element not recreated on update");
		b.dispose();
	}

	@Test
	void visibleAndEnabledBinding() {
		Signal<Boolean> isLoggedIn = Signal.of(false);
		TestElement target = new TestElement();
		Effect visible = Binding.of((Consumer<Boolean>) t -> target.setVisible(t), isLoggedIn);
		assertFalse(target.visible);
		isLoggedIn.set(true);
		SignalDispatcher.flush();
		assertTrue(target.visible);
		visible.dispose();

		Signal<Boolean> canSave = Signal.of(true);
		Effect enabled = Binding.of((Consumer<Boolean>) t -> target.setEnabled(t), canSave);
		assertTrue(target.enabled);
		canSave.set(false);
		SignalDispatcher.flush();
		assertFalse(target.enabled);
		enabled.dispose();
	}

	@Test
	void disposeStopsUpdates() {
		Signal<String> s = Signal.of("a");
		AtomicReference<String> target = new AtomicReference<>("");
		Effect b = Binding.of(target::set, s);
		assertEquals("a", target.get());
		b.dispose();
		s.set("b");
		SignalDispatcher.flush();
		assertEquals("a", target.get());
	}

	@Test
	void directElementWidthAndColorBinding() {
		Element el = new Element();
		Signal<Float> width = Signal.of(100f);
		Signal<Color> color = Signal.of(Color.red);

		Effect bWidth = Binding.bindWidth(el, width);
		Effect bColor = Binding.bindColor(el, color);

		assertEquals(100f, el.getWidth());
		assertEquals(Color.red, el.color);

		width.set(250f);
		color.set(Color.green);
		SignalDispatcher.flush();

		assertEquals(250f, el.getWidth());
		assertEquals(Color.green, el.color);

		bWidth.dispose();
		bColor.dispose();

		width.set(500f);
		color.set(Color.blue);
		SignalDispatcher.flush();
		assertEquals(250f, el.getWidth());
		assertEquals(Color.green, el.color);
	}

	@Test
	void directElementVisibleAndDisabledBinding() {
		Element el = new Element();
		Signal<Boolean> vis = Signal.of(false);
		Effect bVis = Binding.bindVisible(el, vis);
		assertFalse(el.visible);
		vis.set(true);
		SignalDispatcher.flush();
		assertTrue(el.visible);
		bVis.dispose();

		if (Core.scene != null) {
			Button btn = new Button();
			Signal<Boolean> dis = Signal.of(false);
			Effect bDis = Binding.bindDisabled(btn, dis);
			assertFalse(btn.isDisabled());
			dis.set(true);
			SignalDispatcher.flush();
			assertTrue(btn.isDisabled());
			bDis.dispose();
		}
	}
}
