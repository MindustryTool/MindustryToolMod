package solim.layout;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import arc.graphics.Color;
import arc.scene.ui.Image;
import arc.util.Scaling;

class DividerTest {

    @BeforeAll
    static void initArc() {
        if (Core.app == null) {
            Core.app = new MockApplication();
        }
        if (Core.graphics == null) {
            Core.graphics = new MockGraphics();
        }
    }

    @Test
    void defaultDirectionIsX() {
        Divider d = new Divider();
        assertEquals(Direction.X, d.direction());
    }

    @Test
    void horizontalDividerSetsGrowX() {
        Divider d = new Divider(Direction.X);
        assertTrue(d.cellConfig().growX);
        assertFalse(d.cellConfig().growY);
    }

    @Test
    void verticalDividerSetsGrowY() {
        Divider d = new Divider(Direction.Y);
        assertTrue(d.cellConfig().growY);
        assertFalse(d.cellConfig().growX);
    }

    @Test
    void nullDirectionDefaultsToX() {
        Divider d = new Divider(null);
        assertEquals(Direction.X, d.direction());
    }

    @Test
    void nameModifierUpdatesName() {
        Divider d = new Divider();
        d.name("my-divider");
        assertEquals("my-divider", d.element().name);
    }

    @Test
    void elementIsImage() {
        Divider d = new Divider();
        assertTrue(d.element() instanceof Image);
    }

    @Test
    void colorAndHeightModifiers() {
        Divider d = new Divider();
        d.color(Color.green).height(2f);
        assertEquals(Color.green, d.image().color);
        assertEquals(2f, d.cellConfig().prefHeight.get());
    }

    @Test
    void dividerStretchesDrawableToFillCell() {
        // Scaling.fit would shrink the square source pixel into a centered dot
        // instead of a line, rendering the divider effectively invisible.
        assertEquals(Scaling.stretch, new Divider(Direction.X).solimImage().getScaling());
        assertEquals(Scaling.stretch, new Divider(Direction.Y).solimImage().getScaling());
    }
}
