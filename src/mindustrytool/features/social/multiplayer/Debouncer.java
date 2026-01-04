package mindustrytool.features.social.multiplayer;

import java.util.concurrent.*;
import arc.Core;

/** Debouncer for delaying and coalescing rapid function calls. */
public class Debouncer {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> future;
    private final long delay;

    public Debouncer(long delay, TimeUnit unit) {
        this.delay = unit.toMillis(delay);
    }

    public synchronized void debounce(Runnable task) {
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
        future = scheduler.schedule(() -> Core.app.post(task), delay, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
