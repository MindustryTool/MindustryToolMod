package mindustrytool.features.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import mindustrytool.test.MindustryTestEnv;

class ChatFilterTest extends MindustryTestEnv {

    @Test
    void testNullAndEmptyInputs() {
        assertFalse(ChatFilter.shouldTranslateIncoming(null));
        assertFalse(ChatFilter.shouldTranslateIncoming(""));
        assertFalse(ChatFilter.shouldTranslateIncoming("   "));
        assertFalse(ChatFilter.shouldTranslateOutgoing(null));
        assertFalse(ChatFilter.shouldTranslateOutgoing(""));
        assertFalse(ChatFilter.shouldTranslateOutgoing("   "));
        assertTrue(ChatFilter.isNoise(null));
        assertTrue(ChatFilter.isNoise(""));
        assertTrue(ChatFilter.isNoise("   "));
    }

    @Test
    void testSystemAndBotMessages() {
        assertTrue(ChatFilter.isSystemOrBotMessage("[Server] Game starts now!"));
        assertTrue(ChatFilter.isSystemOrBotMessage("<server> Wave 20 coming"));
        assertTrue(ChatFilter.isSystemOrBotMessage("[Discord] <User>: Hello"));
        assertTrue(ChatFilter.isSystemOrBotMessage("[Bot] Status check"));
        assertTrue(ChatFilter.isSystemOrBotMessage("[Admin] Please respect rules"));
        assertTrue(ChatFilter.isSystemOrBotMessage("[System] Memory cleared"));

        assertFalse(ChatFilter.shouldTranslateIncoming("[Server] Server restart in 5 minutes"));
        assertFalse(ChatFilter.shouldTranslateIncoming("[Discord] <Gamer>: hi all"));
        assertFalse(ChatFilter.shouldTranslateIncoming("[Admin] Notice to players"));

        // System event phrases
        assertFalse(ChatFilter.shouldTranslateIncoming("Player1 has connected."));
        assertFalse(ChatFilter.shouldTranslateIncoming("Player2 has disconnected."));
        assertFalse(ChatFilter.shouldTranslateIncoming("Game Over - Core Destroyed"));
    }

    @Test
    void testCoordinatesAndNumbers() {
        assertTrue(ChatFilter.isNoise("12345"));
        assertTrue(ChatFilter.isNoise("-50"));
        assertTrue(ChatFilter.isNoise("3.14"));
        assertTrue(ChatFilter.isNoise("x: 100, y: 200"));
        assertTrue(ChatFilter.isNoise("x=50 y=-30"));
        assertTrue(ChatFilter.isNoise("(100, 200)"));
        assertTrue(ChatFilter.isNoise("100, 200"));

        assertFalse(ChatFilter.shouldTranslateIncoming("123"));
        assertFalse(ChatFilter.shouldTranslateIncoming("Player: (120, 80)"));
    }

    @Test
    void testEmoticonsAndPunctuation() {
        assertTrue(ChatFilter.isNoise(":)"));
        assertTrue(ChatFilter.isNoise(":("));
        assertTrue(ChatFilter.isNoise(":D"));
        assertTrue(ChatFilter.isNoise("xD"));
        assertTrue(ChatFilter.isNoise("^^"));
        assertTrue(ChatFilter.isNoise("???"));
        assertTrue(ChatFilter.isNoise("!!!"));
        assertTrue(ChatFilter.isNoise("..."));
        assertTrue(ChatFilter.isNoise("---"));

        assertFalse(ChatFilter.shouldTranslateIncoming("Player: :)"));
        assertFalse(ChatFilter.shouldTranslateIncoming("Player: ???"));
    }

    @Test
    void testUniversalAcronyms() {
        assertTrue(ChatFilter.isNoise("gg"));
        assertTrue(ChatFilter.isNoise("GG"));
        assertTrue(ChatFilter.isNoise("ggwp"));
        assertTrue(ChatFilter.isNoise("glhf"));
        assertTrue(ChatFilter.isNoise("ty"));
        assertTrue(ChatFilter.isNoise("np"));
        assertTrue(ChatFilter.isNoise("thx"));
        assertTrue(ChatFilter.isNoise("pls"));
        assertTrue(ChatFilter.isNoise("afk"));

        assertFalse(ChatFilter.shouldTranslateIncoming("Player: gg"));
    }

    @Test
    void testUrlsAndSchematics() {
        assertTrue(ChatFilter.isNoise("https://mindustry-tool.com"));
        assertTrue(ChatFilter.isNoise("http://example.com/test"));
        assertTrue(ChatFilter.isNoise("www.google.com"));

        String dummySchematic = "bXN6AAAABAAAAAAADwAAAAAAAAAAAAAA12345678901234567890";
        assertTrue(ChatFilter.isSchematic(dummySchematic));
        assertTrue(ChatFilter.isNoise(dummySchematic));
        assertFalse(ChatFilter.shouldTranslateIncoming(dummySchematic));
    }

    @Test
    void testValidIncomingChatMessages() {
        assertTrue(ChatFilter.shouldTranslateIncoming("Hello everyone!"));
        assertTrue(ChatFilter.shouldTranslateIncoming("[#ff0000]Player[white]: Please send thorium to core"));
        assertTrue(ChatFilter.shouldTranslateIncoming("Chúng tôi cần thêm titan ở phía tây"));
        assertTrue(ChatFilter.shouldTranslateIncoming("Нам нужно больше кремния"));
    }

    @Test
    void testOutgoingMessageFiltering() {
        // Escape prefix
        assertFalse(ChatFilter.shouldTranslateOutgoing("//Hello"));
        assertFalse(ChatFilter.shouldTranslateOutgoing("// /vote kick"));

        // Commands
        assertFalse(ChatFilter.shouldTranslateOutgoing("/vote kick 1"));
        assertFalse(ChatFilter.shouldTranslateOutgoing("/help"));
        assertFalse(ChatFilter.shouldTranslateOutgoing("/sync"));

        // Fast translation commands
        assertTrue(ChatFilter.shouldTranslateOutgoing("/tr Hello"));
        assertTrue(ChatFilter.shouldTranslateOutgoing("/dich Xin chao"));
        assertFalse(ChatFilter.shouldTranslateOutgoing("/tr "));

        // Team and admin chat
        assertTrue(ChatFilter.shouldTranslateOutgoing("/t need defense here"));
        assertTrue(ChatFilter.shouldTranslateOutgoing("/a server lag check"));
        assertFalse(ChatFilter.shouldTranslateOutgoing("/t :)"));

        // Normal text
        assertTrue(ChatFilter.shouldTranslateOutgoing("We need to rebuild the reactors"));
        assertFalse(ChatFilter.shouldTranslateOutgoing("12345"));
        assertFalse(ChatFilter.shouldTranslateOutgoing(":)"));
        assertFalse(ChatFilter.shouldTranslateOutgoing("gg"));
    }

    @Test
    void testExtractMessageContent() {
        assertEquals("Hello world", ChatFilter.extractMessageContent("Player: Hello world"));
        assertEquals("Attack now", ChatFilter.extractMessageContent("[Admin] Boss: Attack now"));
        assertEquals("Plain text", ChatFilter.extractMessageContent("Plain text"));
        assertEquals("", ChatFilter.extractMessageContent(null));
    }
}
