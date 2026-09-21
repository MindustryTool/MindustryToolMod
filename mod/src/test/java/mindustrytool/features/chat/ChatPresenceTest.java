package mindustrytool.features.chat;

import static org.junit.jupiter.api.Assertions.*;

import mindustrytool.features.chat.state.ChatSession;
import org.junit.jupiter.api.Test;
import solim.test.SolimEnv;

class ChatPresenceTest extends SolimEnv {

    @Test
    void testMenuStashKeepsPlayingPresence() {
        assertEquals("server: Alpha",
                ChatPresence.stashForMenu("server: Alpha", ChatSession.MENU_STATE, false));
    }

    @Test
    void testMenuStashKeepsPreviousWhenAlreadyMenu() {
        assertEquals("campaign: Glacier",
                ChatPresence.stashForMenu(ChatSession.MENU_STATE, "campaign: Glacier", false));
    }

    @Test
    void testMenuStashPreservesGameTruthWhilePcOpen() {
        assertEquals("custom-game",
                ChatPresence.stashForMenu(ChatSession.PLAYER_CONNECT_PREFIX + "Relay", "custom-game", true));
    }

    @Test
    void testRestoreWithoutPcReturnsLastNonMenu() {
        assertEquals("server: Alpha", ChatPresence.restoreForPlaying("server: Alpha", null));
    }

    @Test
    void testRestoreWithPcReturnsPcPresence() {
        assertEquals(ChatSession.PLAYER_CONNECT_PREFIX + "Relay",
                ChatPresence.restoreForPlaying("custom-game", "Relay"));
    }

    @Test
    void testRestoreWithBlankPcReturnsLastNonMenu() {
        assertEquals("custom-game", ChatPresence.restoreForPlaying("custom-game", "  "));
    }

    @Test
    void testEffectiveValueWithoutPc() {
        assertEquals("campaign: Glacier", ChatPresence.effectiveForValue("campaign: Glacier", null));
    }

    @Test
    void testEffectiveValueWithPcKeepsPcPresence() {
        assertEquals(ChatSession.PLAYER_CONNECT_PREFIX + "Relay",
                ChatPresence.effectiveForValue("campaign: Glacier", "Relay"));
    }

    @Test
    void testUnchangedCleanPresenceSendsNothing() {
        assertFalse(ChatPresence.shouldSend("menu", "menu", false, false));
    }

    @Test
    void testChangedPresenceSends() {
        assertTrue(ChatPresence.shouldSend("server: Alpha", "menu", false, false));
    }

    @Test
    void testDirtyPresenceSends() {
        assertTrue(ChatPresence.shouldSend("menu", "menu", true, false));
    }

    @Test
    void testForcedPresenceSends() {
        assertTrue(ChatPresence.shouldSend("menu", "menu", false, true));
    }

    @Test
    void testCurrentGenerationIsNotStale() {
        assertFalse(ChatPresence.isStalePing(3L, 3L));
    }

    @Test
    void testOlderGenerationIsStale() {
        assertTrue(ChatPresence.isStalePing(2L, 3L));
    }

    @Test
    void testEligibleWhenLoggedInAndOptedIn() {
        assertTrue(ChatPresence.isEligible(true, true));
    }

    @Test
    void testNotEligibleWhenLoggedOut() {
        assertFalse(ChatPresence.isEligible(false, true));
    }

    @Test
    void testNotEligibleWhenOptedOut() {
        assertFalse(ChatPresence.isEligible(true, false));
    }
}
