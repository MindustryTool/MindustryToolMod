package solim.signal;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import solim.core.ReactiveObserver;
import solim.runtime.ReactiveContext;

class ReactiveContextTest {
	@AfterEach
	void clear() {
		ReactiveContext.clear();
	}

	@Test
	void pushPopCurrent() {
		ReactiveObserver dummy = new ReactiveObserver() {
			@Override
			public void addDependency(Object observable) {}

			@Override
			public void invalidate() {}
		};
		assertNull(ReactiveContext.current());
		ReactiveContext.push(dummy);
		assertEquals(dummy, ReactiveContext.current());
		assertEquals(1, ReactiveContext.size());
		ReactiveContext.push(dummy);
		assertEquals(2, ReactiveContext.size());
		ReactiveContext.pop();
		assertEquals(1, ReactiveContext.size());
		ReactiveContext.pop();
		assertNull(ReactiveContext.current());
		assertEquals(0, ReactiveContext.size());
	}

	@Test
	void nestedEvaluationStack() {
		Signal<Integer> s = Signal.of(1);
		Computed<String> inner = Signal.computed(() -> "inner:" + s.get());
		Computed<String> outer = Signal.computed(() -> "outer:" + inner.get() + s.get());
		assertEquals("outer:inner:11", outer.get());
		// after get, stack should be empty
		assertEquals(0, ReactiveContext.size());
	}

	@Test
	void trackWithoutObserverIsNoOp() {
		ReactiveContext.clear();
		Signal<Integer> s = Signal.of(5);
		// should not throw when no observer
		assertDoesNotThrow(() -> ReactiveContext.track(s));
		assertEquals(5, s.get());
	}

	@Test
	void testUntrackedExecution() {
		Signal<Integer> count = Signal.of(0);
		int[] effectRuns = new int[]{0};

		Effect.of(() -> {
			effectRuns[0]++;
			// read inside untracked
			int val = ReactiveContext.untracked(count::get);
			assertEquals(val, count.peek());
		});

		assertEquals(1, effectRuns[0]);
		count.set(1);
		count.set(2);
		assertEquals(1, effectRuns[0], "Effect should not re-run because signal read was untracked");
	}
}
