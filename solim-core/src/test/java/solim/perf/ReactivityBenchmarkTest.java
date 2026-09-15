package solim.perf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import solim.signal.Computed;
import solim.signal.Effect;
import solim.signal.Signal;
import solim.runtime.SignalDispatcher;

public class ReactivityBenchmarkTest {

    @Test
    void benchmark10000SignalUpdates() {
        Signal<Integer> source = Signal.of(0);
        Computed<Integer> doubled = new Computed<>(() -> source.get() * 2);
        AtomicInteger effectRuns = new AtomicInteger(0);
        AtomicInteger lastValue = new AtomicInteger(-1);

        Effect.of(() -> {
            lastValue.set(doubled.get());
            effectRuns.incrementAndGet();
        });

        // Warmup
        for (int i = 0; i < 1000; i++) {
            source.set(i);
        }
        SignalDispatcher.flush();

        // Measure 10,000 updates
        int iterations = 10000;
        long t0 = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            source.set(i);
        }
        SignalDispatcher.flush();
        long elapsedNanos = System.nanoTime() - t0;
        double elapsedMs = elapsedNanos / 1_000_000.0;

        System.out.printf("ReactivityBenchmark [10,000 updates]: %.3f ms (%.3f µs/op)%n",
                elapsedMs, (elapsedNanos / (double) iterations) / 1000.0);

        assertEquals(iterations - 1, (int) source.get());
        assertEquals((iterations - 1) * 2, lastValue.get());
        // 10,000 updates should take well under 100ms in modern JVM
        assertTrue(elapsedMs < 100.0, "10,000 updates took " + elapsedMs + " ms, expected < 100 ms");
    }
}
