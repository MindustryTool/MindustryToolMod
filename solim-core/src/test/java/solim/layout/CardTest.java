package solim.layout;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.graphics.Color;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.Element;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.util.Align;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.signal.Signal;
import solim.runtime.SignalDispatcher;
import solim.runtime.ParentStack;

class CardTest {

    @BeforeAll
    static void initArc() {
        if (Core.app == null) {
            Core.app = new MockApplication();
        }
        if (Core.graphics == null) {
            Core.graphics = new MockGraphics();
        }
    }

    @Test
    void createsWithDefaultNames() {
        Card c = new Card();
        assertEquals("solim-card-table", c.table().name);
        assertEquals("solim-card-table", c.container().name);
        assertEquals("solim-card-table", c.element().name);
        c.dispose();
    }

    @Test
    void elementTableAndContainerAreSameInstance() {
        Card c = new Card();
        assertSame(c.table(), c.element());
        assertSame(c.container(), c.element());
        c.dispose();
    }

    @Test
    void noClickListenerWithoutOnClick() {
        Card c = new Card();
        assertFalse(hasClickListener(c), "Card without onClick must install no ClickListener");
        c.dispose();
    }

    @Test
    void childrenAreAddedToContainer() {
        Card c = new Card();
        Element first = new Element();
        Element second = new Element();

        c.children(() -> {
            ParentStack.add(first);
            ParentStack.add(second);
        });

        assertEquals(2, c.container().getChildren().size);
        assertSame(first, c.container().getChildren().get(0));
        assertSame(second, c.container().getChildren().get(1));
        c.dispose();
    }

    @Test
    void onClickExecutesCallback() {
        boolean[] clicked = { false };
        Card c = new Card().onClick(() -> clicked[0] = true);

        InputEvent event = new InputEvent();
        c.table().getListeners().forEach(listener -> {
            if (listener instanceof ClickListener) {
                ((ClickListener) listener).clicked(event, 0f, 0f);
            }
        });

        assertTrue(clicked[0]);
        c.dispose();
    }

    @Test
    void onClickDoesNotExecuteWhenEventStopped() {
        boolean[] clicked = { false };
        Card c = new Card().onClick(() -> clicked[0] = true);

        InputEvent stoppedEvent = new InputEvent();
        stoppedEvent.stop();
        c.table().getListeners().forEach(listener -> {
            if (listener instanceof ClickListener) {
                ((ClickListener) listener).clicked(stoppedEvent, 0f, 0f);
            }
        });

        assertFalse(clicked[0]);
        c.dispose();
    }

    @Test
    void repeatedOnClickDoesNotStackListeners() {
        Card c = new Card().onClick(() -> {
        }).onClick(() -> {
        });

        int clicks = 0;
        for (Object listener : c.table().getListeners()) {
            if (listener instanceof ClickListener) {
                clicks++;
            }
        }
        assertEquals(1, clicks, "Repeated onClick must not stack ClickListeners");
        c.dispose();
    }

    @Test
    void colorModifierUpdatesTableColor() {
        Card c = new Card();
        c.color(Color.scarlet);
        assertEquals(Color.scarlet, c.table().color);
        c.dispose();
    }

    @Test
    void reactiveColorUpdatesTableColor() {
        Signal<Color> colorSig = Signal.of(Color.green);
        Card c = new Card().color(colorSig);
        assertEquals(Color.green, c.table().color);

        colorSig.set(Color.blue);
        SignalDispatcher.flush();
        assertEquals(Color.blue, c.table().color);
        c.dispose();
    }

    @Test
    void reactiveWidthUpdatesTablePrefWidth() {
        Signal<Float> widthSig = Signal.of(200f);
        Card c = new Card();
        c.width(widthSig);
        assertEquals(200f, c.table().getWidth(), 0.01f);
        assertEquals(200f, c.cellConfig().prefWidth.get(), 0.01f);

        widthSig.set(300f);
        SignalDispatcher.flush();
        assertEquals(300f, c.table().getWidth(), 0.01f);
        assertEquals(300f, c.cellConfig().prefWidth.get(), 0.01f);
        c.dispose();
    }

    @Test
    void reactiveHeightUpdatesTablePrefHeight() {
        Signal<Float> heightSig = Signal.of(150f);
        Card c = new Card();
        c.height(heightSig);
        assertEquals(150f, c.table().getHeight(), 0.01f);
        assertEquals(150f, c.cellConfig().prefHeight.get(), 0.01f);

        heightSig.set(200f);
        SignalDispatcher.flush();
        assertEquals(200f, c.table().getHeight(), 0.01f);
        assertEquals(200f, c.cellConfig().prefHeight.get(), 0.01f);
        c.dispose();
    }

    @Test
    void visibleModifierChangesTableVisibility() {
        Card c = new Card();

        c.visible(false);
        assertFalse(c.table().visible);

        c.visible(true);
        assertTrue(c.table().visible);
        c.dispose();
    }

    @Test
    void reactiveVisibleUpdatesTableVisibility() {
        Signal<Boolean> vis = Signal.of(true);
        Card c = new Card().visible(vis);
        assertTrue(c.table().visible);

        vis.set(false);
        SignalDispatcher.flush();
        assertFalse(c.table().visible);
        c.dispose();
    }

