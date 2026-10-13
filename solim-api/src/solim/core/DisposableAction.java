package solim.core;
 
import arc.util.Nullable;

public final class DisposableAction implements Disposable {
    private final @Nullable Runnable action;
    private final boolean stateless;
    private boolean disposed = false;

    public static final DisposableAction EMPTY = new DisposableAction(null);

    public DisposableAction(@Nullable Runnable action) {
        this.action = action;
        this.stateless = action == null;
    }

    public static DisposableAction of(Runnable action) {
        return action != null ? new DisposableAction(action) : EMPTY;
    }

    public static DisposableAction empty() {
        return EMPTY;
    }

    @Override
    public void dispose() {
        if (stateless || disposed) {
            return;
        }
        disposed = true;
        action.run();
    }

    @Override
    public boolean isDisposed() {
        return stateless ? false : disposed;
    }
}