package solim.signal;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
	void noThreadLocalImport() throws Exception {
		// try multiple relative locations because test working dir may vary
		Path[] candidates = new Path[] {
			Path.of("solim-runtime/src/solim/runtime/ReactiveContext.java"),
			Path.of("../solim-runtime/src/solim/runtime/ReactiveContext.java"),
			Path.of(System.getProperty("user.dir"), "solim-runtime/src/solim/runtime/ReactiveContext.java"),
			Path.of(System.getProperty("user.dir"), "../solim-runtime/src/solim/runtime/ReactiveContext.java")
		};
		String content = null;
		for (Path p : candidates) {
			if (Files.exists(p)) {
				content = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
				break;
			}
		}
		// fallback search from root
		if (content == null) {
			Path root = Path.of(System.getProperty("user.dir"));
			// walk up to find repo root containing solim folder
			Path cur = root;
			for (int i = 0; i < 5; i++) {
				Path candidate = cur.resolve("solim-runtime/src/solim/runtime/ReactiveContext.java");
				if (Files.exists(candidate)) {
					content = new String(Files.readAllBytes(candidate), StandardCharsets.UTF_8);
					break;
				}
				cur = cur.getParent();
				if (cur == null) break;
			}
		}
		assertNotNull(content, "ReactiveContext.java not found");
		assertFalse(
				content.contains("import java.lang.ThreadLocal")
						|| content.contains("import java.util.concurrent") && content.contains("ThreadLocal"),
				"ReactiveContext must not use ThreadLocal");
		// also ensure no ThreadLocal usage via import line
		long threadLocalImportCount = content.lines()
				.filter(l -> l.trim().startsWith("import") && l.contains("ThreadLocal"))
				.count();
		assertEquals(0, threadLocalImportCount, "ReactiveContext must not import ThreadLocal");
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
