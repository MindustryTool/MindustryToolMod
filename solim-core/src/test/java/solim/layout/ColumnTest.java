package solim.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.util.Align;
import solim.reactive.Signal;
import solim.runtime.AttachmentStack;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class ColumnTest extends SolimEnv {


    @Test
    void createsTableWithDefaultName() {
        Column col = new Column();
        assertEquals("solim-column-table", col.table().name);
    }

    @Test
    void preservesChildOrder() {
        Column col = new Column();
        Element first = new Element();
        Element second = new Element();
        Element third = new Element();

        col.add(first).row();
        col.add(second).row();
        col.add(third);

        assertSame(first, col.table().getChildren().get(0));
        assertSame(second, col.table().getChildren().get(1));
        assertSame(third, col.table().getChildren().get(2));
    }

    @Test
    void gapAppliesDirectionalSpacingToExistingCells() {
        Column col = new Column();
        Element a = new Element();
        Element b = new Element();
        col.add(a);
        col.add(b);

        col.gap(16f);

        assertEquals(0f, CellAccess.padTop(col.table().getCell(a)), 0.01f);
        assertEquals(16f, CellAccess.padTop(col.table().getCell(b)), 0.01f);
        assertEquals(0f, CellAccess.padLeft(col.table().getCell(b)), 0.01f);
        assertEquals(2, col.table().getChildren().size);
    }

    @Test
    void paddingPreservesChildrenAndReturnsSelf() {
        Column col = new Column();
        Element child = new Element();
        col.add(child);

        assertSame(col, col.padding(24f));
        assertSame(col, col.padding(1f, 2f, 3f, 4f));
        assertEquals(1, col.table().getChildren().size);
        assertSame(child, col.table().getChildren().get(0));
    }

    @Test
    void visibleModifierChangesTableVisibility() {
        Column col = new Column();

        col.visible(false);
        assertFalse(col.table().visible);

        col.visible(true);
        assertTrue(col.table().visible);
    }

    @Test
    void reactiveVisibleUpdatesTableVisibility() {
        Signal<Boolean> vis = Signal.of(true);
        Column col = new Column();
        col.visible(vis);

        assertTrue(col.table().visible);

        vis.set(false);
        SignalDispatcher.flush();
        assertFalse(col.table().visible);

        vis.set(true);
        SignalDispatcher.flush();
        assertTrue(col.table().visible);
    }

    @Test
    void positionSetsTableCoordinates() {
        Column col = new Column();

        col.x(10f);
        assertEquals(10f, col.table().x, 0.01f);

        col.y(20f);
        assertEquals(20f, col.table().y, 0.01f);

        col.position(30f, 40f);
        assertEquals(30f, col.table().x, 0.01f);
        assertEquals(40f, col.table().y, 0.01f);
    }

    @Test
    void nameModifierUpdatesTableName() {
        Column col = new Column();
        col.name("my-column");
        assertEquals("my-column", col.table().name);
    }

    @Test
    void childrenRunnableAddsElements() {
        Column col = new Column();
        Element child = new Element();

        col.children(() -> {
            AttachmentStack.add(child);
        });

        assertEquals(1, col.table().getChildren().size);
        assertSame(child, col.table().getChildren().get(0));
    }

    @Test
    void fluentApiReturnsSameColumn() {
        Column col = new Column();

        assertSame(col, col.gap(8f));
        assertSame(col, col.padding(4f));
        assertSame(col, col.name("test"));
        assertSame(col, col.visible(true));
        assertSame(col, col.x(0f));
        assertSame(col, col.y(0f));
    }

    @Test
    void tableIsSameAsElement() {
        Column col = new Column();
        assertSame(col.table(), col.element());
    }

    @Test
    void cellConfigReturnsNonNull() {
        Column col = new Column();
        assertNotNull(col.cellConfig());
    }

    @Test
    void columnTopLeftAlignsChildren() {
        Column col = new Column();
        col.top().left();
        Element e1 = new Element() {
            @Override
            public float getPrefWidth() {
                return 50f;
            }

            @Override
            public float getPrefHeight() {
                return 30f;
            }
        };
        Element e2 = new Element() {
            @Override
            public float getPrefWidth() {
                return 100f;
            }

            @Override
            public float getPrefHeight() {
                return 60f;
            }
        };
        col.children(() -> {
            AttachmentStack.add(e1);
            AttachmentStack.add(e2);
        });
        col.table().setSize(200f, 300f);
        col.table().layout();

        Cell<?> c1 = col.table().getCell(e1);
        Cell<?> c2 = col.table().getCell(e2);

        assertEquals(Align.top | Align.left, CellAccess.align(c1) & (Align.top | Align.left));
        assertEquals(Align.top | Align.left, CellAccess.align(c2) & (Align.top | Align.left));

        // Left aligned: both start at x = 0
        assertEquals(0f, e1.x, 0.01f, "Child 1 must be at left edge");
        assertEquals(0f, e2.x, 0.01f, "Child 2 must be at left edge");
    }

    @Test
    void columnBottomRightAlignsChildren() {
        Column col = new Column();
        col.bottom().right();
        Element e1 = new Element() {
            @Override
            public float getPrefWidth() {
                return 50f;
            }

            @Override
            public float getPrefHeight() {
                return 30f;
            }
        };
        Element e2 = new Element() {
            @Override
            public float getPrefWidth() {
                return 100f;
            }

            @Override
            public float getPrefHeight() {
                return 60f;
            }
        };
        col.children(() -> {
            AttachmentStack.add(e1);
            AttachmentStack.add(e2);
        });
        col.table().setSize(200f, 300f);
        col.table().layout();

        // Right aligned: both end at x = 200
        assertEquals(200f, e1.x + e1.getWidth(), 0.01f, "Child 1 must be flush with right edge");
        assertEquals(200f, e2.x + e2.getWidth(), 0.01f, "Child 2 must be flush with right edge");
    }

    @Test
    void columnAlignmentAfterChildrenUpdatesExistingCells() {
        Column col = new Column();
        Element e1 = new Element() {
            @Override
            public float getPrefWidth() {
                return 50f;
            }

            @Override
            public float getPrefHeight() {
                return 30f;
            }
        };
        col.children(() -> AttachmentStack.add(e1));

        col.right();
        col.table().setSize(200f, 300f);
        col.table().layout();

        Cell<?> c1 = col.table().getCell(e1);
        assertEquals(Align.right, CellAccess.align(c1) & Align.right);
        assertEquals(200f, e1.x + e1.getWidth(), 0.01f);
    }

    @Test
    void bareColumnDefaultsToTopLeft() {
        Column col = new Column();
        Element e1 = new Element() {
            @Override
            public float getPrefWidth() {
                return 50f;
            }

            @Override
            public float getPrefHeight() {
                return 30f;
            }
        };
        Element e2 = new Element() {
            @Override
            public float getPrefWidth() {
                return 100f;
            }

            @Override
            public float getPrefHeight() {
                return 60f;
            }
        };
        col.children(() -> {
            AttachmentStack.add(e1);
            AttachmentStack.add(e2);
        });
        col.table().setSize(200f, 300f);
        col.table().layout();

        Cell<?> c1 = col.table().getCell(e1);
        Cell<?> c2 = col.table().getCell(e2);

        assertEquals(Align.top | Align.left, CellAccess.align(c1) & (Align.top | Align.left));
        assertEquals(Align.top | Align.left, CellAccess.align(c2) & (Align.top | Align.left));
        assertEquals(0f, e1.x, 0.01f, "Child 1 must be at left edge");
        assertEquals(0f, e2.x, 0.01f, "Child 2 must be at left edge");
    }

    @Test
    void explicitCenterOverridesTopLeftDefault() {
        Column col = new Column();
        col.center();
        Element e1 = new Element();
        col.children(() -> {
            AttachmentStack.add(e1);
        });

        Cell<?> c1 = col.table().getCell(e1);
        assertEquals(Align.center, CellAccess.align(c1) & Align.center);
    }
}