    @Test
    void positionSetsTableCoordinates() {
        Card c = new Card();

        c.x(10f);
        assertEquals(10f, c.table().x, 0.01f);

        c.y(20f);
        assertEquals(20f, c.table().y, 0.01f);

        c.position(30f, 40f);
        assertEquals(30f, c.table().x, 0.01f);
        assertEquals(40f, c.table().y, 0.01f);
        c.dispose();
    }

    @Test
    void nameModifierUpdatesTableName() {
        Card c = new Card();
        c.name("my-card");
        assertEquals("my-card", c.table().name);
        c.dispose();
    }

    @Test
    void cellConfigReturnsNonNull() {
        Card c = new Card();
        assertNotNull(c.cellConfig());
        c.dispose();
    }

    @Test
    void bareCardDefaultsToTopLeft() {
        Card c = new Card();
        Element first = new Element();
        Element second = new Element();

        c.children(() -> {
            ParentStack.add(first);
            ParentStack.add(second);
        });

        Cell<?> c1 = c.container().getCell(first);
        Cell<?> c2 = c.container().getCell(second);

        assertEquals(Align.top | Align.left, CellAccess.align(c1) & (Align.top | Align.left));
        assertEquals(Align.top | Align.left, CellAccess.align(c2) & (Align.top | Align.left));
        c.dispose();
    }

    @Test
    void minHeightSetsParentCellFloor() {
        Row parent = new Row();
        Card card = new Card().minHeight(120f);
        parent.children(() -> {
            card.children(() -> {
                ParentStack.add(fixedSize(20f, 20f));
            });
        });

        Cell<?> cell = parent.table().getCell(card.table());
        assertNotNull(cell);
        assertEquals(120f, CellAccess.minHeight(cell), 0.01f);
        card.dispose();
    }

    @Test
    void reactiveMinHeightFollowsSignal() {
        Signal<Float> minSig = Signal.of(120f);
        Row parent = new Row();
        Card card = new Card().minHeight(minSig);
        parent.children(() -> {
            card.children(() -> {
                ParentStack.add(fixedSize(20f, 20f));
            });
        });

        Cell<?> cell = parent.table().getCell(card.table());
        assertNotNull(cell);
        assertEquals(120f, CellAccess.minHeight(cell), 0.01f);

        minSig.set(200f);
        SignalDispatcher.flush();
        assertEquals(200f, CellAccess.minHeight(cell), 0.01f);
        card.dispose();
    }

    @Test
    void minHeightLiftsShortContent() {
        Row parent = new Row();
        Card card = new Card().minHeight(120f);
        parent.children(() -> {
            card.children(() -> {
                ParentStack.add(fixedSize(20f, 20f));
            });
        });

        parent.table().validate();
        parent.table().layout();
        assertEquals(120f, parent.table().getPrefHeight(), 0.5f);
        card.dispose();
    }

    @Test
    void minHeightLeavesTallContentUntouched() {
        Row parent = new Row();
        Card card = new Card().minHeight(120f);
        parent.children(() -> {
            card.children(() -> {
                ParentStack.add(fixedSize(20f, 300f));
            });
        });

        parent.table().validate();
        parent.table().layout();
        assertEquals(300f, parent.table().getPrefHeight(), 0.5f);
        card.dispose();
    }

    @Test
    void negativeMinHeightClampsToZero() {
        Row parent = new Row();
        Card card = new Card().minHeight(-50f);
        parent.children(() -> {
            card.children(() -> {
                ParentStack.add(fixedSize(20f, 20f));
            });
        });

        Cell<?> cell = parent.table().getCell(card.table());
        assertNotNull(cell);
        assertEquals(0f, CellAccess.minHeight(cell), 0.01f);
        card.dispose();
    }

    @Test
    void maxHeightSetsParentCellCeiling() {
        Row parent = new Row();
        Card card = new Card().maxHeight(120f);
        parent.children(() -> {
            card.children(() -> {
                ParentStack.add(fixedSize(20f, 20f));
            });
        });

        Cell<?> cell = parent.table().getCell(card.table());
        assertNotNull(cell);
        assertEquals(120f, CellAccess.maxHeight(cell), 0.01f);
        card.dispose();
    }

    @Test
    void reactiveMaxHeightFollowsSignal() {
        Signal<Float> maxSig = Signal.of(120f);
        Row parent = new Row();
        Card card = new Card().maxHeight(maxSig);
        parent.children(() -> {
            card.children(() -> {
                ParentStack.add(fixedSize(20f, 20f));
            });
        });

        Cell<?> cell = parent.table().getCell(card.table());
        assertNotNull(cell);
        assertEquals(120f, CellAccess.maxHeight(cell), 0.01f);

        maxSig.set(200f);
        SignalDispatcher.flush();
        assertEquals(200f, CellAccess.maxHeight(cell), 0.01f);
        card.dispose();
    }

