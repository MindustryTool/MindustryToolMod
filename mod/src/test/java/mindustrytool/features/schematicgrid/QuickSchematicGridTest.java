package mindustrytool.features.schematicgrid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import arc.files.Fi;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import arc.struct.StringMap;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustrytool.test.MindustryTestEnv;
import solim.reactive.Computed;
import solim.reactive.Signal;

class QuickSchematicGridTest extends MindustryTestEnv {

    @BeforeAll
    static void initCore() {
        Icon.book = new TextureRegionDrawable();
    }

    private static QuickSchematicEntry entry(String id, String name, String file) {
        return new QuickSchematicEntry(id, name, file, null, null);
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
    void customIcon_serializationRoundTrip() {
        List<QuickSchematicEntry> original = threeEntries();
        original.get(1).customIcon = "X";

        List<QuickSchematicEntry> parsed = QuickSchematicEntry.fromJson(QuickSchematicEntry.toJson(original));
        assertEquals(original, parsed);
        assertEquals("X", parsed.get(1).customIcon);
        assertTrue(parsed.get(1).hasCustomIcon());
    }

    @Test
    void hasCustomIcon_reflectsValue() {
        assertFalse(entry("a", "Alpha", "alpha.msch").hasCustomIcon());

        QuickSchematicEntry blank = entry("b", "Beta", "beta.msch");
        blank.customIcon = "   ";
        assertFalse(blank.hasCustomIcon());

        QuickSchematicEntry set = entry("c", "Gamma", "gamma.msch");
        set.customIcon = "Y";
        assertTrue(set.hasCustomIcon());
    }

    @Test
    void fromJson_ignoresLegacyIconFields() {
        String legacy = "[{\"id\":\"a\",\"schematicName\":\"Alpha\",\"schematicFile\":\"alpha.msch\","
                + "\"customIconType\":\"icon\",\"customIconName\":\"home\",\"customLabel\":null}]";

        List<QuickSchematicEntry> parsed = QuickSchematicEntry.fromJson(legacy);
        assertEquals(1, parsed.size());
        assertEquals("a", parsed.get(0).id);
        assertEquals("Alpha", parsed.get(0).schematicName);
        assertNull(parsed.get(0).customIcon);
        assertFalse(parsed.get(0).hasCustomIcon());
    }

    @Test
    void updateEntry_mutatesAndPersists() {
        QuickSchematicGridFeature feature = new QuickSchematicGridFeature();
        feature.addSchematic("Alpha", "alpha.msch");
        String id = feature.getEntries().get(0).id;

        assertTrue(feature.updateEntry(id, entry -> {
            entry.customLabel = "Custom";
            entry.customIcon = "Z";
        }));

        QuickSchematicEntry stored = feature.getEntry(id);
        assertNotNull(stored);
        assertEquals("Custom", stored.customLabel);
        assertEquals("Z", stored.customIcon);
        assertEquals("Custom", stored.displayName());
        assertEquals(feature.getEntries(), QuickSchematicEntry.fromJson(feature.entriesJsonConfig.get()));

        assertFalse(feature.updateEntry("unknown", entry -> entry.customLabel = "X"));
        assertFalse(feature.updateEntry(null, entry -> entry.customLabel = "X"));
        assertFalse(feature.updateEntry(id, null));
        assertNull(feature.getEntry("unknown"));
    }

    @Test
    void replaceEntries_persistsFullOrder() {
        QuickSchematicGridFeature feature = new QuickSchematicGridFeature();
        feature.addSchematic("Alpha", "alpha.msch");
        feature.addSchematic("Beta", "beta.msch");
        feature.addSchematic("Gamma", "gamma.msch");

        List<QuickSchematicEntry> reordered = new ArrayList<>(feature.getEntries());
        QuickSchematicEntry first = reordered.remove(0);
        reordered.add(first);
        feature.replaceEntries(reordered);

        List<QuickSchematicEntry> stored = feature.getEntries();
        assertEquals(3, stored.size());
        assertEquals("Beta", stored.get(0).schematicName);
        assertEquals("Alpha", stored.get(2).schematicName);
        assertEquals(stored, QuickSchematicEntry.fromJson(feature.entriesJsonConfig.get()));

        feature.replaceEntries(null);
        assertTrue(feature.getEntries().isEmpty());
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

    @Test
    void coordinates_roundTripAndEquality() {
        QuickSchematicEntry entry = QuickSchematicEntry.of(2, 1, 3, "Thorium", "thorium.msch");
        assertEquals(2, entry.page);
        assertEquals(1, entry.row);
        assertEquals(3, entry.col);

        String json = QuickSchematicEntry.toJson(Arrays.asList(entry));
        List<QuickSchematicEntry> parsed = QuickSchematicEntry.fromJson(json);
        assertEquals(1, parsed.size());
        assertEquals(entry, parsed.get(0));
        assertEquals(2, parsed.get(0).page);
        assertEquals(1, parsed.get(0).row);
        assertEquals(3, parsed.get(0).col);
    }

    @Test
    void slotManagement_setGetAndClear() {
        QuickSchematicGridFeature feature = new QuickSchematicGridFeature();
        feature.addSchematic(0, 1, 2, "Reactor", "reactor.msch");

        QuickSchematicEntry entry = feature.getEntryAt(0, 1, 2);
        assertNotNull(entry);
        assertEquals("Reactor", entry.schematicName);

        assertNull(feature.getEntryAt(0, 0, 0));
        assertNull(feature.getEntryAt(1, 1, 2));

        // Overwrite slot
        feature.addSchematic(0, 1, 2, "Solar", "solar.msch");
        assertEquals(1, feature.getEntries().size());
        assertEquals("Solar", feature.getEntryAt(0, 1, 2).schematicName);

        // Clear slot
        assertTrue(feature.clearSlotAt(0, 1, 2));
        assertNull(feature.getEntryAt(0, 1, 2));
        assertFalse(feature.clearSlotAt(0, 1, 2));
    }

    @Test
    void pageManagement_addDeleteAndReindex() {
        QuickSchematicGridFeature feature = new QuickSchematicGridFeature();
        assertEquals(1, feature.pageCountConfig.get().intValue());

        assertTrue(feature.addPage());
        assertEquals(2, feature.pageCountConfig.get().intValue());
        assertEquals(1, feature.getActivePage());

        feature.addSchematic(0, 0, 0, "Page0Item", null);
        feature.addSchematic(1, 0, 0, "Page1Item", null);

        assertTrue(feature.addPage()); // pageCount = 3
        feature.addSchematic(2, 0, 0, "Page2Item", null);

        // Delete middle page (page 1)
        assertTrue(feature.deletePage(1));
        assertEquals(2, feature.pageCountConfig.get().intValue());

        // Page 0 entry intact
        assertNotNull(feature.getEntryAt(0, 0, 0));
        assertEquals("Page0Item", feature.getEntryAt(0, 0, 0).schematicName);

        // Former Page 2 entry shifted to Page 1
        assertNotNull(feature.getEntryAt(1, 0, 0));
        assertEquals("Page2Item", feature.getEntryAt(1, 0, 0).schematicName);

        // Cannot delete below 1 page
        assertTrue(feature.deletePage(1));
        assertEquals(1, feature.pageCountConfig.get().intValue());
        assertFalse(feature.deletePage(0)); // Only 1 page left
    }

    @Test
    void pageIcons_getSetAndClear() {
        QuickSchematicGridFeature feature = new QuickSchematicGridFeature();

        assertNull(feature.getPageIcon(0));
        assertNull(feature.getPageIcon(1));

        feature.setPageIcon(0, "⚡");
        feature.setPageIcon(2, "🛡");

        assertEquals("⚡", feature.getPageIcon(0));
        assertNull(feature.getPageIcon(1));
        assertEquals("🛡", feature.getPageIcon(2));

        String json = feature.pageIconsJsonConfig.get();
        assertNotNull(json);
        assertTrue(json.contains("⚡"));
        assertTrue(json.contains("🛡"));

        feature.clearPageIcon(0);
        assertNull(feature.getPageIcon(0));
        assertEquals("🛡", feature.getPageIcon(2));

        // Blank string behaves as null
        feature.setPageIcon(2, "   ");
        assertNull(feature.getPageIcon(2));
    }

    @Test
    void pageIcons_deletePageShiftsIcons() {
        QuickSchematicGridFeature feature = new QuickSchematicGridFeature();
        assertTrue(feature.addPage()); // page 1
        assertTrue(feature.addPage()); // page 2

        feature.setPageIcon(0, "Icon0");
        feature.setPageIcon(1, "Icon1");
        feature.setPageIcon(2, "Icon2");

        // Delete middle page (page 1)
        assertTrue(feature.deletePage(1));

        assertEquals("Icon0", feature.getPageIcon(0));
        assertEquals("Icon2", feature.getPageIcon(1)); // Shifted from index 2 to index 1
        assertNull(feature.getPageIcon(2));
    }

    @Test
    void pageIcons_persistenceAcrossInstances() {
        QuickSchematicGridFeature feature1 = new QuickSchematicGridFeature();
        feature1.addPage();
        feature1.setPageIcon(0, "First");
        feature1.setPageIcon(1, "Second");

        // New feature instance reading same settings
        QuickSchematicGridFeature feature2 = new QuickSchematicGridFeature();
        assertEquals("First", feature2.getPageIcon(0));
        assertEquals("Second", feature2.getPageIcon(1));
    }

    @Test
    void pagePosition_defaultsToLeftAndHorizontalReflectsValue() {
        QuickSchematicGridFeature feature = new QuickSchematicGridFeature();

        assertEquals(QuickSchematicGridFeature.PAGE_LEFT, feature.pagePositionConfig.get());
        assertEquals(QuickSchematicGridFeature.PAGE_LEFT, feature.getPagePosition());
        assertFalse(feature.isPageHorizontal());

        feature.pagePositionConfig.set(QuickSchematicGridFeature.PAGE_TOP);
        assertEquals(QuickSchematicGridFeature.PAGE_TOP, feature.getPagePosition());
        assertTrue(feature.isPageHorizontal());

        feature.pagePositionConfig.set(QuickSchematicGridFeature.PAGE_BOTTOM);
        assertTrue(feature.isPageHorizontal());

        feature.pagePositionConfig.set(QuickSchematicGridFeature.PAGE_RIGHT);
        assertFalse(feature.isPageHorizontal());
    }

    @Test
    void pagePosition_normalizesUnknownToLeft() {
        QuickSchematicGridFeature feature = new QuickSchematicGridFeature();
        feature.pagePositionConfig.set("diagonal");

        assertEquals(QuickSchematicGridFeature.PAGE_LEFT, feature.getPagePosition());
        assertFalse(feature.isPageHorizontal());
    }

    @Test
    void pagePosition_persistsAcrossInstances() {
        QuickSchematicGridFeature feature1 = new QuickSchematicGridFeature();
        feature1.pagePositionConfig.set(QuickSchematicGridFeature.PAGE_RIGHT);

        QuickSchematicGridFeature feature2 = new QuickSchematicGridFeature();
        assertEquals(QuickSchematicGridFeature.PAGE_RIGHT, feature2.pagePositionConfig.get());
        assertEquals(QuickSchematicGridFeature.PAGE_RIGHT, feature2.getPagePosition());
    }

    @Test
    void buttonOpacity_defaultsToFullAndClampsOnRead() {
        QuickSchematicGridFeature feature = new QuickSchematicGridFeature();

        assertEquals(1f, feature.buttonOpacityConfig.get(), 0.001f);
        assertEquals(1f, feature.getButtonOpacity(), 0.001f);

        feature.buttonOpacityConfig.set(0.5f);
        assertEquals(0.5f, feature.buttonOpacityConfig.get(), 0.001f);
        assertEquals(0.5f, feature.getButtonOpacity(), 0.001f);

        feature.buttonOpacityConfig.set(0f);
        assertEquals(QuickSchematicGridFeature.MIN_BUTTON_OPACITY, feature.getButtonOpacity(), 0.001f);

        feature.buttonOpacityConfig.set(2f);
        assertEquals(QuickSchematicGridFeature.MAX_BUTTON_OPACITY, feature.getButtonOpacity(), 0.001f);
    }

    @Test
    void buttonOpacity_persistsAcrossInstances() {
        QuickSchematicGridFeature feature1 = new QuickSchematicGridFeature();
        feature1.buttonOpacityConfig.set(0.6f);

        QuickSchematicGridFeature feature2 = new QuickSchematicGridFeature();
        assertEquals(0.6f, feature2.buttonOpacityConfig.get(), 0.001f);
        assertEquals(0.6f, feature2.getButtonOpacity(), 0.001f);
    }

    @Test
    void pickerBatching_takeFirstCapsToAvailableItems() {
        Seq<Schematic> all = Seq.with(
                schematic("Alpha", "alpha.msch"),
                schematic("Beta", "beta.msch"),
                schematic("Gamma", "gamma.msch"));

        assertEquals(2, SchematicPickerDialog.takeFirst(all, 2).size);
        assertEquals(3, SchematicPickerDialog.takeFirst(all, 36).size);
        assertEquals(0, SchematicPickerDialog.takeFirst(all, 0).size);
        assertTrue(SchematicPickerDialog.takeFirst(null, 36).isEmpty());
    }

    private static QuickSchematicGridHudView.SlotModel slotOf(QuickSchematicEntry entry) {
        return new QuickSchematicGridHudView.SlotModel(entry.page, entry.row, entry.col, entry);
    }

    private static QuickSchematicEntry slotEntry() {
        return new QuickSchematicEntry("slot-1", 0, 1, 2, "Alpha", "alpha.msch", null, null);
    }

    @Test
    void slotKey_changesWhenIconSetClearedOrChanged() {
        QuickSchematicEntry entry = slotEntry();
        String base = slotOf(entry).key();

        entry.customIcon = "X";
        String withIcon = slotOf(entry).key();
        assertNotEquals(base, withIcon);

        entry.customIcon = "Y";
        assertNotEquals(withIcon, slotOf(entry).key());

        entry.customIcon = null;
        assertEquals(base, slotOf(entry).key());
    }

    @Test
    void slotKey_normalizesBlankIconAndLabel() {
        QuickSchematicEntry entry = slotEntry();
        String base = slotOf(entry).key();

        entry.customIcon = "";
        assertEquals(base, slotOf(entry).key());

        entry.customIcon = "   ";
        assertEquals(base, slotOf(entry).key());

        entry.customIcon = null;
        entry.customLabel = "   ";
        assertEquals(base, slotOf(entry).key());
    }

    @Test
    void slotKey_changesWhenSchematicRefOrLabelChanges() {
        QuickSchematicEntry entry = slotEntry();
        String base = slotOf(entry).key();

        entry.schematicName = "Beta";
        assertNotEquals(base, slotOf(entry).key());

        entry.schematicName = "Alpha";
        entry.schematicFile = "alpha-copy.msch";
        assertNotEquals(base, slotOf(entry).key());

        entry.schematicFile = "alpha.msch";
        entry.customLabel = "Custom";
        assertNotEquals(base, slotOf(entry).key());
    }

    @Test
    void slotKey_preservesSlotIdentity() {
        QuickSchematicEntry entry = slotEntry();
        String base = slotOf(entry).key();

        assertNotEquals(base, new QuickSchematicGridHudView.SlotModel(1, 1, 2, entry).key());
        assertNotEquals(base, new QuickSchematicGridHudView.SlotModel(0, 0, 2, entry).key());
        assertNotEquals(base, new QuickSchematicGridHudView.SlotModel(0, 1, 2, null).key());

        QuickSchematicEntry otherId = new QuickSchematicEntry("slot-2", 0, 1, 2, "Alpha", "alpha.msch", null, null);
        assertNotEquals(base, slotOf(otherId).key());
    }

    @Test
    void updateEntry_replacesInsteadOfMutating() {
        QuickSchematicGridFeature feature = new QuickSchematicGridFeature();
        feature.addSchematic("Alpha", "alpha.msch");
        String id = feature.getEntries().get(0).id;

        QuickSchematicEntry before = feature.getEntry(id);
        assertNotNull(before);
        assertNull(before.customIcon);

        assertTrue(feature.updateEntry(id, entry -> entry.customIcon = "X"));

        assertNull(before.customIcon);

        QuickSchematicEntry stored = feature.getEntry(id);
        assertNotNull(stored);
        assertEquals("X", stored.customIcon);
    }

    @Test
    void updateEntry_notifiesEntriesObservers() {
        QuickSchematicGridFeature feature = new QuickSchematicGridFeature();
        feature.addSchematic("Alpha", "alpha.msch");
        String id = feature.getEntries().get(0).id;

        Computed<List<QuickSchematicEntry>> watcher = Signal.computed(() -> feature.entries().get());
        int[] notifications = {0};
        watcher.subscribe(value -> notifications[0]++);
        watcher.get();
        int before = notifications[0];

        assertTrue(feature.updateEntry(id, entry -> entry.customIcon = "X"));

        assertEquals(before + 1, notifications[0]);
        assertEquals("X", feature.getEntry(id).customIcon);
    }
}
