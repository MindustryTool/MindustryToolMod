package mindustrytool.features.freecamera;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.Settings;
import arc.graphics.Camera;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.gen.Player;
import mindustry.gen.UnitEntity;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FreeCameraFeatureTest {

    @BeforeAll
    static void initCore() {
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        Core.camera = new Camera();
        if (Vars.content == null) {
            Vars.content = new ContentLoader();
        }
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
        Vars.player = null;
    }

    @Test
    void metadata_hasExpectedValues() {
        FreeCameraFeature feature = new FreeCameraFeature();
        FeatureMetadata meta = feature.getMetadata();

        assertEquals("free-camera", meta.getId());
        assertEquals(6, meta.getOrder());
        assertTrue(meta.isEnabledByDefault());
        assertTrue(meta.isQuickAccessByDefault());
        assertFalse(meta.isDevelopment());
    }

    @Test
    void initialState_isEnabledByDefault() {
        FreeCameraFeature feature = new FreeCameraFeature();

        assertTrue(feature.isEnabled());
        assertEquals(Boolean.TRUE, feature.enabled().peek());
    }

    @Test
    void quickAccessClick_togglesState() {
        FreeCameraFeature feature = new FreeCameraFeature();
        assertTrue(feature.isEnabled());

        feature.onQuickAccessClick(null);
        assertFalse(feature.isEnabled());

        feature.onQuickAccessClick(null);
        assertTrue(feature.isEnabled());
    }

    @Test
    void snapToPlayer_safeWhenPlayerNull() {
        FreeCameraFeature feature = new FreeCameraFeature();
        Vars.player = null;

        assertDoesNotThrow(feature::snapToPlayer);
        assertDoesNotThrow(() -> feature.onQuickAccessLongClick(null));
    }

    @Test
    void snapToPlayer_setsCameraPositionWhenPlayerAlive() {
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

        feature.snapToPlayer();

        assertEquals(240f, Core.camera.position.x, 0.001f);
        assertEquals(360f, Core.camera.position.y, 0.001f);
    }

    @Test
    void onDisable_snapsCameraToPlayerPosition() {
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

        Core.camera.position.set(1000f, 2000f);
        assertTrue(feature.isEnabled());

        feature.setEnabled(false);

        assertEquals(240f, Core.camera.position.x, 0.001f);
        assertEquals(360f, Core.camera.position.y, 0.001f);
    }

    @Test
    void onDisable_snapsCameraToUnitPositionWhenUnitAlive() {
        FreeCameraFeature feature = new FreeCameraFeature();
        UnitEntity mockUnit = UnitEntity.create();
        mockUnit.x = 550f;
        mockUnit.y = 750f;
        mockUnit.dead = false;

        Player mockPlayer = new Player() {
            @Override
            public boolean dead() {
                return false;
            }

            @Override
            public UnitEntity unit() {
                return mockUnit;
            }
        };
        mockPlayer.x = 240f;
        mockPlayer.y = 360f;

        Vars.player = mockPlayer;
        Core.camera.position.set(1000f, 2000f);

        feature.setEnabled(false);

        assertEquals(550f, Core.camera.position.x, 0.001f);
        assertEquals(750f, Core.camera.position.y, 0.001f);
    }

    @Test
    void snapToPlayer_fallsBackToPlayerPosWhenUnitDead() {
        FreeCameraFeature feature = new FreeCameraFeature();
        UnitEntity mockUnit = UnitEntity.create();
        mockUnit.x = 550f;
        mockUnit.y = 750f;
        mockUnit.dead = true;

        Player mockPlayer = new Player() {
            @Override
            public boolean dead() {
                return false;
            }

            @Override
            public UnitEntity unit() {
                return mockUnit;
            }
        };
        mockPlayer.x = 240f;
        mockPlayer.y = 360f;

        Vars.player = mockPlayer;
        Core.camera.position.set(1000f, 2000f);

        feature.snapToPlayer();

        assertEquals(240f, Core.camera.position.x, 0.001f);
        assertEquals(360f, Core.camera.position.y, 0.001f);
    }

    @Test
    void onDisable_safeWhenCameraOrPlayerNull() {
        FreeCameraFeature feature = new FreeCameraFeature();
        Vars.player = null;
        assertDoesNotThrow(() -> feature.setEnabled(false));

        feature.setEnabled(true);
        Core.camera = null;
        assertDoesNotThrow(() -> feature.setEnabled(false));

        // Restore camera
        Core.camera = new Camera();
    }
}
