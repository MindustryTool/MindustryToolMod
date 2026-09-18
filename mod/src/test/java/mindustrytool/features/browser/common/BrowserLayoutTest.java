package mindustrytool.features.browser.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class BrowserLayoutTest {

    @Test
    void calculateColumnsAcrossResolutions() {
        assertEquals(1, BrowserLayout.calculateColumns(200f));
        assertEquals(1, BrowserLayout.calculateColumns(400f));
        assertEquals(4, BrowserLayout.calculateColumns(1280f));
        assertEquals(7, BrowserLayout.calculateColumns(1920f));
        assertEquals(10, BrowserLayout.calculateColumns(2560f));
        assertEquals(15, BrowserLayout.calculateColumns(3840f));
    }

    @Test
    void calculateRowsAcrossResolutions() {
        assertEquals(1, BrowserLayout.calculateRows(200f));
        assertEquals(1, BrowserLayout.calculateRows(720f));
        assertEquals(3, BrowserLayout.calculateRows(1080f));
        assertEquals(4, BrowserLayout.calculateRows(1440f));
        assertEquals(6, BrowserLayout.calculateRows(2160f));
    }

    @Test
    void calculateCapacityAndPageSizeClamping() {
        // Small laptop / mobile viewport: capacity is small, clamped to minimum 20
        assertEquals(3, BrowserLayout.calculateCapacity(800f, 600f));
        assertEquals(BrowserLayout.PAGE_SIZE_MIN, BrowserLayout.calculatePageSize(800f, 600f));

        // 720p: 4 cols * 1 row = 4 items, clamped to 20
        assertEquals(4, BrowserLayout.calculateCapacity(1280f, 720f));
        assertEquals(BrowserLayout.PAGE_SIZE_MIN, BrowserLayout.calculatePageSize(1280f, 720f));

        // 1080p: 7 cols * 3 rows = 21 items
        assertEquals(21, BrowserLayout.calculateCapacity(1920f, 1080f));
        assertEquals(21, BrowserLayout.calculatePageSize(1920f, 1080f));

        // 1440p (matching user's screen): 10 cols * 4 rows = 40 items
        assertEquals(40, BrowserLayout.calculateCapacity(2560f, 1440f));
        assertEquals(40, BrowserLayout.calculatePageSize(2560f, 1440f));

        // 4K UHD: 15 cols * 6 rows = 90 items
        assertEquals(90, BrowserLayout.calculateCapacity(3840f, 2160f));
        assertEquals(90, BrowserLayout.calculatePageSize(3840f, 2160f));

        // Giant viewport: capacity exceeds 100, clamped to maximum 100
        assertTrue(BrowserLayout.calculateCapacity(8000f, 3000f) > 100);
        assertEquals(BrowserLayout.PAGE_SIZE_MAX, BrowserLayout.calculatePageSize(8000f, 3000f));
    }

    @Test
    void calculateContentWidthAndGutterSymmetry() {
        float viewportWidth = 1920f;
        float availableWidth = viewportWidth - BrowserLayout.HORIZONTAL_PADDING * 2f;
        float cardsWidth = BrowserLayout.calculateCardsWidth(viewportWidth);
        float contentWidth = BrowserLayout.calculateContentWidth(viewportWidth);

        // Content width includes symmetrical gutters on both sides
        assertEquals(cardsWidth + BrowserLayout.SCROLLBAR_GUTTER * 2f, contentWidth);
        assertTrue(contentWidth <= availableWidth);
        assertTrue(cardsWidth > 0f);
    }

    @Test
    void browserStatePageSizeReactivityAndClamping() {
        BrowserState<String> state = new BrowserState<>(s -> CompletableFuture.completedFuture(Collections.emptyList()));
        assertEquals(BrowserLayout.PAGE_SIZE_MIN, state.getPageSize());

        // Navigating to page 3
        state.goToPage(3);
        assertEquals(3, state.page().peek());

        // Setting a new valid page size should update and reset page to 0
        state.setPageSize(40);
        assertEquals(40, state.getPageSize());
        assertEquals(0, state.page().peek());

        // Underflow clamp
        state.setPageSize(5);
        assertEquals(BrowserLayout.PAGE_SIZE_MIN, state.getPageSize());

        // Overflow clamp
        state.setPageSize(250);
        assertEquals(BrowserLayout.PAGE_SIZE_MAX, state.getPageSize());

        // Setting identical page size does not reset page
        state.goToPage(2);
        state.setPageSize(BrowserLayout.PAGE_SIZE_MAX);
        assertEquals(2, state.page().peek());
    }
}
