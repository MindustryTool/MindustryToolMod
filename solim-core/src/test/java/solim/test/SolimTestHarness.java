package solim.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGL20;
import arc.mock.MockGraphics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import solim.modifier.PendingCellConfig;
import solim.runtime.ComponentContext;
import solim.runtime.ParentStack;
import solim.runtime.ReactiveContext;
import solim.runtime.SignalDispatcher;

/**
 * Base test harness for all Solim unit and integration tests.
 * Automatically initializes headless Arc environment, and enforces strict
 * teardown assertions ensuring no ambient context leaks between tests.
 */
public abstract class SolimTestHarness {

	@BeforeAll
	public static void initArcHeadless() {
		if (Core.app == null) {
			Core.app = new MockApplication();
		}
		if (Core.graphics == null) {
			Core.graphics = new MockGraphics();
		}
		if (Core.gl == null) {
			Core.gl = new MockGL20();
			Core.gl20 = (MockGL20) Core.gl;
		}
		Core.scene = null;
	}

	@BeforeEach
	public void setUpHarness() {
		resetAmbientState();
	}

	@AfterEach
	public void verifyAndTearDownHarness() {
		int parentStackSize = ParentStack.size();
		int componentContextSize = ComponentContext.size();
		int reactiveContextSize = ReactiveContext.size();
		int pendingEffects = SignalDispatcher.size();
		boolean isFlushing = SignalDispatcher.isFlushing();

		// Clean up immediately so subsequent tests are not contaminated even on failure
		resetAmbientState();

		assertEquals(0, parentStackSize, "ParentStack must be empty at teardown");
		assertNull(ParentStack.current(), "ParentStack.current() must be null at teardown");

		assertEquals(0, componentContextSize, "ComponentContext must be empty at teardown");
		assertNull(ComponentContext.current(), "ComponentContext.current() must be null at teardown");

		assertEquals(0, reactiveContextSize, "ReactiveContext must be empty at teardown");
		assertNull(ReactiveContext.current(), "ReactiveContext.current() must be null at teardown");

		assertFalse(isFlushing, "SignalDispatcher must not be mid-flush at teardown");
		assertEquals(0, pendingEffects, "SignalDispatcher must have no pending effects at teardown");
	}

	public static void resetAmbientState() {
		ParentStack.clear();
		PendingCellConfig.install();
		ComponentContext.clear();
		ReactiveContext.clear();
		SignalDispatcher.resetForTests();
		Core.scene = null;
	}
}
