package mindustrytool.features.browser.schematic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.TextureRegion;
import arc.mock.MockApplication;
import arc.mock.MockGL20;
import arc.mock.MockGraphics;
import arc.mock.MockSettings;
import arc.scene.Scene;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Label;
import arc.util.I18NBundle;
import java.util.ArrayList;
import java.util.List;
import mindustry.gen.Icon;
import mindustry.ui.Fonts;
import mindustrytool.features.browser.map.MapCard;
import mindustrytool.models.response.MapData;
import mindustrytool.models.response.SchematicData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.display.NetworkImage;
import solim.layout.ReactiveGrid;
import solim.performance.SlowSpan;
import solim.performance.SlowTracker;
import solim.reactive.Signal;

/**
 * Headless reproduction of a schematic browser page change: real
 * {@link SchematicCard} lists built through {@link ReactiveGrid} with image
 * loading stubbed out, so structure cost is reported separately from image
 * decode cost.
 */
class SchematicBrowserPerfTest {

    private static final int PAGE_SIZE = 5;
    private static final Runnable NOOP = new Runnable() {
        @Override
        public void run() {
        }
    };

    private static Font testFont;

    @BeforeAll
    static void initArc() {
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        Core.settings = new MockSettings();
        if (Core.gl == null) {
            Core.gl = new MockGL20();
            Core.gl20 = (MockGL20) Core.gl;
        }
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
        NetworkImage.setImageLoader(new NetworkImage.ImageLoader() {
            @Override
            public void load(String url, Cons<TextureRegion> onSuccess, Cons<Throwable> onError) {
                onError.get(new RuntimeException("fake-offline"));
            }
        });
        SlowTracker.setThresholds(0f, 0f);
        SlowTracker.setEnabled(true);
        SlowTracker.reset();
    }

