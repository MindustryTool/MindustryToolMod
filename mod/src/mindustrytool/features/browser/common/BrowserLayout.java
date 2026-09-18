package mindustrytool.features.browser.common;

import static solim.UI.unit;

/**
 * Layout constants and responsive capacity calculations for schematic and map browsers.
 */
public final class BrowserLayout {

    public static final float CARD_SIZE = unit(58f);
    public static final float CARD_WIDTH = CARD_SIZE;
    public static final float CARD_INTERNAL_GAP = unit(2f);
    public static final float CARD_ACTION_ROW_HEIGHT = unit(9f);
    public static final float CARD_HEIGHT = CARD_SIZE + CARD_INTERNAL_GAP + CARD_ACTION_ROW_HEIGHT;
    public static final float CARD_GAP = unit(4f);
    public static final float HORIZONTAL_PADDING = unit(4f);
    public static final float VERTICAL_OVERHEAD = unit(44f);

    public static final int PAGE_SIZE_MIN = 20;
    public static final int PAGE_SIZE_MAX = 100;

    private BrowserLayout() {
    }

    public static int calculateColumns(float viewportWidth) {
        float availableWidth = Math.max(0f, viewportWidth - HORIZONTAL_PADDING * 2f);
        return Math.max(1, (int) ((availableWidth + CARD_GAP) / (CARD_WIDTH + CARD_GAP)));
    }

    public static float calculateContentWidth(float viewportWidth) {
        float availableWidth = Math.max(0f, viewportWidth - HORIZONTAL_PADDING * 2f);
        int cols = calculateColumns(viewportWidth);
        float calculatedWidth = cols * CARD_WIDTH + (cols - 1) * CARD_GAP;
        return Math.min(availableWidth, calculatedWidth);
    }

    public static int calculateRows(float viewportHeight) {
        float availableHeight = Math.max(0f, viewportHeight - VERTICAL_OVERHEAD);
        return Math.max(1, (int) ((availableHeight + CARD_GAP) / (CARD_HEIGHT + CARD_GAP)));
    }

    public static int calculateCapacity(float viewportWidth, float viewportHeight) {
        return calculateColumns(viewportWidth) * calculateRows(viewportHeight);
    }

    public static int calculatePageSize(float viewportWidth, float viewportHeight) {
        int capacity = calculateCapacity(viewportWidth, viewportHeight);
        return Math.min(PAGE_SIZE_MAX, Math.max(PAGE_SIZE_MIN, capacity));
    }
}
