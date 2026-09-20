package solim.reactive;

import arc.Core;
import arc.util.Nullable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import solim.core.Disposable;
import solim.runtime.ComponentContext;

/**
 * Asynchronous reactive action primitive for write operations, with pending/error/result
 * state tracking and optimistic update lifecycle hooks.
 *
 * @param <T> input parameter type
 * @param <R> mutation result type
 */
public final class Mutation<T, R> implements Disposable {

	private final Function<T, CompletableFuture<R>> mutator;

	private final Signal<Boolean> pending = Signal.of(false);
	private final Signal<Throwable> error = Signal.of(null);
	private final Signal<R> result = Signal.of(null);

	private @Nullable Function<T, ?> onMutate;
	private @Nullable BiConsumer<R, Object> onSuccess;
	private @Nullable BiConsumer<Throwable, Object> onError;

	private int mutationGeneration = 0;
	private boolean disposed = false;

	private Mutation(Function<T, CompletableFuture<R>> mutator) {
		this.mutator = Objects.requireNonNull(mutator, "mutator cannot be null");
		ComponentContext.register(this);
	}

	public static <T, R> Mutation<T, R> of(Function<T, CompletableFuture<R>> mutator) {
		return new Mutation<>(mutator);
	}

	public static <R> Mutation<Void, R> of(Supplier<CompletableFuture<R>> mutator) {
		Objects.requireNonNull(mutator, "mutator cannot be null");
		return new Mutation<>(ignored -> mutator.get());
	}

	public <C> Mutation<T, R> onMutate(Function<T, C> onMutate) {
		this.onMutate = onMutate;
		return this;
	}

	public Mutation<T, R> onMutate(Consumer<T> onMutate) {
		if (onMutate != null) {
			this.onMutate = input -> {
				onMutate.accept(input);
				return null;
			};
		} else {
			this.onMutate = null;
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	public <C> Mutation<T, R> onSuccess(BiConsumer<R, C> onSuccess) {
		this.onSuccess = (BiConsumer<R, Object>) onSuccess;
		return this;
	}

	public Mutation<T, R> onSuccess(Consumer<R> onSuccess) {
		if (onSuccess != null) {
			this.onSuccess = (res, ctx) -> onSuccess.accept(res);
		} else {
			this.onSuccess = null;
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	public <C> Mutation<T, R> onError(BiConsumer<Throwable, C> onError) {
		this.onError = (BiConsumer<Throwable, Object>) onError;
		return this;
	}

	public Mutation<T, R> onError(Consumer<Throwable> onError) {
		if (onError != null) {
			this.onError = (err, ctx) -> onError.accept(err);
		} else {
			this.onError = null;
		}
		return this;
	}

	public void mutate(T input) {
		if (disposed) return;

		int gen = ++mutationGeneration;
		pending.set(true);
		error.set(null);

		Object context = null;
		if (onMutate != null) {
			try {
				context = onMutate.apply(input);
			} catch (Throwable t) {
				handleError(gen, t, null);
				return;
			}
		}

		CompletableFuture<R> future;
		try {
			future = mutator.apply(input);
		} catch (Throwable t) {
			handleError(gen, t, context);
			return;
		}

		final Object finalContext = context;
		future.whenComplete((res, throwable) -> {
			postToMain(() -> {
				if (disposed || gen != mutationGeneration) {
					return;
				}
				if (throwable != null) {
					handleError(gen, throwable, finalContext);
				} else {
					handleSuccess(gen, res, finalContext);
				}
			});
		});
	}

	public void mutate() {
		mutate(null);
	}

	private void handleSuccess(int gen, R res, @Nullable Object context) {
		if (disposed || gen != mutationGeneration) return;

		pending.set(false);
		result.set(res);
		error.set(null);

		if (onSuccess != null) {
			try {
				onSuccess.accept(res, context);
			} catch (Throwable t) {
				// Prevent callback errors from corrupting state
			}
		}
	}

	private void handleError(int gen, Throwable throwable, @Nullable Object context) {
		if (disposed || gen != mutationGeneration) return;

		Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;

		pending.set(false);
		error.set(cause);

		if (onError != null) {
			try {
				onError.accept(cause, context);
			} catch (Throwable t) {
				// Prevent callback errors from corrupting state
			}
		}
	}

	public void reset() {
		mutationGeneration++;
		pending.set(false);
		error.set(null);
		result.set(null);
	}

	public Readable<Boolean> isPending() {
		return pending;
	}

	public Readable<Throwable> error() {
		return error;
	}

	public Readable<R> result() {
		return result;
	}

	@Override
	public void dispose() {
		if (disposed) return;
		disposed = true;
		mutationGeneration++;
		pending.set(false);
	}

	@Override
	public boolean isDisposed() {
		return disposed;
	}

	private static void postToMain(Runnable runnable) {
		if (Core.app != null) {
			Core.app.post(runnable);
		} else {
			runnable.run();
		}
	}
}
