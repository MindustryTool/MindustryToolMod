package solim.test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import arc.Core;
import arc.Settings;
import arc.scene.Scene;
import arc.scene.ui.Label.LabelStyle;

class ArcTestEnvBehaviorTest extends ArcTestEnv {

    @Test
    void arcStaticsWithinTestAreInitialized() {
        assertNotNull(Core.app);
        assertNotNull(Core.graphics);
        assertNotNull(Core.gl);
        assertNotNull(Core.gl20);
    }

    @Test
    void sceneIsHeadlessNullByDefault() {
        assertNull(Core.scene);
    }

    @Test
    void newSceneIsStyledAndRestoredOnTeardown() {
        Scene sentinel = newScene();

        assertNotNull(Core.scene);
        LabelStyle style = Core.scene.getStyle(LabelStyle.class);
        assertNotNull(style.font);

        tearDownArcTestEnv();

        assertNull(Core.scene);

        Scene second = newScene();
        assertNotEquals(sentinel, second);
    }

    @Test
    void settingsAreFreshAndEmpty() {
        assertNotNull(Core.settings);
        assertNull(Core.settings.getString("arc.test.env.absent", null));
    }

    @Test
    void teardownRestoresPreTestSettings() {

        Core.settings = null;

        setUpArcTestEnv();
        Core.settings = null;

        tearDownArcTestEnv();

        assertNull(Core.settings);
    }

    @Test
    void setupIsReRunnableAndCreatesFreshState() {
        Settings first = Core.settings;

        setUpArcTestEnv();

        assertNotNull(Core.settings);
        assertNotEquals(first, Core.settings);
    }
}
