package mindustrytool.features.quickaccess;

import static org.junit.jupiter.api.Assertions.*;

import arc.Application;
import arc.Core;
import arc.Graphics;
import arc.Settings;
import arc.audio.Audio;
import arc.func.Prov;
import arc.graphics.GL20;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Font.FontData;
import arc.graphics.g2d.TextureRegion;
import arc.mock.MockApplication;
import arc.mock.MockAudio;
import arc.mock.MockGL20;
import arc.mock.MockGraphics;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.Scene;
import arc.scene.event.ClickListener;
import arc.scene.event.EventListener;
import arc.scene.event.InputEvent;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Button;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.Dialog.DialogStyle;
import arc.scene.ui.Image;
import arc.scene.ui.Label.LabelStyle;
import arc.scene.ui.TextButton.TextButtonStyle;
import arc.scene.ui.TextField.TextFieldStyle;
import arc.util.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.chat.ChatFeature;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.overlay.SolimDialog;

class QuickAccessClickDelegationTest {

    static Application prevApp;
    static Graphics prevGraphics;
    static GL20 prevGl;
    static Audio prevAudio;
    static Scene prevScene;

    static class DefaultTestFeature extends Feature {
        DefaultTestFeature() {
            super(FeatureMetadata.builder()
                    .id("default-test")
                    .icon(Icon.book)
                    .enabledByDefault(false)
                    .quickAccess(true)
                    .build());
        }
    }

    static class CustomClickFeature extends Feature {
        boolean clicked = false;
        @Nullable Element receivedAnchor;

        CustomClickFeature() {
            super(FeatureMetadata.builder()
                    .id("custom-click-test")
                    .icon(Icon.book)
                    .enabledByDefault(false)
                    .quickAccess(true)
                    .build());
        }

        @Override
        public void onQuickAccessClick(@Nullable Element anchor) {
            clicked = true;
            receivedAnchor = anchor;
        }
    }

    static class AnchorRecordingFeature extends Feature {
        final TextureRegionDrawable iconDrawable;
        boolean clicked;
        boolean longClicked;
        @Nullable Element clickAnchor;
        @Nullable Element longClickAnchor;

        AnchorRecordingFeature(TextureRegionDrawable iconDrawable) {
            super(FeatureMetadata.builder()
                    .id("anchor-recording-test")
                    .icon(iconDrawable)
                    .enabledByDefault(false)
                    .quickAccess(true)
                    .build());
            this.iconDrawable = iconDrawable;
        }

        @Override
        public void onQuickAccessClick(@Nullable Element anchor) {
            clicked = true;
            clickAnchor = anchor;
        }

        @Override
        public void onQuickAccessLongClick(@Nullable Element anchor) {
            longClicked = true;
            longClickAnchor = anchor;
        }
    }

    static class TestDialog extends SolimDialog {
        boolean shown = false;

        TestDialog() {
            super("");
        }

        @Override
        public SolimDialog show() {
            shown = true;
            return this;
        }
    }

    static class DialogTestFeature extends Feature {
        final @Nullable TestDialog settingDialog;
        final @Nullable TestDialog mainDialog;

        DialogTestFeature(@Nullable TestDialog settingDialog, @Nullable TestDialog mainDialog) {
            super(FeatureMetadata.builder()
                    .id("dialog-test")
                    .icon(Icon.book)
                    .enabledByDefault(true)
                    .quickAccess(true)
                    .build());
            this.settingDialog = settingDialog;
            this.mainDialog = mainDialog;
        }

        @Override
        public @Nullable Prov<SolimDialog> getSettingDialog() {
            return settingDialog != null ? () -> settingDialog : null;
        }

        @Override
        public @Nullable Prov<SolimDialog> getMainDialog() {
            return mainDialog != null ? () -> mainDialog : null;
        }
    }

