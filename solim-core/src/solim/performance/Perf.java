package solim.performance;

import arc.Core;
import arc.files.Fi;
import arc.util.Log;
import arc.util.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import mindustry.Vars;
import solim.runtime.AttachmentStack;
import solim.runtime.DebugAttachmentStack;
import solim.runtime.DebugReactiveContext;
import solim.runtime.DebugSignalDispatcher;
import solim.runtime.ReactiveContext;
import solim.runtime.SignalDispatcher;

/**
 * Public profiling facade for Solim UI. Delegates to the active runtime service singletons so that
 * profiling can be enabled and queried without exposing the runtime module to callers.
 *
 * <p>{@link #setEnabled(boolean)} swaps all three runtime services — {@link AttachmentStack},
 * {@link ReactiveContext}, and {@link SignalDispatcher} — between their normal base implementations
 * and their debug subclasses as one operation, so debug state is always installed and released
 * together.
 *
 * <p>While enabled, {@code DebugSignalDispatcher} polls {@code Ctrl + D} each frame to toggle a
 * profiling session. Starting enables unified flame tracing and clears session counters; stopping
 * writes the interactive HTML report and opens it in the system browser.
 *
 * <p>Debug mode can also be entered without a programmatic call by setting the
 * {@code solim.debug.enabled} JVM system property, mirroring how {@code McpConfig} reads its own
 * {@code solim.mcp.*} properties. The property defaults to {@code "false"}, so the only way into the
 * debug path is to set it explicitly — which is what keeps published builds at zero cost. The
 * property is read by {@link #init()}, which the host calls during bootstrap.
 */
public final class Perf {

    /**
     * System property enabling the debug runtime services at class load. Mirrors the
     * {@code solim.mcp.*} naming convention.
     */
    public static final String debugProperty = "solim.debug.enabled";

    private static boolean profilingRequested = false;
    private static boolean tracingRequested = false;
    private static float thresholdMs = 50f;
    private static boolean profiling = false;

    /**
     * Enters debug mode if {@link #debugProperty} opts this runtime into it, otherwise does nothing.
     *
     * <p>Called explicitly from the host's bootstrap. This is deliberately <b>not</b> a static
     * initializer: nothing else in a shipped build references {@code Perf}, so a class-load-time
     * block would never run and the property would be inert. An explicit call gives the enablement
     * a trigger that is visible in the source of the thing being bootstrapped.
     *
     * <p>Invoking this more than once is harmless — {@link #setEnabled(boolean)} is idempotent for
     * the debug stack and wholesale-replaces the other two services.
     */
    public static void init() {
        if (!debugEnabledByProperty()) {
            return;
        }

        Log.info("[Solim] @ enabled via system property; Ctrl+D toggles a profiling session",
                debugProperty);
        setEnabled(true);
    }

    /**
     * Returns whether {@link #debugProperty} opts this runtime into debug mode.
     *
     * <p>Public so tests can assert the property's contract independently of whether
     * {@link #init()} has been called.
     */
    public static boolean debugEnabledByProperty() {
        return Boolean.parseBoolean(System.getProperty(debugProperty, "false"));
    }

    private Perf() {
    }

    /**
     * Enables or disables profiling. Enabling installs fresh debug instances of all three runtime
     * services; disabling reverts every one of them to its base implementation and discards all
     * recorded spans, reactive metrics, and creation-ownership state.
     *
     * <p>Explicit tracing via {@link #setTracingEnabled(boolean)} keeps the debug stack installed
     * independently of this flag.
     */
    public static void setEnabled(boolean state) {
        profilingRequested = state;
        if (tracingRequested) {
            if (state) {
                installDebugServices();
            }
            return;
        }
        if (state) {
            installDebugServices();
        } else {
            uninstallDebugServices();
        }
    }

