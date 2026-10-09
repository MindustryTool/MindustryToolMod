package solim.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import arc.Application;
import arc.Core;
import arc.mock.MockApplication;
import solim.test.SolimEnv;

class ReactivityFailFastTest extends SolimEnv {

	@Test
	void computedThrowsWhenSupplierFails() {
		Signal<Integer> count = Signal.of(1);
		Computed<String> computed = Signal.computed(() -> {
			if (count.get() < 0) {
				throw new IllegalArgumentException("Negative count not allowed: " + count.get());
			}
			return "Count: " + count.get();
		});

		assertEquals("Count: 1", computed.get());

		count.set(-5);
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, computed::get);
		assertEquals("Negative count not allowed: -5", thrown.getMessage());
	}

	@Test
	void signalPropagatesSubscriberException() {
		Signal<String> signal = Signal.of("initial");
		signal.subscribe(val -> {
			if ("bad".equals(val)) {
				throw new IllegalStateException("Bad value: " + val);
			}
		});

		IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> signal.set("bad"));
		assertEquals("Bad value: bad", thrown.getMessage());
	}

	@Test
	void mapSignalPropagatesObserverException() {
		MapSignal<String, Integer> mapSignal = new MapSignal<>();
		mapSignal.put("k1", 10);
		Readable<Integer> item = mapSignal.readable("k1");

		// Observing item via a Computed that has a subscriber ensures eager recomputation during invalidation
		Computed<String> derived = Signal.computed(() -> {
			Integer v = item.get();
			if (v != null && v > 50) {
				throw new RuntimeException("Overflow value: " + v);
			}
			return "val:" + v;
		});
		// Initialize the computed so it subscribes to KeyReadable
		derived.get();
		derived.subscribe(val -> {}); // Forces eager recomputation on invalidate

		RuntimeException thrown = assertThrows(RuntimeException.class, () -> mapSignal.put("k1", 100));
		assertEquals("Overflow value: 100", thrown.getMessage());
	}

	@Test
	void mutationPropagatesOnSuccessException() {
		Application originalApp = Core.app;
		AtomicReference<Throwable> postedError = new AtomicReference<>();
		Core.app = new MockApplication() {
			@Override
			public void post(Runnable r) {
				try {
					r.run();
				} catch (Throwable t) {
					postedError.set(t);
					throw t;
				}
			}
		};

		try {
			CompletableFuture<String> future = new CompletableFuture<>();
			Mutation<String, String> mutation = Mutation.<String, String>of(input -> future)
					.onSuccess((res, ctx) -> {
						throw new IllegalStateException("Success callback failed for " + res);
					});

			mutation.mutate("test");
			future.complete("test_result");

			assertEquals(IllegalStateException.class, postedError.get().getClass());
			assertEquals("Success callback failed for test_result", postedError.get().getMessage());
		} finally {
			Core.app = originalApp;
		}
	}

	@Test
	void mutationPropagatesOnErrorException() {
		Application originalApp = Core.app;
		AtomicReference<Throwable> postedError = new AtomicReference<>();
		Core.app = new MockApplication() {
			@Override
			public void post(Runnable r) {
				try {
					r.run();
				} catch (Throwable t) {
					postedError.set(t);
					throw t;
				}
			}
		};

		try {
			CompletableFuture<String> future = new CompletableFuture<>();
			Mutation<String, String> mutation = Mutation.<String, String>of(input -> future)
					.onError((err, ctx) -> {
						throw new IllegalStateException("Error callback failed");
					});

			mutation.mutate("test");
			future.completeExceptionally(new RuntimeException("Root error"));

			assertEquals(IllegalStateException.class, postedError.get().getClass());
			assertEquals("Error callback failed", postedError.get().getMessage());
		} finally {
			Core.app = originalApp;
		}
	}
}
