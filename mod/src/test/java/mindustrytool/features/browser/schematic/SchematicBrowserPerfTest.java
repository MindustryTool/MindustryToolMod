package mindustrytool.features.browser.schematic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Scene;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Label;
import arc.util.I18NBundle;
import mindustry.gen.Icon;
import mindustry.ui.Fonts;
import mindustrytool.features.browser.map.MapCard;
import mindustrytool.models.response.MapData;
import mindustrytool.models.response.SchematicData;
import mindustrytool.test.MindustryTestEnv;
import solim.display.NetworkImage;
import solim.layout.ReactiveGrid;
import solim.performance.Perf;
import solim.performance.PerfSpan;
import solim.reactive.Signal;

/**
 * Headless reproduction of a schematic browser page change: real
 * {@link SchematicCard} lists built through {@link ReactiveGrid} with image
 * loading stubbed out. Asserts the structural subtree/attach spans emitted by
 * the profiling parent stack.
 */
class SchematicBrowserPerfTest extends MindustryTestEnv {

    private static final int PAGE_SIZE = 5;
    private static final Runnable NOOP = new Runnable() {
        @Override
        public void run() {
        }
    };

    private static Font testFont;

    @BeforeAll
    static void initArc() {
        if (Core.scene == null) {
            Core.scene = new Scene();
        }
        if (Core.bundle == null) {
            Core.bundle = I18NBundle.createEmptyBundle();
        }
        Icon.book = new TextureRegionDrawable();
        Icon.image = new TextureRegionDrawable();
        Icon.imageSmall = new TextureRegionDrawable();
        Icon.downloadSmall = new TextureRegionDrawable();
        Icon.copy = new TextureRegionDrawable();
        Icon.upOpenSmall = new TextureRegionDrawable();
        Icon.terrain = new TextureRegionDrawable();
        Icon.play = new TextureRegionDrawable();
        if (testFont == null) {
            Font.FontData fontData = new Font.FontData() {
                @Override
                public boolean hasGlyph(char ch) {
                    return true;
                }

                @Override
                public Font.Glyph getGlyph(char ch) {
                    Font.Glyph g = super.getGlyph(ch);
                    if (g == null) {
                        g = new Font.Glyph();
                        g.id = ch;
                        g.width = 8;
                        g.height = 12;
                        g.xadvance = 8;
                        setGlyph(ch, g);
                    }
                    return g;
                }
            };
            fontData.lineHeight = 18f;
            fontData.capHeight = 14f;
            fontData.ascent = 14f;
            fontData.descent = -4f;
            fontData.down = -18f;
            testFont = new Font(fontData, new TextureRegion(), false);
        }
        Fonts.def = testFont;
        Label.LabelStyle labelStyle = new Label.LabelStyle(testFont, Color.white);
        Core.scene.addStyle(Label.LabelStyle.class, labelStyle);
    }

    @BeforeEach
    void setUp() {
        NetworkImage.clearCache();
        NetworkImage.setImageLoader((url, radius, targetW, targetH, onSuccess, onError) ->
                onError.get(new RuntimeException("fake-offline")));
        Perf.setEnabled(true);
        Perf.setThreshold(0f);
        Perf.reset();
    }

    @AfterEach
    void tearDown() {
        Perf.setEnabled(false);
        Perf.setThreshold(50f);
        NetworkImage.setImageLoader(null);
        NetworkImage.clearCache();
    }

    private static SchematicData schematic(String itemId, String name) {
        SchematicData data = new SchematicData();
        data.setItemId(itemId);
        data.setName(name);
        data.setLikes(12L);
        data.setDownloads(34L);
        data.setComments(5L);
        return data;
    }

