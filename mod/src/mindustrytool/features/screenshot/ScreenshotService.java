package mindustrytool.features.screenshot;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.func.Cons;
import arc.graphics.Pixmap;
import arc.graphics.PixmapIO;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.ScreenUtils;
import arc.util.Threads;
import mindustry.game.EventType.Trigger;

/**
 * Schedules framebuffer captures on the render thread and encodes PNGs
 * on a background daemon thread so gameplay never stutters.
 * Without UI captures at uiDrawBegin, with UI captures at postDraw.
 */
public class ScreenshotService {

    private static final long CAPTURE_COOLDOWN_MILLIS = 500L;

    private volatile boolean pendingWithoutUi;
    private volatile boolean pendingWithUi;
    private volatile @Nullable Fi pendingDirectory;
    private volatile @Nullable String pendingFilename;
    private volatile @Nullable Cons<Fi> pendingSuccess;
    private volatile @Nullable Cons<Exception> pendingError;
    private volatile long lastRequestMillis;

    public ScreenshotService() {
        Events.run(Trigger.uiDrawBegin, this::onUiDrawBegin);
        Events.run(Trigger.postDraw, this::onPostDraw);
    }

    /**
     * Queues a capture for the next suitable render trigger.
     * Returns false when debounced to avoid flooding memory with pixmaps.
     */
    public boolean capture(@Nullable Fi directory, @Nullable String filename, boolean includeUi,
            @Nullable Cons<Fi> onSuccess, @Nullable Cons<Exception> onError) {
        long now = System.currentTimeMillis();
        if (now - lastRequestMillis < CAPTURE_COOLDOWN_MILLIS) {
            return false;
        }
        lastRequestMillis = now;

        pendingDirectory = directory;
        pendingFilename = filename;
        pendingSuccess = onSuccess;
        pendingError = onError;
        if (includeUi) {
            pendingWithUi = true;
        } else {
            pendingWithoutUi = true;
        }
        return true;
    }

    private void onUiDrawBegin() {
        if (!pendingWithoutUi) {
            return;
        }
        pendingWithoutUi = false;
        grabPending();
    }

    private void onPostDraw() {
        if (!pendingWithUi) {
            return;
        }
        pendingWithUi = false;
        grabPending();
    }

    private void grabPending() {
        Fi directory = pendingDirectory;
        String filename = pendingFilename;
        Cons<Fi> onSuccess = pendingSuccess;
        Cons<Exception> onError = pendingError;
        pendingDirectory = null;
        pendingFilename = null;
        pendingSuccess = null;
        pendingError = null;

        try {
            int width = Core.graphics.getBackBufferWidth();
            int height = Core.graphics.getBackBufferHeight();
            Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, width, height, true);
            Fi target = ScreenshotNaming.resolveCollision(directory, filename);
            writeAsync(target, pixmap, onSuccess, onError);
        } catch (Exception e) {
            Log.err("Screenshot capture failed", e);
            if (onError != null) {
                Core.app.post(() -> onError.get(e));
            }
        }
    }

    /**
     * Encodes and writes the pixmap off the render thread, disposing it afterwards.
     */
    public static void writeAsync(Fi target, Pixmap pixmap,
            @Nullable Cons<Fi> onSuccess, @Nullable Cons<Exception> onError) {        Threads.daemon(() -> {
            try {
                if (target.parent() != null) {
                    target.parent().mkdirs();
                }
                PixmapIO.writePng(target, pixmap);
                if (onSuccess != null) {
                    Core.app.post(() -> onSuccess.get(target));
                }
            } catch (Exception e) {
                Log.err("Screenshot write failed: " + target, e);
                if (onError != null) {
                    Core.app.post(() -> onError.get(e));
                }
            } finally {
                pixmap.dispose();
            }
        });
    }

}
