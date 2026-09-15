package solim.perf;

import arc.Core;
import arc.scene.Element;
import arc.scene.Group;
import arc.util.Nullable;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Lightweight runtime profiler for Solim UI performance.
 * Tracks per-phase timing (act, layout, draw), layout invalidation counts,
 * element tree scale, and framerate.
 *
 * <p>When disabled (the default), all recording hooks are zero-overhead no-ops.
 */
public final class UiProfiler {

    public static volatile boolean enabled = false;

    private static final int BUFFER_SIZE = 120;
    private static final PerfFrame[] buffer = new PerfFrame[BUFFER_SIZE];
    private static int head = 0;
    private static int totalRecorded = 0;

    private static float curActMs = 0f;
    private static float curLayoutMs = 0f;
    private static float curDrawMs = 0f;
    private static int curDrawCalls = 0;
    private static int curLayoutPasses = 0;

    private UiProfiler() {}

    public static void setEnabled(boolean state) {
        enabled = state;
        if (!state) {
            reset();
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static synchronized void reset() {
        curActMs = 0f;
        curLayoutMs = 0f;
        curDrawMs = 0f;
        curDrawCalls = 0;
        curLayoutPasses = 0;
        head = 0;
        totalRecorded = 0;
        for (int i = 0; i < BUFFER_SIZE; i++) {
            buffer[i] = null;
        }
    }

    public static void recordAct(float ms) {
        if (!enabled) return;
        curActMs += ms;
    }

    public static void recordLayout(float ms) {
        if (!enabled) return;
        curLayoutMs += ms;
    }

    public static void recordDraw(float ms, int drawCalls) {
        if (!enabled) return;
        curDrawMs += ms;
        curDrawCalls += drawCalls;
    }

    public static void recordLayoutPass() {
        if (!enabled) return;
        curLayoutPasses++;
    }

    public static synchronized void commitFrame(@Nullable Element sceneRoot) {
        if (!enabled) return;

        PerfFrame frame = new PerfFrame();
        frame.timestamp = System.currentTimeMillis();
        frame.fps = Core.graphics != null ? Core.graphics.getFramesPerSecond() : 0f;

        if (sceneRoot != null) {
            int[] counts = countElements(sceneRoot);
            frame.totalElements = counts[0];
            frame.visibleElements = counts[1];
        }

        frame.actMs = curActMs;
        frame.layoutMs = curLayoutMs;
        frame.drawMs = curDrawMs;
        frame.drawCallCount = curDrawCalls;
        frame.layoutPasses = curLayoutPasses;

        curActMs = 0f;
        curLayoutMs = 0f;
        curDrawMs = 0f;
        curDrawCalls = 0;
        curLayoutPasses = 0;

        buffer[head] = frame;
        head = (head + 1) % BUFFER_SIZE;
        if (totalRecorded < BUFFER_SIZE) {
            totalRecorded++;
        }
    }

    public static synchronized @Nullable PerfFrame snapshotAverages(int maxFrames) {
        if (totalRecorded == 0) return null;

        int n = Math.min(maxFrames, totalRecorded);
        PerfFrame avg = new PerfFrame();
        avg.timestamp = System.currentTimeMillis();

        float totalFps = 0f;
        float totalAct = 0f;
        float totalLayout = 0f;
        float totalDraw = 0f;
        long totalDrawCalls = 0;
        long totalLayoutPasses = 0;
        long totalElementsSum = 0;
        long visibleElementsSum = 0;

        for (int i = 0; i < n; i++) {
            int idx = (head - 1 - i + BUFFER_SIZE) % BUFFER_SIZE;
            PerfFrame f = buffer[idx];
            if (f != null) {
                totalFps += f.fps;
                totalAct += f.actMs;
                totalLayout += f.layoutMs;
                totalDraw += f.drawMs;
                totalDrawCalls += f.drawCallCount;
                totalLayoutPasses += f.layoutPasses;
                totalElementsSum += f.totalElements;
                visibleElementsSum += f.visibleElements;
            }
        }

        avg.fps = totalFps / n;
        avg.actMs = totalAct / n;
        avg.layoutMs = totalLayout / n;
        avg.drawMs = totalDraw / n;
        avg.drawCallCount = (int) Math.round((double) totalDrawCalls / n);
        avg.layoutPasses = (int) Math.round((double) totalLayoutPasses / n);
        avg.totalElements = (int) Math.round((double) totalElementsSum / n);
        avg.visibleElements = (int) Math.round((double) visibleElementsSum / n);

        return avg;
    }

    public static int[] countElements(@Nullable Element root) {
        if (root == null) return new int[]{0, 0};
        Map<Element, Boolean> visited = new IdentityHashMap<>();
        int[] counts = new int[2]; // [total, visible]
        countRecursive(root, counts, visited);
        return counts;
    }

    private static void countRecursive(Element element, int[] counts, Map<Element, Boolean> visited) {
        if (element == null || visited.put(element, Boolean.TRUE) != null) return;
        counts[0]++;
        if (element.visible) {
            counts[1]++;
        }
        if (element instanceof Group) {
            Group group = (Group) element;
            for (Element child : group.getChildren()) {
                countRecursive(child, counts, visited);
            }
        }
    }
}
