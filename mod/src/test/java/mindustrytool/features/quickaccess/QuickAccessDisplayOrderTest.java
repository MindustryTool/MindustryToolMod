package mindustrytool.features.quickaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.test.MindustryTestEnv;

class QuickAccessDisplayOrderTest extends MindustryTestEnv {

    static class TestFeature extends Feature {
        TestFeature(String id, boolean quickAccess, boolean dev) {
            super(FeatureMetadata.builder()
                    .id(id)
                    .icon(Icon.book)
                    .quickAccess(quickAccess)
                    .development(dev)
                    .build());
        }
    }

    @BeforeAll
    static void initCore() {
        Icon.book = new TextureRegionDrawable();
    }

    static Seq<Feature> candidates(TestFeature... features) {
        return Seq.with(features);
    }

    @Test
    void normalize_dropsStaleDuplicatesAndDevelopment() {
        TestFeature a = new TestFeature("qa-a", true, false);
        TestFeature b = new TestFeature("qa-b", true, false);

        Seq<String> normalized = QuickAccessFeature.normalizeDisplayOrder(
                candidates(a, b), Seq.with("qa-b", "stale-id", "qa-a", "qa-b"));

        assertEquals(Seq.with("qa-b", "qa-a"), normalized);
    }

    @Test
    void normalize_appendsMissingAtEndInRegistrationOrder() {
        TestFeature a = new TestFeature("qa-a", true, false);
        TestFeature b = new TestFeature("qa-b", true, false);
        TestFeature c = new TestFeature("qa-c", true, false);

        Seq<String> normalized = QuickAccessFeature.normalizeDisplayOrder(
                candidates(a, b, c), Seq.with("qa-b"));

        assertEquals(Seq.with("qa-b", "qa-a", "qa-c"), normalized);
    }

    @Test
    void normalize_nullStoredReturnsAllCandidates() {
        TestFeature a = new TestFeature("qa-a", true, false);
        TestFeature b = new TestFeature("qa-b", true, false);

        assertEquals(Seq.with("qa-a", "qa-b"), QuickAccessFeature.normalizeDisplayOrder(candidates(a, b), null));
    }

    @Test
    void isDisplayOrderDirty_comparesPositions() {
        assertTrue(QuickAccessFeature.isDisplayOrderDirty(null, Seq.with("a")));
        assertFalse(QuickAccessFeature.isDisplayOrderDirty(Seq.with("a", "b"), Seq.with("a", "b")));
        assertTrue(QuickAccessFeature.isDisplayOrderDirty(Seq.with("a", "b"), Seq.with("b", "a")));
        assertTrue(QuickAccessFeature.isDisplayOrderDirty(Seq.with("a"), Seq.with("a", "b")));
    }

    @Test
    void getDisplayOrder_healsAndPersists() {
        TestFeature a = new TestFeature("qa-a", true, false);
        TestFeature b = new TestFeature("qa-b", true, false);
        TestFeature c = new TestFeature("qa-c", true, false);
        FeatureManager.register(a, b, c);
        QuickAccessFeature feature = new QuickAccessFeature();

        feature.displayOrderConfig.set(Seq.with("qa-b", "stale-id"));

        assertEquals(Seq.with("qa-b", "qa-a", "qa-c"), feature.getDisplayOrder());
        assertEquals(Seq.with("qa-b", "qa-a", "qa-c"), feature.displayOrderConfig.get());
    }

    @Test
    void getDisplayOrder_defaultsToRegistrationOrder() {
        TestFeature a = new TestFeature("qa-a", true, false);
        TestFeature b = new TestFeature("qa-b", true, false);
        FeatureManager.register(a, b);
        QuickAccessFeature feature = new QuickAccessFeature();

        assertEquals(Seq.with("qa-a", "qa-b"), feature.getDisplayOrder());
    }