    @Test
    void maxHeightCapsTallContent() {
        Row parent = new Row();
        Card card = new Card().maxHeight(120f);
        parent.children(() -> {
            card.children(() -> {
                ParentStack.add(fixedSize(20f, 300f));
            });
        });

        parent.table().setSize(500f, 500f);
        parent.table().validate();
        parent.table().layout();
        assertEquals(120f, card.table().getHeight(), 0.5f);
        card.dispose();
    }

    @Test
    void maxHeightLeavesShortContentUntouched() {
        Row parent = new Row();
        Card card = new Card().maxHeight(120f);
        parent.children(() -> {
            card.children(() -> {
                ParentStack.add(fixedSize(20f, 20f));
            });
        });

        parent.table().validate();
        parent.table().layout();
        assertEquals(20f, parent.table().getPrefHeight(), 0.5f);
        card.dispose();
    }

    @Test
    void minWidthSetsParentCellFloor() {
        Row parent = new Row();
        Card card = new Card().minWidth(120f);
        parent.children(() -> {
            card.children(() -> {
                ParentStack.add(fixedSize(20f, 20f));
            });
        });

        Cell<?> cell = parent.table().getCell(card.table());
        assertNotNull(cell);
        assertEquals(120f, CellAccess.minWidth(cell), 0.01f);
        card.dispose();
    }

    @Test
    void reactiveMinWidthFollowsSignal() {
        Signal<Float> minSig = Signal.of(120f);
        Row parent = new Row();
        Card card = new Card().minWidth(minSig);
        parent.children(() -> {
            card.children(() -> {
                ParentStack.add(fixedSize(20f, 20f));
            });
        });

        Cell<?> cell = parent.table().getCell(card.table());
        assertNotNull(cell);
        assertEquals(120f, CellAccess.minWidth(cell), 0.01f);

        minSig.set(200f);
        SignalDispatcher.flush();
        assertEquals(200f, CellAccess.minWidth(cell), 0.01f);
        card.dispose();
    }

    @Test
    void minWidthWidensNarrowContent() {
        Row parent = new Row();
        Card card = new Card().minWidth(120f);
        parent.children(() -> {
            card.children(() -> {
                ParentStack.add(fixedSize(20f, 20f));
            });
        });

        parent.table().validate();
        parent.table().layout();
        assertEquals(120f, parent.table().getPrefWidth(), 0.5f);
        card.dispose();
    }

    @Test
    void minWidthLeavesWideContentUntouched() {
        Row parent = new Row();
        Card card = new Card().minWidth(120f);
        parent.children(() -> {
            card.children(() -> {
                ParentStack.add(fixedSize(300f, 20f));
            });
        });

        parent.table().validate();
        parent.table().layout();
        assertEquals(300f, parent.table().getPrefWidth(), 0.5f);
        card.dispose();
    }

    @Test
    void maxWidthSetsParentCellCeiling() {
        Row parent = new Row();
        Card card = new Card().maxWidth(120f);
        parent.children(() -> {
            card.children(() -> {
                ParentStack.add(fixedSize(20f, 20f));
            });
        });

        Cell<?> cell = parent.table().getCell(card.table());
        assertNotNull(cell);
        assertEquals(120f, CellAccess.maxWidth(cell), 0.01f);
        card.dispose();
    }

    @Test
    void reactiveMaxWidthFollowsSignal() {
        Signal<Float> maxSig = Signal.of(120f);
        Row parent = new Row();
        Card card = new Card().maxWidth(maxSig);
        parent.children(() -> {
            card.children(() -> {
                ParentStack.add(fixedSize(20f, 20f));
            });
        });

        Cell<?> cell = parent.table().getCell(card.table());
        assertNotNull(cell);
        assertEquals(120f, CellAccess.maxWidth(cell), 0.01f);

        maxSig.set(200f);
        SignalDispatcher.flush();
        assertEquals(200f, CellAccess.maxWidth(cell), 0.01f);
        card.dispose();
    }

    @Test
    void maxWidthCapsWideContent() {
        Row parent = new Row();
        Card card = new Card().maxWidth(120f);
        parent.children(() -> {
            card.children(() -> {
                ParentStack.add(fixedSize(300f, 20f));
            });
        });

        parent.table().setSize(500f, 500f);
        parent.table().validate();
        parent.table().layout();
        assertEquals(120f, card.table().getWidth(), 0.5f);
        card.dispose();
    }

    @Test
    void maxWidthLeavesNarrowContentUntouched() {
        Row parent = new Row();
        Card card = new Card().maxWidth(120f);
        parent.children(() -> {
            card.children(() -> {
                ParentStack.add(fixedSize(20f, 20f));
            });
        });

        parent.table().validate();
        parent.table().layout();
        assertEquals(20f, parent.table().getPrefWidth(), 0.5f);
        card.dispose();
    }

    private static Element fixedSize(float prefW, float prefH) {
        return new Element() {
            @Override
            public float getPrefWidth() {
                return prefW;
            }

            @Override
            public float getPrefHeight() {
                return prefH;
            }
        };
    }

    private static boolean hasClickListener(Card c) {
        for (Object listener : c.table().getListeners()) {
            if (listener instanceof ClickListener) {
                return true;
            }
        }
        return false;
    }
}
