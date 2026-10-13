package solim.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import solim.core.Component;
import solim.display.Text;
import solim.input.Button;
import solim.layout.LayoutTestFixtures.ComponentAdapter;
import solim.modifier.CellConfig;
import solim.modifier.TableConfig;
import solim.test.SolimEnv;

public class UnifiedSpacingCompositionTest extends SolimEnv {

    static List<ComponentAdapter> containers() {
        return LayoutTestFixtures.containers();
    }

    @ParameterizedTest(name = "{0} - padding inner insets isolation")
    @MethodSource("containers")
    void testPaddingInnerInsetsIsolation(ComponentAdapter adapter) {
        Component comp = adapter.create();
        if (!(comp instanceof TableConfig))
            return;
        TableConfig<?> tc = (TableConfig<?>) comp;

        tc.padding(12f, 15f, 18f, 21f);

        Table t = tc.table();
        assertNotNull(t);
        assertEquals(12f, t.getMarginTop(), 0.01f);
        assertEquals(15f, t.getMarginLeft(), 0.01f);
        assertEquals(18f, t.getMarginBottom(), 0.01f);
        assertEquals(21f, t.getMarginRight(), 0.01f);

        Table parent = new Table();
        Cell<?> cell = parent.add(comp.element());
        assertEquals(0f, CellAccess.padTop(cell), 0.01f);
        assertEquals(0f, CellAccess.padLeft(cell), 0.01f);
        assertEquals(0f, CellAccess.padBottom(cell), 0.01f);
        assertEquals(0f, CellAccess.padRight(cell), 0.01f);
    }

    @Test
    void testMarginOuterPaddingIsolation() {

        Component[] components = new Component[] {
                new Column(),
                new Row(),
                new Card(),
                new Button(),
                new Text("label")
        };

        for (Component comp : components) {
            Table parent = new Table();
            Cell<?> cell = parent.add(comp.element());

            if (comp instanceof CellConfig) {
                ((CellConfig<?>) comp).margin(5f, 10f, 15f, 20f);
            } else if (comp instanceof Text) {
                ((Text) comp).margin(5f, 10f, 15f, 20f);
            }

            assertEquals(5f, CellAccess.padTop(cell), 0.01f, comp.getClass().getSimpleName() + " padTop");
            assertEquals(10f, CellAccess.padLeft(cell), 0.01f, comp.getClass().getSimpleName() + " padLeft");
            assertEquals(15f, CellAccess.padBottom(cell), 0.01f, comp.getClass().getSimpleName() + " padBottom");
            assertEquals(20f, CellAccess.padRight(cell), 0.01f, comp.getClass().getSimpleName() + " padRight");

            if (comp instanceof TableConfig) {
                Table inner = ((TableConfig<?>) comp).table();
                if (inner != null) {
                    assertEquals(0f, inner.getMarginTop(), 0.01f);
                    assertEquals(0f, inner.getMarginLeft(), 0.01f);
                    assertEquals(0f, inner.getMarginBottom(), 0.01f);
                    assertEquals(0f, inner.getMarginRight(), 0.01f);
                }
            }
        }
    }

    @Test
    void testGapAndMarginCoexistenceInColumn() {
        Column col = new Column().gap(12f);
        Button btn1 = new Button().marginTop(4f).marginLeft(6f);
        Button btn2 = new Button().marginTop(4f).marginLeft(6f);

        col.children(() -> {
            col.table().add(btn1.element());
            col.table().row();
            col.table().add(btn2.element());
        });

        Cell<?> cell1 = col.table().getCell(btn1.element());
        Cell<?> cell2 = col.table().getCell(btn2.element());

        assertEquals(4f, CellAccess.padTop(cell1), 0.01f);
        assertEquals(6f, CellAccess.padLeft(cell1), 0.01f);

        assertEquals(16f, CellAccess.padTop(cell2), 0.01f);
        assertEquals(6f, CellAccess.padLeft(cell2), 0.01f);
    }

    @Test
    void testGapAndMarginCoexistenceInRow() {
        Row row = new Row().gap(10f);
        Button btn1 = new Button().marginLeft(3f).marginTop(7f);
        Button btn2 = new Button().marginLeft(3f).marginTop(7f);

        row.children(() -> {
            row.table().add(btn1.element());
            row.table().add(btn2.element());
        });

        Cell<?> cell1 = row.table().getCell(btn1.element());
        Cell<?> cell2 = row.table().getCell(btn2.element());

        assertEquals(3f, CellAccess.padLeft(cell1), 0.01f);
        assertEquals(7f, CellAccess.padTop(cell1), 0.01f);

        assertEquals(13f, CellAccess.padLeft(cell2), 0.01f);
        assertEquals(7f, CellAccess.padTop(cell2), 0.01f);
    }

    @Test
    void testDynamicVisibilityTogglingShiftsGapOffset() {
        Column col = new Column().gap(14f);
        Button btn1 = new Button().marginTop(5f);
        Button btn2 = new Button().marginTop(5f);

        col.children(() -> {
            col.table().add(btn1.element());
            col.table().row();
            col.table().add(btn2.element());
        });

        Cell<?> cell1 = col.table().getCell(btn1.element());
        Cell<?> cell2 = col.table().getCell(btn2.element());

        assertEquals(5f, CellAccess.padTop(cell1), 0.01f);
        assertEquals(19f, CellAccess.padTop(cell2), 0.01f);

        btn1.element().visible = false;
        col.respace();

        assertEquals(0f, CellAccess.padTop(cell1), 0.01f);

        assertEquals(5f, CellAccess.padTop(cell2), 0.01f);

        btn1.element().visible = true;
        col.respace();

        assertEquals(5f, CellAccess.padTop(cell1), 0.01f);
        assertEquals(19f, CellAccess.padTop(cell2), 0.01f);
    }
}
