package mindustrytool.features;

import static org.junit.jupiter.api.Assertions.*;

import arc.scene.style.TextureRegionDrawable;
import org.junit.jupiter.api.Test;
import solim.test.SolimEnv;

class FeatureMetadataTest extends SolimEnv {

    @Test
    void defaults_quickAccessByDefaultIsFalse() {
        FeatureMetadata meta = FeatureMetadata.builder()
                .id("test-feat")
                .icon(new TextureRegionDrawable())
                .build();

        assertFalse(meta.isQuickAccessByDefault());
        assertFalse(meta.isQuickAccess());
    }

    @Test
    void explicitQuickAccessByDefault_setsValueAndAliasMatches() {
        FeatureMetadata metaTrue = FeatureMetadata.builder()
                .id("test-true")
                .icon(new TextureRegionDrawable())
                .quickAccessByDefault(true)
                .build();

        assertTrue(metaTrue.isQuickAccessByDefault());
        assertTrue(metaTrue.isQuickAccess());

        FeatureMetadata metaAlias = FeatureMetadata.builder()
                .id("test-alias")
                .icon(new TextureRegionDrawable())
                .quickAccess(true)
                .build();

        assertTrue(metaAlias.isQuickAccessByDefault());
        assertTrue(metaAlias.isQuickAccess());
    }
}
