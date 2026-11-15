package mindustrytool.services.donation;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.Settings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.test.ArcTestEnv;

class DonationPromptTest extends ArcTestEnv {

    @BeforeEach
    void setup() {
        Core.settings = new Settings();
        Core.settings.clear();
    }

    @Test
    void shouldPrompt_returnsTrueWhenNeverPrompted() {
        assertTrue(DonationPrompt.shouldPrompt(1_000_000L));
    }

    @Test
    void shouldPrompt_returnsFalseWhenUnderCooldown() {
        long now = 10_000_000L;
        DonationPrompt.recordShown(now);

        long underCooldown = now + DonationPrompt.COOLDOWN_MILLIS - 1000L;
        assertFalse(DonationPrompt.shouldPrompt(underCooldown));
    }

    @Test
    void shouldPrompt_returnsTrueWhenCooldownElapsed() {
        long now = 10_000_000L;
        DonationPrompt.recordShown(now);

        long afterCooldown = now + DonationPrompt.COOLDOWN_MILLIS;
        assertTrue(DonationPrompt.shouldPrompt(afterCooldown));
    }

    @Test
    void shouldPrompt_returnsFalseWhenDontShowAgainIsTrue() {
        long now = 10_000_000L;
        DonationPrompt.recordShown(now);
        DonationPrompt.setDontShowAgain(true);

        long farFuture = now + DonationPrompt.COOLDOWN_MILLIS * 10L;
        assertFalse(DonationPrompt.shouldPrompt(farFuture));
    }

    @Test
    void setDontShowAgain_persistsFlag() {
        assertFalse(DonationPrompt.isDontShowAgain());

        DonationPrompt.setDontShowAgain(true);
        assertTrue(DonationPrompt.isDontShowAgain());

        DonationPrompt.setDontShowAgain(false);
        assertFalse(DonationPrompt.isDontShowAgain());
    }

    @Test
    void recordShown_persistsTimestamp() {
        assertEquals(0L, DonationPrompt.getLastShown());

        long timestamp = 123456789L;
        DonationPrompt.recordShown(timestamp);
        assertEquals(timestamp, DonationPrompt.getLastShown());
    }

    @Test
    void shouldPrompt_handlesClockGoingBackward() {
        long recorded = 50_000_000L;
        DonationPrompt.recordShown(recorded);

        long backwards = recorded - 10_000L;
        assertTrue(DonationPrompt.shouldPrompt(backwards));
    }
}
