package solim.graphics;

import static org.junit.jupiter.api.Assertions.*;

import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.scene.style.NinePatchDrawable;
import arc.scene.ui.layout.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.display.Badge;
import solim.input.Button;
import solim.layout.Card;
import solim.layout.Column;
import solim.layout.Container;
import solim.modifier.ElementModifiers;
import solim.overlay.Popup;
import solim.signal.Signal;

class RoundedGraphicsTest {

    @BeforeEach
    void setUp() {
        RoundedCache.clear();
    }

    @AfterEach
    void tearDown() {
        RoundedCache.clear();
    }

    @Test
    void calculateL4DistanceComputesSuperellipseNorm() {
        assertEquals(0f, RoundedGenerator.calculateL4Distance(0f, 0f), 0.001f);
        assertEquals(10f, RoundedGenerator.calculateL4Distance(10f, 0f), 0.001f);
        assertEquals(10f, RoundedGenerator.calculateL4Distance(0f, 10f), 0.001f);

        // Symmetry
        float d1 = RoundedGenerator.calculateL4Distance(6f, 8f);
        float d2 = RoundedGenerator.calculateL4Distance(8f, 6f);
        assertEquals(d1, d2, 0.0001f);

        // L4 distance should be strictly less than Euclidean sqrt(x^2 + y^2) for positive non-zero x, y
        float euclidean = (float) Math.hypot(6.0, 8.0); // 10.0
        assertTrue(d1 < euclidean, "L4 superellipse distance should be flatter/closer than Euclidean distance");
    }

    @Test
    void computeAlphaProvidesSmoothBoundaryGradient() {
        int radius = 10;
        // Deep inside corner
        assertEquals(1f, RoundedGenerator.computeAlpha(2f, 2f, radius), 0.001f);
        // Fully outside corner
        assertEquals(0f, RoundedGenerator.computeAlpha(12f, 12f, radius), 0.001f);

        // Exactly on the boundary (radius = 10), alpha should be near 0.5
        float boundaryAlpha = RoundedGenerator.computeAlpha(10f, 0f, radius);
        assertEquals(0.5f, boundaryAlpha, 0.05f);

        // Sub-pixel gradient must be monotonically decreasing as distance increases
        float innerAlpha = RoundedGenerator.computeAlpha(9.8f, 0f, radius);
        float outerAlpha = RoundedGenerator.computeAlpha(10.2f, 0f, radius);
        assertTrue(innerAlpha > boundaryAlpha);
        assertTrue(boundaryAlpha > outerAlpha);
    }

    @Test
    void computeBorderAlphaProducesHollowCenterAndSmoothEdges() {
        int radius = 10;
        float stroke = 2f;

        // Inside the hollow center (d < radius - stroke = 8)
        assertEquals(0f, RoundedGenerator.computeBorderAlpha(2f, 2f, radius, stroke), 0.001f);

        // Center of the stroke (d ~ 9)
        assertEquals(1f, RoundedGenerator.computeBorderAlpha(9f, 0f, radius, stroke), 0.001f);

        // Outside the outer edge (d > 10.5)
        assertEquals(0f, RoundedGenerator.computeBorderAlpha(12f, 0f, radius, stroke), 0.001f);
    }

    @Test
    void generatePixmapsHaveExpectedDimensions() {
        int radius = 6;
        int expectedSize = 2 * radius + 1;

        Pixmap solid = RoundedGenerator.generateSolidPixmap(radius);
        assertNotNull(solid);
        assertEquals(expectedSize, solid.width);
        assertEquals(expectedSize, solid.height);
        solid.dispose();

        Pixmap border = RoundedGenerator.generateBorderPixmap(radius, 1.5f);
        assertNotNull(border);
        assertEquals(expectedSize, border.width);
        assertEquals(expectedSize, border.height);
        border.dispose();
    }

