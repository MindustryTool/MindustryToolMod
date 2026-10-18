package mindustrytool.features.browser.common;

import static solim.UI.*;
import static solim.modifier.Values.border;
import static solim.modifier.Values.color;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.paddingX;
import static solim.modifier.Values.paddingY;
import static solim.modifier.Values.rounded;

import java.util.List;

import arc.Core;
import arc.graphics.Color;
import mindustrytool.components.WebStyles;
import mindustrytool.models.response.TagData;

public final class BrowserLayout {

    public static final float CARD_SIZE = unit(58f);
    public static final float CARD_WIDTH = CARD_SIZE;
    public static final float CARD_INTERNAL_GAP = unit(2f);
    public static final float CARD_ACTION_ROW_HEIGHT = unit(9f);
    public static final float CARD_HEIGHT = CARD_SIZE + CARD_INTERNAL_GAP + CARD_ACTION_ROW_HEIGHT;
    public static final float CARD_GAP = unit(4f);
    public static final float HORIZONTAL_PADDING = unit(4f);
    public static final float SCROLLBAR_GUTTER = unit(0f);
    public static final float VERTICAL_OVERHEAD = unit(50f);

    public static final int PAGE_SIZE_MIN = 20;
    public static final int PAGE_SIZE_MAX = 100;

    public static final int RENDER_CHUNK_SIZE = 8;

    private BrowserLayout() {
    }

    public static int calculateColumns(float viewportWidth) {
        float availableWidth = Math.max(0f, viewportWidth - HORIZONTAL_PADDING * 2f - SCROLLBAR_GUTTER * 2f);
        return Math.max(1, (int) ((availableWidth + CARD_GAP) / (CARD_WIDTH + CARD_GAP)));
    }

    public static float calculateCardsWidth(float viewportWidth) {
        float availableWidth = Math.max(0f, viewportWidth - HORIZONTAL_PADDING * 2f - SCROLLBAR_GUTTER * 2f);
        int cols = calculateColumns(viewportWidth);
        float calculatedWidth = cols * CARD_WIDTH + (cols - 1) * CARD_GAP;
        return Math.min(availableWidth, calculatedWidth);
    }

    public static float calculateContentWidth(float viewportWidth) {
        return calculateCardsWidth(viewportWidth) + SCROLLBAR_GUTTER * 2f;
    }

    public static int calculateRows(float viewportHeight) {
        float availableHeight = Math.max(0f, viewportHeight - VERTICAL_OVERHEAD);
        return Math.max(1, (int) ((availableHeight + CARD_GAP) / (CARD_HEIGHT + CARD_GAP))) + 1;
    }

    public static int calculateCapacity(float viewportWidth, float viewportHeight) {
        return calculateColumns(viewportWidth) * calculateRows(viewportHeight);
    }

    public static int calculatePageSize(float viewportWidth, float viewportHeight) {
        int capacity = calculateCapacity(viewportWidth, viewportHeight);
        return Math.min(PAGE_SIZE_MAX, Math.max(PAGE_SIZE_MIN, capacity));
    }

    public static void renderTags(List<TagData> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }

        card(WebStyles.previewCardBackground()).props(growX(), padding(unit(4)), top(), left(), gap(unit(4))).children(() -> {
            text(Core.bundle.get("browser.detail.tags")).props(color(Color.white), growX(), left());
            wrap().props(growX(), left(), gap(unit(2))).children(() -> {
                for (TagData tag : tags) {
                    row().props(rounded(12), paddingX(unit(3)), paddingY(unit(1)), border(2, Color.darkGray)).children(() -> {
                        text(tag.getName()).props(color(tag.color()));
                    });
                }
            });
        });
    }

}
