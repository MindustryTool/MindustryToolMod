package mindustrytool.features.freecamera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import arc.Core;
import arc.graphics.Camera;
import arc.scene.Element;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.gen.Player;
import mindustry.gen.UnitEntity;
import mindustrytool.test.MindustryTestEnv;

class FreeCameraFeatureTest extends MindustryTestEnv {

    @BeforeAll
    static void initCore() {
        Core.camera = new Camera();
        if (Vars.content == null) {
            Vars.content = new ContentLoader();
        }
    }

    @AfterEach
    void tearDown() {
        Vars.player = null;
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
    void hudLifecycle_mountsOnEnableAndCleansUpOnDisable() {
        FreeCameraFeature feature = new FreeCameraFeature();
        assertNull(feature.getHudView());

        feature.onEnable();
        assertNotNull(feature.getHudView());

        Element rootElement = feature.getHudView().element();
        assertNotNull(rootElement);

        feature.onDisable();
        assertNull(feature.getHudView());
    }

    @Test
    void hudView_clickingButtonDisablesFeatureAndSnapsCamera() {
        FreeCameraFeature feature = new FreeCameraFeature();
        Player mockPlayer = new Player() {
            @Override
            public boolean dead() {
                return false;
            }
        };
        mockPlayer.x = 120f;
        mockPlayer.y = 180f;
        Vars.player = mockPlayer;

        Core.camera.position.set(999f, 888f);
        feature.setEnabled(true);
        assertTrue(feature.isEnabled());
        assertNotNull(feature.getHudView());

        FreeCameraHudView hudView = feature.getHudView();
        Element root = hudView.element();
        Button clickButton = findButton(root);
        assertNotNull(clickButton);

        clickButton.fireClick();

        assertFalse(feature.isEnabled());
        assertEquals(120f, Core.camera.position.x, 0.001f);
        assertEquals(180f, Core.camera.position.y, 0.001f);
    }

    private Button findButton(Element element) {
        if (element instanceof Button) {
            return (Button) element;
        }
        if (element instanceof Table) {
            for (Element child : ((Table) element).getChildren()) {
                Button found = findButton(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
