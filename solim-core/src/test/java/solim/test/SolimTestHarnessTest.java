package solim.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.Element;
import org.junit.jupiter.api.Test;
import solim.reactive.Effect;
import solim.reactive.Signal;
import solim.runtime.ComponentContext;
import solim.runtime.ParentStack;
import solim.runtime.ReactiveContext;
import solim.runtime.SignalDispatcher;

class SolimTestHarnessTest extends SolimTestHarness {

	@Test
	void harnessEnsuresCleanAmbientContexts() {
		assertEquals(0, ParentStack.size());
		assertEquals(0, ComponentContext.size());
		assertEquals(0, ReactiveContext.size());
		assertEquals(0, SignalDispatcher.size());
		assertFalse(SignalDispatcher.isFlushing());
	}

	@Test
	void testDisposableTracksLifecycleAndOrder() {
		TestDisposable.resetSequence();
		TestDisposable d1 = new TestDisposable("d1");
		TestDisposable d2 = new TestDisposable("d2");

		assertFalse(d1.isDisposed());
		assertFalse(d2.isDisposed());

		d2.dispose();
		d1.dispose();

		assertTrue(d1.isDisposed());
		assertTrue(d2.isDisposed());
		assertEquals(1, d2.getDisposalOrder());
		assertEquals(2, d1.getDisposalOrder());
		assertEquals(1, d1.getDisposeCount());

		// Idempotency
		d1.dispose();
		assertEquals(2, d1.getDisposeCount());
		assertTrue(d1.isDisposed());
	}

	@Test
	void testDisposableFailingThrows() {
		TestDisposable failing = TestDisposable.failing("error");
		assertThrows(RuntimeException.class, failing::dispose);
		assertTrue(failing.isDisposed());
	}

	@Test
	void testComponentSingleMaterializationAndDisposal() {
		TestDisposable owned = new TestDisposable("owned");
		TestComponent comp = new TestComponent("test-comp").withDisposable(owned);

		assertEquals(0, comp.getBuildCount());
		Element el1 = comp.element();
		Element el2 = comp.element();

		assertSame(el1, el2);
		assertEquals(1, comp.getBuildCount());
		assertFalse(comp.isDisposed());
		assertFalse(owned.isDisposed());

		comp.dispose();

		assertTrue(comp.isDisposed());
		assertTrue(owned.isDisposed());
		assertEquals(1, comp.getDisposeCount());
	}

	@Test
	void testSchedulerFlushesDeterministically() {
		Signal<Integer> sig = Signal.of(10);
		int[] runs = new int[1];

		Effect effect = Effect.of(() -> {
			runs[0]++;
			sig.get();
		});

		assertEquals(1, runs[0]);
		sig.set(20);
		assertEquals(1, TestScheduler.pendingCount());

		TestScheduler.flush();

		assertEquals(2, runs[0]);
		assertEquals(0, TestScheduler.pendingCount());

		effect.dispose();
	}

	@Test
	void testObserverRecordsDependenciesAndInvalidations() {
		TestObserver observer = new TestObserver("obs");
		Signal<String> sig1 = Signal.of("A");
		Signal<String> sig2 = Signal.of("B");

		ReactiveContext.push(observer);
		ReactiveContext.track(sig1);
		ReactiveContext.track(sig2);
		ReactiveContext.pop();

		assertEquals(2, observer.getDependencies().size());
		assertSame(sig1, observer.getDependencies().get(0));
		assertSame(sig2, observer.getDependencies().get(1));

		observer.invalidate();
		assertEquals(1, observer.getInvalidateCount());
	}
}
