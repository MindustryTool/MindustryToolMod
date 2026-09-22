package solim.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import solim.core.BaseComponent;
import solim.layout.Column;
import solim.layout.Row;
import solim.runtime.SignalDispatcher;
import solim.test.SolimCoreEnv;

class WhenComponentTest extends SolimCoreEnv {

    static class Probe extends BaseComponent {
        boolean wasDisposed = false;

        @Override
        protected Element build() {
            return new Element();
        }

        @Override
        protected void onDispose() {
            wasDisposed = true;
        }
    }

    @Test
    void trueMountsThenBranch() {
        Signal<Boolean> cond = Signal.of(true);
        Probe[] thenBox = new Probe[1];
        Probe[] elseBox = new Probe[1];
        When when = When.of(cond)
                .thenDo(() -> {
                    thenBox[0] = new Probe();
                })
                .elseDo(() -> {
                    elseBox[0] = new Probe();
                });
        when.element();
        SignalDispatcher.flush();
        assertEquals(1, when.container().getChildren().size);
        assertTrue(when.container().visible);
        assertFalse(thenBox[0].wasDisposed);
        assertTrue(elseBox[0] == null);
        when.dispose();
    }

    @Test
    void falseMountsElseBranch() {
        Signal<Boolean> cond = Signal.of(false);
        Probe[] thenBox = new Probe[1];
        Probe[] elseBox = new Probe[1];
        When when = When.of(cond)
                .thenDo(() -> {
                    thenBox[0] = new Probe();
                })
                .elseDo(() -> {
                    elseBox[0] = new Probe();
                });
        when.element();
        SignalDispatcher.flush();
        assertEquals(1, when.container().getChildren().size);
        assertTrue(when.container().visible);
        assertTrue(thenBox[0] == null);
        assertFalse(elseBox[0].wasDisposed);
        when.dispose();
    }

    @Test
    void nullMountsElseBranch() {
        Signal<Boolean> cond = Signal.<Boolean>of(null);
        Probe[] elseBox = new Probe[1];
        When when = When.of(cond)
                .thenDo(() -> new Probe())
                .elseDo(() -> {
                    elseBox[0] = new Probe();
                });
        when.element();
        SignalDispatcher.flush();
        assertEquals(1, when.container().getChildren().size);
        assertFalse(elseBox[0].wasDisposed);
        when.dispose();
    }

    @Test
    void missingElseCollapsesWhenFalse() {
        Signal<Boolean> cond = Signal.of(false);
        When when = When.of(cond).thenDo(() -> new Probe());
        when.element();
        SignalDispatcher.flush();
        assertEquals(0, when.container().getChildren().size);
        assertFalse(when.container().visible);
        when.dispose();
    }

    @Test
    void missingThenCollapsesWhenTrue() {
        Signal<Boolean> cond = Signal.of(true);
        When when = When.of(cond).elseDo(() -> new Probe());
        when.element();
        SignalDispatcher.flush();
        assertEquals(0, when.container().getChildren().size);
        assertFalse(when.container().visible);
        when.dispose();
    }

    @Test
    void multipleRootsInBranchAllMount() {
        Signal<Boolean> cond = Signal.of(true);
        Probe[] first = new Probe[1];
        Probe[] second = new Probe[1];
        When when = When.of(cond).thenDo(() -> {
            first[0] = new Probe();
            second[0] = new Probe();
        });
        when.element();
        SignalDispatcher.flush();
        assertEquals(2, when.container().getChildren().size);
        when.dispose();
        assertTrue(first[0].wasDisposed);
        assertTrue(second[0].wasDisposed);
    }

    @Test
    void branchCanUseNestedContainers() {
        Signal<Boolean> cond = Signal.of(true);
        Probe[] rowProbe = new Probe[1];
        Probe[] elseProbe = new Probe[1];
        Row[] rowBox = new Row[1];
        When when = When.of(cond)
                .thenDo(() -> {
                    Row row = new Row();
                    rowBox[0] = row;
                    row.children(() -> {
                        rowProbe[0] = new Probe();
                    });
                })
                .elseDo(() -> {
                    elseProbe[0] = new Probe();
                });
        when.element();
        SignalDispatcher.flush();
        assertEquals(1, when.container().getChildren().size);
        assertTrue(rowBox[0].table().getChildren().contains(rowProbe[0].element(), true));

        cond.set(false);
        SignalDispatcher.flush();
        assertTrue(rowProbe[0].wasDisposed);
        assertEquals(1, when.container().getChildren().size);
        assertFalse(elseProbe[0].wasDisposed);
        when.dispose();
    }

