package mindustrytool.features.modmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mindustry.core.Version;
import org.junit.jupiter.api.Test;

class ModStatTest {

    @Test
    void parsesMajorVersionFromGameVersionStrings() {
        assertEquals(146, ModStat.parseMajorVersion("146.2"));
        assertEquals(136, ModStat.parseMajorVersion("v136"));
        assertEquals(150, ModStat.parseMajorVersion("V150.1-build2"));
        assertEquals(0, ModStat.parseMajorVersion(null));
        assertEquals(0, ModStat.parseMajorVersion(""));
        assertEquals(0, ModStat.parseMajorVersion("unknown"));
    }

    @Test
    void evaluatesBitmaskPredicatesAndValidity() {
        int cleanEnabledStat = ModStat.ENABLED | ModStat.UP_TO_DATE | ModStat.JAR_MOD;
        assertTrue(ModStat.isValid(cleanEnabledStat));
        assertTrue(ModStat.isEnabled(cleanEnabledStat));
        assertTrue(ModStat.isUpToDate(cleanEnabledStat));
        assertTrue(ModStat.isJAR(cleanEnabledStat));
        assertFalse(ModStat.isDeprecated(cleanEnabledStat));
        assertFalse(ModStat.isLibMissing(cleanEnabledStat));

        int brokenStat = cleanEnabledStat | ModStat.LIB_MISSING;
        assertFalse(ModStat.isValid(brokenStat));
        assertTrue(ModStat.isLibMissing(brokenStat));

        int circularStat = ModStat.LIB_CIRCLE_DEPENDING | ModStat.ERROR;
        assertFalse(ModStat.isValid(circularStat));
        assertTrue(ModStat.isLibCircleDepending(circularStat));
        assertTrue(ModStat.isError(circularStat));
    }

    @Test
    void evaluatesRemoteModStatFromModListing() {
        int previousBuild = Version.build;
        try {
            Version.build = 146;

            ModListing modernJavaMod = new ModListing();
            modernJavaMod.setHasJava(true);
            modernJavaMod.setHasScripts(true);
            modernJavaMod.setMinGameVersion("140");
            int modernStat = ModStat.checkRemoteModStat(modernJavaMod);
            assertTrue(ModStat.isValid(modernStat));
            assertTrue(ModStat.isJAR(modernStat));
            assertTrue(ModStat.isJS(modernStat));

            ModListing outdatedMod = new ModListing();
            outdatedMod.setMinGameVersion("126");
            int outdatedStat = ModStat.checkRemoteModStat(outdatedMod);
            assertTrue(ModStat.isDeprecated(outdatedStat));
            assertFalse(ModStat.isValid(outdatedStat));

            ModListing futureMod = new ModListing();
            futureMod.setMinGameVersion("999");
            int futureStat = ModStat.checkRemoteModStat(futureMod);
            assertTrue(ModStat.isUnsupported(futureStat));
            assertFalse(ModStat.isValid(futureStat));
        } finally {
            Version.build = previousBuild;
        }
    }
}
