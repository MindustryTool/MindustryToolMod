package solim.runtime;

import static org.junit.jupiter.api.Assertions.*;

import arc.scene.Element;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import solim.core.Component;

class StructuralReconcilerDisposalTest {

	static final class TestComponent implements Component {
		final String id;
		boolean disposed;

		TestComponent(String id) {
			this.id = id;
		}

		@Override
		public Element element() {
			return new Element();
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
	void removedKeysAreDisposedOnReconcile() {
		StructuralReconciler<String, TestComponent> reconciler = new StructuralReconciler<>();
		Map<String, TestComponent> first = reconciler.reconcile(
			Arrays.asList("A", "B"), item -> item, TestComponent::new);
		TestComponent compA = first.get("A");
		TestComponent compB = first.get("B");
		assertFalse(compA.isDisposed());
		assertFalse(compB.isDisposed());
		assertFalse(reconciler.isDisposed());

		Map<String, TestComponent> second = reconciler.reconcile(
			Arrays.asList("B", "C"), item -> item, TestComponent::new);
		assertTrue(compA.isDisposed(), "Removed key must be disposed");
		assertFalse(compB.isDisposed(), "Retained key must survive");
		assertSame(compB, second.get("B"), "Retained key must reuse the instance");
		TestComponent compC = second.get("C");
		assertFalse(reconciler.isDisposed());

		reconciler.dispose();
		assertTrue(reconciler.isDisposed());
		assertTrue(compB.isDisposed());
		assertTrue(compC.isDisposed());
	}

	@Test
	void disposeClearsActiveComponentsAndIsIdempotent() {
		StructuralReconciler<String, TestComponent> reconciler = new StructuralReconciler<>();
		reconciler.reconcile(Arrays.asList("A", "B"), item -> item, TestComponent::new);
		assertFalse(reconciler.isEmpty());

		reconciler.dispose();
		assertTrue(reconciler.isDisposed());
		assertTrue(reconciler.isEmpty());

		assertDoesNotThrow(reconciler::dispose, "Double dispose must be safe");
		assertTrue(reconciler.isDisposed());
	}

	@Test
	void emptyReconcileDisposesEverything() {
		StructuralReconciler<String, TestComponent> reconciler = new StructuralReconciler<>();
		Map<String, TestComponent> first = reconciler.reconcile(
			Arrays.asList("A", "B"), item -> item, TestComponent::new);
		List<TestComponent> created = new ArrayList<>(first.values());

		reconciler.reconcile(new ArrayList<String>(), item -> item, TestComponent::new);
		for (TestComponent comp : created) {
			assertTrue(comp.isDisposed(), "Clearing all items must dispose every component");
		}
		assertTrue(reconciler.isEmpty());
		reconciler.dispose();
	}

	@Test
	void duplicateKeysThrowIllegalArgumentExceptionBeforeBuilding() {
		StructuralReconciler<String, TestComponent> reconciler = new StructuralReconciler<>();
		List<String> built = new ArrayList<>();

		assertThrows(IllegalArgumentException.class, () ->
			reconciler.reconcile(
				Arrays.asList("A", "B", "A"),
				item -> item,
				item -> {
					built.add(item);
					return new TestComponent(item);
				}
			)
		);

		assertTrue(built.isEmpty(), "No components should be constructed if duplicate keys exist");
		assertTrue(reconciler.isEmpty());
	}

	@Test
	void factoryFailureRollsBackNewlyCreatedComponentsAndPreservesActive() {
		StructuralReconciler<String, TestComponent> reconciler = new StructuralReconciler<>();
		Map<String, TestComponent> initial = reconciler.reconcile(
			Arrays.asList("A", "B"), item -> item, TestComponent::new
		);
		TestComponent compA = initial.get("A");
		TestComponent compB = initial.get("B");

		List<TestComponent> newlyCreated = new ArrayList<>();

		assertThrows(RuntimeException.class, () ->
			reconciler.reconcile(
				Arrays.asList("B", "C", "FAIL"),
				item -> item,
				item -> {
					if ("FAIL".equals(item)) {
						throw new RuntimeException("Simulated factory failure");
					}
					TestComponent tc = new TestComponent(item);
					newlyCreated.add(tc);
					return tc;
				}
			)
		);

		// Active components must still contain the original A and B, untouched!
		assertEquals(2, reconciler.activeComponents().size());
		assertSame(compA, reconciler.activeComponents().get("A"));
		assertSame(compB, reconciler.activeComponents().get("B"));
		assertFalse(compA.isDisposed(), "Active component A must not be disposed on failed reconcile");
		assertFalse(compB.isDisposed(), "Active component B must not be disposed on failed reconcile");

		// Newly created component C must have been rolled back and disposed
		assertEquals(1, newlyCreated.size());
		TestComponent compC = newlyCreated.get(0);
		assertEquals("C", compC.id);
		assertTrue(compC.isDisposed(), "Newly created component C must be disposed on rollback");
	}
}
