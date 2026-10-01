package mindustrytool.features.quickaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import mindustrytool.test.MindustryTestEnv;
import org.junit.jupiter.api.Test;

class QuickAccessCollapsibleTest extends MindustryTestEnv {

    @Test
    void defaultConfiguration_collapsibleAndCollapsedAreFalse() {
        QuickAccessFeature qa = new QuickAccessFeature();

        assertFalse(qa.isCollapsible());
        assertFalse(qa.isCollapsed());
        assertFalse(qa.collapsibleConfig.get());
        assertFalse(qa.collapsedConfig.get());
    }

    @Test
    void setCollapsible_updatesConfigAndSignal() {
        QuickAccessFeature qa = new QuickAccessFeature();
        AtomicInteger notifications = new AtomicInteger(0);
        qa.collapsibleConfig.signal().subscribe(v -> notifications.incrementAndGet());

        qa.setCollapsible(true);

        assertTrue(qa.isCollapsible());
        assertTrue(qa.collapsibleConfig.get());
        assertEquals(1, notifications.get());

        qa.setCollapsible(false);
        assertFalse(qa.isCollapsible());
        assertFalse(qa.collapsibleConfig.get());
        assertEquals(2, notifications.get());
    }

    @Test
    void toggleCollapsed_whenCollapsible_togglesState() {
        QuickAccessFeature qa = new QuickAccessFeature();
        qa.setCollapsible(true);

        assertFalse(qa.isCollapsed());

        qa.toggleCollapsed();
        assertTrue(qa.isCollapsed());
        assertTrue(qa.collapsedConfig.get());

        qa.toggleCollapsed();
        assertFalse(qa.isCollapsed());
        assertFalse(qa.collapsedConfig.get());
    }

    @Test
    void toggleCollapsed_whenCollapsibleDisabled_forcesCollapsedToFalse() {
        QuickAccessFeature qa = new QuickAccessFeature();
        qa.setCollapsible(false);
        qa.collapsedConfig.set(true);

        qa.toggleCollapsed();

        assertFalse(qa.isCollapsed());
        assertFalse(qa.collapsedConfig.get());
    }
}
