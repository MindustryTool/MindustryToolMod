package solim.signal;

import static org.junit.jupiter.api.Assertions.*;

import arc.scene.Element;
import org.junit.jupiter.api.Test;
import solim.core.BaseComponent;

class SignalReactivityWarningTest {

	@Test
	void peekReturnsValueWithoutTracking() {
		Signal<Integer> sig = Signal.of(42);
		assertEquals(42, sig.peek());

		Computed<Integer> comp = Signal.computed(() -> sig.get() * 2);
		assertEquals(84, comp.peek());

		sig.set(10);
		assertEquals(20, comp.peek());
	}

	@Test
	void getInsideComponentBuildIsDetected() {
		Signal<String> testSignal = Signal.of("initial");
		boolean[] getCalledInsideBuild = {false};

		class SampleComponent extends BaseComponent {
			@Override
			protected Element build() {
				// Calling get() inside build()
				String val = testSignal.get();
				if ("initial".equals(val)) {
					getCalledInsideBuild[0] = true;
				}
				return new Element();
			}
		}

		SampleComponent c = new SampleComponent();
		c.element();

		assertTrue(getCalledInsideBuild[0]);
	}
}
