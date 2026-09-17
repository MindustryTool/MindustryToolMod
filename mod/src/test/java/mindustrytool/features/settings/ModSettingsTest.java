package mindustrytool.features.settings;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.Settings;
import arc.struct.Seq;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.config.OrderedSeqPersister;

class ModSettingsTest {

    @BeforeAll
    static void initSettings() {
        Core.settings = new Settings();
    }

    @BeforeEach
    void clearSettings() {
        Core.settings.clear();
        ModSettings.betaParticipate.set(false);
        ModSettings.featureOrder.set(new Seq<>());
        ModSettings.favoriteFeatures.set(java.util.Collections.emptySet());
    }

    @AfterEach
    void resetSettings() {
        ModSettings.betaParticipate.set(false);
        ModSettings.featureOrder.set(new Seq<>());
        ModSettings.favoriteFeatures.set(java.util.Collections.emptySet());
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

    @Test
    void featureOrder_defaultsEmpty() {
        assertNotNull(ModSettings.featureOrder.get());
        assertTrue(ModSettings.featureOrder.get().isEmpty());
    }

    @Test
    void featureOrder_persistsAndReloads() {
        Seq<String> order = Seq.with("feat-b", "feat-a");
        ModSettings.featureOrder.set(order);

        assertEquals(2, ModSettings.featureOrder.get().size);
        assertEquals("feat-b", ModSettings.featureOrder.get().get(0));
        assertEquals("feat-a", ModSettings.featureOrder.get().get(1));

        ConfigValue<Seq<String>> reloaded =
                ModSettings.GROUP.value("feature-order", new Seq<>(), new OrderedSeqPersister());
        assertEquals(2, reloaded.get().size);
        assertEquals("feat-b", reloaded.get().get(0));
        assertEquals("feat-a", reloaded.get().get(1));
    }

    @Test
    void featureOrder_keyNamespaced() {
        assertEquals("mindustrytool.settings.feature-order", ModSettings.featureOrder.getKey());
    }

    @Test
    void favoriteFeatures_defaultsEmpty() {
        assertNotNull(ModSettings.favoriteFeatures.get());
        assertTrue(ModSettings.favoriteFeatures.get().isEmpty());
    }

    @Test
    void favoriteFeatures_persistsAndReloads() {
        Set<String> favs = new HashSet<>(Arrays.asList("feat-a", "feat-b"));
        ModSettings.favoriteFeatures.set(favs);

        assertEquals(2, ModSettings.favoriteFeatures.get().size());
        assertTrue(ModSettings.favoriteFeatures.get().contains("feat-a"));
        assertTrue(ModSettings.favoriteFeatures.get().contains("feat-b"));

        ConfigValue<Set<String>> reloaded =
                ModSettings.GROUP.setValue("favorites", String.class, Collections.emptySet());
        assertEquals(2, reloaded.get().size());
        assertTrue(reloaded.get().contains("feat-a"));
        assertTrue(reloaded.get().contains("feat-b"));
    }

    @Test
    void favoriteFeatures_keyNamespaced() {
        assertEquals("mindustrytool.settings.favorites", ModSettings.favoriteFeatures.getKey());
    }

    @Test
    void favoriteFeatures_signalReflectsSet() {
        Set<String> favs = new HashSet<>(Collections.singletonList("feat-x"));
        ModSettings.favoriteFeatures.set(favs);
        assertEquals(favs, ModSettings.favoriteFeatures.signal().peek());

        ModSettings.favoriteFeatures.set(Collections.emptySet());
        assertEquals(Collections.emptySet(), ModSettings.favoriteFeatures.signal().peek());
    }

    @Test
    void favoriteFeatures_toggleMembership() {
        String id = "feat-toggle";
        Set<String> current = ModSettings.favoriteFeatures.get();
        Set<String> updated = current != null ? new HashSet<>(current) : new HashSet<>();
        updated.add(id);
        ModSettings.favoriteFeatures.set(updated);

        assertTrue(ModSettings.favoriteFeatures.get().contains(id));

        Set<String> next = new HashSet<>(ModSettings.favoriteFeatures.get());
        next.remove(id);
        ModSettings.favoriteFeatures.set(next);

        assertFalse(ModSettings.favoriteFeatures.get().contains(id));
    }
}
