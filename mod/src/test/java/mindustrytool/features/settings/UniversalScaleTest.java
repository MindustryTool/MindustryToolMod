package mindustrytool.features.settings;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.Settings;
import mindustrytool.test.MindustryTestEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.reactive.Computed;
import solim.reactive.Readable;
import solim.reactive.Signal;

class UniversalScaleTest extends MindustryTestEnv {

    @BeforeAll
    static void initSettings() {
        if (Core.settings == null) {
            Core.settings = new Settings();
        }
    }

    @BeforeEach
    void clearSettings() {
        Core.settings.clear();
        ModSettings.universalScale.set(1f);
    }

    @AfterEach
    void resetSettings() {
        ModSettings.universalScale.set(1f);
        Core.settings.clear();
    }

    @Test
    void universalScale_defaultsToOneOnDesktop() {
        Core.settings.clear();
        ModSettings.universalScale.reset();
        assertEquals(1f, ModSettings.universalScale.get(), 0.0001f);
    }

    @Test
    void universalScale_persistsToCoreSettings() {
        ModSettings.universalScale.set(1.2f);

        assertEquals(1.2f, Core.settings.getFloat("mindustrytool.settings.universal-scale", 0f), 0.0001f);
        assertEquals(1.2f, ModSettings.universalScale.get(), 0.0001f);
    }

    @Test
    void effectiveScale_reactiveMultipliesUniversalAndFeature() {
        ModSettings.universalScale.set(1f);
        Signal<Float> featureScale = Signal.of(1.2f);
        Computed<Float> effective = ModSettings.effectiveScale(featureScale);
        try {
            assertEquals(1.2f, effective.get(), 0.0001f);

            ModSettings.universalScale.set(0.8f);
            assertEquals(0.96f, effective.get(), 0.0001f);

            featureScale.set(1.5f);
            assertEquals(1.2f, effective.get(), 0.0001f);
        } finally {
            effective.dispose();
        }
    }

    @Test
    void effectiveScale_fallsBackToOneWhenFeatureNull() {
        ModSettings.universalScale.set(0.8f);
        Computed<Float> effective = ModSettings.effectiveScale((Readable<Float>) null);
        try {
            assertEquals(0.8f, effective.get(), 0.0001f);
        } finally {
            effective.dispose();
        }
    }

    @Test
    void effectiveScale_treatsNullValuesAsOne() {
        ModSettings.universalScale.set(0.8f);
        Computed<Float> effective = ModSettings.effectiveScale(Readable.of(null));
        try {
            assertEquals(0.8f, effective.get(), 0.0001f);
        } finally {
            effective.dispose();
        }
    }

    @Test
    void effectiveScale_floatOverloadMultiplies() {
        ModSettings.universalScale.set(0.8f);
        assertEquals(1.2f, ModSettings.effectiveScale(1.5f), 0.0001f);
    }
}
