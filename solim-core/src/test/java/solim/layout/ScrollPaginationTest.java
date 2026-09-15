package solim.layout;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ScrollPaginationTest {

	@Test
	void reachTopTriggersOnThresholdAndDebounces() {
		Scroll scroll = new Scroll();
		AtomicInteger topTriggerCount = new AtomicInteger(0);

		scroll.onReachTop(50f, topTriggerCount::incrementAndGet);

		// When maxY <= threshold (content smaller than threshold), should not trigger
		scroll.checkScrollBoundary(10f, 40f);
		assertEquals(0, topTriggerCount.get());

		// When scrolling near top (scrollY = 30 <= 50, maxY = 500)
		scroll.checkScrollBoundary(30f, 500f);
		assertEquals(1, topTriggerCount.get(), "First entrance into top zone triggers callback");

		// Continuous scrolling inside zone (scrollY = 10) should NOT re-trigger
		scroll.checkScrollBoundary(10f, 500f);
		assertEquals(1, topTriggerCount.get(), "Remaining inside top zone does not re-trigger");

		// Scrolling away past threshold (scrollY = 120 > 50)
		scroll.checkScrollBoundary(120f, 500f);
		assertEquals(1, topTriggerCount.get());

		// Re-entering zone (scrollY = 45 <= 50) triggers again
		scroll.checkScrollBoundary(45f, 500f);
		assertEquals(2, topTriggerCount.get(), "Re-entering top zone triggers callback again");
	}

	@Test
	void reachBottomTriggersOnThresholdAndDebounces() {
		Scroll scroll = new Scroll();
		AtomicInteger bottomTriggerCount = new AtomicInteger(0);

		scroll.onReachBottom(60f, bottomTriggerCount::incrementAndGet);

		// Far from bottom (maxY = 1000, scrollY = 500 -> diff = 500 > 60)
		scroll.checkScrollBoundary(500f, 1000f);
		assertEquals(0, bottomTriggerCount.get());

		// Near bottom (maxY = 1000, scrollY = 960 -> diff = 40 <= 60)
		scroll.checkScrollBoundary(960f, 1000f);
		assertEquals(1, bottomTriggerCount.get(), "First entrance into bottom zone triggers callback");

		// Still near bottom (scrollY = 980)
		scroll.checkScrollBoundary(980f, 1000f);
		assertEquals(1, bottomTriggerCount.get(), "Remaining near bottom does not re-trigger");

		// Scroll away (scrollY = 800)
		scroll.checkScrollBoundary(800f, 1000f);
		assertEquals(1, bottomTriggerCount.get());

		// Return to bottom (scrollY = 990)
		scroll.checkScrollBoundary(990f, 1000f);
		assertEquals(2, bottomTriggerCount.get(), "Re-entering bottom zone triggers callback again");
	}
}