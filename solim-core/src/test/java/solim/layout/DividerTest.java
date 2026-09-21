package solim.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import arc.graphics.Color;
import arc.scene.ui.Image;
import arc.util.Scaling;
import solim.test.SolimEnv;

class DividerTest extends SolimEnv {


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
