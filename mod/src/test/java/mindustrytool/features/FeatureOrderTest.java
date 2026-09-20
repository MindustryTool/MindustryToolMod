package mindustrytool.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import mindustry.gen.Icon;
import mindustrytool.features.settings.ModSettings;
import mindustrytool.test.MindustryTestEnv;

class FeatureOrderTest extends MindustryTestEnv {

    static class TestFeature extends Feature {
        TestFeature(String id, int order, boolean dev) {
            super(FeatureMetadata.builder()
                    .id(id)
                    .icon(Icon.book)
                    .order(order)
                    .development(dev)
                    .build());
        }
    }

    @BeforeAll
    static void initCore() {
        Icon.book = new TextureRegionDrawable();
    }

    @BeforeEach
    void setUp() {
        ModSettings.featureOrder.set(new Seq<>());
    }

    @AfterEach
    void tearDown() {
        ModSettings.featureOrder.set(new Seq<>());
    }

    @Test
    void normalize_healsDuplicatesKeepingFirst() {
        Feature a = new TestFeature("feat-a", 1, false);
        Feature b = new TestFeature("feat-b", 2, false);
        Seq<Feature> all = Seq.with(a, b);

        Seq<String> stored = Seq.with("feat-b", "feat-a", "feat-b");
        Seq<String> normalized = FeatureManager.normalizeOrder(all, stored);

        assertEquals(2, normalized.size);
        assertEquals("feat-b", normalized.get(0));
        assertEquals("feat-a", normalized.get(1));
    }

    @Test
    void normalize_removesStaleAndDevelopmentIds() {
        Feature a = new TestFeature("feat-a", 1, false);
        Feature b = new TestFeature("feat-b", 2, false);
        Feature dev = new TestFeature("dev-stub", 99, true);
        Seq<Feature> all = Seq.with(a, b, dev);

        Seq<String> stored = Seq.with("feat-a", "stale-id", "dev-stub", "feat-b");
        Seq<String> normalized = FeatureManager.normalizeOrder(all, stored);

        assertEquals(2, normalized.size);
        assertEquals("feat-a", normalized.get(0));
        assertEquals("feat-b", normalized.get(1));
    }

    @Test
    void normalize_appendsMissingNonDevIdsSortedByOrderThenId() {
        Feature a = new TestFeature("feat-a", 10, false);
        Feature b = new TestFeature("feat-b", 5, false);
        Feature c = new TestFeature("feat-c", 10, false);
        Feature d = new TestFeature("feat-d", 1, false);
        Seq<Feature> all = Seq.with(a, b, c, d);

        // Stored only has 'd'
        Seq<String> stored = Seq.with("feat-d");
        Seq<String> normalized = FeatureManager.normalizeOrder(all, stored);

        assertEquals(4, normalized.size);
        assertEquals("feat-d", normalized.get(0));
        assertEquals("feat-b", normalized.get(1)); // order 5
        assertEquals("feat-a", normalized.get(2)); // order 10, id 'a' < 'c'
        assertEquals("feat-c", normalized.get(3)); // order 10, id 'c'
    }

    @Test
    void normalize_corruptOrNullPayloadResetsToDefaultOrder() {
        Feature a = new TestFeature("feat-a", 10, false);
        Feature b = new TestFeature("feat-b", 5, false);
        Seq<Feature> all = Seq.with(a, b);

        Seq<String> normalized = FeatureManager.normalizeOrder(all, null);

        assertEquals(2, normalized.size);
        assertEquals("feat-b", normalized.get(0)); // order 5
        assertEquals("feat-a", normalized.get(1)); // order 10
    }

    @Test
    void init_cleanListCausesNoPersistWrite() {
        Feature a = new TestFeature("feat-a", 1, false);
        Feature b = new TestFeature("feat-b", 2, false);
        FeatureManager.register(a, b);

        // Pre-save clean normalized order into settings
        Seq<String> cleanOrder = Seq.with("feat-b", "feat-a");
        ModSettings.featureOrder.set(cleanOrder);

        // Spy on settings write by checking settings value before and after init
        FeatureManager.init();

        assertEquals(cleanOrder, ModSettings.featureOrder.get());
        assertEquals(2, FeatureManager.getFeatures().size);
        assertEquals(b, FeatureManager.getFeatures().get(0));
        assertEquals(a, FeatureManager.getFeatures().get(1));
    }