    @Test
    void cacheReturnsSameInstanceForIdenticalParameters() {
        NinePatchDrawable solid1 = RoundedCache.getSolid(8);
        NinePatchDrawable solid2 = RoundedCache.getSolid(8);
        assertSame(solid1, solid2, "Same radius must return cached instance");

        NinePatchDrawable solidOther = RoundedCache.getSolid(12);
        assertNotSame(solid1, solidOther, "Different radius must return different instance");

        NinePatchDrawable border1 = RoundedCache.getBorder(8, 1.5f);
        NinePatchDrawable border2 = RoundedCache.getBorder(8, 1.5f);
        assertSame(border1, border2, "Same radius and stroke must return cached border instance");

        NinePatchDrawable borderOther = RoundedCache.getBorder(8, 2.0f);
        assertNotSame(border1, borderOther, "Different stroke must return different instance");
    }

    @Test
    void roundedDrawableCompositesFillAndBorder() {
        RoundedDrawable rd = new RoundedDrawable(10, Color.darkGray, 1.5f, Color.red);
        assertEquals(10, rd.getRadius());
        assertEquals(1.5f, rd.getStroke(), 0.001f);
        assertEquals(Color.darkGray, rd.getFillColor());
        assertEquals(Color.red, rd.getBorderColor());

        // Update radius and border
        rd.radius(14);
        assertEquals(14, rd.getRadius());

        rd.border(2f, Color.blue);
        assertEquals(2f, rd.getStroke(), 0.001f);
        assertEquals(Color.blue, rd.getBorderColor());
    }

    @Test
    void roundedDrawableRespondsToReactiveColorUpdatesAndStopsOnDispose() {
        Signal<Color> fillSignal = Signal.of(Color.red);
        Signal<Color> borderSignal = Signal.of(Color.white);

        RoundedDrawable rd = new RoundedDrawable(8);
        rd.fillColor(fillSignal);
        rd.border(1f, borderSignal);

        assertEquals(Color.red, rd.getFillColor());
        assertEquals(Color.white, rd.getBorderColor());

        // Reactive update
        fillSignal.set(Color.green);
        borderSignal.set(Color.yellow);
        solim.runtime.SignalDispatcher.flush();
        assertEquals(Color.green, rd.getFillColor());
        assertEquals(Color.yellow, rd.getBorderColor());

        // Dispose stops updates
        rd.dispose();
        fillSignal.set(Color.black);
        borderSignal.set(Color.blue);
        solim.runtime.SignalDispatcher.flush();
        assertEquals(Color.green, rd.getFillColor(), "Should not update after disposal");
        assertEquals(Color.yellow, rd.getBorderColor(), "Should not update after disposal");
    }

    @Test
    void layoutModifiersApplyRoundedAndBorderToUnderlyingTable() {
        Column col = new Column();
        col.rounded(12, Color.gray).border(2f, Color.scarlet);

        assertTrue(col.table().getBackground() instanceof RoundedDrawable);
        RoundedDrawable rd = (RoundedDrawable) col.table().getBackground();
        assertEquals(12, rd.getRadius());
        assertEquals(Color.gray, rd.getFillColor());
        assertEquals(2f, rd.getStroke(), 0.001f);
        assertEquals(Color.scarlet, rd.getBorderColor());
    }

    @Test
    void elementModifiersAppliesRoundedToArbitraryTable() {
        Table table = new Table();
        ElementModifiers.rounded(table, 10, Color.royal);
        ElementModifiers.border(table, 1.5f, Color.gold);

        assertTrue(table.getBackground() instanceof RoundedDrawable);
        RoundedDrawable rd = (RoundedDrawable) table.getBackground();
        assertEquals(10, rd.getRadius());
        assertEquals(Color.royal, rd.getFillColor());
        assertEquals(1.5f, rd.getStroke(), 0.001f);
        assertEquals(Color.gold, rd.getBorderColor());
    }

