package mindustrytool.features.prettychat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import mindustrytool.test.MindustryTestEnv;

class BuiltinPrettiersTest extends MindustryTestEnv {

    @Test
    void testRainbowPrettier() {
        Prettier p = new BuiltinPrettiers.RainbowPrettier();
        assertEquals("rainbow", p.id());
        String out = p.transform("hello world");
        assertTrue(out.startsWith("["), "Should start with a color tag");
        assertTrue(out.endsWith("[]"), "Should end with clear tag");
        assertTrue(out.contains("hello"));
        assertTrue(out.contains("world"));
    }

    @Test
    void testRainbowPrettierAdaptiveClustering() {
        BuiltinPrettiers.RainbowPrettier p = new BuiltinPrettiers.RainbowPrettier();
        String longText = "This is a rather long sentence with fifteen different words to test adaptive clustering";
        String out = p.transform(longText);
        assertTrue(out.length() <= 150, "Output length must stay <= 150 (was " + out.length() + ")");
        assertTrue(out.endsWith("[]"), "Should end with clear tag");
        for (String w : longText.split(" ")) {
            assertTrue(out.contains(w), "Word '" + w + "' should be preserved");
        }
    }

    @Test
    void testUwuPrettier() {
        Prettier p = new BuiltinPrettiers.UwuPrettier();
        assertEquals("uwu", p.id());
        String out = p.transform("real love");
        assertEquals("weaw wuv uwu", out);
    }

    @Test
    void testUwuPrettierPreservesColorTags() {
        Prettier p = new BuiltinPrettiers.UwuPrettier();
        String out = p.transform("[red]real love[]");
        assertTrue(out.startsWith("[red]"), "Color tag [red] should not be corrupted");
        assertTrue(out.contains("weaw wuv"), "Text should be uwuified");
    }

    @Test
    void testMockingPrettier() {
        Prettier p = new BuiltinPrettiers.MockingPrettier();
        assertEquals("mocking", p.id());
        String out = p.transform("hello");
        assertEquals("hElLo", out);
    }

    @Test
    void testCapsPrettier() {
        Prettier p = new BuiltinPrettiers.CapsPrettier();
        assertEquals("caps", p.id());
        assertEquals("ABC 123", p.transform("abc 123"));
    }

    @Test
    void testLowercasePrettier() {
        Prettier p = new BuiltinPrettiers.LowercasePrettier();
        assertEquals("lowercase", p.id());
        assertEquals("abc 123", p.transform("ABC 123"));
    }

    @Test
    void testReversePrettier() {
        Prettier p = new BuiltinPrettiers.ReversePrettier();
        assertEquals("reverse", p.id());
        assertEquals("321 cba", p.transform("abc 123"));
    }

    @Test
    void testSmallCapsPrettier() {
        Prettier p = new BuiltinPrettiers.SmallCapsPrettier();
        assertEquals("smallcaps", p.id());
        String out = p.transform("hello");
        assertNotEquals("hello", out);
        assertEquals(5, out.length());
    }

    @Test
    void testBubblePrettier() {
        Prettier p = new BuiltinPrettiers.BubblePrettier();
        assertEquals("bubble", p.id());
        String out = p.transform("hi 1");
        assertNotEquals("hi 1", out);
    }

    @Test
    void testCustomPrettier() {
        Prettier p = new BuiltinPrettiers.CustomPrettier();
        assertEquals("custom", p.id());
        assertTrue(p.isEditable());

        // Default template
        assertEquals("<message>", p.getDefaultScript());
        assertEquals("test text", p.transform("test text"));

        // Custom template
        p.setScript(">>> <message> <<<");
        assertEquals(">>> test text <<<", p.transform("test text"));

        // Reset
        p.resetScript();
        assertEquals("<message>", p.getScript());
    }
}
