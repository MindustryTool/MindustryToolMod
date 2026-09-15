package solim.input;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.signal.Signal;

class CheckboxComponentTest {

	@BeforeAll
	static void checkArcContext() {
		Assumptions.assumeTrue(Core.scene != null, "Arc Core.scene is null; skipping skin-dependent tests");
	}

	@Test
	void signalUpdatesCheckedState() {
		Signal<Boolean> enabled = Signal.of(false);
		Checkbox cb = new Checkbox("Enable", enabled);

		assertFalse(cb.checkBox().isChecked());

		enabled.set(true);
		assertTrue(cb.checkBox().isChecked());

		enabled.set(false);
		assertFalse(cb.checkBox().isChecked());
		cb.dispose();
	}

	@Test
	void callbackReceivesToggleValue() {
		boolean[] toggled = {true};
		Checkbox cb = new Checkbox("Enable", true, val -> toggled[0] = val);

		assertTrue(cb.checkBox().isChecked());
		cb.checkBox().setChecked(false);
		assertFalse(toggled[0]);
		cb.dispose();
	}

	@Test
	void nameModifierUpdatesElementName() {
		Signal<Boolean> b = Signal.of(false);
		Checkbox cb = new Checkbox("Test", b).name("cb");
		assertEquals("cb", cb.element().name);
		cb.dispose();
	}

	@Test
	void elementIsSameAsCheckBox() {
		Checkbox cb = new Checkbox("Test", Signal.of(false));
		assertSame(cb.checkBox(), cb.element());
		cb.dispose();
	}
}