    @Test
    void rendersInsideParentColumn() {
        Signal<Boolean> cond = Signal.of(true);
        Probe[] thenBox = new Probe[1];
        Probe[] elseBox = new Probe[1];
        When[] whenBox = new When[1];
        Column col = new Column().children(() -> {
            whenBox[0] = When.of(cond)
                    .thenDo(() -> {
                        thenBox[0] = new Probe();
                    })
                    .elseDo(() -> {
                        elseBox[0] = new Probe();
                    });
        });
        Table table = col.table();
        assertTrue(table.getChildren().contains(whenBox[0].container(), true));
        assertEquals(1, whenBox[0].container().getChildren().size);

        cond.set(false);
        SignalDispatcher.flush();
        assertTrue(thenBox[0].wasDisposed);
        assertEquals(1, whenBox[0].container().getChildren().size);
        assertFalse(elseBox[0].wasDisposed);

        whenBox[0].dispose();
        col.dispose();
    }

    @Test
    void modifiersAfterBranchesRender() {
        Signal<Boolean> cond = Signal.of(true);
        Probe[] thenBox = new Probe[1];
        When when = When.of(cond)
                .thenDo(() -> {
                    thenBox[0] = new Probe();
                })
                .elseDo(() -> new Probe())
                .growX();
        when.element();
        SignalDispatcher.flush();
        assertTrue(when.cellConfig().growX);
        assertEquals(1, when.container().getChildren().size);
        assertFalse(thenBox[0].wasDisposed);
        when.dispose();
    }

    @Test
    void modifierBeforeBranchesStillRendersViaRefresh() {
        Signal<Boolean> cond = Signal.of(true);
        Probe[] thenBox = new Probe[1];
        Probe[] elseBox = new Probe[1];
        When when = When.of(cond).growX();
        assertEquals(0, when.container().getChildren().size);

        when.thenDo(() -> {
            thenBox[0] = new Probe();
        }).elseDo(() -> {
            elseBox[0] = new Probe();
        });
        assertEquals(1, when.container().getChildren().size);
        assertFalse(thenBox[0].wasDisposed);

        cond.set(false);
        SignalDispatcher.flush();
        assertTrue(thenBox[0].wasDisposed);
        assertEquals(1, when.container().getChildren().size);
        assertFalse(elseBox[0].wasDisposed);
        when.dispose();
    }

    @Test
    void toggleDisposesPreviousBranch() {
        Signal<Boolean> cond = Signal.of(true);
        Probe[] thenBox = new Probe[1];
        Probe[] elseBox = new Probe[1];
        When when = When.of(cond)
                .thenDo(() -> {
                    thenBox[0] = new Probe();
                })
                .elseDo(() -> {
                    elseBox[0] = new Probe();
                });
        when.element();
        SignalDispatcher.flush();

        cond.set(false);
        SignalDispatcher.flush();
        assertTrue(thenBox[0].wasDisposed);
        assertEquals(1, when.container().getChildren().size);
        assertFalse(elseBox[0].wasDisposed);
        when.dispose();
    }

    @Test
    void collapseOnFalseDisposesBranch() {
        Signal<Boolean> cond = Signal.of(true);
        Probe[] thenBox = new Probe[1];
        When when = When.of(cond).thenDo(() -> {
            thenBox[0] = new Probe();
        });
        when.element();
        SignalDispatcher.flush();
        assertEquals(1, when.container().getChildren().size);

        cond.set(false);
        SignalDispatcher.flush();
        assertTrue(thenBox[0].wasDisposed);
        assertEquals(0, when.container().getChildren().size);
        assertFalse(when.container().visible);
        when.dispose();
    }

    @Test
    void disposeDisposesBranch() {
        Signal<Boolean> cond = Signal.of(true);
        Probe[] thenBox = new Probe[1];
        When when = When.of(cond).thenDo(() -> {
            thenBox[0] = new Probe();
        });
        when.element();
        SignalDispatcher.flush();
        when.dispose();
        assertTrue(thenBox[0].wasDisposed);
    }

