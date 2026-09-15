package solim.perf;

public class PerfFrame {
    public long timestamp;
    public float fps;
    public int totalElements;
    public int visibleElements;
    public int registeredComponents;
    public int activeBindings;

    // Per-phase timings in milliseconds for the instrumented overlay
    public float actMs;
    public float layoutMs;
    public float drawMs;
    public int drawCallCount;
    public int layoutPasses;

    public PerfFrame() {
    }

    public PerfFrame(PerfFrame other) {
        this.timestamp = other.timestamp;
        this.fps = other.fps;
        this.totalElements = other.totalElements;
        this.visibleElements = other.visibleElements;
        this.registeredComponents = other.registeredComponents;
        this.activeBindings = other.activeBindings;
        this.actMs = other.actMs;
        this.layoutMs = other.layoutMs;
        this.drawMs = other.drawMs;
        this.drawCallCount = other.drawCallCount;
        this.layoutPasses = other.layoutPasses;
    }
}
