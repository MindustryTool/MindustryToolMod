package solim.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.Element;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.core.SolimToken;
import solim.display.Text;
import solim.layout.Row;
import solim.runtime.ComponentContext;
import solim.runtime.ParentStack;
import solim.runtime.StructuralReconciler;
import solim.test.SolimEnv;
import solim.test.TestObserver;
import solim.test.TestScheduler;

public class PropertyAndRandomizedRegressionTest extends SolimEnv {

	// ==========================================
	// Phase 22: Randomized Property Tests
	// ==========================================

	@Test
	void randomizedSignalAndDynamicComputedNetwork() {
		Random random = new Random(42);
		int numSignals = 8;
		List<Signal<Integer>> signals = new ArrayList<>();
		for (int i = 0; i < numSignals; i++) {
			signals.add(Signal.of(i * 10));
		}

		// Dynamic branching computed:
		// if (signals.get(0).get() % 2 == 0) {
		//     sum(signals[1..3])
		// } else {
		//     sum(signals[4..7])
		// }
		Computed<Integer> dynamicSum = Signal.computed(() -> {
			int selector = signals.get(0).get();
			int sum = 0;
			if (selector % 2 == 0) {
				for (int i = 1; i <= 3; i++) {
					sum += signals.get(i).get();
				}
			} else {
				for (int i = 4; i < numSignals; i++) {
					sum += signals.get(i).get();
				}
			}
			return sum;
		});

		// 1000 randomized state transitions
		for (int step = 0; step < 1000; step++) {
			int targetSignal = random.nextInt(numSignals);
			int newValue = random.nextInt(100);
			signals.get(targetSignal).set(newValue);

			// Calculate expected value using oracle logic
			int expectedSum = 0;
			int currentSelector = signals.get(0).peek();
			if (currentSelector % 2 == 0) {
				for (int i = 1; i <= 3; i++) {
					expectedSum += signals.get(i).peek();
				}
			} else {
				for (int i = 4; i < numSignals; i++) {
					expectedSum += signals.get(i).peek();
				}
			}

			// Verify computed value matches oracle calculation
			assertEquals(expectedSum, dynamicSum.get().intValue(), "Computed must match oracle at step " + step);
		}

		dynamicSum.dispose();
		TestScheduler.flush();
		for (Signal<Integer> s : signals) {
			assertEquals(0, s.observerCount(), "All signals must have zero observers after computed disposal");
		}
	}

	private static final class TestRowComponent implements Component {
		private final Element element;
		private boolean disposed = false;

		TestRowComponent(String key) {
			this.element = new Element();
			this.element.name = "Row " + key;
			ComponentContext.register(this);
		}

		@Override
		public Element element() {
			return element;
		}

		@Override
		public void dispose() {
			disposed = true;
		}

		@Override
		public boolean isDisposed() {
			return disposed;
		}
	}

	@Test
	void randomizedStructuralReconcilerOperations() {
		Random random = new Random(1337);
		StructuralReconciler<String, TestRowComponent> reconciler = new StructuralReconciler<>();
		Table container = new Table();
		ParentStack.push(container);

		List<String> shadowKeys = new ArrayList<>();
		Map<String, TestRowComponent> createdComponents = new HashMap<>();
		int nextKeyId = 0;

		// Initial seed
		for (int i = 0; i < 5; i++) {
			String k = "k" + (nextKeyId++);
			shadowKeys.add(k);
		}

		reconciler.reconcile(
				shadowKeys,
				k -> k,
				k -> {
					TestRowComponent comp = new TestRowComponent(k);
					createdComponents.put(k, comp);
					return comp;
				}
		);

		assertEquals(5, reconciler.activeComponents().size());

		// 500 randomized mutations
		for (int op = 0; op < 500; op++) {
			int action = random.nextInt(6);
			switch (action) {
				case 0: // Add key
					String newKey = "k" + (nextKeyId++);
					int insertIdx = shadowKeys.isEmpty() ? 0 : random.nextInt(shadowKeys.size() + 1);
					shadowKeys.add(insertIdx, newKey);
					break;
				case 1: // Remove key
					if (!shadowKeys.isEmpty()) {
						int removeIdx = random.nextInt(shadowKeys.size());
						shadowKeys.remove(removeIdx);
					}
					break;
				case 2: // Swap / Reorder
					if (shadowKeys.size() >= 2) {
						int i1 = random.nextInt(shadowKeys.size());
						int i2 = random.nextInt(shadowKeys.size());
						Collections.swap(shadowKeys, i1, i2);
					}
					break;
				case 3: // Reverse
					if (shadowKeys.size() >= 2) {
						Collections.reverse(shadowKeys);
					}
					break;
				case 4: // Clear
					shadowKeys.clear();
					break;
				case 5: // Batch append
					int toAdd = random.nextInt(4) + 1;
					for (int b = 0; b < toAdd; b++) {
						shadowKeys.add("k" + (nextKeyId++));
					}
					break;
			}

			// Reconcile
			reconciler.reconcile(
					shadowKeys,
					k -> k,
					k -> {
						TestRowComponent comp = new TestRowComponent(k);
						createdComponents.put(k, comp);
						return comp;
					}
			);

			// Assertions against shadow oracle:
			// 1. Component count matches shadow keys
			assertEquals(shadowKeys.size(), reconciler.activeComponents().size(), "Active component count mismatch at op " + op);

			// 2. Keys match shadow keys in order
			List<String> activeKeys = new ArrayList<>(reconciler.activeComponents().keySet());
			assertEquals(shadowKeys, activeKeys, "Active keys mismatch at op " + op);

			// 3. Invariant: all active components must NOT be disposed
			for (String k : shadowKeys) {
				TestRowComponent activeComp = reconciler.activeComponents().get(k);
				assertNotNull(activeComp);
				assertFalse(activeComp.isDisposed(), "Active component " + k + " must not be disposed at op " + op);
			}

			// 5. Invariant: all previously created but currently inactive components MUST be disposed
			for (Map.Entry<String, TestRowComponent> entry : createdComponents.entrySet()) {
				if (!shadowKeys.contains(entry.getKey())) {
					assertTrue(entry.getValue().isDisposed(), "Removed component " + entry.getKey() + " must be disposed at op " + op);
				}
			}
		}

		ParentStack.pop();
		reconciler.dispose();
		TestScheduler.flush();

		assertTrue(reconciler.isDisposed());
		for (TestRowComponent comp : createdComponents.values()) {
			assertTrue(comp.isDisposed(), "All components must be disposed after reconciler disposal");
		}
	}

