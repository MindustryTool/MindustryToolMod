package solim.core;

import static org.junit.jupiter.api.Assertions.*;

import arc.scene.Element;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import solim.test.SolimEnv;

class ComponentTest extends SolimEnv {

	@Test
	void buildsOnlyOnceAndCachesElement() {
		AtomicInteger buildCount = new AtomicInteger();

		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				buildCount.incrementAndGet();
				return new Element();
			}
		};

		Element first = comp.element();
		Element second = comp.element();

		assertSame(first, second);
		assertEquals(1, buildCount.get());
	}

	@Test
	void appliesNameBeforeBuild() {
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				return new Element();
			}
		};

		comp.name("my-component");
		assertEquals("my-component", comp.element().name);
	}

	@Test
	void appliesNameAfterBuild() {
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				return new Element();
			}
		};

		comp.element();
		comp.name("renamed");
		assertEquals("renamed", comp.element().name);
	}

	@Test
	void disposesOwnedResourcesInReverseOrder() {
		StringBuilder order = new StringBuilder();

		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				own(DisposableAction.of(() -> order.append("first")));
				own(DisposableAction.of(() -> order.append("second")));
				own(DisposableAction.of(() -> order.append("third")));
				return new Element();
			}
		};

		comp.element();
		comp.dispose();

		assertEquals("thirdsecondfirst", order.toString());
	}

	@Test
	void disposeIsIdempotent() {
		AtomicInteger disposeCount = new AtomicInteger();

		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				own(DisposableAction.of(() -> disposeCount.incrementAndGet()));
				return new Element();
			}
		};

		comp.element();
		comp.dispose();
		comp.dispose();

		assertEquals(1, disposeCount.get());
	}

	@Test
	void markedAsDisposedAfterDispose() {
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				return new Element();
			}
		};

		assertFalse(comp.isDisposed());
		comp.element();
		comp.dispose();
		assertTrue(comp.isDisposed());
	}

	@Test
	void elementThrowsAfterDispose() {
		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				return new Element();
			}
		};

		comp.element();
		comp.dispose();

		assertThrows(IllegalStateException.class, comp::element);
	}

	@Test
	void buildNullThrowsAndDisposesResources() {
		AtomicInteger disposeCount = new AtomicInteger();

		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				own(DisposableAction.of(() -> disposeCount.incrementAndGet()));
				return null;
			}
		};

		assertThrows(IllegalStateException.class, comp::element);
		assertEquals(1, disposeCount.get());
	}

	@Test
	void onDisposeHookRunsDuringDispose() {
		AtomicInteger hookRuns = new AtomicInteger();

		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				return new Element();
			}

			@Override
			protected void onDispose() {
				hookRuns.incrementAndGet();
			}
		};

		comp.element();
		comp.dispose();

		assertEquals(1, hookRuns.get());
	}

	@Test
	void interfaceDefaultDisposeIsNoOpAndPreservesElement() {
		Element element = new Element();
		element.name = "original";
		Component comp = () -> element;

		comp.dispose();
		comp.dispose();

		assertSame(element, comp.element());
		assertEquals("original", comp.element().name);
	}


}
