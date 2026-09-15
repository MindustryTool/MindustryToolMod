package solim.input;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import java.util.Arrays;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.signal.Signal;

class InputWidgetsTest {

	@BeforeAll
	static void checkArcContext() {
		Assumptions.assumeTrue(Core.scene != null, "Arc Core.scene is null; skipping skin-dependent tests");
	}

	@Test
	void textFieldTwoWay() {
		Signal<String> input = Signal.of("");
		SolimTextField tf = new SolimTextField(input);
		assertEquals("", tf.field().getText());
		input.set("hello");
		assertEquals("hello", tf.field().getText());
		tf.dispose();
	}

	@Test
	void textFieldFeedbackLoopGuard() {
		Signal<String> input = Signal.of("a");
		SolimTextField tf = new SolimTextField(input);
		// programmatic set
		input.set("b");
		// field should update
		assertEquals("b", tf.field().getText());
		// simulate user typing by changing field text
		tf.field().setText("c");
		// signal should update via changed listener
		assertEquals("c", input.get());
		// setting same value should be no-op
		tf.dispose();
	}

	@Test
	void checkboxBinding() {
		Signal<Boolean> enabled = Signal.of(false);
		Checkbox cb = new Checkbox("Enable", enabled);
		assertFalse(cb.checkBox().isChecked());
		enabled.set(true);
		assertTrue(cb.checkBox().isChecked());
		cb.dispose();
	}

	@Test
	void switchBinding() {
		Signal<Boolean> on = Signal.of(false);
		Switch sw = new Switch(on);
		assertEquals("OFF", sw.button().getText());
		on.set(true);
		assertEquals("ON", sw.button().getText());
		sw.dispose();
	}

	@Test
	void sliderBinding() {
		Signal<Float> v = Signal.of(0.3f);
		SolimSlider sl = new SolimSlider(v, 0f, 1f, 0.1f);
		sl.slider().setValue(0.7f);
		assertEquals(0.7f, v.get(), 0.0001f);
		v.set(0.2f);
		assertEquals(0.2f, sl.slider().getValue(), 0.0001f);
		sl.dispose();
	}

	@Test
	void integerSliderBinding() {
		Signal<Integer> cols = Signal.of(3);
		SolimSlider sl = new SolimSlider(cols, 1, 9, 1);
		sl.slider().setValue(6f);
		assertEquals(6, cols.get().intValue());
		cols.set(4);
		assertEquals(4f, sl.slider().getValue(), 0.0001f);
		sl.dispose();
	}

	@Test
	void checkboxCallback() {
		boolean[] toggled = {true};
		Checkbox cb = new Checkbox("Enable", true, val -> toggled[0] = val);
		assertTrue(cb.checkBox().isChecked());
		cb.checkBox().setChecked(false);
		assertFalse(toggled[0]);
		cb.dispose();
	}

	@Test
	void selectBinding() {
		Signal<String> sel = Signal.of("A");
		SolimSelect<String> s = new SolimSelect<>(sel, Arrays.asList("A", "B", "C"));
		assertEquals("A", s.selectBox().getText().toString());
		sel.set("B");
		assertEquals("B", s.selectBox().getText().toString());
		s.dispose();
	}

	@Test
	void inputWidgetsName() {
		Signal<Boolean> b = Signal.of(false);
		Checkbox cb = new Checkbox("Test", b).name("cb");
		assertEquals("cb", cb.element().name);
		cb.dispose();

		SolimSlider sl = new SolimSlider(Signal.of(0f), 0f, 1f, 0.1f).name("sl");
		assertEquals("sl", sl.element().name);
		sl.dispose();

		SolimTextField tf = new SolimTextField(Signal.of("")).name("tf");
		assertEquals("tf", tf.element().name);
		tf.dispose();

		Switch sw = new Switch(Signal.of(false)).name("sw");
		assertEquals("sw", sw.element().name);
		sw.dispose();

		SolimSelect<String> sel = new SolimSelect<>(Signal.of("a"), Arrays.asList("a", "b")).name("sel");
		assertEquals("sel", sel.element().name);
		sel.dispose();
	}
}
