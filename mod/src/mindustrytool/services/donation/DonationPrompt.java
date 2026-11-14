package mindustrytool.services.donation;

import arc.Core;

public final class DonationPrompt {

    public static final String KEY_DONT_SHOW = "mindustrytool.donation.dont-show-again";
    public static final String KEY_LAST_SHOWN = "mindustrytool.donation.last-shown";
    public static final long COOLDOWN_MILLIS = 3L * 24L * 60L * 60L * 1000L;

    public static boolean shouldPrompt(long currentTime) {
        if (isDontShowAgain()) {
            return false;
        }

        long lastShown = getLastShown();
        return lastShown <= 0L || currentTime < lastShown || (currentTime - lastShown) >= COOLDOWN_MILLIS;
    }

    public static boolean isDontShowAgain() {
        return Core.settings != null && Core.settings.getBool(KEY_DONT_SHOW, false);
    }

    public static void setDontShowAgain(boolean value) {
        if (Core.settings != null) {
            Core.settings.put(KEY_DONT_SHOW, value);
        }
    }

    public static long getLastShown() {
        return Core.settings != null ? Core.settings.getLong(KEY_LAST_SHOWN, 0L) : 0L;
    }

    public static void recordShown(long timestamp) {
        if (Core.settings != null) {
            Core.settings.put(KEY_LAST_SHOWN, timestamp);
        }
    }

    public static void maybePrompt() {
        long now = System.currentTimeMillis();
        if (shouldPrompt(now)) {
            recordShown(now);
            new DonationDialog().show();
        }
    }

    private DonationPrompt() {
    }
}
