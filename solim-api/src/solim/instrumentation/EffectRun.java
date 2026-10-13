package solim.instrumentation;

import java.util.Collections;
import java.util.List;
import solim.core.ReactiveSource;

public final class EffectRun implements InstrumentationEvent {

    public final Object effect;
    public final long startNs;
    public final long durationNs;
    public final List<ReactiveSource> dependencies;

    public EffectRun(Object effect, long startNs, long durationNs, List<ReactiveSource> dependencies) {
        this.effect = effect;
        this.startNs = startNs;
        this.durationNs = durationNs < 0L ? 0L : durationNs;
        this.dependencies = dependencies != null ? dependencies : Collections.<ReactiveSource>emptyList();
    }

    public long endNs() {
        return startNs + durationNs;
    }
}