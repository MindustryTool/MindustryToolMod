package solim.core;

import solim.runtime.ComponentContext;

import static org.junit.jupiter.api.Assertions.*;

import arc.scene.Element;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import solim.signal.Effect;
import solim.signal.Signal;
import solim.runtime.SignalDispatcher;
import solim.runtime.StructuralReconciler;

class LifecycleCorrectnessTest {

	@Test
	void disposalOrderIsLIFO() {
		List<String> order = new ArrayList<>();
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				own(() -> order.add("first"));
				own(() -> order.add("second"));
				own(() -> order.add("third"));
				return new Element();
			}
		};

		comp.element();
		comp.dispose();

		assertEquals(Arrays.asList("third", "second", "first"), order,
				"Resources must be disposed in reverse registration order (LIFO)");
	}

	@Test
	void errorIsolationDuringDisposal() {
		List<String> disposed = new ArrayList<>();
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				own(() -> disposed.add("first"));
				own(() -> {
					disposed.add("failing");
					throw new RuntimeException("Simulated disposal failure");
				});
				own(() -> disposed.add("third"));
				return new Element();
			}
		};

		comp.element();
		assertDoesNotThrow(comp::dispose, "Disposal should not throw even if a disposable fails");
		assertEquals(Arrays.asList("third", "failing", "first"), disposed,
				"All disposables must be attempted even if one throws");
	}

	@Test
	void cannotBuildDisposedComponent() {
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				return new Element();
			}
		};

		comp.dispose();
		assertThrows(IllegalStateException.class, comp::element,
				"Calling element() after dispose() must throw IllegalStateException");
	}

	@Test
	void doubleDisposalIsSafe() {
		AtomicInteger count = new AtomicInteger(0);
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				own(count::incrementAndGet);
				return new Element();
			}
		};

		comp.element();
		comp.dispose();
		assertEquals(1, count.get());
		comp.dispose();
		assertEquals(1, count.get(), "Second dispose() must be a no-op");
	}

	@Test
	void failedBuildDisposesPartialResources() {
		AtomicBoolean resourceDisposed = new AtomicBoolean(false);
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				own(() -> resourceDisposed.set(true));
				throw new RuntimeException("Build crashed");
			}
		};

		assertThrows(RuntimeException.class, comp::element);
		assertTrue(resourceDisposed.get(), "Partially registered resources must be disposed when build() throws");
		assertTrue(comp.isDisposed());
		assertEquals(0, ComponentContext.size(), "ComponentContext must be popped after build failure");
	}

	@Test
	void nullBuildReturnsThrowsAndDisposes() {
		AtomicBoolean resourceDisposed = new AtomicBoolean(false);
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				own(() -> resourceDisposed.set(true));
				return null;
			}
		};

		assertThrows(IllegalStateException.class, comp::element);
		assertTrue(resourceDisposed.get(), "Resources must be disposed if build() returns null");
		assertTrue(comp.isDisposed());
		assertEquals(0, ComponentContext.size());
	}

	@Test
	void effectAutoRegisteredInsideBuild() {
		Signal<Integer> sig = Signal.of(0);
		AtomicInteger runs = new AtomicInteger(0);

		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				Effect.of(() -> {
					sig.get();
					runs.incrementAndGet();
				});
				return new Element();
			}
		};

		comp.element();
		assertEquals(1, runs.get());

		sig.set(1);
		SignalDispatcher.flush();
		assertEquals(2, runs.get());

		comp.dispose();
		sig.set(2);
		SignalDispatcher.flush();
		assertEquals(2, runs.get(), "Auto-registered Effect must be disposed with component");
	}

	@Test
	void effectNotAutoRegisteredOutsideBuild() {
		Signal<Integer> sig = Signal.of(0);
		AtomicInteger runs = new AtomicInteger(0);

		Effect e = Effect.of(() -> {
			sig.get();
			runs.incrementAndGet();
		});

		assertEquals(1, runs.get());
		sig.set(1);
		SignalDispatcher.flush();
		assertEquals(2, runs.get());

		e.dispose();
		sig.set(2);
		SignalDispatcher.flush();
		assertEquals(2, runs.get());
	}

	@Test
	void withoutAutoOwnershipSuppressesRegistration() {
		AtomicBoolean childDisposed = new AtomicBoolean(false);
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				ComponentContext.withoutAutoOwnership(() -> {
					new BaseComponent() {
						@Override
						protected Element build() {
							return new Element();
						}

						@Override
						protected void onDispose() {
							childDisposed.set(true);
						}
					};
				});
				return new Element();
			}
		};

		comp.element();
		comp.dispose();
		assertFalse(childDisposed.get(), "Component created inside withoutAutoOwnership must not be owned");
	}

	@Test
	void withoutAutoOwnershipRestoresOnException() {
		assertThrows(RuntimeException.class, () -> {
			ComponentContext.withoutAutoOwnership(() -> {
				throw new RuntimeException("boom");
			});
		});

		AtomicBoolean owned = new AtomicBoolean(false);
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				own(() -> owned.set(true));
				return new Element();
			}
		};
		comp.element();
		comp.dispose();
		assertTrue(owned.get(), "Auto-ownership must be restored even if withoutAutoOwnership lambda throws");
	}

	@Test
	void structuralReconcilerKeyedReuseAndDisposal() {
		StructuralReconciler<String, TestComp> reconciler = new StructuralReconciler<>();
		List<String> disposed = new ArrayList<>();

		// Initial reconciliation: A, B
		Map<String, TestComp> map1 = reconciler.reconcile(
				Arrays.asList("A", "B"),
				k -> k,
				k -> new TestComp(k, () -> disposed.add(k))
		);

		assertEquals(2, map1.size());
		TestComp compA = map1.get("A");
		TestComp compB = map1.get("B");
		assertEquals("A", compA.id);
		assertEquals("B", compB.id);

		// Second reconciliation: B, C (A removed, B reused, C added)
		Map<String, TestComp> map2 = reconciler.reconcile(
				Arrays.asList("B", "C"),
				k -> k,
				k -> new TestComp(k, () -> disposed.add(k))
		);

		assertEquals(2, map2.size());
		assertSame(compB, map2.get("B"), "Existing key must reuse the same component instance");
		assertEquals(Arrays.asList("A"), disposed, "Removed key must be disposed");

		// Total cleanup
		reconciler.dispose();
		assertTrue(disposed.contains("B"));
		assertTrue(disposed.contains("C"));
	}

	static class TestComp extends BaseComponent {
		final String id;
		final Runnable onDisposed;

		TestComp(String id, Runnable onDisposed) {
			this.id = id;
			this.onDisposed = onDisposed;
		}

		@Override
		protected Element build() {
			return new Element();
		}

		@Override
		protected void onDispose() {
			if (onDisposed != null) {
				onDisposed.run();
			}
		}
	}
}