    /**
     * Installs the debug triple. All three are swapped together so a caller can never observe a
     * partially-instrumented runtime.
     */
    private static void installDebugServices() {
        if (!(AttachmentStack.instance() instanceof DebugAttachmentStack)) {
            AttachmentStack.install(new DebugAttachmentStack());
            AttachmentStack.setThreshold(thresholdMs);
        }
        ReactiveContext.install(new DebugReactiveContext());
        DebugSignalDispatcher dispatcher = new DebugSignalDispatcher();
        dispatcher.setToggleAction(Perf::toggleProfiling);
        dispatcher.setKeybindPolling(true);
        SignalDispatcher.install(dispatcher);
    }

    /** Reverts every runtime service to its base implementation, releasing all debug state. */
    private static void uninstallDebugServices() {
        // Clear the outgoing debug instances rather than only dropping the reference, so nothing
        // is retained by a caller that still holds the previous instance.
        ReactiveContext context = ReactiveContext.instance();
        if (context instanceof DebugReactiveContext) {
            ((DebugReactiveContext) context).clearState();
        }
        SignalDispatcher dispatcher = SignalDispatcher.instance();
        if (dispatcher instanceof DebugSignalDispatcher) {
            ((DebugSignalDispatcher) dispatcher).setKeybindPolling(false);
            ((DebugSignalDispatcher) dispatcher).setToggleAction(null);
        }
        AttachmentStack.install(null);
        ReactiveContext.install(null);
        SignalDispatcher.install(null);
    }

    public static boolean isEnabled() {
        return AttachmentStack.instance() instanceof DebugAttachmentStack
                && ReactiveContext.instance() instanceof DebugReactiveContext
                && SignalDispatcher.instance() instanceof DebugSignalDispatcher;
    }

    /** Sets the slow threshold in milliseconds applied to all structural spans. */
    public static void setThreshold(float ms) {
        thresholdMs = ms < 0f ? 0f : ms;
        AttachmentStack.setThreshold(thresholdMs);
    }

    /** Returns recent spans in recency order, up to the buffer bound. */
    public static List<PerfSpan> snapshot(int max) {
        return AttachmentStack.snapshot(max);
    }

    public static int totalRecorded() {
        return AttachmentStack.totalRecorded();
    }

    /** Clears recorded spans, the maximum observed depth, and all reactive session metrics. */
    public static void reset() {
        AttachmentStack.reset();
        ReactiveContext context = ReactiveContext.instance();
        if (context instanceof DebugReactiveContext) {
            ((DebugReactiveContext) context).clearState();
        }
    }

    public static int maxDepthObserved() {
        return AttachmentStack.maxDepthObserved();
    }

    /**
     * Enables or disables hierarchical flame tracing. Enabling starts a fresh trace generation
     * on the debug variant, preserving slow spans; disabling clears traces and reverts to
     * profiling or base.
     */
    public static void setTracingEnabled(boolean state) {
        tracingRequested = state;
        if (state) {
            if (!(AttachmentStack.instance() instanceof DebugAttachmentStack)) {
                AttachmentStack.install(new DebugAttachmentStack());
                AttachmentStack.setThreshold(thresholdMs);
            }
            AttachmentStack.setTracingEnabled(true);
        } else {
            if (AttachmentStack.instance() instanceof DebugAttachmentStack) {
                AttachmentStack.setTracingEnabled(false);
            }
            if (!profilingRequested) {
                AttachmentStack.install(null);
            }
        }
    }

    public static boolean isTracing() {
        return AttachmentStack.isTracing();
    }

    /** Returns trace spans in document order, up to the buffer bound. */
    public static List<TraceSpan> traceSnapshot(int max) {
        return AttachmentStack.traceSnapshot(max);
    }

    /** Clears recorded trace spans and overflow counters, preserving enablement. */
    public static void traceReset() {
        AttachmentStack.traceReset();
    }

    /** Returns the bounded ring capacity (4096). */
    public static int traceCapacity() {
        return DebugAttachmentStack.TRACE_CAPACITY;
    }

    /** Returns the number of oldest spans evicted on overflow. */
    public static int traceDroppedCount() {
        return AttachmentStack.traceDroppedCount();
    }

    /** Returns total appended spans including evicted ones. */
    public static int traceTotal() {
        return AttachmentStack.traceTotal();
    }

