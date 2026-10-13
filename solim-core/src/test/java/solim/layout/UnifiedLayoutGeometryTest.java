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

        root.add(topLeft).size(40f).expand().top().left();
        root.add(topRight).size(40f).expand().top().right();
        root.row();
        root.add(bottomLeft).size(40f).expand().bottom().left();
        root.add(bottomRight).size(40f).expand().bottom().right();

        root.validate();
        root.layout();

        assertEquals(0f, topLeft.x, 0.01f);
        assertEquals(160f, topLeft.y, 0.01f);

        assertEquals(160f, topRight.x, 0.01f);
        assertEquals(160f, topRight.y, 0.01f);

        assertEquals(0f, bottomLeft.x, 0.01f);
        assertEquals(0f, bottomLeft.y, 0.01f);

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

        assertEquals(70f, topCenter.x, 0.01f);
        assertEquals(160f, topCenter.y, 0.01f);
    }

    @Test
    void testCombinedInsetsPaddingMarginAndGapGeometry() {

        Table container = new Table();
        container.setSize(200f, 200f);
        container.margin(10f);

        Button btn1 = new Button().height(30f).width(100f).marginTop(4f);
        Button btn2 = new Button().height(30f).width(100f).marginTop(4f);

        Column col = new Column().gap(8f);
        col.children(() -> {
            col.table().add(btn1.element()).size(100f, 30f);
            col.table().row();
            col.table().add(btn2.element()).size(100f, 30f);
        });
        container.add(col.table()).grow();

        container.validate();
        container.layout();

        assertEquals(10f, col.table().x, 0.01f);
        assertEquals(10f, col.table().y, 0.01f);
        assertEquals(180f, col.table().getWidth(), 0.01f);
        assertEquals(180f, col.table().getHeight(), 0.01f);

        assertEquals(4f, CellAccess.padTop(col.table().getCell(btn1.element())), 0.01f);

        assertEquals(12f, CellAccess.padTop(col.table().getCell(btn2.element())), 0.01f);

        assertEquals(100f, btn1.element().getWidth(), 0.01f);
        assertEquals(30f, btn1.element().getHeight(), 0.01f);
        assertEquals(100f, btn2.element().getWidth(), 0.01f);
        assertEquals(30f, btn2.element().getHeight(), 0.01f);

        assertEquals(42f, btn1.element().y - btn2.element().y, 0.01f);
    }
}
