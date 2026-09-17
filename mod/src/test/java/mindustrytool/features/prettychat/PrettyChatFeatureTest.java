package mindustrytool.features.prettychat;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.mock.MockSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

class PrettyChatFeatureTest {

    private PrettyChatFeature feature;

    @BeforeEach
    void setUp() {
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        Core.settings = new MockSettings();
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
    void testEnabledByDefault() {
        assertFalse(feature.isEnabled(), "PrettyChat should be disabled by default");
    }

    @Test
    void testConstructorDoesNotThrow() {
        assertDoesNotThrow(() -> new PrettyChatFeature(), "Instantiating PrettyChatFeature should not throw");
    }
}
