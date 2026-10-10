package mindustrytool.features.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import mindustrytool.test.MindustryTestEnv;

class MindustryTranslationDictionaryTest extends MindustryTestEnv {

    @Test
    void testNullAndEmptyInputs() {
        assertNull(MindustryTranslationDictionary.findTranslation(null, "English"));
        assertNull(MindustryTranslationDictionary.findTranslation("", "English"));
        assertNull(MindustryTranslationDictionary.findTranslation("   ", "English"));
        assertNull(MindustryTranslationDictionary.findTranslation("help", null));
        assertNull(MindustryTranslationDictionary.findTranslation("help", ""));
    }

    @Test
    void testTacticalTranslations() {
        // Resource requests
        assertEquals("Cần silicon", MindustryTranslationDictionary.findTranslation("need silicon", "Vietnamese"));
        assertEquals("Need silicon", MindustryTranslationDictionary.findTranslation("cần silicon", "English"));
        assertEquals("Need thorium", MindustryTranslationDictionary.findTranslation("нужен торий", "English"));
        assertEquals("需要钍", MindustryTranslationDictionary.findTranslation("need thorium", "Chinese"));

        // Tactical callouts
        assertEquals("Tấn công!", MindustryTranslationDictionary.findTranslation("attack", "Vietnamese"));
        assertEquals("Attack!", MindustryTranslationDictionary.findTranslation("tấn công!", "English"));
        assertEquals("Атакуем!", MindustryTranslationDictionary.findTranslation("attack!", "Russian"));
        assertEquals("Retreat!", MindustryTranslationDictionary.findTranslation("rút lui", "English"));
        assertEquals("Defend the core!", MindustryTranslationDictionary.findTranslation("bảo vệ core", "English"));

        // Punctuation normalization
        assertEquals("Help!", MindustryTranslationDictionary.findTranslation("cứu???", "English"));
        assertEquals("Cứu với!", MindustryTranslationDictionary.findTranslation("help!!!", "Vietnamese"));
    }

    @Test
    void testUnknownWordFallsBack() {
        assertNull(MindustryTranslationDictionary.findTranslation("This is a random sentence not in dictionary", "Vietnamese"));
        assertNull(MindustryTranslationDictionary.findTranslation("build lancers at choke point", "English"));
    }
}
