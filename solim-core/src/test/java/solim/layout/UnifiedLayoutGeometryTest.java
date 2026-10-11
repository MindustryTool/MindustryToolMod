package solim.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.scene.Element;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import solim.input.Button;
import solim.test.ArcTestEnv;
import solim.test.SolimEnv;

public class UnifiedLayoutGeometryTest extends SolimEnv {

    @BeforeEach
    public void initScene() {
        ArcTestEnv.newScene();
    }

    @Test
    void testCenterCoordinatesInsideFixedContainer() {
        Table root = new Table();
        root.setSize(200f, 200f);

        // Child element 50x50 centered
        Element child = new Element();
        child.setSize(50f, 50f);

        root.add(child).size(50f).center();
        root.validate();
        root.layout();

        assertEquals(50f, child.getWidth(), 0.01f);
        assertEquals(50f, child.getHeight(), 0.01f);
        assertEquals(75f, child.x, 0.01f, "Centered X should be (200 - 50) / 2 = 75");
        assertEquals(75f, child.y, 0.01f, "Centered Y should be (200 - 50) / 2 = 75");
    }

    @Test
    void testCornerCoordinatesInsideFixedContainer() {
        Table root = new Table();
        root.setSize(200f, 200f);

        Element topLeft = new Element();
        Element topRight = new Element();
        Element bottomLeft = new Element();
        Element bottomRight = new Element();

        // 4 cells in a 2x2 layout with expand() so cells span the entire 200x200 table
        root.add(topLeft).size(40f).expand().top().left();
        root.add(topRight).size(40f).expand().top().right();
        root.row();
        root.add(bottomLeft).size(40f).expand().bottom().left();
        root.add(bottomRight).size(40f).expand().bottom().right();

        root.validate();
        root.layout();

        // topLeft sits at x=0, and top of row 0 (y = 200 - 40 = 160)
        assertEquals(0f, topLeft.x, 0.01f);
        assertEquals(160f, topLeft.y, 0.01f);

        // topRight sits at right cell border (x = 200 - 40 = 160) and top (y = 160)
        assertEquals(160f, topRight.x, 0.01f);
        assertEquals(160f, topRight.y, 0.01f);

        // bottomLeft sits at bottom row (y=0) and left (x=0)
        assertEquals(0f, bottomLeft.x, 0.01f);
        assertEquals(0f, bottomLeft.y, 0.01f);

        // bottomRight sits at bottom row (y=0) and right (x=160)
        assertEquals(160f, bottomRight.x, 0.01f);
        assertEquals(0f, bottomRight.y, 0.01f);
    }

    @Test
    void testEdgeCenteredCompositions() {
        Table root = new Table();
        root.setSize(200f, 200f);
        root.top();

        Element topCenter = new Element();
        root.add(topCenter).size(60f, 40f).top().center();
        root.validate();
        root.layout();

        // x is centered: (200 - 60) / 2 = 70
        // y is top: 200 - 40 = 160
        assertEquals(70f, topCenter.x, 0.01f);
        assertEquals(160f, topCenter.y, 0.01f);
    }

    @Test
    void testCombinedInsetsPaddingMarginAndGapGeometry() {
        // Outer table has 10px inner padding on all sides
        Table container = new Table();
        container.setSize(200f, 200f);
        container.margin(10f); // 10px padding on all sides

        Button btn1 = new Button().height(30f).width(100f).marginTop(4f);
        Button btn2 = new Button().height(30f).width(100f).marginTop(4f);

        // Column inside with gap(8f)
        Column col = new Column().gap(8f);
        col.children(() -> {
            col.table().add(btn1.element()).size(100f, 30f);
            col.table().row();
            col.table().add(btn2.element()).size(100f, 30f);
        });
        container.add(col.table()).grow();

        container.validate();
        container.layout();

        // container available area inside padding: x from 10 to 190, y from 10 to 190
        assertEquals(10f, col.table().x, 0.01f);
        assertEquals(10f, col.table().y, 0.01f);
        assertEquals(180f, col.table().getWidth(), 0.01f);
        assertEquals(180f, col.table().getHeight(), 0.01f);

        // Check Cell padding values computed by GapContainer:
        // btn1 has padTop = 4 (marginTop)
        assertEquals(4f, CellAccess.padTop(col.table().getCell(btn1.element())), 0.01f);
        // btn2 has padTop = 4 + 8 = 12 (marginTop + gap)
        assertEquals(12f, CellAccess.padTop(col.table().getCell(btn2.element())), 0.01f);

        // Check button heights and widths
        assertEquals(100f, btn1.element().getWidth(), 0.01f);
        assertEquals(30f, btn1.element().getHeight(), 0.01f);
        assertEquals(100f, btn2.element().getWidth(), 0.01f);
        assertEquals(30f, btn2.element().getHeight(), 0.01f);

        // Verify button relative vertical spacing: btn1 is above btn2 by exactly (height 30 + gap 8 + marginTop 4) = 42
        assertEquals(42f, btn1.element().y - btn2.element().y, 0.01f);
    }
}
