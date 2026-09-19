package solim.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.Element;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import solim.reactive.Computed;
import solim.reactive.Effect;
import solim.reactive.Signal;
import solim.reactive.TwoWayBinding;
import solim.runtime.ComponentContext;
import solim.runtime.StructuralReconciler;
import solim.test.SolimTestHarness;
import solim.test.TestComponent;
import solim.test.TestDisposable;

public class LifecycleAndOwnershipRegressionTest extends SolimTestHarness {

	@Test
	void lazyBuildExecutesOnlyOnFirstElementCall() {
		AtomicInteger builds = new AtomicInteger(0);
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				builds.incrementAndGet();
				Element el = new Element();
				el.name = "lazy-target";
				return el;
			}
		};

		assertEquals(0, builds.get(), "build() must not be executed upon instantiation");

		Element el1 = comp.element();
		assertEquals(1, builds.get(), "build() must execute on first element() call");
		assertEquals("lazy-target", el1.name);

		Element el2 = comp.element();
		assertEquals(1, builds.get(), "build() must not re-run on subsequent element() calls");
		assertSame(el1, el2, "Cached element must be returned");
	}

	@Test
	void elementAccessAfterDisposalThrowsDescriptiveException() {
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				return new Element();
			}
		};

		comp.element();
		comp.dispose();
		assertTrue(comp.isDisposed());

		IllegalStateException ex = assertThrows(IllegalStateException.class, comp::element);
		assertTrue(ex.getMessage().contains("Cannot use disposed component"));
	}

	@Test
	void lifoDisposalOrderWithDeepNesting() {
		List<String> disposalLog = new ArrayList<>();

		BaseComponent parent = new BaseComponent() {
			@Override
			protected Element build() {
				own(new TestDisposable("parent-res-1") {
					@Override
					public void dispose() {
						super.dispose();
						disposalLog.add("parent-res-1");
					}
				});

				new BaseComponent() {
					@Override
					protected Element build() {
						own(new TestDisposable("child-res-1") {
							@Override
							public void dispose() {
								super.dispose();
								disposalLog.add("child-res-1");
							}
						});
						own(new TestDisposable("child-res-2") {
							@Override
							public void dispose() {
								super.dispose();
								disposalLog.add("child-res-2");
							}
						});
						return new Element();
					}
				}.element();

				own(new TestDisposable("parent-res-2") {
					@Override
					public void dispose() {
						super.dispose();
						disposalLog.add("parent-res-2");
					}
				});

				return new Element();
			}
		};

		parent.element();
		parent.dispose();

		assertEquals(
				Arrays.asList("parent-res-2", "child-res-2", "child-res-1", "parent-res-1"),
				disposalLog,
				"Nested owned components and resources must be disposed in strict reverse registration order (LIFO)"
		);
	}

	@Test
	void disposalErrorIsolationPreservesRemainingDisposables() {
		List<String> disposed = new ArrayList<>();
		TestDisposable.resetSequence();

		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				own(DisposableAction.of(() -> disposed.add("res-1")));
				own(DisposableAction.of(() -> {
					disposed.add("failing-res-1");
					throw new RuntimeException("Simulated error 1");
				}));
				own(DisposableAction.of(() -> disposed.add("res-2")));
				own(DisposableAction.of(() -> {
					disposed.add("failing-res-2");
					throw new RuntimeException("Simulated error 2");
				}));
				own(DisposableAction.of(() -> disposed.add("res-3")));
				return new Element();
			}
		};

		comp.element();
		comp.dispose();

		assertEquals(
				Arrays.asList("res-3", "failing-res-2", "res-2", "failing-res-1", "res-1"),
				disposed,
				"All disposables must be invoked in reverse order even when multiple disposables throw"
		);
		assertTrue(comp.isDisposed());
	}

	@Test
	void partialBuildFailureCleansUpAndUnwindsContext() {
		List<String> cleanupLog = new ArrayList<>();

		assertThrows(RuntimeException.class, () -> {
			new BaseComponent() {
				@Override
				protected Element build() {
					own(DisposableAction.of(() -> cleanupLog.add("first")));
					own(DisposableAction.of(() -> cleanupLog.add("second")));
					throw new RuntimeException("Crash during build");
				}
			}.element();
		});

		assertEquals(Arrays.asList("second", "first"), cleanupLog,
				"Partially registered resources must be cleaned up in LIFO order upon build exception");
		assertEquals(0, ComponentContext.size(), "ComponentContext must be empty after partial build failure");
	}

	@Test
	void disposableContractAcrossFrameworkImplementations() {
		// 1. DisposableAction
		AtomicInteger actionRuns = new AtomicInteger(0);
		DisposableAction da = DisposableAction.of(actionRuns::incrementAndGet);
		assertFalse(da.isDisposed());
		da.dispose();
		assertTrue(da.isDisposed());
		assertEquals(1, actionRuns.get());
		da.dispose();
		assertEquals(1, actionRuns.get(), "Repeated dispose must be a no-op");

		// 2. Computed
		Signal<Integer> sig = Signal.of(10);
		Computed<Integer> computed = sig.map(v -> v * 2);
		assertFalse(computed.isDisposed());
		assertEquals(20, (int) computed.get());
		computed.dispose();
		assertTrue(computed.isDisposed());
		computed.dispose();
		assertTrue(computed.isDisposed());

		// 3. Effect
		AtomicInteger effectRuns = new AtomicInteger(0);
		Effect effect = Effect.of(() -> {
			effectRuns.incrementAndGet();
			sig.get();
		});
		assertFalse(effect.isDisposed());
		effect.dispose();
		assertTrue(effect.isDisposed());
		effect.dispose();
		assertTrue(effect.isDisposed());

		// 4. TwoWayBinding
		Signal<String> textSig = Signal.of("init");
		TwoWayBinding<String> twb = new TwoWayBinding<>(
				textSig,
				textSig::get,
				val -> {},
				listener -> DisposableAction.empty()
		);
		assertFalse(twb.isDisposed());
		twb.dispose();
		assertTrue(twb.isDisposed());
		twb.dispose();
		assertTrue(twb.isDisposed());

		// 5. StructuralReconciler
		StructuralReconciler<String, TestComponent> reconciler = new StructuralReconciler<>();
		assertFalse(reconciler.isDisposed());
		reconciler.dispose();
		assertTrue(reconciler.isDisposed());
		reconciler.dispose();
		assertTrue(reconciler.isDisposed());
	}
}