    @Test
    void disposedWhenIgnoresFurtherToggles() {
        Signal<Boolean> cond = Signal.of(true);
        Probe[] thenBox = new Probe[1];
        When when = When.of(cond)
                .thenDo(() -> {
                    thenBox[0] = new Probe();
                })
                .elseDo(() -> new Probe());
        when.element();
        SignalDispatcher.flush();
        when.dispose();
        assertTrue(thenBox[0].wasDisposed);

        cond.set(false);
        SignalDispatcher.flush();
        assertEquals(0, when.container().getChildren().size);
    }

    @Test
    void postMountThenUpdateRemounts() {
        Signal<Boolean> cond = Signal.of(true);
        Probe[] first = new Probe[1];
        Probe[] second = new Probe[1];
        When when = When.of(cond).thenDo(() -> {
            first[0] = new Probe();
        });
        when.element();
        SignalDispatcher.flush();
        assertEquals(1, when.container().getChildren().size);

        when.thenDo(() -> {
            second[0] = new Probe();
        });
        assertTrue(first[0].wasDisposed);
        assertEquals(1, when.container().getChildren().size);
        assertFalse(second[0].wasDisposed);
        when.dispose();
    }

    @Test
    void postMountElseUpdateRemounts() {
        Signal<Boolean> cond = Signal.of(false);
        Probe[] first = new Probe[1];
        Probe[] second = new Probe[1];
        When when = When.of(cond).elseDo(() -> {
            first[0] = new Probe();
        });
        when.element();
        SignalDispatcher.flush();
        assertEquals(1, when.container().getChildren().size);

        when.elseDo(() -> {
            second[0] = new Probe();
        });
        assertTrue(first[0].wasDisposed);
        assertEquals(1, when.container().getChildren().size);
        assertFalse(second[0].wasDisposed);
        when.dispose();
    }

    @Test
    void nestedInsideDynamicRendersAndSwitches() {
        Signal<String> outer = Signal.of("show");
        Signal<Boolean> flag = Signal.of(true);
        Probe[] innerThen = new Probe[1];
        Probe[] innerElse = new Probe[1];
        Dynamic<String> root = Dynamic.of(outer, val -> {
            if (!"show".equals(val)) {
                return;
            }
            When.of(flag)
                    .thenDo(() -> {
                        innerThen[0] = new Probe();
                    })
                    .elseDo(() -> {
                        innerElse[0] = new Probe();
                    });
        });
        root.element();
        SignalDispatcher.flush();
        assertEquals(1, root.container().getChildren().size);

        flag.set(false);
        SignalDispatcher.flush();
        assertTrue(innerThen[0].wasDisposed);
        assertFalse(innerElse[0].wasDisposed);

        outer.set("hide");
        SignalDispatcher.flush();
        assertTrue(innerElse[0].wasDisposed);
        assertEquals(0, root.container().getChildren().size);
        root.dispose();
    }

    @Test
    void nestedWhenInsideWhenSwitchesIndependently() {
        Signal<Boolean> outer = Signal.of(true);
        Signal<Boolean> inner = Signal.of(true);
        Probe[] innerThen = new Probe[1];
        Probe[] innerElse = new Probe[1];
        When root = When.of(outer).thenDo(() -> When.of(inner)
                .thenDo(() -> {
                    innerThen[0] = new Probe();
                })
                .elseDo(() -> {
                    innerElse[0] = new Probe();
                }));
        root.element();
        SignalDispatcher.flush();

        inner.set(false);
        SignalDispatcher.flush();
        assertTrue(innerThen[0].wasDisposed);
        assertFalse(innerElse[0].wasDisposed);
        assertEquals(1, root.container().getChildren().size);

        outer.set(false);
        SignalDispatcher.flush();
        assertTrue(innerElse[0].wasDisposed);
        assertEquals(0, root.container().getChildren().size);
        root.dispose();
    }

    @Test
    void branchChildDoesNotGrowByDefault() {
        Signal<Boolean> cond = Signal.of(true);
        When when = When.of(cond).thenDo(() -> {
            Row row = new Row();
            row.cellConfig().prefWidth = Readable.of(100f);
            row.cellConfig().prefHeight = Readable.of(40f);
        });
        when.element();
        Cell<?> cell = when.container().getCells().first();
        assertEquals(0, CellAccess.expandX(cell));
        assertEquals(0, CellAccess.expandY(cell));
        when.dispose();
    }

