package mindustrytool.services;

import arc.Core;
import arc.struct.Seq;
import arc.util.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

public final class ReactiveStore<T> {

    public enum LoadState {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    public interface Listener<T> {
        void onUpdate(
                @Nullable T value,
                LoadState state,
                @Nullable Throwable error);
    }

    private final Seq<Listener<T>> listeners = new Seq<>();

    private T value;
    private LoadState state = LoadState.IDLE;
    private Throwable lastError;

    private CompletableFuture<T> inFlight;
    private final Supplier<CompletableFuture<T>> fetcher;

    public ReactiveStore(Supplier<CompletableFuture<T>> fetcher) {
        this.fetcher = fetcher;
    }

    public @Nullable T getValue() {
        return value;
    }

    public LoadState getState() {
        return state;
    }

    public @Nullable Throwable getLastError() {
        return lastError;
    }

    public void subscribe(Listener<T> listener) {
        listeners.add(listener);
        listener.onUpdate(value, state, lastError);
    }

    public void unsubscribe(Listener<T> listener) {
        listeners.remove(listener);
    }

    public CompletableFuture<T> fetch() {
        if (inFlight != null && !inFlight.isDone()) {
            return inFlight;
        }

        if (state != LoadState.SUCCESS) {
            setState(LoadState.LOADING, null);
        }

        inFlight = fetcher.get()
                .thenApply(result -> {
                    updateValue(result);
                    return result;
                })
                .exceptionally(err -> {
                    fail(err);
                    throw new CompletionException(err);
                });

        return inFlight;
    }

    public void setValue(T newValue) {
        updateValue(newValue);
    }

    public void clear() {
        value = null;
        state = LoadState.IDLE;
        lastError = null;
        notifyListeners();
    }

    private void updateValue(T newValue) {
        value = newValue;
        state = LoadState.SUCCESS;
        lastError = null;

        notifyListeners();
    }

    private void fail(Throwable error) {
        state = LoadState.ERROR;
        lastError = error;
        notifyListeners();
    }

    private void setState(LoadState newState, Throwable error) {
        state = newState;
        lastError = error;
        notifyListeners();
    }

    private void notifyListeners() {
        Core.app.post(() -> {
            for (Listener<T> l : listeners) {
                l.onUpdate(value, state, lastError);
            }
        });
    }
}
