package solim.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DockEngineTest {

    private static final class TestTarget implements DockTarget {
        private final String id;
        private float x;
        private float y;
        private float width;
        private float height;
        private boolean active = true;
        private final DockSide[] sides;

        private TestTarget(String id, float x, float y, float width, float height, DockSide... sides) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.sides = sides;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public float x() {
            return x;
        }

        @Override
        public float y() {
            return y;
        }

        @Override
        public float width() {
            return width;
        }

        @Override
        public float height() {
            return height;
        }

        @Override
        public boolean active() {
            return active;
        }

        @Override
        public boolean supports(DockSide side) {
            for (DockSide supported : sides) {
                if (supported == side) {
                    return true;
                }
            }
            return false;
        }
    }

    @Test
    void snapsToTargetBottomWithinThreshold() {
        DockEngine engine = new DockEngine();
        engine.register(new TestTarget("minimap", 100f, 100f, 200f, 100f, DockSide.BOTTOM));

        DockCandidate candidate = engine.findSnap(150f, 60f, 100f, 50f);

        assertNotNull(candidate);
        assertEquals("minimap", candidate.target().id());
        assertEquals(DockSide.BOTTOM, candidate.side());
        assertEquals(150f, candidate.x(), 0.01f);
        assertEquals(46f, candidate.y(), 0.01f);
        assertEquals(50f, candidate.offset(), 0.01f);
        assertEquals(10f, candidate.distance(), 0.01f);
    }

    @Test
    void ignoresTargetsOutsideSnapThreshold() {
        DockEngine engine = new DockEngine();
        engine.register(new TestTarget("minimap", 100f, 100f, 200f, 100f, DockSide.BOTTOM));

        assertNull(engine.findSnap(150f, 80f, 100f, 50f));
    }

    @Test
    void ignoresInactiveTargets() {
        DockEngine engine = new DockEngine();
        TestTarget target = new TestTarget("minimap", 100f, 100f, 200f, 100f, DockSide.BOTTOM);
        engine.register(target);
        target.active = false;

        assertNull(engine.findSnap(150f, 60f, 100f, 50f));
    }

    @Test
    void ignoresExcludedTarget() {
        DockEngine engine = new DockEngine();
        engine.register(new TestTarget("self", 100f, 100f, 200f, 100f, DockSide.BOTTOM));

        assertNull(engine.findSnap(150f, 60f, 100f, 50f, "self"));
        assertNotNull(engine.findSnap(150f, 60f, 100f, 50f, "other"));
    }

    @Test
    void breakawayRequiresExceedingHysteresis() {
        DockEngine engine = new DockEngine();
        DockCandidate candidate = new DockCandidate(
                new TestTarget("minimap", 100f, 100f, 200f, 100f, DockSide.BOTTOM),
                DockSide.BOTTOM, 150f, 46f, 50f, 0f);

        assertFalse(engine.breaksAway(candidate, 170f, 46f), "10px of drag stays snapped");
        assertFalse(engine.breaksAway(candidate, 150f, 70f), "24px of drag stays snapped");
        assertTrue(engine.breaksAway(candidate, 150f, 71f), "25px of drag breaks the snap");
    }

    @Test
    void resolvesAnchorAgainstLiveTargetBounds() {
        DockEngine engine = new DockEngine();
        TestTarget target = new TestTarget("minimap", 100f, 100f, 200f, 100f, DockSide.BOTTOM);
        engine.register(target);

        DockAnchor anchor = new DockAnchor("minimap", DockSide.BOTTOM, 50f, 150f, 46f);
        DockCandidate resolved = engine.resolve(anchor, 100f, 50f);

        assertNotNull(resolved);
        assertEquals(150f, resolved.x(), 0.01f);
        assertEquals(46f, resolved.y(), 0.01f);

        target.y = 140f;
        DockCandidate moved = engine.resolve(anchor, 100f, 50f);

        assertNotNull(moved);
        assertEquals(86f, moved.y(), 0.01f, "HUD must follow the target's new bottom edge");
    }

    @Test
    void anchorResolutionFallsBackWhenTargetIsGone() {
        DockEngine engine = new DockEngine();
        DockAnchor anchor = new DockAnchor("missing", DockSide.BOTTOM, 50f, 150f, 46f);

        assertNull(engine.resolve(anchor, 100f, 50f));
    }

    @Test
    void snapsToScreenCornerPoint() {
        DockEngine engine = new DockEngine();
        engine.register(new TestTarget("screen-bottom-left", 0f, 0f, 0f, 0f, DockSide.TOP_RIGHT));

        DockCandidate candidate = engine.findSnap(10f, 10f, 100f, 50f);

        assertNotNull(candidate);
        assertEquals(DockSide.TOP_RIGHT, candidate.side());
        assertEquals(4f, candidate.x(), 0.01f);
        assertEquals(4f, candidate.y(), 0.01f);
    }

    @Test
    void registrationReplacesSameIdentifier() {
        DockEngine engine = new DockEngine();
        engine.register(new TestTarget("minimap", 0f, 0f, 10f, 10f, DockSide.BOTTOM));
        engine.register(new TestTarget("minimap", 100f, 100f, 200f, 100f, DockSide.BOTTOM));

        assertEquals(1, engine.size());
        assertNotNull(engine.findSnap(150f, 60f, 100f, 50f));
    }
}
