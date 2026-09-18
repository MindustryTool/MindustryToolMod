package mindustrytool.features.schematicgrid;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.Settings;
import arc.files.Fi;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import arc.struct.StringMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustrytool.features.FeatureManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QuickSchematicGridTest {

    @BeforeAll
    static void initCore() {
        if (Core.app == null) {
            Core.app = new MockApplication();
        }
        if (Core.graphics == null) {
            Core.graphics = new MockGraphics();
        }
        Icon.book = new TextureRegionDrawable();
    }

    @BeforeEach
    void setUp() {
        Core.settings = new Settings();
        Core.settings.clear();
        FeatureManager.clear();
    }

    @AfterEach
    void tearDown() {
        FeatureManager.clear();
        Core.settings.clear();
    }

    private static QuickSchematicEntry entry(String id, String name, String file) {
        return new QuickSchematicEntry(id, name, file, null, null, null);
    }

    private static List<QuickSchematicEntry> threeEntries() {
        return new ArrayList<>(Arrays.asList(
                entry("a", "Alpha", "alpha.msch"),
                entry("b", "Beta", "beta.msch"),
                entry("c", "Gamma", "gamma.msch")));
    }

    private static Schematic schematic(String name, String fileName) {
        Schematic schematic = new Schematic(new Seq<>(), StringMap.of("name", name), 10, 10);
        schematic.file = new Fi(fileName);
        return schematic;
    }

    @Test
    void serialization_roundTripPreservesEntries() {
        List<QuickSchematicEntry> original = threeEntries();
        original.get(0).customLabel = "Custom";

        String json = QuickSchematicEntry.toJson(original);
        assertNotNull(json);

        List<QuickSchematicEntry> parsed = QuickSchematicEntry.fromJson(json);
        assertEquals(original, parsed);
    }

    @Test
    void fromJson_handlesNullEmptyAndInvalid() {
        assertTrue(QuickSchematicEntry.fromJson(null).isEmpty());
        assertTrue(QuickSchematicEntry.fromJson("").isEmpty());
        assertTrue(QuickSchematicEntry.fromJson("   ").isEmpty());
        assertTrue(QuickSchematicEntry.fromJson("not-json").isEmpty());
    }

    @Test
    void displayName_prefersCustomLabel() {
        QuickSchematicEntry labeled = entry("a", "Alpha", "alpha.msch");
        labeled.customLabel = "My Label";
        assertEquals("My Label", labeled.displayName());

        QuickSchematicEntry plain = entry("b", "Beta", "beta.msch");
        assertEquals("Beta", plain.displayName());
    }

    @Test
    void moveById_swapsAdjacentEntries() {
        List<QuickSchematicEntry> list = threeEntries();

        assertTrue(QuickSchematicGridFeature.moveById(list, "b", -1));
        assertEquals("b", list.get(0).id);
        assertEquals("a", list.get(1).id);

        assertTrue(QuickSchematicGridFeature.moveById(list, "b", 1));
        assertEquals("a", list.get(0).id);
        assertEquals("b", list.get(1).id);
    }

    @Test
    void moveById_respectsBoundariesAndUnknownIds() {
        List<QuickSchematicEntry> list = threeEntries();

        assertFalse(QuickSchematicGridFeature.moveById(list, "a", -1));
        assertFalse(QuickSchematicGridFeature.moveById(list, "c", 1));
        assertFalse(QuickSchematicGridFeature.moveById(list, "unknown", 1));
        assertFalse(QuickSchematicGridFeature.moveById(list, null, 1));
        assertEquals("a", list.get(0).id);
        assertEquals("c", list.get(2).id);
    }

    @Test
    void removeById_deletesEntryAndShifts() {
        List<QuickSchematicEntry> list = threeEntries();

        assertTrue(QuickSchematicGridFeature.removeById(list, "b"));
        assertEquals(2, list.size());
        assertEquals("a", list.get(0).id);
        assertEquals("c", list.get(1).id);

        assertFalse(QuickSchematicGridFeature.removeById(list, "missing"));
        assertFalse(QuickSchematicGridFeature.removeById(list, null));
    }

    @Test
    void canMove_reflectsPositions() {
        List<QuickSchematicEntry> list = threeEntries();

        assertFalse(QuickSchematicGridFeature.canMoveEarlier(list, "a"));
        assertTrue(QuickSchematicGridFeature.canMoveLater(list, "a"));
        assertTrue(QuickSchematicGridFeature.canMoveEarlier(list, "b"));
        assertTrue(QuickSchematicGridFeature.canMoveLater(list, "b"));
        assertTrue(QuickSchematicGridFeature.canMoveEarlier(list, "c"));
        assertFalse(QuickSchematicGridFeature.canMoveLater(list, "c"));
        assertFalse(QuickSchematicGridFeature.canMoveEarlier(list, "unknown"));
    }

    @Test
    void feature_addRemoveMovePersistToJson() {
        QuickSchematicGridFeature feature = new QuickSchematicGridFeature();

        feature.addSchematic("Alpha", "alpha.msch");
        feature.addSchematic("Beta", "beta.msch");
        assertEquals(2, feature.getEntries().size());

        String persisted = feature.entriesJsonConfig.get();
        assertEquals(feature.getEntries(), QuickSchematicEntry.fromJson(persisted));

        String firstId = feature.getEntries().get(0).id;
        assertTrue(feature.moveLater(firstId));
        assertEquals("Beta", feature.getEntries().get(0).schematicName);

        assertTrue(feature.removeEntry(firstId));
        assertEquals(1, feature.getEntries().size());
        assertEquals(feature.getEntries(), QuickSchematicEntry.fromJson(feature.entriesJsonConfig.get()));
    }

    @Test
    void resolveIn_prefersFileThenName() {
        Seq<Schematic> all = Seq.with(
                schematic("Alpha", "alpha.msch"),
                schematic("Alpha", "alpha-copy.msch"),
                schematic("Beta", "beta.msch"));

        Schematic byFile = QuickSchematicGridFeature.resolveIn(all, "alpha-copy.msch", "Beta");
        assertNotNull(byFile);
        assertEquals("alpha-copy.msch", byFile.file.name());

        Schematic byName = QuickSchematicGridFeature.resolveIn(all, null, "Beta");
        assertNotNull(byName);
        assertEquals("Beta", byName.name());

        assertNull(QuickSchematicGridFeature.resolveIn(all, "missing.msch", "Missing"));
        assertNull(QuickSchematicGridFeature.resolveIn(null, "alpha.msch", "Alpha"));
    }
}
