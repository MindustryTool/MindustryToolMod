package solim.input;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import java.util.Arrays;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.signal.Signal;

class SolimSelectComponentTest {

	@BeforeAll
	static void checkArcContext() {
		Assumptions.assumeTrue(Core.scene != null, "Arc Core.scene is null; skipping skin-dependent tests");
	}

	@Test
	void signalUpdatesSelectBoxText() {
		Signal<String> sel = Signal.of("A");
		SolimSelect<String> s = new SolimSelect<>(sel, Arrays.asList("A", "B", "C"));

		assertEquals("A", s.selectBox().getText().toString());

		sel.set("B");
		assertEquals("B", s.selectBox().getText().toString());

		sel.set("C");
		assertEquals("C", s.selectBox().getText().toString());
		s.dispose();
	}

	@Test
	void nameModifierUpdatesElementName() {
		SolimSelect<String> sel = new SolimSelect<>(Signal.of("a"), Arrays.asList("a", "b")).name("sel");
		assertEquals("sel", sel.element().name);
		sel.dispose();
	}

	@Test
	void elementIsSameAsSelectBox() {
		SolimSelect<String> s = new SolimSelect<>(Signal.of("A"), Arrays.asList("A", "B"));
		assertSame(s.selectBox(), s.element());
		s.dispose();
	}
}
