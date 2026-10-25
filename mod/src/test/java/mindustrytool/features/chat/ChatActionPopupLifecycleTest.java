package mindustrytool.features.chat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.mock.MockSettings;
import arc.scene.Element;
import arc.scene.style.TextureRegionDrawable;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustrytool.features.chat.models.ParsedChatMessage;
import mindustrytool.models.response.ChatMessage;
import mindustrytool.test.MindustryTestEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChatActionPopupLifecycleTest extends MindustryTestEnv {

    private ChatFeature feature;

    @BeforeEach
    void setUp() {
        Core.settings = new MockSettings();
        if (Tex.whiteui == null) {
            Tex.whiteui = new TextureRegionDrawable(new TextureRegion());
        }
        if (Styles.black9 == null) {
            Styles.black9 = new TextureRegionDrawable(new TextureRegion());
        }
        ChatActionPopup.dispose();
        feature = new ChatFeature();
    }

    @AfterEach
    void tearDown() {
        ChatActionPopup.dispose();
        feature.onDisable();
    }

    @Test
    void overlayBuildDoesNotInstantiateChatActionPopupMenu() {
        assertNull(ChatActionPopup.menu);

        ChatOverlayHudView view = new ChatOverlayHudView(feature);
        Element el = view.element();
        assertNotNull(el);

        assertNull(ChatActionPopup.menu);
        view.dispose();
    }

    @Test
    void showForLazilyConstructsMenuOutsideOverlayHierarchy() {
        ChatOverlayHudView view = new ChatOverlayHudView(feature);
        Element hudElement = view.element();
        assertNotNull(hudElement);

        ChatMessage raw = new ChatMessage();
        raw.setId("msg-1");
        raw.setContent("Hello world");
        ParsedChatMessage parsed = ChatMessageParser.parse(raw);

        ChatActionPopup.showFor(feature.getStore(), parsed, 50f, 50f);
        assertNotNull(ChatActionPopup.menu);
        assertFalse(ChatActionPopup.menu.isAncestorOf(view));
        assertNull(ChatActionPopup.menu.table().parent);

        ChatActionPopup.dismiss();
        assertFalse(ChatActionPopup.menu.isShowing());

        feature.onDisable();
        assertNull(ChatActionPopup.menu);

        view.dispose();
    }
}
