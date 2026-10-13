package solim.performance;

import arc.Core;
import arc.Events;
import arc.Input;
import arc.files.Fi;
import arc.input.KeyCode;
import arc.util.Log;
import arc.util.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import mindustry.game.EventType.Trigger;
import mindustry.Vars;
import solim.instrumentation.Instrumentation;

public final class Perf {

    public static final String debugProperty = "solim.debug.enabled";

    private static boolean profilingRequested = false;
    private static boolean tracingRequested = false;
    private static boolean keybindPolling = false;
    private static float thresholdMs = 50f;
    private static boolean profiling = false;

    private static @Nullable ProfilerSink sink = null;
    private static boolean hookInstalled = false;

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

    public static synchronized void setEnabled(boolean state) {
        profilingRequested = state;
        if (state) {
            installSink();
        } else {
            uninstallSink();
        }
        keybindPolling = state || tracingRequested;
    }

    private static void installSink() {
        if (sink != null) {
            return;
        }
        ProfilerSink next = new ProfilerSink();
        next.setThreshold(thresholdMs);
        sink = next;
        Instrumentation.install(next);
        installHook();
    }

    private static void uninstallSink() {
        if (sink == null) {
            return;
        }
        ProfilerSink previous = sink;
        sink = null;
        tracingRequested = false;
        profiling = false;
        Instrumentation.uninstall();
        previous.reset();
    }

    private static void installHook() {
        if (hookInstalled) {
            return;
        }
        hookInstalled = true;
        Events.run(Trigger.update, (Runnable) Perf::pollProfilingKeybind);
    }

    private static void pollProfilingKeybind() {
        if (sink == null || !keybindPolling) {
            return;
        }
        Input input = Core.input;
        if (input == null) {
            return;
        }
        try {
            if (input.ctrl() && input.keyTap(KeyCode.d)) {
                toggleProfiling();
            }
        } catch (Throwable t) {
            Log.err("[Solim] Profiling keybind poll failed", t);
            keybindPolling = false;
        }
    }

    public static boolean isEnabled() {
        return sink != null;
    }

    public static void setThreshold(float ms) {
        thresholdMs = ms < 0f ? 0f : ms;
        ProfilerSink current = sink;
        if (current != null) {
            current.setThreshold(thresholdMs);
        }
    }

    public static List<PerfSpan> snapshot(int max) {
        ProfilerSink current = sink;
        return current != null ? current.snapshot(max) : Collections.<PerfSpan>emptyList();
    }

    public static int totalRecorded() {
        ProfilerSink current = sink;
        return current != null ? current.totalRecorded() : 0;
    }

    public static void reset() {
        ProfilerSink current = sink;
        if (current != null) {
            current.reset();
        }
    }

    public static int maxDepthObserved() {
        ProfilerSink current = sink;
        return current != null ? current.maxDepthObserved() : 0;
    }

    public static void setTracingEnabled(boolean state) {
        tracingRequested = state;
        ProfilerSink current = sink;
        if (current == null) {
            if (state) {
                installSink();
                current = sink;
            } else {
                keybindPolling = profilingRequested;
                return;
            }
        }
        current.setTracingEnabled(state);
        keybindPolling = state || profilingRequested;
    }

    public static boolean isTracing() {
        ProfilerSink current = sink;
        return current != null && current.isTracing();
    }

    public static List<TraceSpan> traceSnapshot(int max) {
        ProfilerSink current = sink;
        return current != null ? current.traceSnapshot(max) : Collections.<TraceSpan>emptyList();
    }

    public static void traceReset() {
        ProfilerSink current = sink;
        if (current != null) {
            current.traceReset();
        }
    }

    public static int traceCapacity() {
        return ProfilerSink.TRACE_CAPACITY;
    }

    public static int traceDroppedCount() {
        ProfilerSink current = sink;
        return current != null ? current.traceDroppedCount() : 0;
    }

    public static int traceTotal() {
        ProfilerSink current = sink;
        return current != null ? current.traceTotal() : 0;
    }

    public static long traceId() {
        ProfilerSink current = sink;
        return current != null ? current.traceId() : -1L;
    }

    public static @Nullable ProfilerSink sink() {
        return sink;
    }

    public static boolean isProfiling() {
        return profiling;
    }

    public static void startProfiling() {
        if (profiling || !isEnabled()) {
            return;
        }
        profiling = true;
        traceReset();
        ProfilerSink current = sink;
        if (current != null) {
            current.clearReactiveState();
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
        List<ComponentNodeSnapshot> components = ComponentSnapshotBuilder.capture(sink);
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
                ComponentSnapshotBuilder.capture(sink), target);
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