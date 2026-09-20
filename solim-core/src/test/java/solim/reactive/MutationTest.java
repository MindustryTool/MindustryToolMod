package solim.reactive;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MutationTest {

	@Test
	void initialAndSuccessState() {
		CompletableFuture<String> future = new CompletableFuture<>();
		Mutation<String, String> mutation = Mutation.of(input -> future);

		assertFalse(mutation.isPending().get(), "Mutation should not be pending initially");
		assertNull(mutation.error().get());
		assertNull(mutation.result().get());

		mutation.mutate("hello");
		assertTrue(mutation.isPending().get(), "Mutation should be pending during execution");

		future.complete("HELLO");

		assertFalse(mutation.isPending().get());
		assertEquals("HELLO", mutation.result().get());
		assertNull(mutation.error().get());
	}

	@Test
	void errorState() {
		CompletableFuture<String> future = new CompletableFuture<>();
		Mutation<String, String> mutation = Mutation.of(input -> future);
		RuntimeException failure = new RuntimeException("Server error");

		mutation.mutate("fail");
		future.completeExceptionally(failure);

		assertFalse(mutation.isPending().get());
		assertSame(failure, mutation.error().get());
		assertNull(mutation.result().get());
	}

	@Test
	void optimisticUpdateHooks() {
		CompletableFuture<String> future = new CompletableFuture<>();
		AtomicReference<String> optimisticContext = new AtomicReference<>();
		AtomicReference<String> successResult = new AtomicReference<>();
		AtomicReference<String> successContext = new AtomicReference<>();

		Mutation<String, String> mutation = Mutation.<String, String>of(input -> future)
				.onMutate(input -> {
					String tempId = "temp_" + input;
					optimisticContext.set(tempId);
					return tempId;
				})
				.onSuccess((String res, String ctx) -> {
					successResult.set(res);
					successContext.set(ctx);
				});

		mutation.mutate("msg1");

		assertEquals("temp_msg1", optimisticContext.get(), "onMutate should execute synchronously");
		assertNull(successResult.get());

		future.complete("msg1_confirmed");

		assertEquals("msg1_confirmed", successResult.get());
		assertEquals("temp_msg1", successContext.get());
	}

	@Test
	void errorRollbackHook() {
		CompletableFuture<String> future = new CompletableFuture<>();
		AtomicReference<String> errorContext = new AtomicReference<>();
		AtomicReference<Throwable> caughtError = new AtomicReference<>();

		Mutation<String, String> mutation = Mutation.<String, String>of(input -> future)
				.onMutate(input -> "temp_token")
				.onError((Throwable err, String ctx) -> {
					caughtError.set(err);
					errorContext.set(ctx);
				});

		mutation.mutate("msg2");
		RuntimeException ex = new RuntimeException("failed");
		future.completeExceptionally(ex);

		assertSame(ex, caughtError.get());
		assertEquals("temp_token", errorContext.get());
	}

	@Test
	void concurrentMutationDiscardsEarlierResult() {
		CompletableFuture<String> slow = new CompletableFuture<>();
		CompletableFuture<String> fast = CompletableFuture.completedFuture("second_result");

		AtomicBoolean firstInvoked = new AtomicBoolean(false);
		Mutation<String, String> mutation = Mutation.of(input -> {
			if (!firstInvoked.getAndSet(true)) {
				return slow;
			}
			return fast;
		});

		mutation.mutate("first");
		assertTrue(mutation.isPending().get());

		mutation.mutate("second");
		assertEquals("second_result", mutation.result().get());

		// Slow completes later: must not overwrite
		slow.complete("first_result");
		assertEquals("second_result", mutation.result().get());
	}

	@Test
	void resetAndDisposal() {
		Mutation<String, String> mutation = Mutation.of(CompletableFuture::completedFuture);
		mutation.mutate("val");
		assertEquals("val", mutation.result().get());

		mutation.reset();
		assertFalse(mutation.isPending().get());
		assertNull(mutation.result().get());
		assertNull(mutation.error().get());

		assertFalse(mutation.isDisposed());
		mutation.dispose();
		assertTrue(mutation.isDisposed());
	}
}
