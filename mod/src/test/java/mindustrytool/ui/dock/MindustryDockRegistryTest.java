package mindustrytool.ui.dock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.Scene;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.WidgetGroup;
import org.junit.jupiter.api.Test;
import mindustrytool.test.MindustryTestEnv;
import solim.overlay.DockCandidate;
import solim.overlay.DockTarget;
import solim.test.ArcTestEnv;

class MindustryDockRegistryTest extends MindustryTestEnv {

    @Test
    void resolvesRegisteredHudBoundsAndSnapPosition() {
        Scene scene = ArcTestEnv.newScene();
        scene.resize(800, 600);

        Table target = new Table();
        target.setSize(80f, 40f);
        target.setPosition(200f, 200f);
        scene.root.addChild(target);

        MindustryDockRegistry registry = MindustryDockRegistry.get();
        try {
            registry.registerHud("sibling", target);

            DockTarget resolved = registry.resolve("sibling");
            assertNotNull(resolved);
            assertTrue(resolved.active());
            assertEquals(200f, resolved.x(), 0.01f);
            assertEquals(200f, resolved.y(), 0.01f);
            assertEquals(80f, resolved.width(), 0.01f);
            assertEquals(40f, resolved.height(), 0.01f);

            DockCandidate candidate = registry.engine().findSnap(210f, 140f, 100f, 50f);
            assertNotNull(candidate);
            assertEquals("sibling", candidate.target().id());
            assertEquals(210f, candidate.x(), 0.01f);
            assertEquals(146f, candidate.y(), 0.01f);

            target.visible = false;
            assertNull(registry.engine().findSnap(210f, 140f, 100f, 50f));
        } finally {
            registry.unregisterHud("sibling");
        }

        assertNull(registry.resolve("sibling"));
    }

    @Test
    void resolvesBlockCategoryBarFromPlacementInputTable() {
        WidgetGroup hudGroup = new WidgetGroup();
        Table categoryBar = new Table();
        Table blocksSelect = new Table();
        Table inputTable = new Table();
        inputTable.name = "inputTable";

        blocksSelect.addChild(inputTable);
        categoryBar.addChild(blocksSelect);
        hudGroup.addChild(categoryBar);

        assertSame(categoryBar, MindustryDockRegistry.resolveBlockSelect(hudGroup));

        categoryBar.visible = false;
        assertNull(MindustryDockRegistry.resolveBlockSelect(hudGroup));
    }

    @Test
    void resolvesScreenEdgeTargetsFromCurrentViewport() {
        Scene scene = ArcTestEnv.newScene();
        scene.resize(800, 600);

        MindustryDockRegistry registry = MindustryDockRegistry.get();
        DockTarget bottom = registry.resolve(MindustryDockRegistry.TARGET_SCREEN_BOTTOM);

        assertNotNull(bottom);
        assertTrue(bottom.active());
        assertEquals(800f, bottom.width(), 0.01f);
        assertEquals(0f, bottom.height(), 0.01f);
        assertEquals(0f, bottom.y(), 0.01f);

        DockCandidate candidate = registry.engine().findSnap(100f, 10f, 100f, 50f);
        assertNotNull(candidate);
        assertEquals(MindustryDockRegistry.TARGET_SCREEN_BOTTOM, candidate.target().id());
        assertEquals(4f, candidate.y(), 0.01f);
    }
}
