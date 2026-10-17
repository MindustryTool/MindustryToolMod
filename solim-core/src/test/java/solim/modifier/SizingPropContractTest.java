package solim.modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.width;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Scl;
import solim.layout.Row;
import solim.test.SolimCoreEnv;

class SizingPropContractTest extends SolimCoreEnv {

    private static final float SCALE = 0.7f;

    @AfterEach
    void restoreScale() {
        Scl.setProduct(1f);
    }

    @Test
    void standaloneWidthRendersAtSceneScale() {
        Scl.setProduct(SCALE);

        Row row = new Row().props(width(232f));
        Element element = row.element();

        assertEquals(Scl.scl(232f), element.getWidth(), 0.01f,
                "A cell-less host must render width(232) as Scl.scl(232) scene pixels");
        assertNotEquals(232f, element.getWidth(),
                "The rendered width is scaled scene pixels, not the design-unit value");
        row.dispose();
    }

    @Test
    void widthAloneMatchesPrefWidthCellAndRenderedSize() {
        Row parent = new Row();
        Row byWidth = new Row().props(width(70f));
        Row byPrefWidth = new Row().props(prefWidth(70f));

        parent.children(() -> {
            byWidth.children(() -> {
            });
            byPrefWidth.children(() -> {
            });
        });

        Cell<?> widthCell = parent.table().getCell(byWidth.table());
        Cell<?> prefCell = parent.table().getCell(byPrefWidth.table());
        assertNotNull(widthCell);
        assertNotNull(prefCell);

        assertEquals(Scl.scl(70f), CellAccess.minWidth(widthCell), 0.01f);
        assertEquals(Scl.scl(70f), CellAccess.maxWidth(widthCell), 0.01f);
        assertEquals(CellAccess.minWidth(prefCell), CellAccess.minWidth(widthCell), 0.01f);
        assertEquals(CellAccess.maxWidth(prefCell), CellAccess.maxWidth(widthCell), 0.01f);

        parent.table().setSize(300f, 100f);
        parent.table().validate();
        parent.table().layout();

        assertEquals(Scl.scl(70f), byWidth.element().getWidth(), 0.5f);
        assertEquals(byPrefWidth.element().getWidth(), byWidth.element().getWidth(), 0.5f);

        byWidth.dispose();
        byPrefWidth.dispose();
        parent.dispose();
    }

    @Test
    void growKeepsTheWidthPinnedByPrefWidth() {
        Row parent = new Row();
        Row fixed = new Row().props(prefWidth(70f), growX());

        parent.children(() -> {
            fixed.children(() -> {
            });
        });

        Cell<?> cell = parent.table().getCell(fixed.table());
        assertNotNull(cell);
        assertEquals(Scl.scl(70f), CellAccess.minWidth(cell), 0.01f,
                "growX() must not clear the minimum that prefWidth(x) pinned");
        assertEquals(Scl.scl(70f), CellAccess.maxWidth(cell), 0.01f);
        assertEquals(Scl.scl(70f), parent.table().getPrefWidth(), 0.5f,
                "A parent packing to pref must adopt the pinned width, not the empty child's content width");

        fixed.dispose();
        parent.dispose();
    }
}