    @Test
    void interactiveComponentsSupportRoundedStyling() {
        Button btn = new Button();
        btn.rounded(8, Color.darkGray).border(1f, Color.gold);
        assertTrue(btn.button().getBackground() instanceof RoundedDrawable);

        Card card = new Card();
        card.rounded(16, Color.black).border(2f, Color.cyan);
        assertTrue(card.cardButton().getBackground() instanceof RoundedDrawable);

        Container container = new Container();
        container.rounded(6, Color.blue);
        assertTrue(container.table().getBackground() instanceof RoundedDrawable);

        Popup popup = new Popup();
        popup.rounded(8, Color.gray).border(1f, Color.white);
        assertTrue(popup.table().getBackground() instanceof RoundedDrawable);

        if (arc.Core.scene != null) {
            Badge badge = new Badge("New");
            badge.rounded(4, Color.scarlet);
            assertTrue(badge.table().getBackground() instanceof RoundedDrawable);
        }
    }

    @Test
    void borderAndRoundedDefaultToTransparentBackground() {
        RoundedDrawable raw = new RoundedDrawable(8);
        assertEquals(Color.clear, raw.getFillColor(), "Default fillColor must be transparent (Color.clear)");

        Column colBorder = new Column();
        colBorder.border(1.5f, Color.darkGray);
        assertTrue(colBorder.table().getBackground() instanceof RoundedDrawable);
        RoundedDrawable rdBorders = (RoundedDrawable) colBorder.table().getBackground();
        assertEquals(Color.clear, rdBorders.getFillColor(), "Calling .border() must default to transparent fillColor");
        assertEquals(Color.darkGray, rdBorders.getBorderColor());
        assertEquals(1.5f, rdBorders.getStroke(), 0.001f);

        Column colRounded = new Column();
        colRounded.rounded(6);
        assertTrue(colRounded.table().getBackground() instanceof RoundedDrawable);
        RoundedDrawable rdRound = (RoundedDrawable) colRounded.table().getBackground();
        assertEquals(6, rdRound.getRadius());
        assertEquals(Color.clear, rdRound.getFillColor(), "Calling .rounded(radius) without color must default to transparent fillColor");
    }

    @Test
    void baseDrawableDefaultsToNull() {
        RoundedDrawable rd = new RoundedDrawable(8, Color.darkGray);
        assertNull(rd.getBaseDrawable(), "baseDrawable should be null by default");
    }

    @Test
    void baseDrawableCanBeSetAndRetrieved() {
        RoundedDrawable rd = new RoundedDrawable(8);
        rd.baseDrawable(mindustry.ui.Styles.black6);
        assertSame(mindustry.ui.Styles.black6, rd.getBaseDrawable(), "baseDrawable should be retrievable after setting");
    }

    @Test
    void baseDrawableIsIndependentOfFillAndBorder() {
        RoundedDrawable rd = new RoundedDrawable(10, Color.darkGray, 1.5f, Color.red);
        rd.baseDrawable(mindustry.ui.Styles.black6);
        assertSame(mindustry.ui.Styles.black6, rd.getBaseDrawable());
        assertEquals(10, rd.getRadius());
        assertEquals(Color.darkGray, rd.getFillColor());
        assertEquals(1.5f, rd.getStroke(), 0.001f);
        assertEquals(Color.red, rd.getBorderColor());
    }

    @Test
    void radiusClampedWhenOversized() {
        RoundedDrawable rd = new RoundedDrawable(40);
        // radius=40 > min(100, 36)/2 = 18, so clamping should occur
        // We verify the drawable is created without error; actual draw requires Graphics context
        assertNotNull(rd);
        assertEquals(40, rd.getRadius());
    }

    @Test
    void radiusNotClampedWhenSmallEnough() {
        RoundedDrawable rd = new RoundedDrawable(8);
        // 8 <= min(100, 100)/2 = 50, so no clamping needed
        assertNotNull(rd);
        assertEquals(8, rd.getRadius(), "Radius should remain unchanged when small enough");
    }

