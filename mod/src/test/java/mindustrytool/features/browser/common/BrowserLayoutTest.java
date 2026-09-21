package mindustrytool.features.browser.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.struct.Seq;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import solim.test.SolimEnv;

class BrowserLayoutTest extends SolimEnv {

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
        BrowserState<String> state = new BrowserState<>(
                s -> CompletableFuture.completedFuture(Collections.emptyList()));
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

    @Test
    void firstItemsSlicesForChunkedRendering() {
        Seq<String> all = Seq.with("a", "b", "c", "d");

        assertEquals(2, BrowserState.firstItems(all, 2).size);
        assertEquals("a", BrowserState.firstItems(all, 2).get(0));
        assertEquals("b", BrowserState.firstItems(all, 2).get(1));
        assertEquals(4, BrowserState.firstItems(all, 100).size);
        assertTrue(BrowserState.firstItems(all, 0).isEmpty());
        assertTrue(BrowserState.firstItems(all, -5).isEmpty());
        assertTrue(BrowserState.<String>firstItems(null, 8).isEmpty());
    }
}
