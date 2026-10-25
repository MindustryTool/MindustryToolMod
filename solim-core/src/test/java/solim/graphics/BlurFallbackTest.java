package solim.graphics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.Core;
import arc.scene.ui.layout.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.test.SolimEnv;

public class BlurFallbackTest extends SolimEnv {

    private boolean originalState = true;

    @BeforeEach
    void setUpSettings() {
        if (Core.settings != null) {
            originalState = Core.settings.getBool("solim.blur.enabled", true);
        }
    }

    @AfterEach
    void tearDownSettings() {
        if (Core.settings != null) {
            Core.settings.put("solim.blur.enabled", originalState);
        }
    }

    @Test
    void testBlurToggleSetting() {
        BlurRenderer renderer = BlurRenderer.get();
        renderer.setEnabled(false);
        assertFalse(renderer.isEnabled());

        if (Core.settings != null) {
            assertFalse(Core.settings.getBool("solim.blur.enabled", true));
        }

        renderer.setEnabled(true);
        if (renderer.isSupported()) {
            assertTrue(renderer.isEnabled());
        }
    }

    @Test
    void testDrawDoesNotThrowInHeadlessEnvironment() {
        BlurRenderer renderer = BlurRenderer.get();
        Table table = new Table();
        table.setSize(200f, 100f);

        renderer.drawBackdropBlur(table, 0f, 0f, 200f, 100f, 8f, 1f);
        renderer.beginContentBlur(table);
        renderer.endContentBlur(table, 5f);
    }

    @Test
    void testDrawDoesNotThrowWhenDisabled() {
        BlurRenderer renderer = BlurRenderer.get();
        renderer.setEnabled(false);

        Table table = new Table();
        table.setSize(200f, 100f);

        renderer.drawBackdropBlur(table, 0f, 0f, 200f, 100f, 8f, 1f);
        renderer.beginContentBlur(table);
        renderer.endContentBlur(table, 5f);
    }
}
