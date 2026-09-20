package solim.core;

/**
 * Minimal sink for engine-reported slow spans.
 *
 * <p>Implemented by profiling facilities in upper layers. The runtime engine
 * holds only a volatile reference and performs no allocation when no sink is
 * installed, keeping the disabled path zero-overhead.
 */
public interface PerfSink {

    /**
     * Records a slow span.
     *
     * @param component component or engine area name, never null
     * @param phase phase name such as subtree or attach, never null
     * @param durationMs measured duration in milliseconds
     * @param detail optional detail, may be null
     */
    void record(String component, String phase, float durationMs, String detail);
}
