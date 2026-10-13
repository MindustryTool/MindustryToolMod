package solim.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import solim.core.Component;
import solim.layout.LayoutTestFixtures.ComponentAdapter;
import solim.modifier.PendingCellConfig;
import solim.test.SolimEnv;

public class UnifiedComponentAlignmentTest extends SolimEnv {

    static List<ComponentAdapter> alignableComponents() {
        return LayoutTestFixtures.allAlignableComponents();
    }

    private static void invokeAlignment(Component comp, String methodName) {
        try {
            Method m = comp.getClass().getMethod(methodName);
            m.invoke(comp);
        } catch (Exception e) {
            throw new RuntimeException("Failed invoking " + methodName + " on " + comp.getClass().getSimpleName(), e);
        }
    }

    @ParameterizedTest(name = "{0} - individual alignment directions")
    @MethodSource("alignableComponents")
    void testIndividualAlignmentDirections(ComponentAdapter adapter) {

        Component compTop = adapter.create();
        invokeAlignment(compTop, "top");
        PendingCellConfig cfgTop = ComponentAdapter.getCellConfig(compTop);
        assertNotNull(cfgTop);
        assertNotNull(cfgTop.align);
        assertTrue((cfgTop.align & Align.top) != 0, adapter.name + ".top() must include Align.top");
        assertFalse((cfgTop.align & Align.bottom) != 0, adapter.name + ".top() must not include Align.bottom");

        Component compBottom = adapter.create();
        invokeAlignment(compBottom, "bottom");
        PendingCellConfig cfgBottom = ComponentAdapter.getCellConfig(compBottom);
        assertNotNull(cfgBottom);
        assertNotNull(cfgBottom.align);
        assertTrue((cfgBottom.align & Align.bottom) != 0, adapter.name + ".bottom() must include Align.bottom");
        assertFalse((cfgBottom.align & Align.top) != 0, adapter.name + ".bottom() must not include Align.top");

        Component compLeft = adapter.create();
        invokeAlignment(compLeft, "left");
        PendingCellConfig cfgLeft = ComponentAdapter.getCellConfig(compLeft);
        assertNotNull(cfgLeft);
        assertNotNull(cfgLeft.align);
        assertTrue((cfgLeft.align & Align.left) != 0, adapter.name + ".left() must include Align.left");
        assertFalse((cfgLeft.align & Align.right) != 0, adapter.name + ".left() must not include Align.right");

        Component compRight = adapter.create();
        invokeAlignment(compRight, "right");
        PendingCellConfig cfgRight = ComponentAdapter.getCellConfig(compRight);
        assertNotNull(cfgRight);
        assertNotNull(cfgRight.align);
        assertTrue((cfgRight.align & Align.right) != 0, adapter.name + ".right() must include Align.right");
        assertFalse((cfgRight.align & Align.left) != 0, adapter.name + ".right() must not include Align.left");

        Component compCenter = adapter.create();
        invokeAlignment(compCenter, "center");
        PendingCellConfig cfgCenter = ComponentAdapter.getCellConfig(compCenter);
        assertNotNull(cfgCenter);
        assertEquals(Align.center, cfgCenter.align.intValue(), adapter.name + ".center() must equal Align.center");
    }

    @ParameterizedTest(name = "{0} - parent cell attachment propagation")
    @MethodSource("alignableComponents")
    void testParentCellAttachmentPropagation(ComponentAdapter adapter) {
        Table parent = new Table();
        Component comp = adapter.create();
        Cell<?> cell = parent.add(comp.element());

        invokeAlignment(comp, "top");
        assertEquals(Align.top, CellAccess.align(cell) & (Align.top | Align.bottom));

        invokeAlignment(comp, "bottom");
        assertEquals(Align.bottom, CellAccess.align(cell) & (Align.top | Align.bottom));

        invokeAlignment(comp, "left");
        assertEquals(Align.left, CellAccess.align(cell) & (Align.left | Align.right));

        invokeAlignment(comp, "right");
        assertEquals(Align.right, CellAccess.align(cell) & (Align.left | Align.right));

        invokeAlignment(comp, "center");
        assertEquals(Align.center, CellAccess.align(cell));
    }

