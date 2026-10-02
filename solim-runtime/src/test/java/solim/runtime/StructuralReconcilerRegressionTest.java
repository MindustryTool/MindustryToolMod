package solim.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.Element;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import solim.core.Component;

public class StructuralReconcilerRegressionTest {

	static class ReconcilerTestComponent implements Component {
		final String key;
		boolean disposed = false;
		int buildCount = 0;
		Element element;

		ReconcilerTestComponent(String key) {
			this.key = key;
		}

		@Override
		public Element element() {
			if (element == null) {
				buildCount++;
				element = new Element();
				element.name = key;
			}
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
	void keyedDiffingFullLifecycle() {
		StructuralReconciler<String, ReconcilerTestComponent> reconciler = new StructuralReconciler<>();

		// 1. Initial creation: [A, B, C]
		Map<String, ReconcilerTestComponent> m1 = reconciler.reconcile(
				Arrays.asList("A", "B", "C"),
				k -> k,
				ReconcilerTestComponent::new
		);
		assertEquals(3, m1.size());
		ReconcilerTestComponent compA = m1.get("A");
		ReconcilerTestComponent compB = m1.get("B");
		ReconcilerTestComponent compC = m1.get("C");

		// 2. Full reuse with no changes: [A, B, C]
		Map<String, ReconcilerTestComponent> m2 = reconciler.reconcile(
				Arrays.asList("A", "B", "C"),
				k -> k,
				ReconcilerTestComponent::new
		);
		assertSame(compA, m2.get("A"));
		assertSame(compB, m2.get("B"));
		assertSame(compC, m2.get("C"));

		// 3. Reorder: [C, A, B]
		Map<String, ReconcilerTestComponent> m3 = reconciler.reconcile(
				Arrays.asList("C", "A", "B"),
				k -> k,
				ReconcilerTestComponent::new
		);
		assertSame(compC, m3.get("C"));
		assertSame(compA, m3.get("A"));
		assertSame(compB, m3.get("B"));
		assertFalse(compA.isDisposed());
		assertFalse(compB.isDisposed());
		assertFalse(compC.isDisposed());

		// 4. Removal: [B]
		Map<String, ReconcilerTestComponent> m4 = reconciler.reconcile(
				Arrays.asList("B"),
				k -> k,
				ReconcilerTestComponent::new
		);
		assertEquals(1, m4.size());
		assertSame(compB, m4.get("B"));
		assertTrue(compA.isDisposed(), "Component A must be disposed on removal");
		assertTrue(compC.isDisposed(), "Component C must be disposed on removal");
		assertFalse(compB.isDisposed(), "Retained component B must not be disposed");

		// 5. Total clear: []
		Map<String, ReconcilerTestComponent> m5 = reconciler.reconcile(
				new ArrayList<String>(),
				k -> k,
				ReconcilerTestComponent::new
		);
		assertTrue(m5.isEmpty());
		assertTrue(compB.isDisposed(), "Component B must be disposed when collection cleared");

		reconciler.dispose();
		assertTrue(reconciler.isDisposed());
	}

	@Test
	void duplicateKeysGracefullyIgnoredFirstWins() {
		StructuralReconciler<String, ReconcilerTestComponent> reconciler = new StructuralReconciler<>();

		// 1. Initial list with duplicates [x, y, x]
		List<String> built = new ArrayList<>();
		Map<String, ReconcilerTestComponent> res1 = reconciler.reconcile(
				Arrays.asList("x", "y", "x"),
				k -> k,
				k -> {
					built.add(k);
					return new ReconcilerTestComponent(k);
				}
		);
		assertEquals(Arrays.asList("x", "y"), built, "Only first occurrence of each key must be constructed");
		assertEquals(2, res1.size());
		assertEquals(2, reconciler.activeComponents().size());

		// 2. Populate state [A, B]
		reconciler.reconcile(Arrays.asList("A", "B"), k -> k, ReconcilerTestComponent::new);
		assertEquals(2, reconciler.activeComponents().size());
		assertTrue(reconciler.activeComponents().containsKey("A"));
		assertTrue(reconciler.activeComponents().containsKey("B"));

		// 3. Update with duplicate keys [A, C, C]
		Map<String, ReconcilerTestComponent> res3 = reconciler.reconcile(
				Arrays.asList("A", "C", "C"),
				k -> k,
				ReconcilerTestComponent::new
		);

		// Must retain A and create C once; B must be removed
		assertEquals(2, res3.size());
		assertTrue(res3.containsKey("A"));
		assertTrue(res3.containsKey("C"));
		assertFalse(res3.containsKey("B"));

		reconciler.dispose();
	}

	@Test
	void transactionalRollbackWhenFactoryThrowsMidReconciliation() {
		StructuralReconciler<String, ReconcilerTestComponent> reconciler = new StructuralReconciler<>();

		// Commit initial state [A, B]
		Map<String, ReconcilerTestComponent> initial = reconciler.reconcile(
				Arrays.asList("A", "B"),
				k -> k,
				ReconcilerTestComponent::new
		);
		ReconcilerTestComponent compA = initial.get("A");
		ReconcilerTestComponent compB = initial.get("B");

		List<ReconcilerTestComponent> createdInFailedAttempt = new ArrayList<>();

		// Reconcile to [A, C, FAIL, D]
		assertThrows(RuntimeException.class, () -> {
			reconciler.reconcile(
					Arrays.asList("A", "C", "FAIL", "D"),
					k -> k,
					k -> {
						if ("FAIL".equals(k)) {
							throw new RuntimeException("Simulated factory crash on key: " + k);
						}
						ReconcilerTestComponent comp = new ReconcilerTestComponent(k);
						createdInFailedAttempt.add(comp);
						return comp;
					}
			);
		});

		// Invariants:
		// 1. C was created, so C must have been rolled back and disposed
		assertEquals(1, createdInFailedAttempt.size());
		ReconcilerTestComponent compC = createdInFailedAttempt.get(0);
		assertEquals("C", compC.key);
		assertTrue(compC.isDisposed(), "Partially created component C must be disposed during rollback");

		// 2. A and B must remain active and undisposed
		assertEquals(2, reconciler.activeComponents().size());
		assertSame(compA, reconciler.activeComponents().get("A"));
		assertSame(compB, reconciler.activeComponents().get("B"));
		assertFalse(compA.isDisposed());
		assertFalse(compB.isDisposed());

		// 3. Subsequent reconciliation succeeds cleanly
		Map<String, ReconcilerTestComponent> recovered = reconciler.reconcile(
				Arrays.asList("A", "E"),
				k -> k,
				ReconcilerTestComponent::new
		);
		assertEquals(2, recovered.size());
		assertSame(compA, recovered.get("A"));
		assertTrue(compB.isDisposed(), "B is now removed and must be disposed");
		assertFalse(recovered.get("E").isDisposed());

		reconciler.dispose();
	}
}
