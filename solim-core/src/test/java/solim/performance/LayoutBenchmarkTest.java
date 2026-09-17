package solim.performance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.Element;
import arc.scene.ui.layout.Table;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.layout.Card;
import solim.layout.Column;
import solim.layout.Row;

public class LayoutBenchmarkTest {

    @BeforeAll
    static void initArc() {
        if (Core.app == null) {
            Core.app = new MockApplication();
        }
        if (Core.graphics == null) {
            Core.graphics = new MockGraphics();
        }
    }

    private Table buildList(int count) {
        Column col = new Column();
        col.children(() -> {
            for (int i = 0; i < count; i++) {
                new Card().growX().children(() -> {
                    new Row().growX().children(() -> {
                        Element el = new Element();
                        el.setSize(40f, 40f);
                    });
                });
            }
        });
        return col.table();
    }

    @Test
    void benchmark100ItemsLayoutUnder5ms() {
        // Warmup JIT
        for (int w = 0; w < 10; w++) {
            Table t = buildList(100);
            t.validate();
        }

        // Measure
        long totalNanos = 0;
        int iterations = 20;
        for (int i = 0; i < iterations; i++) {
            Table t = buildList(100);
            long t0 = System.nanoTime();
            t.validate();
            totalNanos += (System.nanoTime() - t0);
        }

        double avgMs = (totalNanos / (double) iterations) / 1_000_000.0;
        System.out.printf("LayoutBenchmark [100 items]: avg validate = %.3f ms%n", avgMs);
        assertTrue(avgMs < 5.0, "100 items layout validate took " + avgMs + " ms, expected < 5.0 ms");
    }

    @Test
    void benchmarkScale500And1000Items() {
        // Warmup
        Table t500Warm = buildList(500);
        t500Warm.validate();

        long t0 = System.nanoTime();
        Table t500 = buildList(500);
        t500.validate();
        double ms500 = (System.nanoTime() - t0) / 1_000_000.0;
        System.out.printf("LayoutBenchmark [500 items]: construct + validate = %.3f ms%n", ms500);

        long t1 = System.nanoTime();
        Table t1000 = buildList(1000);
        t1000.validate();
        double ms1000 = (System.nanoTime() - t1) / 1_000_000.0;
        System.out.printf("LayoutBenchmark [1000 items]: construct + validate = %.3f ms%n", ms1000);

        assertTrue(ms500 < 100.0, "500 items should validate reasonably fast");
        assertTrue(ms1000 < 200.0, "1000 items should validate reasonably fast");
    }
}
