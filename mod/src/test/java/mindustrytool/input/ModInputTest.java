package mindustrytool.input;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.Settings;
import arc.assets.AssetManager;
import arc.graphics.Camera;
import arc.mock.MockApplication;
import arc.mock.MockFiles;
import arc.mock.MockGraphics;
import arc.mock.MockInput;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.core.Control;
import mindustry.input.DesktopInput;
import mindustry.input.MobileInput;
import mindustrytool.features.FeatureManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModInputTest {

    private static Control createMockControl() {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field f = unsafeClass.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object unsafe = f.get(null);
            Method allocate = unsafeClass.getMethod("allocateInstance", Class.class);
            return (Control) allocate.invoke(unsafe, Control.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeAll
    static void initCore() {
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        Core.files = new MockFiles();
        Core.assets = new AssetManager();
        Core.camera = new Camera();
        Core.input = new MockInput();
        if (Vars.content == null) {
            Vars.content = new ContentLoader();
        }
    }

    @BeforeEach
    void setUp() {
        Core.settings = new Settings();
        Core.settings.clear();
        FeatureManager.clear();
        Vars.control = createMockControl();
        Vars.mobile = false;
    }

    @AfterEach
    void tearDown() {
        FeatureManager.clear();
        Core.settings.clear();
        Vars.control = null;
        Vars.player = null;
    }

    @Test
    void ensureCustomInput_safeWhenControlNull() {
        Vars.control = null;
        assertDoesNotThrow(ModInputManager::ensureCustomInput);
    }

    @Test
    void ensureCustomInput_initializesWhenInputNull() {
        Vars.control.input = null;
        Vars.mobile = false;

        ModInputManager.ensureCustomInput();

        assertTrue(Vars.control.input instanceof ModDesktopInput);

        Vars.control.input = null;
        Vars.mobile = true;

        ModInputManager.ensureCustomInput();

        assertTrue(Vars.control.input instanceof ModMobileInput);
    }

    @Test
    void ensureCustomInput_wrapsVanillaDesktopInput() {
        Vars.control.input = new DesktopInput();

        ModInputManager.ensureCustomInput();

        assertTrue(Vars.control.input instanceof ModDesktopInput);
    }

    @Test
    void ensureCustomInput_wrapsVanillaMobileInput() {
        Vars.control.input = new MobileInput();

        ModInputManager.ensureCustomInput();

        assertTrue(Vars.control.input instanceof ModMobileInput);
    }

    @Test
    void ensureCustomInput_doesNotReplaceExistingModInput() {
        ModDesktopInput existing = new ModDesktopInput();
        Vars.control.input = existing;

        ModInputManager.ensureCustomInput();

        assertSame(existing, Vars.control.input);
    }

    @Test
    void modDesktopInput_isInstanceOfDesktopInput() {
        ModDesktopInput input = new ModDesktopInput();
        assertTrue(input instanceof DesktopInput);
    }

    @Test
    void modMobileInput_cancelPanDelaySafe() {
        ModMobileInput input = new ModMobileInput();
        assertDoesNotThrow(input::cancelPanDelay);
    }
}
