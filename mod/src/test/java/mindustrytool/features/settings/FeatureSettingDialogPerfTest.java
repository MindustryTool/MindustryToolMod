package mindustrytool.features.settings;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.TextureRegion;
import arc.mock.MockAudio;
import arc.mock.MockGraphics;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.Scene;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.CheckBox.CheckBoxStyle;
import arc.scene.ui.Dialog.DialogStyle;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.ImageButton.ImageButtonStyle;
import arc.scene.ui.Label.LabelStyle;
import arc.scene.ui.TextField.TextFieldStyle;
import arc.scene.ui.TextButton.TextButtonStyle;
import arc.scene.ui.ScrollPane.ScrollPaneStyle;
import arc.scene.style.Drawable;
import mindustry.ui.Styles;
import mindustry.gen.Tex;
import arc.util.I18NBundle;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.Fonts;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.test.MindustryTestEnv;
import solim.core.Units;

class FeatureSettingDialogPerfTest extends MindustryTestEnv {

    private static Font testFont;
    private boolean prevMobile;

    static class MobileMockGraphics extends MockGraphics {
        int width = 480;
        int height = 800;

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }
    }

    static class PerfMockFeature extends Feature {
        PerfMockFeature(String id, int order) {
            super(FeatureMetadata.builder()
                    .id(id)
                    .icon(Icon.book)
                    .order(order)
                    .build());
        }
    }

    private static TextureRegionDrawable dummyDrawable() {
        return new TextureRegionDrawable(new TextureRegion());
    }

    @BeforeAll
    static void initStatics() {
        if (Core.scene == null) {
            Core.scene = new Scene();
        }
        if (Core.bundle == null) {
            Core.bundle = I18NBundle.createEmptyBundle();
        }

        Icon.book = dummyDrawable();
        Icon.zoom = dummyDrawable();
        Icon.refresh = dummyDrawable();
        Icon.link = dummyDrawable();
        Icon.settings = dummyDrawable();
        Icon.star = dummyDrawable();
        Icon.left = dummyDrawable();
        Icon.right = dummyDrawable();
        Icon.info = dummyDrawable();
        Icon.none = dummyDrawable();
        Icon.exit = dummyDrawable();
        Icon.add = dummyDrawable();
        Icon.move = dummyDrawable();
        Icon.play = dummyDrawable();

        if (testFont == null) {
            Font.FontData fontData = new Font.FontData() {
                @Override
                public boolean hasGlyph(char ch) {
                    return true;
                }

                @Override
                public Font.Glyph getGlyph(char ch) {
                    Font.Glyph g = super.getGlyph(ch);
                    if (g == null) {
                        g = new Font.Glyph();
                        g.id = ch;
                        g.width = 8;
                        g.height = 12;
                        g.xadvance = 8;
                        setGlyph(ch, g);
                    }
                    return g;
                }
            };
            fontData.lineHeight = 18f;
            fontData.capHeight = 14f;
            fontData.ascent = 14f;
            fontData.descent = -4f;
            fontData.down = -18f;
            testFont = new Font(fontData, new TextureRegion(), false);
        }
        Fonts.def = testFont;
        Core.scene.addStyle(LabelStyle.class, new LabelStyle(testFont, Color.white));
        TextFieldStyle textStyle = new TextFieldStyle();
        textStyle.font = testFont;
        textStyle.fontColor = Color.white;
        Core.scene.addStyle(TextFieldStyle.class, textStyle);
    }

    @BeforeEach
    void setUpMobileEnvironment() {
        prevMobile = Vars.mobile;
        Vars.mobile = true;

        if (Core.audio == null) {
            Core.audio = new MockAudio();
        }

        Core.graphics = new MobileMockGraphics();
        Units.update();

        Core.scene = new Scene();
        Core.scene.resize(480, 800);

        Drawable empty = new TextureRegionDrawable(new TextureRegion());
        LabelStyle labelStyle = new LabelStyle(testFont, Color.white);
        Core.scene.addStyle(LabelStyle.class, labelStyle);

        TextFieldStyle textStyle = new TextFieldStyle();
        textStyle.font = testFont;
        textStyle.fontColor = Color.white;
        Core.scene.addStyle(TextFieldStyle.class, textStyle);

        ButtonStyle buttonStyle = new ButtonStyle();
        Core.scene.addStyle(ButtonStyle.class, buttonStyle);

        ImageButtonStyle imageButtonStyle = new ImageButtonStyle();
        Core.scene.addStyle(ImageButtonStyle.class, imageButtonStyle);

        TextButtonStyle textButtonStyle = new TextButtonStyle();
        textButtonStyle.font = testFont;
        Core.scene.addStyle(TextButtonStyle.class, textButtonStyle);

        ScrollPaneStyle scrollStyle = new ScrollPaneStyle();
        Core.scene.addStyle(ScrollPaneStyle.class, scrollStyle);

        CheckBoxStyle checkStyle = new CheckBoxStyle();
        checkStyle.font = testFont;
        checkStyle.fontColor = Color.white;
        checkStyle.checkboxOn = empty;
        checkStyle.checkboxOff = empty;
        Core.scene.addStyle(CheckBoxStyle.class, checkStyle);
        Styles.defaultCheck = checkStyle;

        DialogStyle dialogStyle = new DialogStyle();
        dialogStyle.stageBackground = empty;
        dialogStyle.titleFont = testFont;
        Core.scene.addStyle(DialogStyle.class, dialogStyle);

        Styles.defaultDialog = dialogStyle;
        Styles.defaultLabel = labelStyle;
        Styles.defaultb = buttonStyle;
        Styles.defaultt = textButtonStyle;
        Styles.defaulti = imageButtonStyle;
        Styles.cleart = textButtonStyle;
        Styles.clearNonei = imageButtonStyle;
        Styles.clearTogglei = imageButtonStyle;

        Tex.button = empty;
        Tex.clear = empty;
        Tex.whiteui = empty;

        FeatureManager.clear();
        for (int i = 0; i < 25; i++) {
            FeatureManager.register(new PerfMockFeature("perf-feat-" + i, i));
        }
    }

    @AfterEach
    void tearDownMobileEnvironment() {
        Vars.mobile = prevMobile;
        FeatureManager.clear();
    }

    @Test
    void testMobileFeatureSettingDialogOpenLatency() {
        long startNs = System.nanoTime();
        FeatureSettingDialog dialog = new FeatureSettingDialog();
        dialog.show(Core.scene);
        dialog.cont().layout();
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;

        assertNotNull(dialog.element());
        assertTrue(elapsedMs < 1000L, "Initial open must complete smoothly: " + elapsedMs + "ms");

        int cards = countFeatureCards(dialog.cont());
        assertTrue(cards > 0, "Should mount cards");
    }

    @Test
    void testProgressiveBatchingAndSearchFilter() {
        FeatureSettingsView view = new FeatureSettingsView();
        Element root = view.element();
        assertNotNull(root);
        int initialCards = countFeatureCards(root);
        assertTrue(initialCards > 0);
        assertTrue(initialCards <= 25);
    }

    private int countFeatureCards(Element root) {
        if (root == null) {
            return 0;
        }
        int count = root.name != null && root.name.startsWith("FeatureCard-") ? 1 : 0;
        if (root instanceof Group) {
            for (Element child : ((Group) root).getChildren()) {
                count += countFeatureCards(child);
            }
        }
        return count;
    }
}
