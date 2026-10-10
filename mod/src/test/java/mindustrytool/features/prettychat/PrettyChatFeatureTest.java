package mindustrytool.features.prettychat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import mindustrytool.test.MindustryTestEnv;

class PrettyChatFeatureTest extends MindustryTestEnv {

    private PrettyChatFeature feature;

    @BeforeEach
    void setUp() {
        feature = new PrettyChatFeature();
    }

    @Test
    void testBasicTransformWithSinglePrettier() {
        feature.config().setEnabledIds(Arrays.asList("caps"));
        String result = feature.transform("hello world");
        assertEquals("HELLO WORLD", result);
    }

    @Test
    void testPipelineOrderPreserved() {
        // Order 1: caps then reverse
        feature.config().setEnabledIds(Arrays.asList("caps", "reverse"));
        assertEquals("DLROW OLLEH", feature.transform("hello world"));

        // Order 2: reverse then lowercase
        feature.config().setEnabledIds(Arrays.asList("reverse", "lowercase"));
        assertEquals("dlrow olleh", feature.transform("HELLO WORLD"));
    }

    @Test
    void testRegularServerCommandsAreBypassed() {
        feature.config().setEnabledIds(Arrays.asList("caps", "reverse"));
        assertEquals("/help", feature.transform("/help"));
        assertEquals("/sync", feature.transform("/sync"));
        assertEquals("/votekick 123", feature.transform("/votekick 123"));
    }

    @Test
    void testTeamAndAdminCommandsAreDecorated() {
        feature.config().setEnabledIds(Arrays.asList("caps"));
        assertEquals("/t HELLO TEAM", feature.transform("/t hello team"));
        assertEquals("/a HELLO ADMINS", feature.transform("/a hello admins"));
    }

    @Test
    void testClampSafe() {
        // Normal text within limit
        assertEquals("hello", PrettyChatFeature.clampSafe("hello", 10));

        // Truncate cleanly
        assertEquals("hello", PrettyChatFeature.clampSafe("hello world", 5));

        // Clamps incomplete color bracket
        String colored = "[#ff0000]red text";
        // If truncated inside the bracket, bracket should be discarded
        String clampedInsideBracket = PrettyChatFeature.clampSafe(colored, 5);
        assertFalse(clampedInsideBracket.contains("["), "Dangling open bracket should be removed");

        // Clamps with balanced tags
        String closed = PrettyChatFeature.clampSafe("[#ff0000]hello[]", 16);
        assertTrue(closed.endsWith("[]"), "Color tags should close properly");

        // Clamps strictly without exceeding maxLength when open color tag is cut
        String rainbow = "[#ff0000]a[#00ff00]b[#0000ff]c[#ffff00]d[#ff00ff]e[]";
        for (int limit = 1; limit <= rainbow.length(); limit++) {
            String clamped = PrettyChatFeature.clampSafe(rainbow, limit);
            assertTrue(clamped.length() <= limit, "Length " + clamped.length() + " must be <= limit " + limit);
            if (clamped.contains("[")) {
                assertTrue(clamped.endsWith("[]"), "FAILED for limit=" + limit + ": clamped='" + clamped + "'");
            }
        }
    }

    @Test
    void testConfigToggleAndMove() {
        PrettyChatConfig config = feature.config();
        config.setEnabledIds(Arrays.asList("caps", "reverse"));

        // Toggle existing ID -> disables it
        config.toggle("caps");
        assertFalse(config.isEnabled("caps"));
        assertTrue(config.isEnabled("reverse"));

        // Toggle non-existing ID -> enables it
        config.toggle("uwu");
        assertTrue(config.isEnabled("uwu"));

        // Move item
        config.setEnabledIds(Arrays.asList("caps", "reverse", "uwu"));
        config.move("uwu", -1);
        List<String> current = config.getEnabledIds();
        assertEquals("uwu", current.get(1));
        assertEquals("reverse", current.get(2));
    }

    @Test
    void testGradientPrettier() {
        Prettier gradient = feature.getPrettier("gradient");
        assertTrue(gradient != null, "Gradient prettier must be registered");
        assertTrue(gradient.isEditable());

        String result = gradient.transform("Hello");
        assertTrue(result.contains("[#"), "Gradient should contain hex tags");
        assertTrue(result.endsWith("[]"), "Gradient should close color tags");
        assertTrue(result.length() <= 150, "Gradient must not exceed max text length");

        // Test with preset
        gradient.setScript("cyberpunk");
        String cpResult = gradient.transform("Cyberpunk City");
        assertTrue(cpResult.contains("[#"), "Should contain hex tags for cyberpunk preset");
        assertTrue(cpResult.endsWith("[]"));

        // Reset
        gradient.resetScript();
        assertEquals(BuiltinPrettiers.GradientPrettier.DEFAULT_PALETTE, gradient.getScript());
    }

    @Test
    void testMonoPrettier() {
        Prettier mono = feature.getPrettier("mono");
        assertTrue(mono != null, "Mono prettier must be registered");

        String result = mono.transform("Hello 123!");
        assertEquals('\uFF28', result.charAt(0));
        assertEquals(' ', result.charAt(5));
        assertEquals(10, result.length());
    }

    @Test
    void testStrikethroughPrettier() {
        Prettier strike = feature.getPrettier("strikethrough");
        assertTrue(strike != null, "Strikethrough prettier must be registered");

        String result = strike.transform("Hi");
        assertEquals("H\u0336i\u0336", result);

        String budgetCut = strike.transform("Hello World", 6);
        assertTrue(budgetCut.length() <= 6);
    }

    @Test
    void testUnderlinePrettier() {
        Prettier underline = feature.getPrettier("underline");
        assertTrue(underline != null, "Underline prettier must be registered");

        String result = underline.transform("Hi");
        assertEquals("H\u0332i\u0332", result);

        String budgetCut = underline.transform("Hello World", 6);
        assertTrue(budgetCut.length() <= 6);
    }

    @Test
    void testFancyBracketsPrettier() {
        Prettier brackets = feature.getPrettier("fancybrackets");
        assertTrue(brackets != null, "Fancy brackets prettier must be registered");
        assertTrue(brackets.isEditable());

        String result = brackets.transform("Attack!");
        assertEquals("\u3010 Attack! \u3011", result);

        brackets.setScript("\u2726 <message> \u2726");
        assertEquals("\u2726 Attack! \u2726", brackets.transform("Attack!"));

        brackets.resetScript();
        assertEquals(BuiltinPrettiers.FancyBracketsPrettier.DEFAULT_TEMPLATE, brackets.getScript());
    }
}
