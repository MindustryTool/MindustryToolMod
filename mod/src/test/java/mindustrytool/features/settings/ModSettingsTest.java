package mindustrytool.features.settings;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;

class ModSettingsTest {

    @BeforeAll
    static void initSettings() {
        Core.settings = new Settings();
    }

    @BeforeEach
    void clearSettings() {
        Core.settings.clear();
        ModSettings.betaParticipate.set(false);
    }

    @AfterEach
    void resetSettings() {
        ModSettings.betaParticipate.set(false);
        Core.settings.clear();
    }

    @Test
    void betaParticipate_defaultsFalse() {
        assertEquals(Boolean.FALSE, ModSettings.betaParticipate.get());
    }

    @Test
    void betaParticipate_persistsToCoreSettings() {
        ModSettings.betaParticipate.set(true);

        assertTrue(Core.settings.getBool("mindustrytool.settings.betaParticipate"));
        assertEquals(Boolean.TRUE, ModSettings.betaParticipate.get());
    }

    @Test
    void betaParticipate_reloadsAcrossSessions() {
        ModSettings.betaParticipate.set(true);

        ConfigValue<Boolean> reloaded =
                ConfigGroup.of("mindustrytool.settings").boolValue("betaParticipate", false);

        assertEquals(Boolean.TRUE, reloaded.get());
    }

    @Test
    void betaParticipate_keyNamespaced() {
        assertEquals("mindustrytool.settings.betaParticipate", ModSettings.betaParticipate.getKey());
        assertEquals("mindustrytool.settings", ModSettings.GROUP.getNamespace());
    }

    @Test
    void betaParticipate_signalReflectsSet() {
        ModSettings.betaParticipate.set(true);
        assertEquals(Boolean.TRUE, ModSettings.betaParticipate.signal().peek());

        ModSettings.betaParticipate.set(false);
        assertEquals(Boolean.FALSE, ModSettings.betaParticipate.signal().peek());
    }
}