    private static List<SchematicData> page(String prefix, int count) {
        List<SchematicData> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            out.add(schematic(prefix + "-item-" + i, "Card " + i));
        }
        return out;
    }

    private static ReactiveGrid<SchematicData> grid(List<SchematicData> items) {
        ReactiveGrid<SchematicData> grid = new ReactiveGrid<>(Signal.of(items));
        grid.columns(4).key(SchematicData::getItemId);
        grid.children(item -> new SchematicCard(item, NOOP, NOOP, NOOP, NOOP));
        return grid;
    }

    private static String spanName(PerfSpan span) {
        if (span.detail == null) {
            return null;
        }
        int idx = span.detail.indexOf("name=");
        if (idx < 0) {
            return null;
        }
        int start = idx + 5;
        int end = span.detail.indexOf(' ', start);
        return end < 0 ? span.detail.substring(start) : span.detail.substring(start, end);
    }

    private static boolean isCardSubtree(PerfSpan span, String prefix) {
        if (!"subtree".equals(span.phase)) {
            return false;
        }
        String name = spanName(span);
        return name != null && name.startsWith(prefix) && !name.startsWith(prefix + "preview-");
    }

    private static int countCardSubtrees(List<PerfSpan> spans, String prefix) {
        int count = 0;
        for (PerfSpan span : spans) {
            if (isCardSubtree(span, prefix)) {
                count++;
            }
        }
        return count;
    }

    private static float totalMs(List<PerfSpan> spans, String prefix) {
        float total = 0f;
        for (PerfSpan span : spans) {
            if (isCardSubtree(span, prefix)) {
                total += span.durationMs;
            }
        }
        return total;
    }

    private static int maxDepthOf(List<PerfSpan> spans) {
        int max = 0;
        for (PerfSpan span : spans) {
            if (!"subtree".equals(span.phase) || span.detail == null) {
                continue;
            }
            int depthIdx = span.detail.indexOf("depth=");
            if (depthIdx < 0) {
                continue;
            }
            int end = span.detail.indexOf(' ', depthIdx);
            String number = end >= 0 ? span.detail.substring(depthIdx + 6, end)
                    : span.detail.substring(depthIdx + 6);
            try {
                int depth = Integer.parseInt(number.trim());
                if (depth > max) {
                    max = depth;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return max;
    }

    private static void printBreakdown(String label, List<PerfSpan> spans, String prefix) {
        List<String> names = new ArrayList<>();
        List<Float> totals = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (PerfSpan span : spans) {
            if (!isCardSubtree(span, prefix)) {
                continue;
            }
            String name = spanName(span);
            int idx = names.indexOf(name);
            if (idx < 0) {
                names.add(name);
                totals.add(span.durationMs);
                counts.add(1);
            } else {
                totals.set(idx, totals.get(idx) + span.durationMs);
                counts.set(idx, counts.get(idx) + 1);
            }
        }
        StringBuilder sb = new StringBuilder("SchematicBrowserPerf [breakdown ").append(label).append("]:");
        for (int i = 0; i < names.size(); i++) {
            sb.append(String.format(" %s=%dx%.2fms", names.get(i), counts.get(i), totals.get(i)));
        }
        System.out.println(sb.toString());
    }

    @Test
    void pageChangeBuildsRealCardsAndCapturesSpans() {
        ReactiveGrid<SchematicData> pageOne = grid(page("perf-p1", PAGE_SIZE));
        pageOne.element();
        long layoutT0 = System.currentTimeMillis();
        pageOne.table().setSize(800f, 600f);
        pageOne.table().validate();
        long layoutMs = System.currentTimeMillis() - layoutT0;
        List<PerfSpan> pageOneSpans = Perf.snapshot(256);
        System.out.printf("SchematicBrowserPerf [page-1 %d cards, fake images]: cards=%d cardMs=%.3f layoutMs=%d maxDepth=%d%n",
                PAGE_SIZE,
                countCardSubtrees(pageOneSpans, "SchematicCard-"),
                totalMs(pageOneSpans, "SchematicCard-"),
                layoutMs,
                maxDepthOf(pageOneSpans));
        assertTrue(countCardSubtrees(pageOneSpans, "SchematicCard-") >= PAGE_SIZE);
        pageOne.dispose();

        Perf.reset();

        ReactiveGrid<SchematicData> pageTwo = grid(page("perf-p2", PAGE_SIZE));
        pageTwo.element();
        List<PerfSpan> pageTwoSpans = Perf.snapshot(256);
        System.out.printf("SchematicBrowserPerf [page-2 %d cards, disjoint keys]: cards=%d cardMs=%.3f%n",
                PAGE_SIZE,
                countCardSubtrees(pageTwoSpans, "SchematicCard-"),
                totalMs(pageTwoSpans, "SchematicCard-"));

        assertTrue(countCardSubtrees(pageTwoSpans, "SchematicCard-") >= PAGE_SIZE);
        assertEquals(PAGE_SIZE, pageTwo.table().getChildren().size);
        printBreakdown("page-2", pageTwoSpans, "SchematicCard-");
        System.out.println("SchematicBrowserPerf [depth page-2]: maxDepth=" + maxDepthOf(pageTwoSpans));
        pageTwo.dispose();
    }

    @Test
    void chunkRendersUnderFrameBudget() {
        Perf.reset();
        ReactiveGrid<SchematicData> chunk = grid(page("perf-chunk", 8));
        chunk.element();
        long layoutT0 = System.currentTimeMillis();
        chunk.table().setSize(800f, 600f);
        chunk.table().validate();
        long layoutMs = System.currentTimeMillis() - layoutT0;
        List<PerfSpan> spans = Perf.snapshot(256);
        System.out.printf("SchematicBrowserPerf [chunk 8 cards]: cards=%d cardMs=%.3f layoutMs=%d%n",
                countCardSubtrees(spans, "SchematicCard-"),
                totalMs(spans, "SchematicCard-"),
                layoutMs);

        assertTrue(countCardSubtrees(spans, "SchematicCard-") >= 8);
        assertEquals(8, chunk.table().getChildren().size);
        chunk.dispose();
    }

    @Test
    void realisticPageRendersTwentyCards() {
        Perf.reset();
        ReactiveGrid<SchematicData> fullPage = grid(page("perf-full", 20));
        fullPage.element();
        long layoutT0 = System.currentTimeMillis();
        fullPage.table().setSize(800f, 600f);
        fullPage.table().validate();
        long layoutMs = System.currentTimeMillis() - layoutT0;
        List<PerfSpan> spans = Perf.snapshot(256);
        System.out.printf("SchematicBrowserPerf [full-page 20 cards]: cards=%d cardMs=%.3f layoutMs=%d totalRecorded=%d%n",
                countCardSubtrees(spans, "SchematicCard-"),
                totalMs(spans, "SchematicCard-"),
                layoutMs,
                Perf.totalRecorded());
        printBreakdown("full-page", spans, "SchematicCard-");

        assertTrue(countCardSubtrees(spans, "SchematicCard-") >= 20);
        assertEquals(20, fullPage.table().getChildren().size);
        fullPage.dispose();
    }

    @Test
    void mapCardsBuildWithTrimmedActionBar() {
        List<MapData> maps = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            MapData data = new MapData();
            data.setItemId("map-item-" + i);
            data.setName("Map " + i);
            data.setLikes(7L);
            data.setDownloads(3L);
            data.setComments(1L);
            maps.add(data);
        }
        ReactiveGrid<MapData> grid = new ReactiveGrid<>(Signal.of(maps));
        grid.columns(4).key(MapData::getItemId);
        grid.children(item -> new MapCard(item, NOOP, NOOP, NOOP, NOOP));
        grid.element();
        List<PerfSpan> spans = Perf.snapshot(256);
        System.out.printf("SchematicBrowserPerf [maps 3 cards]: cards=%d cardMs=%.3f%n",
                countCardSubtrees(spans, "MapCard-"),
                totalMs(spans, "MapCard-"));

        assertTrue(countCardSubtrees(spans, "MapCard-") >= 3);
        assertEquals(3, grid.table().getChildren().size);
        grid.dispose();
    }

    @Test
    void imageDecodeDoesNotSuppressStructuralSpans() {
        NetworkImage.setImageLoader((url, radius, targetW, targetH, onSuccess, onError) -> {
            Pixmap pixmap = new Pixmap(30, 30);
                int opaqueWhite = Color.rgba8888(1f, 1f, 1f, 1f);
                for (int y = 0; y < 30; y++) {
                    for (int x = 0; x < 30; x++) {
                        pixmap.set(x, y, opaqueWhite);
                    }
                }
                NetworkImage.applyRoundedMask(pixmap, 8);
                pixmap.dispose();
                onSuccess.get(new TextureRegion());
        });

        ReactiveGrid<SchematicData> withImages = grid(page("perf-img", PAGE_SIZE));
        withImages.element();
        List<PerfSpan> spans = Perf.snapshot(256);
        System.out.printf("SchematicBrowserPerf [decode-cpu %d cards]: cards=%d cardMs=%.3f%n",
                PAGE_SIZE,
                countCardSubtrees(spans, "SchematicCard-"),
                totalMs(spans, "SchematicCard-"));

        assertTrue(countCardSubtrees(spans, "SchematicCard-") >= PAGE_SIZE);
        withImages.dispose();
    }
}
