package solim.performance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.Core;
import arc.scene.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.layout.Card;
import solim.layout.Column;
import solim.layout.Tabs;
import solim.reactive.Signal;
import arc.scene.ui.Button.ButtonStyle;
import solim.test.SolimEnv;

public class TabsPerformanceTest extends SolimEnv {


    private void populateLargeContent(int count) {
        Column col = new Column();
        col.children(() -> {
            for (int i = 0; i < count; i++) {
                new Card().growX().children(() -> {
                    Element el = new Element();
                    el.setSize(30f, 30f);
                });
            }
        });
    }

    @BeforeEach
    void setUp() {
        newScene();
        Core.scene.addStyle(ButtonStyle.class, new ButtonStyle());
    }

    @Test
    void benchmarkDisabledLayoutSkipsSubtree() {
        Signal<Integer> activeTab = Signal.of(0);
        Tabs tabs = new Tabs(activeTab);

        tabs.tab("Tab 0", () -> {
            new Card().growX().children(() -> {
                Element el = new Element();
                el.setSize(100f, 100f);
            });
        });

        tabs.tab("Tab 1", () -> populateLargeContent(300));
        tabs.tab("Tab 2", () -> populateLargeContent(300));

        // When inactive tabs have setLayoutEnabled(false), validate should be fast
        for (int i = 1; i < tabs.contents().size(); i++) {
            tabs.contents().get(i).setLayoutEnabled(false);
            tabs.contents().get(i).visible = false;
        }

        // Warmup
        for (int i = 0; i < 10; i++) {
            tabs.root().invalidate();
            tabs.root().validate();
        }

        long t0 = System.nanoTime();
        int iterations = 50;
        for (int i = 0; i < iterations; i++) {
            tabs.root().invalidate();
            tabs.root().validate();
        }
        double avgDisabledMs = ((System.nanoTime() - t0) / (double) iterations) / 1_000_000.0;
        System.out.printf("TabsPerformance [disabled inactive tabs]: avg validate = %.3f ms%n", avgDisabledMs);

        // When all tabs have setLayoutEnabled(true)
        for (int i = 1; i < tabs.contents().size(); i++) {
            tabs.contents().get(i).setLayoutEnabled(true);
        }

        for (int i = 0; i < 10; i++) {
            tabs.root().invalidate();
            tabs.root().validate();
        }

        long t1 = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            tabs.root().invalidate();
            tabs.root().validate();
        }
        double avgEnabledMs = ((System.nanoTime() - t1) / (double) iterations) / 1_000_000.0;
        System.out.printf("TabsPerformance [enabled inactive tabs]: avg validate = %.3f ms%n", avgEnabledMs);

        assertTrue(avgDisabledMs < 2.0, "Disabled inactive tabs should validate in < 2ms");
    }
}
