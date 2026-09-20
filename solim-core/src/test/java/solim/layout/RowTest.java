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
import solim.core.SolimToken;
import solim.core.Ui;
import solim.reactive.Signal;
import solim.runtime.ParentStack;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class RowTest extends SolimEnv {


    @Test
    void createsTableWithDefaultName() {
        Row row = new Row();
        assertEquals("solim-row-table", row.table().name);
    }

    @Test
    void preservesChildOrder() {
        Row row = new Row();
        Element first = new Element();
        Element second = new Element();
        Element third = new Element();

        row.add(first);
        row.add(second);
        row.add(third);

        assertSame(first, row.table().getChildren().get(0));
        assertSame(second, row.table().getChildren().get(1));
        assertSame(third, row.table().getChildren().get(2));
    }

    @Test
    void gapSetsCellSpacing() {
        Row row = new Row();
        row.gap(8f);

        Element a = new Element();
        Element b = new Element();
        row.add(a);
        row.add(b);

        assertEquals(2, row.table().getChildren().size);
    }

    @Test
    void gapAppliesDirectionalSpacingToCells() {
        Row row = new Row();
        row.gap(8f);
        Element a = new Element();
        Element b = new Element();
        row.add(a);
        row.add(b);

        assertEquals(0f, CellAccess.padLeft(row.table().getCell(a)), 0.01f);
        assertEquals(8f, CellAccess.padLeft(row.table().getCell(b)), 0.01f);
        assertEquals(0f, CellAccess.padTop(row.table().getCell(b)), 0.01f);
    }

    @Test
    void paddingPreservesChildrenAndReturnsSelf() {
        Row row = new Row();
        Element child = new Element();
        row.add(child);

        assertSame(row, row.padding(12f));
        assertSame(row, row.padding(1f, 2f, 3f, 4f));
        assertEquals(1, row.table().getChildren().size);
        assertSame(child, row.table().getChildren().get(0));
    }

    @Test
    void visibleModifierChangesTableVisibility() {
        Row row = new Row();

        row.visible(false);
        assertFalse(row.table().visible);

        row.visible(true);
        assertTrue(row.table().visible);
    }

    @Test
    void reactiveVisibleUpdatesTableVisibility() {
        Signal<Boolean> vis = Signal.of(true);
        Row row = new Row();
        row.visible(vis);

        assertTrue(row.table().visible);

        vis.set(false);
        SignalDispatcher.flush();
        assertFalse(row.table().visible);

        vis.set(true);
        SignalDispatcher.flush();
        assertTrue(row.table().visible);
    }

    @Test
    void positionSetsTableCoordinates() {
        Row row = new Row();

        row.x(10f);
        assertEquals(10f, row.table().x, 0.01f);

        row.y(20f);
        assertEquals(20f, row.table().y, 0.01f);

        row.position(30f, 40f);
        assertEquals(30f, row.table().x, 0.01f);
        assertEquals(40f, row.table().y, 0.01f);
    }

    @Test
    void nameModifierUpdatesTableName() {
        Row row = new Row();
        row.name("my-row");
        assertEquals("my-row", row.table().name);
    }

    @Test
    void childrenRunnableAddsElements() {
        Row row = new Row();
        Element child = new Element();

        row.children(() -> {
            ParentStack.add(child);
        });

        assertEquals(1, row.table().getChildren().size);
        assertSame(child, row.table().getChildren().get(0));
    }

    @Test
    void tableIsSameAsElement() {
        Row row = new Row();
        assertSame(row.table(), row.element());
    }

    @Test
    void cellConfigReturnsNonNull() {
        Row row = new Row();
        assertNotNull(row.cellConfig());
    }

    @Test
    void childRowWithWidthActsAsSpacerInParentRow() {
        Row parent = new Row();
        Row child = new Row();
        parent.children(() -> {
            child.width(40f).minWidth(40f).children(() -> {
            });
        });

        assertEquals(1, parent.table().getChildren().size);
        assertSame(child.table(), parent.table().getChildren().get(0));
        Cell<?> cell = parent.table().getCell(child.table());
        assertNotNull(cell);
        assertEquals(40f, CellAccess.minWidth(cell), 0.01f);
    }

    @Test
    void rowTopLeftAlignsChildrenToTopAndLeft() {
        Row row = new Row();
        row.top().left();
        Element e1 = new Element() {
            @Override
            public float getPrefWidth() {
                return 50f;
            }

            @Override
            public float getPrefHeight() {
                return 50f;
            }
        };
        Element e2 = new Element() {
            @Override
            public float getPrefWidth() {
                return 100f;
            }

            @Override
            public float getPrefHeight() {
                return 100f;
            }
        };
        row.children(() -> {
            ParentStack.add(e1);
            ParentStack.add(e2);
        });
        row.table().setSize(300f, 200f);
        row.table().layout();

        Cell<?> c1 = row.table().getCell(e1);
        Cell<?> c2 = row.table().getCell(e2);

        // Both cell alignments must include Align.top and Align.left
        assertEquals(Align.top | Align.left, CellAccess.align(c1) & (Align.top | Align.left));
        assertEquals(Align.top | Align.left, CellAccess.align(c2) & (Align.top | Align.left));

        // Both children must be flush with the top edge of the row (y + height == 200)
        assertEquals(200f, e1.y + e1.getHeight(), 0.01f, "Child 1 must be aligned to top of row");
        assertEquals(200f, e2.y + e2.getHeight(), 0.01f, "Child 2 must be aligned to top of row");

        // Left alignment: e1 at left (0), e2 immediately following (50)
        assertEquals(0f, e1.x, 0.01f, "Child 1 must be at left edge");
        assertEquals(50f, e2.x, 0.01f, "Child 2 must follow Child 1");
    }

    @Test
    void rowBottomRightAlignsChildrenToBottomAndRight() {
        Row row = new Row();
        row.bottom().right();
        Element e1 = new Element() {
            @Override
            public float getPrefWidth() {
                return 50f;
            }

            @Override
            public float getPrefHeight() {
                return 50f;
            }
        };
        Element e2 = new Element() {
            @Override
            public float getPrefWidth() {
                return 100f;
            }

            @Override
            public float getPrefHeight() {
                return 100f;
            }
        };
        row.children(() -> {
            ParentStack.add(e1);
            ParentStack.add(e2);
        });
        row.table().setSize(300f, 200f);
        row.table().layout();

        // Both children must be flush with the bottom edge of the row (y == 0)
        assertEquals(0f, e1.y, 0.01f, "Child 1 must be aligned to bottom");
        assertEquals(0f, e2.y, 0.01f, "Child 2 must be aligned to bottom");

        // Right alignment: e2 ends at 300, e1 before it
        assertEquals(300f, e2.x + e2.getWidth(), 0.01f, "Child 2 must be at right edge");
        assertEquals(200f, e1.x + e1.getWidth(), 0.01f, "Child 1 must precede Child 2");
    }

    @Test
    void rowAlignmentAfterChildrenUpdatesExistingCells() {
        Row row = new Row();
        Element e1 = new Element() {
            @Override
            public float getPrefWidth() {
                return 50f;
            }

            @Override
            public float getPrefHeight() {
                return 50f;
            }
        };
        row.children(() -> ParentStack.add(e1));

        // Call top() after children are added
        row.top();
        row.table().setSize(300f, 200f);
        row.table().layout();

        Cell<?> c1 = row.table().getCell(e1);
        assertEquals(Align.top, CellAccess.align(c1) & Align.top);
        assertEquals(200f, e1.y + e1.getHeight(), 0.01f);
    }

    @Test
    void landscapeLayoutInScrollAlignsChildrenTopLeft() {
        Element img = new Element() {
            @Override
            public float getPrefWidth() {
                return 120f;
            }

            @Override
            public float getPrefHeight() {
                return 80f;
            }
        };
        Element det = new Element() {
            @Override
            public float getPrefWidth() {
                return 200f;
            }

            @Override
            public float getPrefHeight() {
                return 250f;
            }
        };

        Scroll scroll = Ui.scroll().grow().children(() -> {
            Ui.row().grow().top().left().gap(8f).children(() -> {
                ParentStack.add(img);
                ParentStack.add(det);
            });
        });

        scroll.outer().setSize(600f, 400f);
        scroll.outer().layout();
        if (scroll.pane() != null) {
            scroll.pane().setSize(600f, 400f);
            scroll.pane().layout();
        }
        scroll.content().layout();

        Row row = (Row) SolimToken.getComponent(scroll.content().getChildren().first());
        row.table().layout();

        // Both img and det must be flush with the top of the row table (400)
        assertEquals(400f, img.y + img.getHeight(), 0.01f, "Image must align to the top");
        assertEquals(400f, det.y + det.getHeight(), 0.01f, "Details must align to the top");

        // Left alignment with gap
        assertEquals(0f, img.x, 0.01f, "Image starts at left edge");
        assertEquals(128f, det.x, 0.01f, "Details starts after image + gap");
    }

    @Test
    void bareRowDefaultsToTopLeft() {
        Row row = new Row();
        Element e1 = new Element() {
            @Override
            public float getPrefWidth() {
                return 50f;
            }

            @Override
            public float getPrefHeight() {
                return 50f;
            }
        };
        Element e2 = new Element() {
            @Override
            public float getPrefWidth() {
                return 100f;
            }

            @Override
            public float getPrefHeight() {
                return 100f;
            }
        };
        row.children(() -> {
            ParentStack.add(e1);
            ParentStack.add(e2);
        });
        row.table().setSize(300f, 200f);
        row.table().layout();

        Cell<?> c1 = row.table().getCell(e1);
        Cell<?> c2 = row.table().getCell(e2);

        assertEquals(Align.top | Align.left, CellAccess.align(c1) & (Align.top | Align.left));
        assertEquals(Align.top | Align.left, CellAccess.align(c2) & (Align.top | Align.left));
        assertEquals(200f, e1.y + e1.getHeight(), 0.01f, "Child 1 must be aligned to top of row");
        assertEquals(200f, e2.y + e2.getHeight(), 0.01f, "Child 2 must be aligned to top of row");
        assertEquals(0f, e1.x, 0.01f, "Child 1 must be at left edge");
        assertEquals(50f, e2.x, 0.01f, "Child 2 must follow Child 1");
    }

    @Test
    void explicitCenterOverridesTopLeftDefault() {
        Row row = new Row();
        row.center();
        Element e1 = new Element();
        row.children(() -> {
            ParentStack.add(e1);
        });

        Cell<?> c1 = row.table().getCell(e1);
        assertEquals(Align.center, CellAccess.align(c1) & Align.center);
    }
}
