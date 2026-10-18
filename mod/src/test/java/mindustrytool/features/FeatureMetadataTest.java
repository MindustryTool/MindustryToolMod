package mindustrytool.features;

import static org.junit.jupiter.api.Assertions.*;

import arc.scene.style.TextureRegionDrawable;
import org.junit.jupiter.api.Test;
import solim.test.ArcTestEnv;

class FeatureMetadataTest extends ArcTestEnv {

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

    @Test
    void defaults_displayGroupIsFalse() {
        FeatureMetadata meta = FeatureMetadata.builder()
                .id("test-feat")
                .icon(new TextureRegionDrawable())
                .build();

        assertFalse(meta.isDisplayGroup());
    }

    @Test
    void explicitDisplayGroup_setsValue() {
        FeatureMetadata member = FeatureMetadata.builder()
                .id("test-member")
                .icon(new TextureRegionDrawable())
                .displayGroup(true)
                .build();

        assertTrue(member.isDisplayGroup());
    }

    @Test
    void displayGroup_isIndependentOfOtherMetadataAxes() {
        FeatureMetadata member = FeatureMetadata.builder()
                .id("test-independent")
                .icon(new TextureRegionDrawable())
                .order(7)
                .quickAccessByDefault(false)
                .development(false)
                .displayGroup(true)
                .build();

        assertTrue(member.isDisplayGroup());
        assertFalse(member.isQuickAccessByDefault());
        assertFalse(member.isDevelopment());
        assertEquals(7, member.getOrder());
    }

    @Test
    void changingOrderDoesNotAffectDisplayGroupMembership() {
        FeatureMetadata reordered = FeatureMetadata.builder()
                .id("test-reordered")
                .icon(new TextureRegionDrawable())
                .order(99)
                .displayGroup(true)
                .build();

        assertTrue(reordered.isDisplayGroup());
        assertEquals(99, reordered.getOrder());
    }
}
