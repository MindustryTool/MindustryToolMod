package mindustrytool.config; // Khai báo package chứa các class config/utility

import java.util.concurrent.*; // Import concurrent utilities

import arc.Core; // Import Core để post lên main thread

/**
 * Utility class để debounce các actions.
 * Debounce = delay action và hủy các actions trước nếu có action mới.
 * Hữu ích cho search input để tránh gọi API quá nhiều.
 */
public class Debouncer { // Class Debouncer

    // Executor để schedule tasks
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    // Future của task đang được schedule
    private ScheduledFuture<?> future;
    // Thời gian delay (milliseconds)
    private final long delay;

    /**
     * Constructor tạo Debouncer.
     * @param delay Thời gian delay
     * @param unit Đơn vị thời gian (TimeUnit)
     */
    public Debouncer(long delay, TimeUnit unit) {
        this.delay = unit.toMillis(delay); // Chuyển sang milliseconds
    }

    /**
     * Debounce một task.
     * Nếu có task đang pending, hủy nó và schedule task mới.
     * Task sẽ được post về main thread khi thực thi.
     * @param task Runnable cần thực thi
     */
    public synchronized void debounce(Runnable task) {
        // Nếu có future đang chạy, hủy nó
        if (future != null && !future.isDone()) {
            future.cancel(false); // Cancel nhưng không interrupt
        }
        // Schedule task mới sau delay, post về main thread
        future = scheduler.schedule(() -> Core.app.post(task), delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Shutdown scheduler.
     * Gọi khi không còn dùng debouncer nữa.
     */
    public void shutdown() {
        scheduler.shutdown(); // Shutdown executor
    }
}
