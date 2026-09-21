package solim.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import solim.runtime.ComponentContext;
import solim.runtime.ParentStack;
import solim.runtime.ReactiveContext;
import solim.runtime.SignalDispatcher;

/**
 * Test environment for solim runtime tests (components, signals, layouts).
 *
 * Extends {@link ArcTestEnv} with solim ambient state reset before each test
 * and strict teardown assertions that fail the test if any ambient state
 * leaked — this is what makes shared (reused) test JVMs safe, replacing the
 * old forkEvery = 1 JVM-per-class approach.
 *
 * Runtime-layer only: this class must not reference solim-core, because
 * solim-core tests consume this module and a reverse dependency would create
 * a build-path cycle. Core-specific resets (e.g. the default cell
 * configurator) belong to a solim-core test env overriding
 * {@link #resetModuleState()}.
 */
public class SolimEnv extends ArcTestEnv {

	@BeforeEach
	public void setUpSolimEnv() {
		resetAmbientState();
		resetModuleState();
	}

	@AfterEach
	public void verifyAndTearDownSolimEnv() {
		int parentStackSize = ParentStack.size();
		int componentContextSize = ComponentContext.size();
		int reactiveContextSize = ReactiveContext.size();
		int pendingEffects = SignalDispatcher.size();
		boolean isFlushing = SignalDispatcher.isFlushing();

		// Clean up immediately so subsequent tests are not contaminated even on failure
		resetAmbientState();
		resetModuleState();

		assertEquals(0, parentStackSize, "ParentStack must be empty at teardown");
		assertNull(ParentStack.current(), "ParentStack.current() must be null at teardown");

		assertEquals(0, componentContextSize, "ComponentContext must be empty at teardown");
		assertNull(ComponentContext.current(), "ComponentContext.current() must be null at teardown");

		assertEquals(0, reactiveContextSize, "ReactiveContext must be empty at teardown");
		assertNull(ReactiveContext.current(), "ReactiveContext.current() must be null at teardown");

		assertFalse(isFlushing, "SignalDispatcher must not be mid-flush at teardown");
		assertEquals(0, pendingEffects, "SignalDispatcher must have no pending effects at teardown");
	}

	/**
	 * Hook for module layers above the runtime to restore their own defaults
	 * (e.g. solim-core reinstalling the default cell configurator). Called
	 * after the static ambient reset in both setup and teardown. No-op here.
	 */
	protected void resetModuleState() {
	}

	/**
	 * Pumps all pending reactive effects. Test environments have no frame
	 * loop, so tests call this where production code relies on the frame
	 * lifecycle. Facade keeps runtime types invisible to test consumers.
	 */
	public static void flushEffects() {
		SignalDispatcher.flush();
	}

	public static void resetAmbientState() {
		ParentStack.clear();
		ComponentContext.clear();
		ReactiveContext.clear();
		SignalDispatcher.resetForTests();
	}
}
