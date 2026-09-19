package solim.layout;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.core.BaseComponent;
import solim.core.Ui;
import solim.runtime.ComponentContext;
import solim.runtime.ParentStack;

class ColumnGrowExplorationTest {

    @BeforeAll
    static void initArc() {
        if (Core.app == null) {
            Core.app = new MockApplication();
        }
        if (Core.graphics == null) {
            Core.graphics = new MockGraphics();
        }
    }

    @AfterEach
    void tearDown() {
        ParentStack.clear();
        ComponentContext.clear();
    }

    static class ChannelListProbe extends BaseComponent {
        Column col;

        @Override
        protected Element build() {
            col = Ui.column().name("channel-list").grow().gap(4f).padding(8f).children(() -> {
                new Element();
            });
            return col.element();
        }
    }

    @Test
    void directColumnWithGrowInsideRow() {
        final Column[] colHolder = new Column[1];
        Row row = Ui.row().children(() -> {
            colHolder[0] = Ui.column().name("channel-list").grow().gap(4f).padding(8f).children(() -> {
                new Element();
            });
        });

        Cell<?> cell = row.table().getCell(colHolder[0].element());
        assertNotNull(cell, "Cell must exist for column in row");
        assertEquals(1, CellAccess.expandX(cell), "Cell should have expandX = 1 from grow()");
        assertEquals(1, CellAccess.expandY(cell), "Cell should have expandY = 1 from grow()");
        assertEquals(1f, CellAccess.fillX(cell), 0.01f, "Cell should have fillX = 1 from grow()");
        assertEquals(1f, CellAccess.fillY(cell), 0.01f, "Cell should have fillY = 1 from grow()");
    }

    @Test
    void baseComponentReturningColumnWithGrowInsideRow() {
        ChannelListProbe probe = new ChannelListProbe();
        Row row = Ui.row().children(() -> {
            ParentStack.registerPendingComponent(probe, ParentStack.current());
        });

        Cell<?> cell = row.table().getCell(probe.element());
        assertNotNull(cell, "Cell must exist for BaseComponent element in row");
        assertEquals(1, CellAccess.expandX(cell), "Cell should have expandX = 1 from column().grow()");
        assertEquals(1, CellAccess.expandY(cell), "Cell should have expandY = 1 from column().grow()");
        assertEquals(1f, CellAccess.fillX(cell), 0.01f, "Cell should have fillX = 1 from column().grow()");
        assertEquals(1f, CellAccess.fillY(cell), 0.01f, "Cell should have fillY = 1 from column().grow()");
    }

    @Test
    void directColumnGrowAfterChildren() {
        final Column[] colHolder = new Column[1];
        Row row = Ui.row().children(() -> {
            colHolder[0] = Ui.column().name("channel-list").gap(4f).padding(8f).children(() -> {
                new Element();
            }).grow();
        });

        Cell<?> cell = row.table().getCell(colHolder[0].element());
        assertNotNull(cell, "Cell must exist for column in row");
        assertEquals(1, CellAccess.expandX(cell), "Cell should have expandX = 1 from grow()");
        assertEquals(1, CellAccess.expandY(cell), "Cell should have expandY = 1 from grow()");
    }
}