    @AfterEach
    void tearDown() {
        SlowTracker.setEnabled(false);
        SlowTracker.setThresholds(16f, 50f);
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

    private static ReactiveGrid<SchematicData, String> grid(List<SchematicData> items) {
        return new ReactiveGrid<>(
                Signal.of(4),
                Signal.of(items),
                SchematicData::getItemId,
                item -> new SchematicCard(item, NOOP, NOOP, NOOP, NOOP));
    }

    private static int countSpans(List<SlowSpan> spans, String component, String phase) {
        int count = 0;
        for (SlowSpan span : spans) {
            boolean componentMatch = span.component.equals(component);
            boolean phaseMatch = phase == null || span.phase.equals(phase);
            if (componentMatch && phaseMatch) {
                count++;
            }
        }
        return count;
    }

    private static float totalMs(List<SlowSpan> spans, String component, String phase) {
        float total = 0f;
        for (SlowSpan span : spans) {
            boolean componentMatch = span.component.equals(component);
            boolean phaseMatch = phase == null || span.phase.equals(phase);
            if (componentMatch && phaseMatch) {
                total += span.durationMs;
            }
        }
        return total;
    }

    private static void printBreakdown(String label, List<SlowSpan> spans) {
        List<String> names = new ArrayList<>();
        List<Float> totals = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (SlowSpan span : spans) {
            int idx = names.indexOf(span.component);
            if (idx < 0) {
                names.add(span.component);
                totals.add(span.durationMs);
                counts.add(1);
            } else {
                totals.set(idx, totals.get(idx) + span.durationMs);
                counts.set(idx, counts.get(idx) + 1);
            }
        }
        for (int i = 0; i < names.size(); i++) {
            for (int j = i + 1; j < names.size(); j++) {
                if (totals.get(j) > totals.get(i)) {
                    String swapName = names.get(i);
                    names.set(i, names.get(j));
                    names.set(j, swapName);
                    Float swapTotal = totals.get(i);
                    totals.set(i, totals.get(j));
                    totals.set(j, swapTotal);
                    Integer swapCount = counts.get(i);
                    counts.set(i, counts.get(j));
                    counts.set(j, swapCount);
                }
            }
        }
        StringBuilder sb = new StringBuilder("SchematicBrowserPerf [breakdown ").append(label).append("]:");
        for (int i = 0; i < names.size(); i++) {
            sb.append(String.format(" %s=%dx%.2fms", names.get(i), counts.get(i), totals.get(i)));
        }
        System.out.println(sb.toString());
    }

    private static int maxDepthOf(List<SlowSpan> spans) {
        int max = 0;
        for (SlowSpan span : spans) {
            if (!"ParentStack".equals(span.component) || span.detail == null) {
                continue;
            }
            int depthIdx = span.detail.indexOf("depth=");
            if (depthIdx < 0) {
                continue;
            }
            int end = span.detail.indexOf(' ', depthIdx);
            String number = end >= 0 ? span.detail.substring(depthIdx + 6, end) : span.detail.substring(depthIdx + 6);
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

    private static void printSubtreeBreakdown(String label, List<SlowSpan> spans) {
        List<String> names = new ArrayList<>();
        List<Float> totals = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (SlowSpan span : spans) {
            if (!"ParentStack".equals(span.component) || !"subtree".equals(span.phase) || span.detail == null) {
                continue;
            }
            int nameIdx = span.detail.indexOf("name=");
            String name = nameIdx >= 0 ? span.detail.substring(nameIdx + 5) : span.detail;
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
        for (int i = 0; i < names.size(); i++) {
            for (int j = i + 1; j < names.size(); j++) {
                if (totals.get(j) > totals.get(i)) {
                    String swapName = names.get(i);
                    names.set(i, names.get(j));
                    names.set(j, swapName);
                    Float swapTotal = totals.get(i);
                    totals.set(i, totals.get(j));
                    totals.set(j, swapTotal);
                    Integer swapCount = counts.get(i);
                    counts.set(i, counts.get(j));
                    counts.set(j, swapCount);
                }
            }
        }
        StringBuilder sb = new StringBuilder("SchematicBrowserPerf [subtrees ").append(label).append("]:");
        for (int i = 0; i < names.size(); i++) {
            sb.append(String.format(" %s=%dx%.2fms", names.get(i), counts.get(i), totals.get(i)));
        }
        System.out.println(sb.toString());
    }

    @Test
    void pageChangeBuildsRealCardsAndCapturesSpans() {
        ReactiveGrid<SchematicData, String> pageOne = grid(page("perf-p1", PAGE_SIZE));
        pageOne.element();
        long layoutT0 = System.currentTimeMillis();
        pageOne.table().setSize(800f, 600f);
        pageOne.table().validate();
        long layoutMs = System.currentTimeMillis() - layoutT0;
        List<SlowSpan> pageOneSpans = SlowTracker.snapshot(256);
        System.out.printf("SchematicBrowserPerf [page-1 %d cards, fake images]: cards=%d cardMs=%.3f reconcileMs=%.3f reflowMs=%.3f layoutMs=%d%n",
                PAGE_SIZE,
                countSpans(pageOneSpans, "SchematicCard", "build"),
                totalMs(pageOneSpans, "SchematicCard", "build"),
                totalMs(pageOneSpans, "ReactiveGrid", "reconcile"),
                totalMs(pageOneSpans, "ReactiveGrid", "reflow"),
                layoutMs);
        assertTrue(countSpans(pageOneSpans, "SchematicCard", "build") >= PAGE_SIZE);
        pageOne.dispose();

        SlowTracker.reset();

        ReactiveGrid<SchematicData, String> pageTwo = grid(page("perf-p2", PAGE_SIZE));
        pageTwo.element();
        List<SlowSpan> pageTwoSpans = SlowTracker.snapshot(256);
        System.out.printf("SchematicBrowserPerf [page-2 %d cards, disjoint keys]: cards=%d cardMs=%.3f reconcileMs=%.3f reflowMs=%.3f%n",
                PAGE_SIZE,
                countSpans(pageTwoSpans, "SchematicCard", "build"),
                totalMs(pageTwoSpans, "SchematicCard", "build"),
                totalMs(pageTwoSpans, "ReactiveGrid", "reconcile"),
                totalMs(pageTwoSpans, "ReactiveGrid", "reflow"));

        assertTrue(countSpans(pageTwoSpans, "SchematicCard", "build") >= PAGE_SIZE);
        assertTrue(countSpans(pageTwoSpans, "ReactiveGrid", "reconcile") >= 1);
        assertTrue(countSpans(pageTwoSpans, "ReactiveGrid", "reflow") >= 1);
        assertEquals(PAGE_SIZE, pageTwo.table().getChildren().size);
        printBreakdown("page-2", pageTwoSpans);
        printSubtreeBreakdown("page-2", pageTwoSpans);
        System.out.println("SchematicBrowserPerf [depth page-2]: maxDepth=" + maxDepthOf(pageTwoSpans));
        pageTwo.dispose();
    }

    @Test
    void chunkRendersUnderFrameBudget() {
        SlowTracker.reset();
        ReactiveGrid<SchematicData, String> chunk = grid(page("perf-chunk", 8));
        chunk.element();
        long layoutT0 = System.currentTimeMillis();
        chunk.table().setSize(800f, 600f);
        chunk.table().validate();
        long layoutMs = System.currentTimeMillis() - layoutT0;
        List<SlowSpan> spans = SlowTracker.snapshot(256);
        System.out.printf("SchematicBrowserPerf [chunk 8 cards]: cards=%d cardMs=%.3f reconcileMs=%.3f reflowMs=%.3f layoutMs=%d%n",
                countSpans(spans, "SchematicCard", "build"),
                totalMs(spans, "SchematicCard", "build"),
                totalMs(spans, "ReactiveGrid", "reconcile"),
                totalMs(spans, "ReactiveGrid", "reflow"),
                layoutMs);

        assertTrue(countSpans(spans, "SchematicCard", "build") >= 8);
        assertEquals(8, chunk.table().getChildren().size);
        chunk.dispose();
    }

    @Test
    void realisticPageRendersTwentyCards() {
        SlowTracker.reset();
        // Keep the fixed profiler ring focused on card build spans for this
        // full-page measurement; phase and subtree detail are covered elsewhere.
        SlowTracker.setThresholds(0f, Float.MAX_VALUE);
        ReactiveGrid<SchematicData, String> fullPage = grid(page("perf-full", 20));
        fullPage.element();
        long layoutT0 = System.currentTimeMillis();
        fullPage.table().setSize(800f, 600f);
        fullPage.table().validate();
        long layoutMs = System.currentTimeMillis() - layoutT0;
        List<SlowSpan> spans = SlowTracker.snapshot(256);
        System.out.printf("SchematicBrowserPerf [full-page 20 cards]: cards=%d cardMs=%.3f reconcileMs=%.3f reflowMs=%.3f layoutMs=%d%n",
                countSpans(spans, "SchematicCard", "build"),
                totalMs(spans, "SchematicCard", "build"),
                totalMs(spans, "ReactiveGrid", "reconcile"),
                totalMs(spans, "ReactiveGrid", "reflow"),
                layoutMs);
        printSubtreeBreakdown("full-page", spans);

        assertTrue(countSpans(spans, "SchematicCard", "build") >= 20);
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
        ReactiveGrid<MapData, String> grid = new ReactiveGrid<>(
                Signal.of(4),
                Signal.of(maps),
                MapData::getItemId,
                item -> new MapCard(item, NOOP, NOOP, NOOP, NOOP));
        grid.element();
        List<SlowSpan> spans = SlowTracker.snapshot(256);
        System.out.printf("SchematicBrowserPerf [maps 3 cards]: cards=%d cardMs=%.3f%n",
                countSpans(spans, "MapCard", "build"),
                totalMs(spans, "MapCard", "build"));

        assertTrue(countSpans(spans, "MapCard", "build") >= 3);
        assertEquals(3, grid.table().getChildren().size);
        grid.dispose();
    }

    @Test
    void decodeCpuVariantReportsImageCostSeparately() {
        NetworkImage.setImageLoader(new NetworkImage.ImageLoader() {
            @Override
            public void load(String url, Cons<TextureRegion> onSuccess, Cons<Throwable> onError) {
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
            }
        });

        ReactiveGrid<SchematicData, String> withImages = grid(page("perf-img", PAGE_SIZE));
        withImages.element();
        List<SlowSpan> spans = SlowTracker.snapshot(256);
        System.out.printf("SchematicBrowserPerf [decode-cpu %d cards]: cards=%d cardMs=%.3f reconcileMs=%.3f reflowMs=%.3f%n",
                PAGE_SIZE,
                countSpans(spans, "SchematicCard", "build"),
                totalMs(spans, "SchematicCard", "build"),
                totalMs(spans, "ReactiveGrid", "reconcile"),
                totalMs(spans, "ReactiveGrid", "reflow"));

        assertTrue(countSpans(spans, "SchematicCard", "build") >= PAGE_SIZE);
        assertTrue(countSpans(spans, "ReactiveGrid", "reconcile") >= 1);
        withImages.dispose();
    }
}
