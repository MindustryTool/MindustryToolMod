package solim.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import arc.Core;
import arc.Events;
import arc.mock.MockGraphics;
import mindustry.game.EventType.ResizeEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.reactive.Computed;
import solim.reactive.Signal;
import solim.test.SolimEnv;

class UnitsTest extends SolimEnv {

    static class ResizableMockGraphics extends MockGraphics {
        int width = 1000;
        int height = 500;

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }
    }

    private static ResizableMockGraphics mockGraphics;

    @BeforeAll
    static void initCore() {
        mockGraphics = new ResizableMockGraphics();
        Core.graphics = mockGraphics;
    }

    @BeforeEach
    void resetDimensions() {
        mockGraphics.width = 1000;
        mockGraphics.height = 500;
        Units.update();
    }

    @Test
    void testInitialUnits() {
        assertEquals(10f, Units.dvw.get(), 0.001f);
        assertEquals(5f, Units.dvh.get(), 0.001f);
        assertEquals(1000f, Units.width(), 0.001f);
        assertEquals(500f, Units.height(), 0.001f);
    }

    @Test
    void testPercentageHelpersReturnPlainValues() {
        assertEquals(500f, Units.dvw(50f), 0.001f);
        assertEquals(400f, Units.dvh(80f), 0.001f);
    }

    @Test
    void testReactiveDerivationGoesThroughASignal() {
        Signal<Float> pct = Signal.of(20f);
        Computed<Float> dynamicW = inOwnershipWindow(() -> Units.dvw.map(v -> v * pct.get()));

        assertEquals(200f, dynamicW.get(), 0.001f);

        pct.set(40f);
        assertEquals(400f, dynamicW.get(), 0.001f);
    }

    @Test
    void testAutoUpdateOnResizeEvent() {
        Computed<Float> halfWidth = inOwnershipWindow(() -> Units.dvw.map(v -> v * 50f));

        assertEquals(500f, halfWidth.get(), 0.001f);

        mockGraphics.width = 1600;
        mockGraphics.height = 900;
        Events.fire(new ResizeEvent());

        assertEquals(16f, Units.dvw.get(), 0.001f);
        assertEquals(9f, Units.dvh.get(), 0.001f);
        assertEquals(800f, halfWidth.get(), 0.001f);
        assertEquals(1600f, Units.width(), 0.001f);
        assertEquals(900f, Units.height(), 0.001f);
    }
}
