package solim.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.Element;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import solim.reactive.Effect;
import solim.reactive.Signal;
import solim.test.SolimEnv;
import solim.test.TestObserver;
import solim.test.TestScheduler;

public class SchedulerAndContextsRegressionTest extends SolimEnv {

	// ==========================================
	// Phase 8: Scheduler (SignalDispatcher)
	// ==========================================

	@Test
	void fifoExecutionOrderInFlush() {
		Signal<Integer> trigger = Signal.of(0);
		List<String> order = new ArrayList<>();

		Effect e1 = Effect.of(() -> {
			trigger.get();
			order.add("effect-1");
		});
		Effect e2 = Effect.of(() -> {
			trigger.get();
			order.add("effect-2");
		});
		Effect e3 = Effect.of(() -> {
			trigger.get();
			order.add("effect-3");
		});

		order.clear();

		trigger.set(1);
		assertEquals(3, TestScheduler.pendingCount());

		TestScheduler.flush();

		assertEquals(Arrays.asList("effect-1", "effect-2", "effect-3"), order,
				"Scheduled effects must execute in deterministic FIFO order");

		e1.dispose();
		e2.dispose();
		e3.dispose();
	}

	@Test
	void cascadeCycleProtectionTerminatesCleanly() {
		Signal<Integer> sigA = Signal.of(0);
		Signal<Integer> sigB = Signal.of(0);

		// Ping-pong circular dependency
		Effect effA = Effect.of(() -> {
			int b = sigB.get();
			sigA.set(b + 1);
		});

		Effect effB = Effect.of(() -> {
			int a = sigA.get();
			sigB.set(a + 1);
		});

		// Trigger cascade
		sigA.set(1);

		// Must terminate and clear queue without infinite loop
		TestScheduler.flush();

		assertEquals(0, TestScheduler.pendingCount(), "Queue must be cleared after infinite cascade detection");
		assertFalse(TestScheduler.isFlushing());

		effA.dispose();
		effB.dispose();
	}

	@Test
	void reentrantFlushIsSafelyIgnored() {
		Signal<Integer> sig = Signal.of(1);
		AtomicInteger flushAttempts = new AtomicInteger(0);
		AtomicInteger runs = new AtomicInteger(0);

		Effect effect = Effect.of(() -> {
			runs.incrementAndGet();
			sig.get();
			if (runs.get() == 2) {
				flushAttempts.incrementAndGet();
				// Re-entrant flush during flush must be a no-op
				SignalDispatcher.flush();
			}
		});

		assertEquals(1, runs.get());
		sig.set(2);

		TestScheduler.flush();

		assertEquals(1, flushAttempts.get());
		assertEquals(2, runs.get());
		assertFalse(TestScheduler.isFlushing());

		effect.dispose();
	}

	// ==========================================
	// Phase 9: ReactiveContext
	// ==========================================

	@Test
	void nestedReactiveContextIsolation() {
		TestObserver outerObs = new TestObserver("outer");
		TestObserver innerObs = new TestObserver("inner");

		Signal<String> sigOuter = Signal.of("outer-sig");
		Signal<String> sigInner = Signal.of("inner-sig");

		ReactiveContext.push(outerObs);
		ReactiveContext.track(sigOuter);

		ReactiveContext.push(innerObs);
		ReactiveContext.track(sigInner);
		ReactiveContext.pop();

		// Resuming outer
		ReactiveContext.track(sigOuter);
		ReactiveContext.pop();

		assertEquals(Arrays.asList(sigOuter, sigOuter), outerObs.getDependencies());
		assertEquals(Arrays.asList(sigInner), innerObs.getDependencies(),
				"Inner observer dependencies must not leak to outer observer");
		assertEquals(0, ReactiveContext.size());
	}

	@Test
	void untrackedScopePreservesEnclosingContextOnException() {
		TestObserver obs = new TestObserver("obs");
		ReactiveContext.push(obs);

		assertThrows(RuntimeException.class, () -> {
			ReactiveContext.untracked(() -> {
				throw new RuntimeException("Error inside untracked");
			});
		});

		assertSame(obs, ReactiveContext.current(), "Outer observer must be restored after exception in untracked");
		assertEquals(1, ReactiveContext.size());

		ReactiveContext.pop();
		assertEquals(0, ReactiveContext.size());
	}

	// ==========================================
	// Phase 10: ParentStack
	// ==========================================

	@Test
	void nestedParentsAndSiblingIsolation() {
		Table parentA = new Table();
		parentA.name = "parentA";
		Table parentB = new Table();
		parentB.name = "parentB";

		ParentStack.push(parentA);

		Element childA1 = new Element();
		childA1.name = "childA1";
		ParentStack.add(childA1);

		ParentStack.push(parentB);
		Element childB1 = new Element();
		childB1.name = "childB1";
		ParentStack.add(childB1);
		ParentStack.pop();

		Element childA2 = new Element();
		childA2.name = "childA2";
		ParentStack.add(childA2);

		ParentStack.pop();

		assertTrue(parentA.getChildren().contains(childA1, true));
		assertTrue(parentA.getChildren().contains(childA2, true));
		assertFalse(parentA.getChildren().contains(childB1, true));

		assertTrue(parentB.getChildren().contains(childB1, true));
		assertFalse(parentB.getChildren().contains(childA1, true));
		assertFalse(parentB.getChildren().contains(childA2, true));

		assertEquals(0, ParentStack.size());
	}

	@Test
	void cellConfiguratorSiblingIsolation() {
		Table root = new Table();
		ParentStack.push(root);

		Element el1 = new Element();
		Element el2 = new Element();

		List<Element> configured = new ArrayList<>();
		ParentStack.setCellConfigurator((cell, child, comp) -> {
			configured.add(child);
			if (child == el1) {
				cell.pad(10f);
			}
		});

		ParentStack.add(el1);
		ParentStack.add(el2);
		ParentStack.pop();

		assertEquals(Arrays.asList(el1, el2), configured);
		assertEquals(10f, CellAccess.padTop(root.getCell(el1)), 0.01f);
		assertEquals(0f, CellAccess.padTop(root.getCell(el2)), 0.01f, "Cell configuration for child 1 must not leak to child 2");
	}

	@Test
	void isolateRestoresStackOnException() {
		Table outer = new Table();
		ParentStack.push(outer);

		assertThrows(RuntimeException.class, () -> {
			ParentStack.isolate(() -> {
				throw new RuntimeException("Error inside isolate");
			});
		});

		assertSame(outer, ParentStack.current(), "Outer parent must be restored after exception in isolate");
		assertEquals(1, ParentStack.size());

		ParentStack.pop();
		assertEquals(0, ParentStack.size());
	}
}
