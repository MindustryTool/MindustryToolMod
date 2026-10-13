package solim.test;

import arc.Application;
import arc.Core;
import arc.Graphics;
import arc.Settings;
import arc.graphics.GL20;
import arc.mock.MockApplication;
import arc.mock.MockGL20;
import arc.mock.MockGraphics;
import arc.scene.Scene;
import arc.scene.ui.Label.LabelStyle;
import arc.scene.ui.TextField.TextFieldStyle;
import arc.util.Timer;
import arc.graphics.Texture;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Font.FontData;
import arc.graphics.g2d.TextureAtlas;
import arc.graphics.g2d.TextureRegion;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public class ArcTestEnv {

    private static Application classApp;
    private static Graphics classGraphics;
    private static GL20 classGl;
    private static GL20 classGl20;
    private static Scene classScene;
    private static Settings classSettings;
    private static TextureAtlas classAtlas;

    private Application testApp;
    private Graphics testGraphics;
    private GL20 testGl;
    private GL20 testGl20;
    private Scene prevScene;
    private Settings prevSettings;

    @BeforeAll
    public static void initArcStatics() {
        classApp = Core.app;
        classGraphics = Core.graphics;
        classGl = Core.gl;
        classGl20 = Core.gl20;
        classScene = Core.scene;
        classSettings = Core.settings;
        classAtlas = Core.atlas;

        if (Core.app == null) {
            Core.app = new MockApplication();
        }
        if (Core.graphics == null) {
            Core.graphics = new MockGraphics();
        }
        if (Core.gl == null) {
            Core.gl = new MockGL20();
            Core.gl20 = (MockGL20) Core.gl;
        }
        if (Core.atlas == null) {

            Core.atlas = minimalAtlas();
        }
    }

    public static TextureAtlas minimalAtlas() {
        TextureAtlas atlas = new TextureAtlas();

        Texture placeholder = new MockTexture();
        atlas.addRegion("white", placeholder, 0, 0, 1, 1);
        atlas.addRegion("whiteui", placeholder, 0, 0, 1, 1);
        return atlas;
    }

    private static final class MockTexture extends Texture {
        MockTexture() {
            super();
        }
    }

    @AfterAll
    public static void restoreArcStatics() {
        Core.app = classApp;
        Core.graphics = classGraphics;
        Core.gl = classGl;
        Core.gl20 = classGl20;
        Core.scene = classScene;
        Core.settings = classSettings;
        Core.atlas = classAtlas;
        classApp = null;
        classGraphics = null;
        classGl = null;
        classGl20 = null;
        classScene = null;
        classSettings = null;
        classAtlas = null;
    }

    @BeforeEach
    public void setUpArcTestEnv() {
        testApp = Core.app;
        testGraphics = Core.graphics;
        testGl = Core.gl;
        testGl20 = Core.gl20;
        prevScene = Core.scene;
        prevSettings = Core.settings;

        Timer.instance().clear();

        Core.scene = null;

        Core.settings = new Settings();
        Core.settings.clear();
    }

    @AfterEach
    public void tearDownArcTestEnv() {
        Timer.instance().clear();

        if (testApp != null) {
            Core.app = testApp;
        }
        if (testGraphics != null) {
            Core.graphics = testGraphics;
        }
        if (testGl != null) {
            Core.gl = testGl;
        }
        if (testGl20 != null) {
            Core.gl20 = testGl20;
        }
        Core.scene = prevScene;
        Core.settings = prevSettings;
        testApp = null;
        testGraphics = null;
        testGl = null;
        testGl20 = null;
        prevScene = null;
        prevSettings = null;
    }

    public static Scene newScene() {
        Scene scene = new Scene();
        installMinimalStyles(scene);
        Core.scene = scene;
        return scene;
    }

    public static void installMinimalStyles(Scene scene) {
        Font font = new Font(new FontData() {
            @Override
            public boolean hasGlyph(char ch) {
                return true;
            }
        }, new TextureRegion(), false);

        try {
            scene.getStyle(LabelStyle.class);
        } catch (IllegalArgumentException missing) {
            scene.addStyle(LabelStyle.class, new LabelStyle(font, null));
        }

        try {
            scene.getStyle(TextFieldStyle.class);
        } catch (IllegalArgumentException missing) {
            TextFieldStyle style = new TextFieldStyle();
            style.font = font;
            scene.addStyle(TextFieldStyle.class, style);
        }
    }
}
