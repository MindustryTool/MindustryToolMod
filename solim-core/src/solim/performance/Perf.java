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

public final class Perf {

    public static final String debugProperty = "solim.debug.enabled";

    private static boolean profilingRequested = false;
    private static boolean tracingRequested = false;
    private static float thresholdMs = 50f;
    private static boolean profiling = false;

    public static void init() {
        if (!debugEnabledByProperty()) {
            return;
        }

        Log.info("[Solim] @ enabled via system property; Ctrl+D toggles a profiling session",
                debugProperty);
        setEnabled(true);
    }

    public static boolean debugEnabledByProperty() {
        return Boolean.parseBoolean(System.getProperty(debugProperty, "false"));
    }

    private Perf() {
    }

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

    private static void uninstallDebugServices() {

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

    public static void setThreshold(float ms) {
        thresholdMs = ms < 0f ? 0f : ms;
        AttachmentStack.setThreshold(thresholdMs);
    }

    public static List<PerfSpan> snapshot(int max) {
        return AttachmentStack.snapshot(max);
    }

    public static int totalRecorded() {
        return AttachmentStack.totalRecorded();
    }

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

    public static List<TraceSpan> traceSnapshot(int max) {
        return AttachmentStack.traceSnapshot(max);
    }

    public static void traceReset() {
        AttachmentStack.traceReset();
    }

    public static int traceCapacity() {
        return DebugAttachmentStack.TRACE_CAPACITY;
    }

    public static int traceDroppedCount() {
        return AttachmentStack.traceDroppedCount();
    }

    public static int traceTotal() {
        return AttachmentStack.traceTotal();
    }

    public static long traceId() {
        return AttachmentStack.traceId();
    }

    public static boolean isProfiling() {
        return profiling;
    }

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

    public static void toggleProfiling() {
        if (profiling) {
            stopProfiling();
        } else {
            startProfiling();
        }
    }

    private static File writeProfile(List<TraceSpan> spans, int dropped, long id,
            List<ComponentNodeSnapshot> components) {
        File file = defaultProfileFile().file();
        try {
            ProfileReportExporter.writeFile(spans, dropped, id, components, file);
        } catch (IOException e) {

            Log.err("[Solim] Failed to write profile report to @", file, e);
        }
        return file;
    }

    private static Fi defaultProfileFile() {
        Fi dir = dataDirectory().child("solim-profiles");
        dir.mkdirs();
        return dir.child("profile-" + System.currentTimeMillis() + ".html");
    }

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