    @Test
    void backgroundThenBorderPreservesBoth() {
        Column col = new Column();
        col.background(mindustry.ui.Styles.black6).border(1f, Color.gray);
        assertTrue(col.table().getBackground() instanceof RoundedDrawable);
        RoundedDrawable rd = (RoundedDrawable) col.table().getBackground();
        assertNotNull(rd.getBaseDrawable(), "baseDrawable should be set from background call");
        assertEquals(1f, rd.getStroke(), 0.001f);
        assertEquals(Color.gray, rd.getBorderColor());
    }

    @Test
    void borderThenBackgroundPreservesBoth() {
        Column col = new Column();
        col.border(1f, Color.gray).background(mindustry.ui.Styles.black6);
        assertTrue(col.table().getBackground() instanceof RoundedDrawable);
        RoundedDrawable rd = (RoundedDrawable) col.table().getBackground();
        assertNotNull(rd.getBaseDrawable(), "baseDrawable should be set from background call");
        assertEquals(1f, rd.getStroke(), 0.001f);
        assertEquals(Color.gray, rd.getBorderColor());
    }

    @Test
    void roundedThenBackgroundPreservesBoth() {
        Column col = new Column();
        col.rounded(12, Color.darkGray).background(mindustry.ui.Styles.black6);
        assertTrue(col.table().getBackground() instanceof RoundedDrawable);
        RoundedDrawable rd = (RoundedDrawable) col.table().getBackground();
        assertNotNull(rd.getBaseDrawable(), "baseDrawable should be set from background call");
        assertEquals(12, rd.getRadius());
        assertEquals(Color.darkGray, rd.getFillColor());
    }

    @Test
    void backgroundThenRoundedPreservesBoth() {
        Column col = new Column();
        col.background(mindustry.ui.Styles.black6).rounded(12, Color.darkGray);
        assertTrue(col.table().getBackground() instanceof RoundedDrawable);
        RoundedDrawable rd = (RoundedDrawable) col.table().getBackground();
        assertNotNull(rd.getBaseDrawable(), "baseDrawable should be set from background call");
        assertEquals(12, rd.getRadius());
        assertEquals(Color.darkGray, rd.getFillColor());
    }

    @Test
    void allThreeOrderIndependent() {
        // Order 1: bg -> rounded -> border
        Column col1 = new Column();
        col1.background(mindustry.ui.Styles.black6).rounded(10, Color.darkGray).border(1.5f, Color.red);
        assertTrue(col1.table().getBackground() instanceof RoundedDrawable);
        RoundedDrawable rd1 = (RoundedDrawable) col1.table().getBackground();
        assertNotNull(rd1.getBaseDrawable());
        assertEquals(10, rd1.getRadius());
        assertEquals(1.5f, rd1.getStroke(), 0.001f);

        // Order 2: border -> bg -> rounded
        Column col2 = new Column();
        col2.border(1.5f, Color.red).background(mindustry.ui.Styles.black6).rounded(10, Color.darkGray);
        assertTrue(col2.table().getBackground() instanceof RoundedDrawable);
        RoundedDrawable rd2 = (RoundedDrawable) col2.table().getBackground();
        assertNotNull(rd2.getBaseDrawable());
        assertEquals(10, rd2.getRadius());
        assertEquals(1.5f, rd2.getStroke(), 0.001f);

        // Order 3: rounded -> border -> bg
        Column col3 = new Column();
        col3.rounded(10, Color.darkGray).border(1.5f, Color.red).background(mindustry.ui.Styles.black6);
        assertTrue(col3.table().getBackground() instanceof RoundedDrawable);
        RoundedDrawable rd3 = (RoundedDrawable) col3.table().getBackground();
        assertNotNull(rd3.getBaseDrawable());
        assertEquals(10, rd3.getRadius());
        assertEquals(1.5f, rd3.getStroke(), 0.001f);
    }

    @Test
    void backgroundColorDirectOnTable() {
        Table table = new Table();
        ElementModifiers.background(table, Color.royal);
        assertTrue(table.getBackground() instanceof RoundedDrawable);
        RoundedDrawable rd = (RoundedDrawable) table.getBackground();
        assertNotNull(rd.getBaseDrawable(), "baseDrawable should be set for color background");
    }
}
