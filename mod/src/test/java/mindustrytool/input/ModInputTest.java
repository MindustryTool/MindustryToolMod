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
import arc.struct.Queue;
import arc.math.geom.Vec2;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.core.Control;
import mindustry.core.GameState;
import mindustry.game.Rules;
import mindustry.gen.Player;
import mindustry.gen.UnitEntity;
import mindustry.input.DesktopInput;
import mindustry.input.MobileInput;
import mindustry.type.UnitType;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.autoplay.AutoplayFeature;
import mindustrytool.features.freecamera.FreeCameraFeature;
import mindustrytool.features.joystick.JoystickFeature;
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
        Vars.state = new GameState();
        Vars.state.rules = new Rules();
    }

    @AfterEach
    void tearDown() {
        FeatureManager.clear();
        Core.settings.clear();
        Vars.control = null;
        Vars.player = null;
        Vars.state = null;
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



    @Test
    void modDesktopInput_updateMovement_ignoresKeyboardWhenFreeCamActive() {
        FreeCameraFeature feature = new FreeCameraFeature();
        FeatureManager.register(feature);
        feature.setEnabled(true);

        ModDesktopInput input = new ModDesktopInput();
        UnitType type = new UnitType("test-type-freecam");
        type.omniMovement = true;
        UnitEntity unit = new UnitEntity() {
            @Override
            public float speed() {
                return 2f;
            }
        };
        unit.type = type;
        unit.plans = new Queue<>();

        Player player = new Player() {
            @Override
            public boolean dead() {
                return false;
            }

            @Override
            public mindustry.gen.Unit unit() {
                return unit;
            }
        };
        Vars.player = player;

        input.updateMovement(unit);

        assertEquals(0f, input.movement.x, 0.001f);
        assertEquals(0f, input.movement.y, 0.001f);
    }

    @Test
    void modDesktopInput_updateMovement_preservesAutoplayMovementPreference() {
        AutoplayFeature autoplay = new AutoplayFeature();
        FeatureManager.register(autoplay);
        autoplay.setEnabled(true);

        FreeCameraFeature freeCam = new FreeCameraFeature();
        FeatureManager.register(freeCam);
        freeCam.setEnabled(true);

        ModDesktopInput input = new ModDesktopInput();
        UnitType type = new UnitType("test-type-autoplay");
        type.omniMovement = true;

        final boolean[] movePrefCalled = new boolean[1];
        UnitEntity unit = new UnitEntity() {
            @Override
            public float speed() {
                return 2f;
            }

            @Override
            public void movePref(Vec2 vec) {
                movePrefCalled[0] = true;
            }
        };
        unit.type = type;
        unit.plans = new Queue<>();

        Player player = new Player() {
            @Override
            public boolean dead() {
                return false;
            }

            @Override
            public mindustry.gen.Unit unit() {
                return unit;
            }
        };
        Vars.player = player;

        input.updateMovement(unit);

        // Movement is zero during Free Camera and autoplay is active, so movePref should not be called
        assertFalse(movePrefCalled[0]);
    }

    @Test
    void snapToPlayer_resetsDesktopInputPanning() {
        FreeCameraFeature feature = new FreeCameraFeature();
        Player mockPlayer = new Player() {
            @Override
            public boolean dead() {
                return false;
            }
        };
        mockPlayer.x = 240f;
        mockPlayer.y = 360f;
        Vars.player = mockPlayer;

        DesktopInput desktopInput = new DesktopInput();
        desktopInput.panning = true;
        Vars.control.input = desktopInput;

        feature.snapToPlayer();

        assertFalse(desktopInput.panning);
    }

    @Test
    void modMobileInput_updateMovement_keepsUnitStationaryWhenFreeCamActiveWithoutJoystick() {
        FreeCameraFeature freeCam = new FreeCameraFeature();
        FeatureManager.register(freeCam);
        freeCam.setEnabled(true);

        ModMobileInput input = new ModMobileInput();
        Core.camera.position.set(500f, 500f);

        UnitType type = new UnitType("test-mobile-freecam");
        type.omniMovement = true;
        type.accel = 0.5f;
        type.hitSize = 8f;
        UnitEntity unit = new UnitEntity() {
            @Override
            public float speed() {
                return 2f;
            }
        };
        unit.type = type;
        unit.plans = new Queue<>();

        Player player = new Player() {
            @Override
            public boolean dead() {
                return false;
            }

            @Override
            public mindustry.gen.Unit unit() {
                return unit;
            }
        };
        player.x = 100f;
        player.y = 100f;
        Vars.player = player;

        input.updateMovement(unit);

        // targetPos should stay at player position, not chase camera (500, 500)
        assertEquals(100f, input.targetPos.x, 0.001f);
        assertEquals(100f, input.targetPos.y, 0.001f);
        assertEquals(0f, input.movement.x, 0.001f);
        assertEquals(0f, input.movement.y, 0.001f);
    }

    @Test
    void modMobileInput_updateMovement_movesTowardsCameraWhenFreeCamDisabledAndNoJoystick() {
        FreeCameraFeature freeCam = new FreeCameraFeature();
        FeatureManager.register(freeCam);
        freeCam.setEnabled(false);

        ModMobileInput input = new ModMobileInput();
        Core.camera.position.set(500f, 500f);

        UnitType type = new UnitType("test-mobile-vanilla");
        type.omniMovement = true;
        type.accel = 0.5f;
        type.hitSize = 8f;
        UnitEntity unit = new UnitEntity() {
            @Override
            public float speed() {
                return 2f;
            }
        };
        unit.type = type;
        unit.plans = new Queue<>();

        Player player = new Player() {
            @Override
            public boolean dead() {
                return false;
            }

            @Override
            public mindustry.gen.Unit unit() {
                return unit;
            }
        };
        player.x = 100f;
        player.y = 100f;
        Vars.player = player;

        input.updateMovement(unit);

        // Vanilla behavior: targetPos moves towards camera position
        assertEquals(500f, input.targetPos.x, 0.001f);
        assertEquals(500f, input.targetPos.y, 0.001f);
        assertTrue(input.movement.x > 0f);
        assertTrue(input.movement.y > 0f);
    }

    @Test
    void modMobileInput_updateMovement_yieldsToAutoplayWhenActiveWithoutJoystick() {
        AutoplayFeature autoplay = new AutoplayFeature();
        FeatureManager.register(autoplay);
        autoplay.setEnabled(true);

        FreeCameraFeature freeCam = new FreeCameraFeature();
        FeatureManager.register(freeCam);
        freeCam.setEnabled(true);

        ModMobileInput input = new ModMobileInput();
        UnitType type = new UnitType("test-mobile-autoplay");
        type.omniMovement = true;
        type.accel = 0.5f;
        type.hitSize = 8f;

        final boolean[] movePrefCalled = new boolean[1];
        UnitEntity unit = new UnitEntity() {
            @Override
            public float speed() {
                return 2f;
            }

            @Override
            public void movePref(Vec2 vec) {
                movePrefCalled[0] = true;
            }
        };
        unit.type = type;
        unit.plans = new Queue<>();

        Player player = new Player() {
            @Override
            public boolean dead() {
                return false;
            }

            @Override
            public mindustry.gen.Unit unit() {
                return unit;
            }
        };
        player.x = 100f;
        player.y = 100f;
        Vars.player = player;

        input.updateMovement(unit);

        // Autoplay is active and joystick is not held, so ModMobileInput must yield and not call movePref
        assertFalse(movePrefCalled[0]);
    }

    @Test
    void modMobileInput_updateMovement_allowsManualJoystickOverrideDuringAutoplay() {
        AutoplayFeature autoplay = new AutoplayFeature();
        FeatureManager.register(autoplay);
        autoplay.setEnabled(true);

        JoystickFeature joystick = new JoystickFeature();
        FeatureManager.register(joystick);
        joystick.setEnabled(true);
        joystick.activePointer = 0;
        joystick.moveVector.set(1f, 0f);

        ModMobileInput input = new ModMobileInput();
        UnitType type = new UnitType("test-mobile-autoplay-joystick");
        type.omniMovement = true;
        type.accel = 0.5f;
        type.hitSize = 8f;

        final Vec2 capturedMovePref = new Vec2();
        UnitEntity unit = new UnitEntity() {
            @Override
            public float speed() {
                return 2f;
            }

            @Override
            public void movePref(Vec2 vec) {
                capturedMovePref.set(vec);
            }
        };
        unit.type = type;
        unit.plans = new Queue<>();

        Player player = new Player() {
            @Override
            public boolean dead() {
                return false;
            }

            @Override
            public mindustry.gen.Unit unit() {
                return unit;
            }
        };
        player.x = 100f;
        player.y = 100f;
        Vars.player = player;

        input.updateMovement(unit);

        // Active joystick dragging overrides autoplay movement
        assertTrue(capturedMovePref.x > 0f);
    }
}