    @Test
    void perRootCellConfigAppliesToOwnCell() {
        Signal<Boolean> cond = Signal.of(true);
        When when = When.of(cond).thenDo(() -> {
            new Row().growX();
            new Probe();
        });
        when.element();
        @SuppressWarnings("rawtypes")
        Seq<Cell> cells = when.container().getCells();
        assertEquals(2, cells.size);
        assertEquals(1, CellAccess.expandX(cells.get(0)));
        assertEquals(0, CellAccess.expandX(cells.get(1)));
        when.dispose();
    }

    @Test
    void grownContainerFillsParentWidth() {
        Signal<Boolean> cond = Signal.of(true);
        When[] whenBox = new When[1];
        Column col = new Column().children(() -> {
            whenBox[0] = When.of(cond).thenDo(() -> {
                Row row = new Row();
                row.cellConfig().prefWidth = Readable.of(100f);
                row.cellConfig().prefHeight = Readable.of(40f);
            }).growX();
        });
        Table table = col.table();
        table.setSize(400f, 300f);
        table.validate();
        table.layout();
        assertEquals(400f, whenBox[0].container().getWidth(), 2f);
        whenBox[0].dispose();
        col.dispose();
    }

    @Test
    void branchRootGrowXFillsGrownContainer() {
        Signal<Boolean> cond = Signal.of(true);
        Row[] rowBox = new Row[1];
        When[] whenBox = new When[1];
        Column col = new Column().children(() -> {
            whenBox[0] = When.of(cond).thenDo(() -> {
                Row row = new Row().growX();
                row.cellConfig().prefHeight = Readable.of(40f);
                rowBox[0] = row;
            }).growX();
        });
        Table table = col.table();
        table.setSize(400f, 300f);
        table.validate();
        table.layout();
        assertEquals(400f, whenBox[0].container().getWidth(), 2f);
        assertEquals(400f, rowBox[0].table().getWidth(), 2f);
        whenBox[0].dispose();
        col.dispose();
    }

    @Test
    void whenMatchesDynamicContainerDimensions() {
        Signal<Boolean> cond = Signal.of(true);
        Dynamic<Boolean> dyn = Dynamic.of(cond, val -> {
            Row row = new Row();
            row.cellConfig().prefWidth = Readable.of(100f);
            row.cellConfig().prefHeight = Readable.of(40f);
        });
        When when = When.of(cond).thenDo(() -> {
            Row row = new Row();
            row.cellConfig().prefWidth = Readable.of(100f);
            row.cellConfig().prefHeight = Readable.of(40f);
        });
        Table parent = new Table();
        parent.add(dyn.element());
        parent.add(when.element());
        parent.setSize(400f, 300f);
        parent.validate();
        parent.layout();
        assertEquals(dyn.container().getWidth(), when.container().getWidth(), 0.5f);
        assertEquals(dyn.container().getHeight(), when.container().getHeight(), 0.5f);
        assertEquals(dyn.container().getChildren().first().getWidth(),
                when.container().getChildren().first().getWidth(), 0.5f);
        dyn.dispose();
        when.dispose();
    }

    @Test
    void collapseShrinksParentCell() {
        Signal<Boolean> cond = Signal.of(true);
        When when = When.of(cond).thenDo(() -> new Probe());
        Table parent = new Table();
        parent.defaults().padTop(8f).padBottom(8f);
        Cell<?> parentCell = parent.add(when.element());
        parent.pack();

        assertTrue(when.container().visible);
        assertEquals(1, when.container().getChildren().size);

        cond.set(false);
        SignalDispatcher.flush();
        parent.layout();

        assertFalse(when.container().visible);
        assertEquals(0, when.container().getChildren().size);
        assertEquals(0f, CellAccess.padTop(parentCell), 0.01f);
        assertEquals(0f, CellAccess.padBottom(parentCell), 0.01f);
        when.dispose();
    }

    @Test
    void collapseExpandCycleRestoresChild() {
        Signal<Boolean> cond = Signal.of(true);
        When when = When.of(cond).thenDo(() -> new Probe());
        Table parent = new Table();
        parent.add(when.element());
        parent.setSize(400f, 300f);
        parent.validate();
        parent.layout();

        assertTrue(when.container().visible);
        assertEquals(1, when.container().getChildren().size);

        cond.set(false);
        SignalDispatcher.flush();
        parent.layout();
        assertFalse(when.container().visible);
        assertEquals(0, when.container().getChildren().size);

        cond.set(true);
        SignalDispatcher.flush();
        parent.layout();
        assertTrue(when.container().visible);
        assertEquals(1, when.container().getChildren().size);
        when.dispose();
    }
}