    @BeforeAll
    static void initArc() {
        prevApp = Core.app;
        prevGraphics = Core.graphics;
        prevGl = Core.gl;
        prevAudio = Core.audio;
        prevScene = Core.scene;

        if (Core.app == null) {
            Core.app = new MockApplication();
        }
        if (Core.graphics == null) {
            Core.graphics = new MockGraphics();
        }
        if (Core.audio == null) {
            Core.audio = new MockAudio();
        }
        if (Core.gl == null) {
            Core.gl = new MockGL20();
            Core.gl20 = (MockGL20) Core.gl;
        }
        if (Core.scene == null) {
            Core.scene = new Scene();
        }

        FontData fontData = new FontData() {
            @Override
            public boolean hasGlyph(char ch) {
                return true;
            }
        };
        Font font = new Font(fontData, new TextureRegion(), false);

        try {
            Core.scene.getStyle(ButtonStyle.class);
        } catch (IllegalArgumentException missing) {
            Core.scene.addStyle(ButtonStyle.class, new ButtonStyle());
        }
        try {
            Core.scene.getStyle(DialogStyle.class);
        } catch (IllegalArgumentException missing) {
            DialogStyle style = new DialogStyle();
            style.titleFont = font;
            Core.scene.addStyle(DialogStyle.class, style);
        }
        try {
            Core.scene.getStyle(TextButtonStyle.class);
        } catch (IllegalArgumentException missing) {
            TextButtonStyle style = new TextButtonStyle();
            style.font = font;
            Core.scene.addStyle(TextButtonStyle.class, style);
        }
        try {
            Core.scene.getStyle(LabelStyle.class);
        } catch (IllegalArgumentException missing) {
            LabelStyle style = new LabelStyle();
            style.font = font;
            Core.scene.addStyle(LabelStyle.class, style);
        }
        try {
            Core.scene.getStyle(TextFieldStyle.class);
        } catch (IllegalArgumentException missing) {
            TextFieldStyle style = new TextFieldStyle();
            style.font = font;
            Core.scene.addStyle(TextFieldStyle.class, style);
        }

        Icon.book = new TextureRegionDrawable();
        if (Icon.move == null) {
            Icon.move = new TextureRegionDrawable();
        }
        if (Icon.settings == null) {
            Icon.settings = new TextureRegionDrawable();
        }
    }

    @AfterAll
    static void tearDownArc() {
        Core.app = prevApp;
        Core.graphics = prevGraphics;
        Core.gl = prevGl;
        Core.audio = prevAudio;
        Core.scene = prevScene;
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
    }

    @Test
    void defaultClick_togglesEnabledState() {
        DefaultTestFeature feat = new DefaultTestFeature();
        assertFalse(feat.isEnabled());

        feat.onQuickAccessClick(null);
        assertTrue(feat.isEnabled());

        feat.onQuickAccessClick();
        assertFalse(feat.isEnabled());
    }

    @Test
    void customAnchorOverride_receivesAnchorElement() {
        CustomClickFeature feat = new CustomClickFeature();
        Element anchor = new Element();

        feat.onQuickAccessClick(anchor);
        assertTrue(feat.clicked);
        assertSame(anchor, feat.receivedAnchor);
    }

    @Test
    void defaultLongClick_prefersSettingDialog() {
        TestDialog settingDlg = new TestDialog();
        TestDialog mainDlg = new TestDialog();
        DialogTestFeature feat = new DialogTestFeature(settingDlg, mainDlg);

        feat.onQuickAccessLongClick(null);
        assertTrue(settingDlg.shown);
        assertFalse(mainDlg.shown);
    }

    @Test
    void defaultLongClick_fallsBackToMainDialog() {
        TestDialog mainDlg = new TestDialog();
        DialogTestFeature feat = new DialogTestFeature(null, mainDlg);

        feat.onQuickAccessLongClick();
        assertTrue(mainDlg.shown);
    }

    @Test
    void defaultLongClick_safeWhenBothDialogsNull() {
        DialogTestFeature feat = new DialogTestFeature(null, null);
        assertDoesNotThrow(() -> feat.onQuickAccessLongClick(null));
    }

    @Test
    void chatFeatureClick_togglesCollapsedState() {
        ChatFeature chat = new ChatFeature();
        chat.setEnabled(true);
        chat.collapsedConfig.set(false);

        chat.onQuickAccessClick();
        assertTrue(chat.collapsedConfig.get(), "First click collapses chat");

        chat.onQuickAccessClick(null);
        assertFalse(chat.collapsedConfig.get(), "Second click expands chat");
    }

