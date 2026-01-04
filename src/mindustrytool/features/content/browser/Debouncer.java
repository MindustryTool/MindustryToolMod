package mindustrytool.features.content.browser;

import java.util.concurrent.*;
import arc.Core;

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
