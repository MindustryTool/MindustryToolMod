package mindustrytool.features.chat;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.mock.MockSettings;
import mindustrytool.features.chat.models.ParsedChatMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.ImageMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.MindustryToolLinkMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.RoomInviteMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.TextMessage;
import mindustrytool.models.response.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChatMessageParserTest {

    @BeforeEach
    void setUp() {
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        Core.settings = new MockSettings();
        ChatMessageParser.clearCache();
    }

    @Test
    void testParseRoomInvite() {
        ChatMessage msg = new ChatMessage();
        msg.setId("1");
        msg.setContent("player-connect://127.0.0.1:6567/room1");

        ParsedChatMessage parsed = ChatMessageParser.parse(msg);
        assertTrue(parsed instanceof RoomInviteMessage);
        RoomInviteMessage invite = (RoomInviteMessage) parsed;
        assertEquals("player-connect://127.0.0.1:6567/room1", invite.getConnectLink());
        assertEquals("1", invite.getId());
    }

    @Test
    void testParseImageMessage() {
        ChatMessage msg = new ChatMessage();
        msg.setId("2");
        msg.setContent("https://example.com/assets/screenshot.PNG");

        ParsedChatMessage parsed = ChatMessageParser.parse(msg);
        assertTrue(parsed instanceof ImageMessage);
        ImageMessage img = (ImageMessage) parsed;
        assertEquals("https://example.com/assets/screenshot.PNG", img.getImageUrl());
    }

    @Test
    void testParseMindustryToolLink() {
        ChatMessage msg1 = new ChatMessage();
        msg1.setId("3");
        msg1.setContent("Check out https://mindustrytool.com/schematics/cool-reactor");

        ParsedChatMessage parsed1 = ChatMessageParser.parse(msg1);
        assertTrue(parsed1 instanceof MindustryToolLinkMessage);
        MindustryToolLinkMessage toolLink1 = (MindustryToolLinkMessage) parsed1;
        assertEquals("schematics", toolLink1.getType());
        assertEquals("cool-reactor", toolLink1.getItemId());

        ChatMessage msg2 = new ChatMessage();
        msg2.setId("4");
        msg2.setContent("https://mindustrytool.com/maps/sector-24");

        ParsedChatMessage parsed2 = ChatMessageParser.parse(msg2);
        assertTrue(parsed2 instanceof MindustryToolLinkMessage);
        MindustryToolLinkMessage toolLink2 = (MindustryToolLinkMessage) parsed2;
        assertEquals("maps", toolLink2.getType());
        assertEquals("sector-24", toolLink2.getItemId());
    }

    @Test
    void testParseStandardTextMessage() {
        ChatMessage msg = new ChatMessage();
        msg.setId("5");
        msg.setContent("Hello Mindustry players!");

        ParsedChatMessage parsed = ChatMessageParser.parse(msg);
        assertTrue(parsed instanceof TextMessage);
        TextMessage txt = (TextMessage) parsed;
        assertEquals("Hello Mindustry players!", txt.getText());
        assertFalse(txt.isMentionsCurrentUser());
    }

    @Test
    void testParseNullOrEmpty() {
        ChatMessage msg = new ChatMessage();
        msg.setId("6");
        msg.setContent(null);

        ParsedChatMessage parsed = ChatMessageParser.parse(msg);
        assertTrue(parsed instanceof TextMessage);
        assertEquals("", ((TextMessage) parsed).getText());

        ParsedChatMessage nullMsg = ChatMessageParser.parse(null);
        assertTrue(nullMsg instanceof TextMessage);
    }

    @Test
    void testParserCaching() {
        ChatMessage msg = new ChatMessage();
        msg.setId("cached-1");
        msg.setContent("https://example.com/image.png");

        ParsedChatMessage first = ChatMessageParser.parse(msg);
        ParsedChatMessage second = ChatMessageParser.parse(msg);

        assertSame(first, second);
    }
}
