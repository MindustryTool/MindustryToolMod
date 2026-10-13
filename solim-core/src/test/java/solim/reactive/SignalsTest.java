package solim.reactive;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.Events;
import arc.mock.MockGraphics;
import solim.test.SolimEnv;
import java.util.concurrent.atomic.AtomicInteger;
import mindustry.game.EventType.ResizeEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.runtime.SignalDispatcher;

class SignalsTest extends SolimEnv {

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

        @Override
        public boolean isPortrait() {
            return height > width;
        }
    }

    private static ResizableMockGraphics mockGraphics;

    @BeforeAll
    static void initCore() {
        mockGraphics = new ResizableMockGraphics();
        Core.graphics = mockGraphics;
        Signals.init();
    }

    @BeforeEach
    void resetDimensions() {
        mockGraphics.width = 1000;
        mockGraphics.height = 500;
        Events.fire(new ResizeEvent());
    }

    @Test
    void testInitialStateIsLandscape() {
        assertFalse(Signals.isPortrait().peek());
    }

    @Test
    void testOrientationChangeOnResizeEvent() {

        mockGraphics.width = 500;
        mockGraphics.height = 1000;
        Events.fire(new ResizeEvent());

        assertTrue(Signals.isPortrait().peek());

        mockGraphics.width = 1200;
        mockGraphics.height = 800;
        Events.fire(new ResizeEvent());

        assertFalse(Signals.isPortrait().peek());
    }

    @Test
    void testDeduplicationWhenOrientationUnchanged() {
        AtomicInteger fireCount = new AtomicInteger(0);
        Effect e = Effect.of(Signals.isPortrait(), val -> fireCount.incrementAndGet());
        assertEquals(1, fireCount.get());

        mockGraphics.width = 1600;
        mockGraphics.height = 900;
        Events.fire(new ResizeEvent());
        SignalDispatcher.flush();

        assertEquals(1, fireCount.get());

        mockGraphics.width = 400;
        mockGraphics.height = 800;
        Events.fire(new ResizeEvent());
        SignalDispatcher.flush();

        assertEquals(2, fireCount.get());
        e.dispose();
    }

    @Test
    void testMultipleSubscribers() {
        AtomicInteger sub1 = new AtomicInteger(0);
        AtomicInteger sub2 = new AtomicInteger(0);

        Effect e1 = Effect.of(Signals.isPortrait(), val -> sub1.incrementAndGet());
        Effect e2 = Effect.of(Signals.isPortrait(), val -> sub2.incrementAndGet());
        assertEquals(1, sub1.get());
        assertEquals(1, sub2.get());

        mockGraphics.width = 300;
        mockGraphics.height = 600;
        Events.fire(new ResizeEvent());
        SignalDispatcher.flush();

        assertEquals(2, sub1.get());
        assertEquals(2, sub2.get());
        e1.dispose();
        e2.dispose();
    }
}