    @ParameterizedTest(name = "{0} - 4 corner alignment compositions")
    @MethodSource("alignableComponents")
    void testCornerAlignmentCompositions(ComponentAdapter adapter) {

        Component c1 = adapter.create();
        invokeAlignment(c1, "top");
        invokeAlignment(c1, "left");
        PendingCellConfig cfg1 = ComponentAdapter.getCellConfig(c1);
        assertNotNull(cfg1);
        assertEquals(Align.top | Align.left, cfg1.align.intValue());

        Component c1b = adapter.create();
        invokeAlignment(c1b, "left");
        invokeAlignment(c1b, "top");
        PendingCellConfig cfg1b = ComponentAdapter.getCellConfig(c1b);
        assertNotNull(cfg1b);
        assertEquals(Align.top | Align.left, cfg1b.align.intValue());

        Component c2 = adapter.create();
        invokeAlignment(c2, "top");
        invokeAlignment(c2, "right");
        PendingCellConfig cfg2 = ComponentAdapter.getCellConfig(c2);
        assertNotNull(cfg2);
        assertEquals(Align.top | Align.right, cfg2.align.intValue());

        Component c3 = adapter.create();
        invokeAlignment(c3, "bottom");
        invokeAlignment(c3, "left");
        PendingCellConfig cfg3 = ComponentAdapter.getCellConfig(c3);
        assertNotNull(cfg3);
        assertEquals(Align.bottom | Align.left, cfg3.align.intValue());

        Component c4 = adapter.create();
        invokeAlignment(c4, "bottom");
        invokeAlignment(c4, "right");
        PendingCellConfig cfg4 = ComponentAdapter.getCellConfig(c4);
        assertNotNull(cfg4);
        assertEquals(Align.bottom | Align.right, cfg4.align.intValue());
    }

    @ParameterizedTest(name = "{0} - center compositions")
    @MethodSource("alignableComponents")
    void testCenterCompositions(ComponentAdapter adapter) {

        Component c1 = adapter.create();
        invokeAlignment(c1, "top");
        invokeAlignment(c1, "center");
        PendingCellConfig cfg1 = ComponentAdapter.getCellConfig(c1);
        assertNotNull(cfg1);
        assertEquals(Align.center, cfg1.align.intValue());

        Component c2 = adapter.create();
        invokeAlignment(c2, "center");
        invokeAlignment(c2, "top");
        PendingCellConfig cfg2 = ComponentAdapter.getCellConfig(c2);
        assertNotNull(cfg2);
        assertTrue((cfg2.align & Align.top) != 0);

        Component c3 = adapter.create();
        invokeAlignment(c3, "center");
        invokeAlignment(c3, "bottom");
        PendingCellConfig cfg3 = ComponentAdapter.getCellConfig(c3);
        assertNotNull(cfg3);
        assertTrue((cfg3.align & Align.bottom) != 0);

        Component c4 = adapter.create();
        invokeAlignment(c4, "center");
        invokeAlignment(c4, "left");
        PendingCellConfig cfg4 = ComponentAdapter.getCellConfig(c4);
        assertNotNull(cfg4);
        assertTrue((cfg4.align & Align.left) != 0);

        Component c5 = adapter.create();
        invokeAlignment(c5, "center");
        invokeAlignment(c5, "right");
        PendingCellConfig cfg5 = ComponentAdapter.getCellConfig(c5);
        assertNotNull(cfg5);
        assertTrue((cfg5.align & Align.right) != 0);
    }

    @ParameterizedTest(name = "{0} - directional override and replacement")
    @MethodSource("alignableComponents")
    void testDirectionalOverrideAndReplacement(ComponentAdapter adapter) {
        Component comp = adapter.create();
        PendingCellConfig cfg = ComponentAdapter.getCellConfig(comp);
        assertNotNull(cfg);

        invokeAlignment(comp, "top");
        assertTrue((cfg.align & Align.top) != 0);
        invokeAlignment(comp, "bottom");
        assertFalse((cfg.align & Align.top) != 0, "bottom() after top() must clear top");
        assertTrue((cfg.align & Align.bottom) != 0, "bottom() after top() must set bottom");

        invokeAlignment(comp, "left");
        assertTrue((cfg.align & Align.left) != 0);
        invokeAlignment(comp, "right");
        assertFalse((cfg.align & Align.left) != 0, "right() after left() must clear left");
        assertTrue((cfg.align & Align.right) != 0, "right() after left() must set right");
    }

    @Test
    void testContainerChildDefaultsPropagation() {
        for (ComponentAdapter adapter : LayoutTestFixtures.containers()) {
            Component comp = adapter.create();
            invokeAlignment(comp, "bottom");
            invokeAlignment(comp, "right");

            if (comp instanceof Column) {
                Column col = (Column) comp;
                col.children(() -> {
                    Element child = new Element();
                    col.table().add(child);
                });
                Cell<?> cell = col.table().getCells().first();
                assertNotNull(cell);
                assertEquals(Align.bottom | Align.right, CellAccess.align(cell));
            } else if (comp instanceof Row) {
                Row row = (Row) comp;
                row.children(() -> {
                    Element child = new Element();
                    row.table().add(child);
                });
                Cell<?> cell = row.table().getCells().first();
                assertNotNull(cell);
                assertEquals(Align.bottom | Align.right, CellAccess.align(cell));
            } else if (comp instanceof Card) {
                Card card = (Card) comp;
                card.children(() -> {
                    Element child = new Element();
                    card.table().add(child);
                });
                Cell<?> cell = card.table().getCells().first();
                assertNotNull(cell);
                assertEquals(Align.bottom | Align.right, CellAccess.align(cell));
            }
        }
    }
}
