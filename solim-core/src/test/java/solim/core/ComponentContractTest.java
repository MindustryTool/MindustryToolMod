package solim.core;

import static org.junit.jupiter.api.Assertions.*;

import arc.scene.Element;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import solim.reactive.Signal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import solim.runtime.SignalDispatcher;
import solim.reactive.Effect;

class ComponentContractTest {

	@Test
	void componentInterfaceDisposeIsNoOpAndPreservesElement() {
		Element element = new Element();
		element.name = "contract-element";
		Component comp = () -> element;

		comp.dispose();
		comp.dispose();

		assertSame(element, comp.element());
		assertEquals("contract-element", comp.element().name);
	}


	@Test
	void baseComponentElementCachesResult() {
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				return new Element();
			}
		};
		Element e1 = comp.element();
		Element e2 = comp.element();
		assertSame(e1, e2, "element() must return the same cached instance");
	}

	@Test
	void baseComponentBuildRunsOnce() {
		AtomicInteger buildCount = new AtomicInteger(0);
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				buildCount.incrementAndGet();
				return new Element();
			}
		};
		comp.element();
		comp.element();
		assertEquals(1, buildCount.get(), "build() must run exactly once");
	}

	@Test
	void baseComponentDisposeMarksDisposed() {
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				return new Element();
			}
		};
		assertFalse(comp.isDisposed());
		comp.dispose();
		assertTrue(comp.isDisposed());
	}

	@Test
	void baseComponentCannotUseAfterDispose() {
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				return new Element();
			}
		};
		comp.dispose();
		assertThrows(IllegalStateException.class, comp::element);
	}

	@Test
	void baseComponentDoubleDisposeIsSafe() {
		AtomicInteger disposeCount = new AtomicInteger(0);
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				own(DisposableAction.of(disposeCount::incrementAndGet));
				return new Element();
			}
		};
		comp.element();
		comp.dispose();
		comp.dispose();
		assertEquals(1, disposeCount.get(), "dispose() must be idempotent");
	}

	@Test
	void baseComponentOwnAddsDisposable() {
		AtomicBoolean disposed = new AtomicBoolean(false);
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				own(DisposableAction.of(() -> disposed.set(true)));
				return new Element();
			}
		};
		comp.element();
		assertFalse(disposed.get());
		comp.dispose();
		assertTrue(disposed.get(), "owned resource must be disposed");
	}

	@Test
	void baseComponentDisposalOrderIsLIFO() {
		List<String> order = new ArrayList<>();
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				own(DisposableAction.of(() -> order.add("first")));
				own(DisposableAction.of(() -> order.add("second")));
				own(DisposableAction.of(() -> order.add("third")));
				return new Element();
			}
		};
		comp.element();
		comp.dispose();
		assertEquals(Arrays.asList("third", "second", "first"), order);
	}

	@Test
	void baseComponentFailedBuildDisposesPartialResources() {
		AtomicBoolean disposed = new AtomicBoolean(false);
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				own(DisposableAction.of(() -> disposed.set(true)));
				throw new RuntimeException("build failed");
			}
		};
		assertThrows(RuntimeException.class, comp::element);
		assertTrue(disposed.get(), "partial resources must be disposed on build failure");
		assertTrue(comp.isDisposed());
	}

	@Test
	void baseComponentNullBuildThrowsAndDisposes() {
		AtomicBoolean disposed = new AtomicBoolean(false);
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				own(DisposableAction.of(() -> disposed.set(true)));
				return null;
			}
		};
		assertThrows(IllegalStateException.class, comp::element);
		assertTrue(disposed.get());
		assertTrue(comp.isDisposed());
	}

	@Test
	void baseComponentNameBeforeBuild() {
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				return new Element();
			}
		};
		comp.name("before");
		assertEquals("before", comp.element().name);
	}

	@Test
	void baseComponentNameAfterBuild() {
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				return new Element();
			}
		};
		comp.element();
		comp.name("after");
		assertEquals("after", comp.element().name);
	}

	@Test
	void baseComponentEffectAutoRegisteredInsideBuild() {
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
		assertEquals(2, runs.get(), "Effect must be disposed with component");
	}
}
