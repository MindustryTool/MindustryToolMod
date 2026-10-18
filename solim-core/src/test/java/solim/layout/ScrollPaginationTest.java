package solim.layout;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import solim.test.SolimEnv;

class ScrollPaginationTest extends SolimEnv {

    @Test
    void reachTopTriggersOnThresholdAndDebounces() {
        Scroll scroll = new Scroll();
        AtomicInteger topTriggerCount = new AtomicInteger(0);

        scroll.onReachTop(50f, topTriggerCount::incrementAndGet);

        scroll.checkScrollBoundary(10f, 40f);
        assertEquals(0, topTriggerCount.get());

        scroll.checkScrollBoundary(30f, 500f);
        assertEquals(1, topTriggerCount.get(), "First entrance into top zone triggers callback");

        scroll.checkScrollBoundary(10f, 500f);
        assertEquals(1, topTriggerCount.get(), "Remaining inside top zone does not re-trigger");

        scroll.checkScrollBoundary(120f, 500f);
        assertEquals(1, topTriggerCount.get());

        scroll.checkScrollBoundary(45f, 500f);
        assertEquals(2, topTriggerCount.get(), "Re-entering top zone triggers callback again");
    }

    @Test
    void reachBottomTriggersOnThresholdAndDebounces() {
        Scroll scroll = new Scroll();
        AtomicInteger bottomTriggerCount = new AtomicInteger(0);

        scroll.onReachBottom(60f, bottomTriggerCount::incrementAndGet);

        scroll.checkScrollBoundary(500f, 1000f);
        assertEquals(0, bottomTriggerCount.get());

        scroll.checkScrollBoundary(960f, 1000f);
        assertEquals(1, bottomTriggerCount.get(), "First entrance into bottom zone triggers callback");

        scroll.checkScrollBoundary(980f, 1000f);
        assertEquals(1, bottomTriggerCount.get(), "Remaining near bottom does not re-trigger");

        scroll.checkScrollBoundary(800f, 1000f);
        assertEquals(1, bottomTriggerCount.get());

        scroll.checkScrollBoundary(990f, 1000f);
        assertEquals(2, bottomTriggerCount.get(), "Re-entering bottom zone triggers callback again");
    }
}