    @Test
    void init_corruptStorageResetsAndOverwrites() {
        Feature a = new TestFeature("feat-a", 10, false);
        Feature b = new TestFeature("feat-b", 5, false);
        FeatureManager.register(a, b);

        // Corrupt storage payload
        Core.settings.put(ModSettings.featureOrder.getKey(), "{not valid json");

        FeatureManager.init();

        Seq<String> healed = ModSettings.featureOrder.get();
        assertEquals(2, healed.size);
        assertEquals("feat-b", healed.get(0));
        assertEquals("feat-a", healed.get(1));
    }

    @Test
    void init_placesDevelopmentFeaturesLastSortedById() {
        Feature devZ = new TestFeature("dev-z", 1, true);
        Feature devA = new TestFeature("dev-a", 2, true);
        Feature normalB = new TestFeature("normal-b", 1, false);
        Feature normalA = new TestFeature("normal-a", 2, false);

        FeatureManager.register(devZ, devA, normalB, normalA);

        ModSettings.featureOrder.set(Seq.with("normal-b", "normal-a"));
        FeatureManager.init();

        Seq<Feature> list = FeatureManager.getFeatures();
        assertEquals(4, list.size);
        assertEquals(normalB, list.get(0));
        assertEquals(normalA, list.get(1));
        assertEquals(devA, list.get(2)); // dev block sorted by id
        assertEquals(devZ, list.get(3));
    }

    @Test
    void swap_adjacentMiddleExchangesPositionsAndPersists() {
        Feature a = new TestFeature("feat-a", 1, false);
        Feature b = new TestFeature("feat-b", 2, false);
        Feature c = new TestFeature("feat-c", 3, false);
        FeatureManager.register(a, b, c);

        ModSettings.featureOrder.set(Seq.with("feat-a", "feat-b", "feat-c"));
        FeatureManager.init();

        // Move b left
        assertTrue(FeatureManager.moveLeft(b));

        assertEquals(Seq.with("feat-b", "feat-a", "feat-c"), ModSettings.featureOrder.get());
        assertEquals(b, FeatureManager.getFeatures().get(0));
        assertEquals(a, FeatureManager.getFeatures().get(1));

        // Move b right
        assertTrue(FeatureManager.moveRight(b));
        assertEquals(Seq.with("feat-a", "feat-b", "feat-c"), ModSettings.featureOrder.get());
        assertEquals(a, FeatureManager.getFeatures().get(0));
        assertEquals(b, FeatureManager.getFeatures().get(1));
    }

    @Test
    void swap_boundariesAreDisabled() {
        Feature a = new TestFeature("feat-a", 1, false);
        Feature b = new TestFeature("feat-b", 2, false);
        Feature dev = new TestFeature("dev-x", 3, true);
        FeatureManager.register(a, b, dev);

        ModSettings.featureOrder.set(Seq.with("feat-a", "feat-b"));
        FeatureManager.init();

        // First item cannot move left
        assertFalse(FeatureManager.canMoveLeft(a));
        assertFalse(FeatureManager.moveLeft(a));
        assertFalse(FeatureManager.canMoveLeftSignal(a).get());

        // First item can move right
        assertTrue(FeatureManager.canMoveRight(a));
        assertTrue(FeatureManager.canMoveRightSignal(a).get());

        // Last non-dev item cannot move right
        assertFalse(FeatureManager.canMoveRight(b));
        assertFalse(FeatureManager.moveRight(b));
        assertFalse(FeatureManager.canMoveRightSignal(b).get());

        // Last non-dev item can move left
        assertTrue(FeatureManager.canMoveLeft(b));
        assertTrue(FeatureManager.canMoveLeftSignal(b).get());
    }

    @Test
    void swap_developmentFeaturesExcluded() {
        Feature a = new TestFeature("feat-a", 1, false);
        Feature dev = new TestFeature("dev-x", 2, true);
        FeatureManager.register(a, dev);

        ModSettings.featureOrder.set(Seq.with("feat-a"));
        FeatureManager.init();

        assertFalse(FeatureManager.canMoveLeft(dev));
        assertFalse(FeatureManager.canMoveRight(dev));
        assertFalse(FeatureManager.moveLeft(dev));
        assertFalse(FeatureManager.moveRight(dev));
        assertFalse(FeatureManager.canMoveLeftSignal(dev).get());
        assertFalse(FeatureManager.canMoveRightSignal(dev).get());
    }
}
