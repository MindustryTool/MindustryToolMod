package mindustrytool.features.autoplay;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.mock.MockSettings;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.I18NBundle;
import mindustry.gen.Icon;
import mindustrytool.features.autoplay.tasks.AutoplayTask;
import mindustrytool.features.autoplay.tasks.FleeTask;
import mindustrytool.features.autoplay.tasks.MiningTask;
import mindustrytool.features.autoplay.tasks.SelfHealTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AutoplayFeatureTest {

    private AutoplayFeature feature;

    @BeforeEach
    void setUp() {
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        Core.settings = new MockSettings();
        Core.bundle = new I18NBundle();
        Core.bundle.setProperties(new ObjectMap<>());

        Icon.refresh = new TextureRegionDrawable();
        Icon.move = new TextureRegionDrawable();
        Icon.warning = new TextureRegionDrawable();
        Icon.hammer = new TextureRegionDrawable();
        Icon.players = new TextureRegionDrawable();
        Icon.filter = new TextureRegionDrawable();
        Icon.up = new TextureRegionDrawable();
        Icon.down = new TextureRegionDrawable();
        Icon.none = new TextureRegionDrawable();

        feature = new AutoplayFeature();
    }

    @Test
    void metadataProperties() {
        assertEquals("autoplay", feature.getMetadata().getId());
        assertFalse(feature.getMetadata().isDevelopment(), "Autoplay must not be marked as development");
        assertFalse(feature.getMetadata().isEnabledByDefault(), "Autoplay must default to disabled");
        assertTrue(feature.getMetadata().isQuickAccess(), "Autoplay should be quick access enabled");
    }

    @Test
    void configDefaults() {
        assertFalse(feature.followUnit.get(), "followUnit should default to false");
        assertEquals(2.0f, feature.overrideCooldown.get(), 0.001f, "overrideCooldown should default to 2.0s");

        Seq<AutoplayTask> tasks = feature.tasks().peek();
        assertEquals(8, tasks.size, "Should have 8 autoplay tasks");
        assertEquals(SelfHealTask.ID, tasks.get(0).getId());
        assertEquals(FleeTask.ID, tasks.get(1).getId());
        assertEquals(MiningTask.ID, tasks.get(7).getId());
    }

    @Test
    void enableDisableTask() {
        assertTrue(feature.isTaskEnabled(SelfHealTask.ID), "Tasks should be enabled by default");

        feature.setTaskEnabled(SelfHealTask.ID, false);
        assertFalse(feature.isTaskEnabled(SelfHealTask.ID), "Task should be disabled");
        assertTrue(feature.disabledTasks.get().contains(SelfHealTask.ID));

        feature.setTaskEnabled(SelfHealTask.ID, true);
        assertTrue(feature.isTaskEnabled(SelfHealTask.ID), "Task should be re-enabled");
        assertFalse(feature.disabledTasks.get().contains(SelfHealTask.ID));
    }

    @Test
    void taskReordering() {
        Seq<AutoplayTask> initial = feature.tasks().peek();
        assertEquals(SelfHealTask.ID, initial.get(0).getId());
        assertEquals(FleeTask.ID, initial.get(1).getId());

        feature.moveTaskDown(SelfHealTask.ID);

        Seq<AutoplayTask> movedDown = feature.tasks().peek();
        assertEquals(FleeTask.ID, movedDown.get(0).getId());
        assertEquals(SelfHealTask.ID, movedDown.get(1).getId());

        feature.moveTaskUp(SelfHealTask.ID);

        Seq<AutoplayTask> movedUp = feature.tasks().peek();
        assertEquals(SelfHealTask.ID, movedUp.get(0).getId());
        assertEquals(FleeTask.ID, movedUp.get(1).getId());
    }
}