    @Test
    void chatFeatureClick_enablesAndExpandsWhenDisabled() {
        ChatFeature chat = new ChatFeature();
        chat.setEnabled(false);
        chat.collapsedConfig.set(true);

        chat.onQuickAccessClick();
        assertTrue(chat.isEnabled(), "Clicking disabled chat enables it");
        assertFalse(chat.collapsedConfig.get(), "Clicking disabled chat uncollapses it");
    }

    @Test
    void hudButtonClick_passesLiveHudElementAsAnchor() {
        AnchorRecordingFeature feat = new AnchorRecordingFeature(new TextureRegionDrawable());
        QuickAccessHudView view = mountHudWith(feat);
        try {
            Element hudRoot = view.getHud().element();
            assertNotNull(hudRoot, "HUD root element must exist after build");

            Button button = findFeatureButton(hudRoot, feat.iconDrawable);
            assertNotNull(button, "Feature button should exist in the HUD");

            fireClick(button);

            assertTrue(feat.clicked, "Click should reach the feature");
            assertNotNull(feat.clickAnchor, "Anchor must be non-null when HUD is active");
            assertSame(hudRoot, feat.clickAnchor, "Anchor must be the live HUD root element");
        } finally {
            view.dispose();
        }
    }

    @Test
    void hudButtonLongClick_passesLiveHudElementAsAnchor() throws Exception {
        AnchorRecordingFeature feat = new AnchorRecordingFeature(new TextureRegionDrawable());
        QuickAccessHudView view = mountHudWith(feat);
        try {
            Element hudRoot = view.getHud().element();
            assertNotNull(hudRoot, "HUD root element must exist after build");

            Button button = findFeatureButton(hudRoot, feat.iconDrawable);
            assertNotNull(button, "Feature button should exist in the HUD");

            fireLongClick(button);

            assertTrue(feat.longClicked, "Long-click should reach the feature");
            assertNotNull(feat.longClickAnchor, "Anchor must be non-null when HUD is active");
            assertSame(hudRoot, feat.longClickAnchor, "Anchor must be the live HUD root element");
        } finally {
            view.dispose();
        }
    }

    private static QuickAccessHudView mountHudWith(Feature... extras) {
        QuickAccessFeature quickAccess = new QuickAccessFeature();
        FeatureManager.register(quickAccess);
        FeatureManager.register(extras);
        QuickAccessHudView view = new QuickAccessHudView(quickAccess);
        view.element();
        assertNotNull(view.getHud(), "HUD must be initialized after build");
        return view;
    }

    private static Button findFeatureButton(Element root, TextureRegionDrawable icon) {
        Deque<Element> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            Element current = queue.poll();
            if (current instanceof Button
                    && containsDrawable(current, icon)) {
                return (Button) current;
            }
            if (current instanceof Group) {
                for (Element child : ((Group) current).getChildren()) {
                    queue.add(child);
                }
            }
        }
        return null;
    }

    private static boolean containsDrawable(Element root, TextureRegionDrawable icon) {
        Deque<Element> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            Element current = queue.poll();
            if (current instanceof Image && ((Image) current).getDrawable() == icon) {
                return true;
            }
            if (current instanceof Group) {
                for (Element child : ((Group) current).getChildren()) {
                    queue.add(child);
                }
            }
        }
        return false;
    }

    private static void fireClick(Button button) {
        InputEvent event = new InputEvent();
        for (EventListener listener : button.getListeners()) {
            if (listener instanceof ClickListener) {
                ((ClickListener) listener).clicked(event, 0f, 0f);
            }
        }
    }

    private static void fireLongClick(Button button) throws Exception {
        assertTrue(button.userObject instanceof solim.input.Button,
                "HUD buttons must be Solim buttons");
        Field field = solim.input.Button.class.getDeclaredField("onLongClick");
        field.setAccessible(true);
        Runnable action = (Runnable) field.get(button.userObject);
        assertNotNull(action, "Button must have a long-click handler");
        action.run();
    }
}