	// ==========================================
	// Phase 24: Permanent Historical Regressions
	// ==========================================

	@Test
	void regressionSolimTokenMetadataPreservedAcrossArcMutations() {
		Table table = new Table();
		table.userObject = "external-payload";
		Row row = new Row();
		SolimToken.bind(table, row, null);

		assertNotNull(SolimToken.get(table));
		assertSame(row, SolimToken.getComponent(table));
		assertEquals("external-payload", SolimToken.get(table).userPayload);
	}

	@Test
	void regressionComponentSingleMaterializationAndCaching() {
		AtomicInteger buildRuns = new AtomicInteger(0);
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				buildRuns.incrementAndGet();
				return new Element();
			}
		};

		Element el1 = comp.element();
		Element el2 = comp.element();
		Element el3 = comp.element();

		assertSame(el1, el2);
		assertSame(el1, el3);
		assertEquals(1, buildRuns.get(), "build() must run exactly once across repeated element() calls");

		comp.dispose();
		assertThrows(IllegalStateException.class, comp::element, "Calling element() on disposed component must throw");
	}

	@Test
	void regressionReconcilerRollbackOnFactoryFailureLeavesExistingStateUntouched() {
		StructuralReconciler<String, TestRowComponent> reconciler = new StructuralReconciler<>();
		Table container = new Table();
		ParentStack.push(container);

		List<String> initialKeys = new ArrayList<>();
		initialKeys.add("item1");
		initialKeys.add("item2");

		reconciler.reconcile(
				initialKeys,
				k -> k,
				TestRowComponent::new
		);

		TestRowComponent c1 = reconciler.activeComponents().get("item1");
		TestRowComponent c2 = reconciler.activeComponents().get("item2");

		// Attempt reconciliation with a failing item: item1, item2, crashItem
		List<String> newKeys = new ArrayList<>();
		newKeys.add("item1");
		newKeys.add("item2");
		newKeys.add("crash");

		assertThrows(RuntimeException.class, () -> {
			reconciler.reconcile(
					newKeys,
					k -> k,
					k -> {
						if ("crash".equals(k)) {
							throw new RuntimeException("Simulated factory crash");
						}
						return new TestRowComponent(k);
					}
			);
		});

		// State must roll back: active keys still item1 and item2, neither disposed
		assertEquals(2, reconciler.activeComponents().size());
		assertSame(c1, reconciler.activeComponents().get("item1"));
		assertSame(c2, reconciler.activeComponents().get("item2"));
		assertFalse(c1.isDisposed());
		assertFalse(c2.isDisposed());

		ParentStack.pop();
		reconciler.dispose();
		TestScheduler.flush();
	}

	@Test
	void regressionLayoutPassDoesNotRegisterReactiveDependencies() {
		Signal<String> textSignal = Signal.of("Hello");
		Text text = Text.of(textSignal);
		Label label = text.label();

		assertEquals(1, textSignal.observerCount(), "Text component creates an internal effect observing textSignal");

		// Run layout passes
		TestObserver observer = new TestObserver("layout-test");
		try {
			label.layout();
			label.getPrefWidth();
			label.getPrefHeight();
			label.act(0.016f);
		} finally {
			// Layout passes must not have invoked .get() inside any ambient tracking
			// observer count on textSignal must remain unchanged
			assertEquals(1, textSignal.observerCount());
			assertEquals(0, observer.getDependencies().size());
		}

		text.dispose();
		TestScheduler.flush();
		assertEquals(0, textSignal.observerCount());
	}
}
