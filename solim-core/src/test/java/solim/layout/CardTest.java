package solim.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static solim.modifier.Values.height;
import static solim.modifier.Values.marginLeft;
import static solim.modifier.Values.marginTop;
import static solim.modifier.Values.maxHeight;
import static solim.modifier.Values.maxWidth;
import static solim.modifier.Values.minHeight;
import static solim.modifier.Values.minWidth;
import static solim.modifier.Values.onClick;
import static solim.modifier.Values.position;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.visible;
import static solim.modifier.Values.width;
import static solim.modifier.Values.x;
import static solim.modifier.Values.y;

import org.junit.jupiter.api.Test;

import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Scl;
import arc.util.Align;
import solim.reactive.Signal;
import solim.runtime.AttachmentStack;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class CardTest extends SolimEnv {

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
            AttachmentStack.add(first);
            AttachmentStack.add(second);
        });

        assertEquals(2, c.container().getChildren().size);
        assertSame(first, c.container().getChildren().get(0));
        assertSame(second, c.container().getChildren().get(1));
        c.dispose();
    }

    @Test
    void onClickExecutesCallback() {
        boolean[] clicked = { false };
        Card c = new Card().props(onClick(() -> clicked[0] = true));

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
        Card c = new Card().props(onClick(() -> clicked[0] = true));

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
        Card c = new Card().props(onClick(() -> {
        }), onClick(() -> {
        }));

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
        c.props(width(widthSig), prefWidth(widthSig));
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
        c.props(height(heightSig), prefHeight(heightSig));
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

        c.props(visible(false));
        assertFalse(c.table().visible);

        c.props(visible(true));
        assertTrue(c.table().visible);
        c.dispose();
    }

    @Test
    void reactiveVisibleUpdatesTableVisibility() {
        Signal<Boolean> vis = Signal.of(true);
        Card c = new Card().props(visible(vis));
        assertTrue(c.table().visible);

        vis.set(false);
        SignalDispatcher.flush();
        assertFalse(c.table().visible);
        c.dispose();
    }

    @Test
    void positionSetsTableCoordinates() {
        Card c = new Card();

        c.props(x(10f));
        assertEquals(10f, c.table().x, 0.01f);

        c.props(y(20f));
        assertEquals(20f, c.table().y, 0.01f);

        c.props(position(30f, 40f));
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
            AttachmentStack.add(first);
            AttachmentStack.add(second);
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
        Card card = new Card().props(minHeight(120f));
        parent.children(() -> {
            card.children(() -> {
                AttachmentStack.add(fixedSize(20f, 20f));
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
        Card card = new Card().props(minHeight(minSig));
        parent.children(() -> {
            card.children(() -> {
                AttachmentStack.add(fixedSize(20f, 20f));
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
        Card card = new Card().props(minHeight(120f));
        parent.children(() -> {
            card.children(() -> {
                AttachmentStack.add(fixedSize(20f, 20f));
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
        Card card = new Card().props(minHeight(120f));
        parent.children(() -> {
            card.children(() -> {
                AttachmentStack.add(fixedSize(20f, 300f));
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
        Card card = new Card().props(minHeight(-50f));
        parent.children(() -> {
            card.children(() -> {
                AttachmentStack.add(fixedSize(20f, 20f));
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
        Card card = new Card().props(maxHeight(120f));
        parent.children(() -> {
            card.children(() -> {
                AttachmentStack.add(fixedSize(20f, 20f));
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
        Card card = new Card().props(maxHeight(maxSig));
        parent.children(() -> {
            card.children(() -> {
                AttachmentStack.add(fixedSize(20f, 20f));
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
        Card card = new Card().props(maxHeight(120f));
        parent.children(() -> {
            card.children(() -> {
                AttachmentStack.add(fixedSize(20f, 300f));
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
        Card card = new Card().props(maxHeight(120f));
        parent.children(() -> {
            card.children(() -> {
                AttachmentStack.add(fixedSize(20f, 20f));
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
        Card card = new Card().props(minWidth(120f));
        parent.children(() -> {
            card.children(() -> {
                AttachmentStack.add(fixedSize(20f, 20f));
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
        Card card = new Card().props(minWidth(minSig));
        parent.children(() -> {
            card.children(() -> {
                AttachmentStack.add(fixedSize(20f, 20f));
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
    void reactiveSizeBindingsTrackOnlyTheirOwnConstraint() {
        Signal<Float> minWidthSig = Signal.of(120f);
        Signal<Float> maxHeightSig = Signal.of(60f);

        Row parent = new Row();
        Card card = new Card().props(minWidth(minWidthSig), maxHeight(maxHeightSig));
        parent.children(() -> card.children(() -> AttachmentStack.add(fixedSize(20f, 20f))));

        Cell<?> cell = parent.table().getCell(card.table());
        assertNotNull(cell);
        assertEquals(120f, CellAccess.minWidth(cell), 0.01f);
        assertEquals(60f, CellAccess.maxHeight(cell), 0.01f);

        maxHeightSig.set(90f);
        SignalDispatcher.flush();

        assertEquals(90f, CellAccess.maxHeight(cell), 0.01f);
        assertEquals(120f, CellAccess.minWidth(cell), 0.01f);

        minWidthSig.set(200f);
        SignalDispatcher.flush();
        assertEquals(200f, CellAccess.minWidth(cell), 0.01f);
        card.dispose();
    }

    @Test
    void reactiveMarginBindingsTrackOnlyTheirOwnEdge() {
        Signal<Float> padTopSig = Signal.of(4f);
        Signal<Float> padLeftSig = Signal.of(8f);

        Row parent = new Row();
        Card card = new Card().props(marginTop(padTopSig), marginLeft(padLeftSig));
        parent.children(() -> card.children(() -> AttachmentStack.add(fixedSize(20f, 20f))));

        Cell<?> cell = parent.table().getCell(card.table());
        assertNotNull(cell);
        assertEquals(4f, CellAccess.padTop(cell), 0.01f);
        assertEquals(8f, CellAccess.padLeft(cell), 0.01f);

        padTopSig.set(12f);
        SignalDispatcher.flush();

        assertEquals(12f, CellAccess.padTop(cell), 0.01f);
        assertEquals(8f, CellAccess.padLeft(cell), 0.01f,
                "Changing padTop must not disturb the padLeft binding");

        padLeftSig.set(16f);
        SignalDispatcher.flush();
        assertEquals(16f, CellAccess.padLeft(cell), 0.01f);
        card.dispose();
    }

    @Test
    void minWidthWidensNarrowContent() {
        Row parent = new Row();
        Card card = new Card().props(minWidth(120f));
        parent.children(() -> {
            card.children(() -> {
                AttachmentStack.add(fixedSize(20f, 20f));
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
        Card card = new Card().props(minWidth(120f));
        parent.children(() -> {
            card.children(() -> {
                AttachmentStack.add(fixedSize(300f, 20f));
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
        Card card = new Card().props(maxWidth(120f));
        parent.children(() -> {
            card.children(() -> {
                AttachmentStack.add(fixedSize(20f, 20f));
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
        Card card = new Card().props(maxWidth(maxSig));
        parent.children(() -> {
            card.children(() -> {
                AttachmentStack.add(fixedSize(20f, 20f));
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
        Card card = new Card().props(maxWidth(120f));
        parent.children(() -> {
            card.children(() -> {
                AttachmentStack.add(fixedSize(300f, 20f));
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
        Card card = new Card().props(maxWidth(120f));
        parent.children(() -> {
            card.children(() -> {
                AttachmentStack.add(fixedSize(20f, 20f));
            });
        });

        parent.table().validate();
        parent.table().layout();
        assertEquals(20f, parent.table().getPrefWidth(), 0.5f);
        card.dispose();
    }

    @Test
    void heightBeforeAttachSetsParentCellFixedSlot() {
        Row parent = new Row();
        Card card = new Card().props(height(70f), prefHeight(70f));
        parent.children(() -> {
            card.children(() -> {
                AttachmentStack.add(fixedSize(20f, 20f));
            });
        });

        Cell<?> cell = parent.table().getCell(card.table());
        assertNotNull(cell);

        assertEquals(Scl.scl(70f), CellAccess.minHeight(cell), 0.01f);
        assertEquals(Scl.scl(70f), CellAccess.maxHeight(cell), 0.01f);
        card.dispose();
    }

    @Test
    void heightAfterAttachMatchesBeforeAttach() {
        Row beforeParent = new Row();
        Card before = new Card().props(height(70f), prefHeight(70f));
        beforeParent.children(() -> {
            before.children(() -> {
                AttachmentStack.add(fixedSize(20f, 20f));
            });
        });

        Row afterParent = new Row();
        Card after = new Card();
        afterParent.children(() -> {
            after.children(() -> {
                AttachmentStack.add(fixedSize(20f, 20f));
            });
        });
        after.props(height(70f), prefHeight(70f));

        beforeParent.table().validate();
        beforeParent.table().layout();
        afterParent.table().validate();
        afterParent.table().layout();

        assertEquals(before.table().getHeight(), after.table().getHeight(), 0.01f);
        assertEquals(Scl.scl(70f), after.table().getHeight(), 0.5f);
        before.dispose();
        after.dispose();
    }

    @Test
    void heightLiftsShortContentToFixedSlot() {
        Row parent = new Row();
        Card card = new Card().props(height(70f), prefHeight(70f));
        parent.children(() -> {
            card.children(() -> {
                AttachmentStack.add(fixedSize(20f, 20f));
            });
        });

        parent.table().validate();
        parent.table().layout();
        assertEquals(Scl.scl(70f), card.table().getHeight(), 0.5f);
        card.dispose();
    }

    @Test
    void heightClampsTallContentToFixedSlot() {
        Row parent = new Row();
        Card card = new Card().props(height(70f), prefHeight(70f));
        parent.children(() -> {
            card.children(() -> {
                AttachmentStack.add(fixedSize(20f, 300f));
            });
        });

        parent.table().setSize(500f, 500f);
        parent.table().validate();
        parent.table().layout();
        assertEquals(Scl.scl(70f), card.table().getHeight(), 0.5f);
        card.dispose();
    }

    @Test
    void heightInColumnParentMatchesRow() {
        Column parent = new Column();
        Card card = new Card().props(height(70f), prefHeight(70f));
        parent.children(() -> {
            card.children(() -> {
                AttachmentStack.add(fixedSize(20f, 20f));
            });
        });

        parent.table().validate();
        parent.table().layout();
        assertEquals(Scl.scl(70f), card.table().getHeight(), 0.5f);
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
