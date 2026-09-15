package solim.input;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.signal.Signal;

class SwitchComponentTest {

	@BeforeAll
	static void checkArcContext() {
		Assumptions.assumeTrue(Core.scene != null, "Arc Core.scene is null; skipping skin-dependent tests");
	}

	@Test
	void signalUpdatesButtonText() {
		Signal<Boolean> on = Signal.of(false);
		Switch sw = new Switch(on);

		assertEquals("OFF", sw.button().getText());

		on.set(true);
		assertEquals("ON", sw.button().getText());

		on.set(false);
		assertEquals("OFF", sw.button().getText());
		sw.dispose();
	}

	@Test
	void staticFactoryCreatesSwitch() {
		Switch sw = Switch.of(Signal.of(true));
		assertEquals("ON", sw.button().getText());
		sw.dispose();
	}

	@Test
	void nameModifierUpdatesElementName() {
		Switch sw = new Switch(Signal.of(false)).name("sw");
		assertEquals("sw", sw.element().name);
		sw.dispose();
	}

	@Test
	void elementIsSameAsButton() {
		Switch sw = new Switch(Signal.of(false));
		assertSame(sw.button(), sw.element());
		sw.dispose();
	}
}
