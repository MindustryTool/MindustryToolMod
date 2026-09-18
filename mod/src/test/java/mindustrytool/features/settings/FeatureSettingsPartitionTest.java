package mindustrytool.features.settings;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.Settings;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FeatureSettingsPartitionTest {

    static class DummyFeature extends Feature {
        private final String customName;

        DummyFeature(String id, String customName, int order) {
            super(FeatureMetadata.builder()
                    .id(id)
                    .icon(Icon.book != null ? Icon.book : new TextureRegionDrawable())
                    .order(order)
                    .build());
            this.customName = customName;
        }

        @Override
        public String getName() {
            return customName;
        }
    }

    @BeforeAll
    static void initCore() {
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        Icon.book = new TextureRegionDrawable();
        Icon.star = new TextureRegionDrawable();
    }

    @BeforeEach
    void setUp() {
        Core.settings = new Settings();
        Core.settings.clear();
        FeatureManager.clear();
        ModSettings.featureOrder.set(new Seq<>());
        ModSettings.favoriteFeatures.set(Collections.emptySet());
    }

    @AfterEach
    void tearDown() {
        FeatureManager.clear();
        ModSettings.featureOrder.set(new Seq<>());
        ModSettings.favoriteFeatures.set(Collections.emptySet());
        Core.settings.clear();
    }

    @Test
    void matchesFilter_checksCaseInsensitiveName() {
        Feature feat = new DummyFeature("camera-zoom", "Camera Zoom", 1);

        assertTrue(FeatureSettingsView.matchesFilter(feat, ""));
        assertTrue(FeatureSettingsView.matchesFilter(feat, "   "));
        assertTrue(FeatureSettingsView.matchesFilter(feat, null));
        assertTrue(FeatureSettingsView.matchesFilter(feat, "camera"));
        assertTrue(FeatureSettingsView.matchesFilter(feat, "ZOOM"));
        assertTrue(FeatureSettingsView.matchesFilter(feat, "era"));
        assertFalse(FeatureSettingsView.matchesFilter(feat, "radar"));
    }

    @Test
    void partitioning_separatesFavoritesFromStandardFeatures() {
        Feature f1 = new DummyFeature("feat-1", "Feature One", 1);
        Feature f2 = new DummyFeature("feat-2", "Feature Two", 2);
        Feature f3 = new DummyFeature("feat-3", "Feature Three", 3);

        Seq<Feature> all = Seq.with(f1, f2, f3);
        Set<String> favorites = new HashSet<>(Collections.singletonList("feat-2"));

        Seq<Feature> favList = all.select(f -> favorites.contains(f.getMetadata().getId()));
        Seq<Feature> stdList = all.select(f -> !favorites.contains(f.getMetadata().getId()));

        assertEquals(1, favList.size);
        assertEquals("feat-2", favList.get(0).getMetadata().getId());

        assertEquals(2, stdList.size);
        assertEquals("feat-1", stdList.get(0).getMetadata().getId());
        assertEquals("feat-3", stdList.get(1).getMetadata().getId());
    }

    @Test
    void partitioning_searchFiltersBothPartitions() {
        Feature f1 = new DummyFeature("feat-1", "Alpha Drill", 1);
        Feature f2 = new DummyFeature("feat-2", "Beta Zoom", 2);
        Feature f3 = new DummyFeature("feat-3", "Alpha Zoom", 3);

        Seq<Feature> all = Seq.with(f1, f2, f3);
        Set<String> favorites = new HashSet<>(Arrays.asList("feat-2", "feat-3"));

        String query = "drill";
        Seq<Feature> matching = all.select(f -> FeatureSettingsView.matchesFilter(f, query));

        Seq<Feature> favMatching = matching.select(f -> favorites.contains(f.getMetadata().getId()));
        Seq<Feature> stdMatching = matching.select(f -> !favorites.contains(f.getMetadata().getId()));

        assertTrue(favMatching.isEmpty());
        assertEquals(1, stdMatching.size);
        assertEquals("feat-1", stdMatching.get(0).getMetadata().getId());

        String queryZoom = "zoom";
        Seq<Feature> matchingZoom = all.select(f -> FeatureSettingsView.matchesFilter(f, queryZoom));
        Seq<Feature> favMatchingZoom = matchingZoom.select(f -> favorites.contains(f.getMetadata().getId()));
        Seq<Feature> stdMatchingZoom = matchingZoom.select(f -> !favorites.contains(f.getMetadata().getId()));

        assertEquals(2, favMatchingZoom.size);
        assertTrue(stdMatchingZoom.isEmpty());
    }
}
