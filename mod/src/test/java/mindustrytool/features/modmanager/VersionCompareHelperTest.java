package mindustrytool.features.modmanager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionCompareHelperTest {

    @Test
    void comparesNumericSegmentsCorrectly() {
        assertTrue(VersionCompareHelper.tryCompareVersion("1.2.10", "1.2.2") > 0);
        assertTrue(VersionCompareHelper.tryCompareVersion("1.2.2", "1.2.10") < 0);
        assertEquals(0, VersionCompareHelper.tryCompareVersion("v1.2.0", "1.2"));
        assertEquals(0, VersionCompareHelper.tryCompareVersion("V2.0.0", "2"));
    }

    @Test
    void ordersPreReleaseAndPostReleaseStages() {
        assertTrue(VersionCompareHelper.tryCompareVersion("1.0-alpha.1", "1.0-alpha.2") < 0);
        assertTrue(VersionCompareHelper.tryCompareVersion("1.0-alpha.2", "1.0-beta.1") < 0);
        assertTrue(VersionCompareHelper.tryCompareVersion("1.0-beta.1", "1.0-rc.1") < 0);
        assertTrue(VersionCompareHelper.tryCompareVersion("1.0-rc.1", "1.0") < 0);
        assertTrue(VersionCompareHelper.tryCompareVersion("1.0", "1.0-sp.1") < 0);
        assertTrue(VersionCompareHelper.tryCompareVersion("1.0-sp.1", "1.0-patch.2") < 0);
    }

    @Test
    void stripsBuildMetadataAndHandlesLargeNumbers() {
        assertEquals(0, VersionCompareHelper.tryCompareVersion("1.4.2+20260101", "1.4.2+20260999"));
        assertTrue(VersionCompareHelper.tryCompareVersion(
                "1.99999999999999999999999999",
                "1.99999999999999999999999998") > 0);
    }

    @Test
    void handlesNullAndEmptyVersionsGracefully() {
        assertEquals(0, VersionCompareHelper.tryCompareVersion(null, null));
        assertEquals(0, VersionCompareHelper.tryCompareVersion("", "   "));
        assertTrue(VersionCompareHelper.tryCompareVersion("1.0", "") > 0);
        assertTrue(VersionCompareHelper.tryCompareVersion("", "1.0") < 0);
    }
}
