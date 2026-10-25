package solim.graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static solim.modifier.Values.backdropBlur;
import static solim.modifier.Values.contentBlur;
import static solim.modifier.Values.rounded;

import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import org.junit.jupiter.api.Test;
import solim.layout.BlurContainer;
import solim.layout.BlurTable;
import solim.modifier.BlurCapability;
import solim.modifier.Prop;
import solim.modifier.Props;
import solim.test.SolimEnv;

public class BlurModifierTest extends SolimEnv {

    @Test
    void testBlurCapabilityHierarchy() {
        BlurContainer container = new BlurContainer();
        assertTrue(container instanceof BlurCapability);
        container.backdropBlur(10f);
        container.contentBlur(4f);
        assertEquals(10f, container.backdropBlur(), 0.001f);
        assertEquals(4f, container.contentBlur(), 0.001f);
    }

    @Test
    void testBackdropBlurPropApplication() {
        BlurContainer container = new BlurContainer();
        Props<BlurContainer> props = new Props<>(container, new Prop[] {
                backdropBlur(12f)
        });
        props.applyAll();

        assertTrue(container.table().getBackground() instanceof BlurDrawable);
        BlurDrawable bd = (BlurDrawable) container.table().getBackground();
        assertEquals(12f, bd.getBlurRadius(), 0.001f);
    }

    @Test
    void testContentBlurPropApplication() {
        BlurContainer container = new BlurContainer();
        Props<BlurContainer> props = new Props<>(container, new Prop[] {
                contentBlur(6f)
        });
        props.applyAll();

        assertTrue(container.table() instanceof BlurTable);
        BlurTable bt = (BlurTable) container.table();
        assertEquals(6f, bt.contentBlurRadius(), 0.001f);
    }

    @Test
    void testBackdropBlurAndRoundedChaining() {
        BlurContainer container = new BlurContainer();
        Props<BlurContainer> props = new Props<>(container, new Prop[] {
                rounded(8, Color.black),
                backdropBlur(15f)
        });
        props.applyAll();

        assertTrue(container.table().getBackground() instanceof RoundedDrawable);
        RoundedDrawable rd = (RoundedDrawable) container.table().getBackground();
        assertNotNull(rd.getBaseDrawable());
        assertTrue(rd.getBaseDrawable() instanceof BlurDrawable);

        BlurDrawable bd = (BlurDrawable) rd.getBaseDrawable();
        assertEquals(15f, bd.getBlurRadius(), 0.001f);
    }

    @Test
    void testBlurDrawableLifecycle() {
        Table table = new Table();
        BlurDrawable bd = BlurDrawable.of(table, 8f);
        assertEquals(8f, bd.getBlurRadius(), 0.001f);
        assertEquals(table, bd.getOwner());

        bd.blurRadius(14f);
        assertEquals(14f, bd.getBlurRadius(), 0.001f);

        bd.dispose();
    }
}