    /** Returns the current trace generation id, or -1 when not tracing. */
    public static long traceId() {
        return AttachmentStack.traceId();
    }

    // --- profiling session ------------------------------------------------------------------------

    /** Returns whether a profiling session is currently recording. */
    public static boolean isProfiling() {
        return profiling;
    }

    /**
     * Starts a profiling session: enables unified flame tracing across component builds and
     * reactive operations, clears all session counters, and shows a start notification.
     *
     * <p>Does nothing when profiling is disabled, since the debug services that drive tracing and
     * input polling are not installed in that case.
     */
    public static void startProfiling() {
        if (profiling || !isEnabled()) {
            return;
        }
        profiling = true;
        AttachmentStack.traceReset();
        ReactiveContext context = ReactiveContext.instance();
        if (context instanceof DebugReactiveContext) {
            ((DebugReactiveContext) context).clearState();
        }
        setTracingEnabled(true);
        notify("Solim profiling started (Ctrl+D to stop)");
    }

    /**
     * Stops the profiling session, writes the self-contained HTML report under
     * {@code solim-profiles/}, notifies the player, and opens the report in the system browser.
     */
    public static void stopProfiling() {
        if (!profiling) {
            return;
        }
        profiling = false;
        List<TraceSpan> spans = traceSnapshot(traceCapacity());
        int dropped = traceDroppedCount();
        long id = traceId();
        List<ComponentNodeSnapshot> components = ComponentSnapshotBuilder.capture();
        boolean wasTracingRequested = tracingRequested;
        setTracingEnabled(false);

        try {
            File file = writeProfile(spans, dropped, id, components);
            notify("Solim profiling stopped: " + file.getName());
            openReport(file);
        } finally {
            tracingRequested = wasTracingRequested;
        }
    }

    /** Starts profiling when idle, stops it when already recording. */
    public static void toggleProfiling() {
        if (profiling) {
            stopProfiling();
        } else {
            startProfiling();
        }
    }

    /**
     * Writes the report to the default {@code solim-profiles} directory under the game's data
     * directory, creating it if needed.
     */
    private static File writeProfile(List<TraceSpan> spans, int dropped, long id,
            List<ComponentNodeSnapshot> components) {
        File file = defaultProfileFile().file();
        try {
            ProfileReportExporter.writeFile(spans, dropped, id, components, file);
        } catch (IOException e) {
            // A failed export must not propagate into the frame flush that triggered it.
            Log.err("[Solim] Failed to write profile report to @", file, e);
        }
        return file;
    }

    /**
     * Returns the default report path, {@code <data dir>/solim-profiles/profile-<timestamp>.html},
     * creating the parent directory when it does not yet exist.
     */
    private static Fi defaultProfileFile() {
        Fi dir = dataDirectory().child("solim-profiles");
        dir.mkdirs();
        return dir.child("profile-" + System.currentTimeMillis() + ".html");
    }

    /** Writes the report to an explicit file. Used by tests and headless tooling. */
    public static void stopProfiling(@Nullable File target) throws IOException {
        if (!profiling) {
            return;
        }
        profiling = false;
        ProfileReportExporter.writeFile(traceSnapshot(traceCapacity()), traceDroppedCount(), traceId(),
                ComponentSnapshotBuilder.capture(), target);
    }

    private static Fi dataDirectory() {
        if (Core.settings != null) {
            return Core.settings.getDataDirectory();
        }
        return new Fi(".");
    }

    /** Opens the report in the system browser, guarded for headless environments. */
    private static void openReport(File file) {
        if (Core.app == null) {
            return;
        }
        try {
            Core.app.openURI(file.toURI().toString());
        } catch (Throwable t) {
            Log.warn("[Solim] Could not open profile report @", file, t);
        }
    }

    private static void notify(String message) {
        if (Vars.ui == null || message == null) {
            return;
        }
        try {
            Vars.ui.showInfoFade(message);
        } catch (Throwable t) {
            Log.warn("[Solim] Could not show profiler notification", t);
        }
    }
}