    @Test
    void moveUpAndDown_swapAdjacentAndPersist() {
        TestFeature a = new TestFeature("qa-a", true, false);
        TestFeature b = new TestFeature("qa-b", true, false);
        TestFeature c = new TestFeature("qa-c", true, false);
        FeatureManager.register(a, b, c);
        QuickAccessFeature feature = new QuickAccessFeature();
        feature.displayOrderConfig.set(Seq.with("qa-a", "qa-b", "qa-c"));

        assertTrue(feature.moveDown("qa-a"));
        assertEquals(Seq.with("qa-b", "qa-a", "qa-c"), feature.getDisplayOrder());

        assertTrue(feature.moveUp("qa-a"));
        assertEquals(Seq.with("qa-a", "qa-b", "qa-c"), feature.getDisplayOrder());

        assertEquals(Seq.with("qa-a", "qa-b", "qa-c"), feature.displayOrderConfig.get());
    }

    @Test
    void move_respectsBoundariesAndUnknownIds() {
        TestFeature a = new TestFeature("qa-a", true, false);
        TestFeature b = new TestFeature("qa-b", true, false);
        FeatureManager.register(a, b);
        QuickAccessFeature feature = new QuickAccessFeature();
        feature.displayOrderConfig.set(Seq.with("qa-a", "qa-b"));

        assertFalse(feature.moveUp("qa-a"));
        assertFalse(feature.moveDown("qa-b"));
        assertFalse(feature.moveUp("unknown-id"));
        assertFalse(feature.moveUp(null));
        assertEquals(Seq.with("qa-a", "qa-b"), feature.getDisplayOrder());
    }

    @Test
    void canMove_reflectsPositions() {
        TestFeature a = new TestFeature("qa-a", true, false);
        TestFeature b = new TestFeature("qa-b", true, false);
        FeatureManager.register(a, b);
        QuickAccessFeature feature = new QuickAccessFeature();
        feature.displayOrderConfig.set(Seq.with("qa-a", "qa-b"));

        assertFalse(feature.canMoveUp("qa-a"));
        assertTrue(feature.canMoveDown("qa-a"));
        assertTrue(feature.canMoveUp("qa-b"));
        assertFalse(feature.canMoveDown("qa-b"));
        assertFalse(feature.canMoveUp("unknown-id"));

        assertFalse(feature.canMoveUpSignal("qa-a").peek());
        assertTrue(feature.canMoveDownSignal("qa-a").peek());

        feature.moveDown("qa-a");

        assertTrue(feature.canMoveUpSignal("qa-a").peek());
        assertFalse(feature.canMoveDownSignal("qa-a").peek());
    }

    @Test
    void hiddenFeatures_keepPositionsAndParticipate() {
        TestFeature a = new TestFeature("qa-a", true, false);
        TestFeature b = new TestFeature("qa-b", true, false);
        TestFeature c = new TestFeature("qa-c", true, false);
        FeatureManager.register(a, b, c);
        QuickAccessFeature feature = new QuickAccessFeature();
        feature.displayOrderConfig.set(Seq.with("qa-a", "qa-b", "qa-c"));

        feature.setFeatureVisible("qa-b", false);

        assertTrue(feature.moveDown("qa-a"));
        assertEquals(Seq.with("qa-b", "qa-a", "qa-c"), feature.getDisplayOrder());
    }

    @Test
    void orderedFeatures_resolvesInOrderSkippingStale() {
        TestFeature a = new TestFeature("qa-a", true, false);
        TestFeature b = new TestFeature("qa-b", true, false);
        FeatureManager.register(a, b);
        QuickAccessFeature feature = new QuickAccessFeature();

        List<Feature> ordered = feature.orderedFeatures(Seq.with("qa-b", "stale-id", "qa-a"));

        assertEquals(2, ordered.size());
        assertSame(b, ordered.get(0));
        assertSame(a, ordered.get(1));
    }

    @Test
    void quickAccessFeatures_includesDefaultFalseAndExcludesDev() {
        TestFeature defaultOn = new TestFeature("qa-on", true, false);
        TestFeature defaultOff = new TestFeature("qa-off", false, false);
        TestFeature devStub = new TestFeature("qa-dev", true, true);
        FeatureManager.register(defaultOn, defaultOff, devStub);

        QuickAccessFeature feature = new QuickAccessFeature();
        Seq<Feature> candidates = feature.quickAccessFeatures();

        assertTrue(candidates.contains(defaultOn));
        assertTrue(candidates.contains(defaultOff));
        assertFalse(candidates.contains(devStub));
        assertFalse(candidates.contains(feature));
    }
}
