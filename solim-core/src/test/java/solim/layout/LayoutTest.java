package solim.layout;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.graphics.Color;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.Element;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.display.Text;
import solim.signal.Signal;
import solim.runtime.ParentStack;
import solim.ui.Ui;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Font.FontData;
import arc.graphics.g2d.TextureRegion;
import arc.scene.event.EventListener;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.struct.Seq;
import org.junit.jupiter.api.Assumptions;
import solim.input.Checkbox;
import solim.input.SolimTextField;
import solim.runtime.SignalDispatcher;

class LayoutTest {

	@BeforeAll
	static void checkArcContext() {
		if (Core.app == null) {
			Core.app = new MockApplication();
		}
		if (Core.graphics == null) {
			Core.graphics = new MockGraphics();
		}
	}

	@Test
	void rowJustifyAndGap() {
		Table root = new Table();
		ParentStack.push(root);
		Row r = new Row();
		r.gap(8f);
		r.justify(Justify.BETWEEN);
		r.align(Align.CENTER);
		Element a = new Element();
		a.name = "a";
		Element b = new Element();
		b.name = "b";
		r.add(a);
		r.add(b);
		assertEquals(2, r.table().getChildren().size);
		assertSame(a, r.table().getChildren().get(0));
		assertSame(b, r.table().getChildren().get(1));
		ParentStack.pop();
	}

	@Test
	void columnGapAndPadding() {
		Column c = new Column();
		c.gap(16f);
		c.padding(24f);
		Element e1 = new Element();
		Element e2 = new Element();
		c.add(e1).row();
		c.add(e2);
		assertEquals(2, c.table().getChildren().size);
	}

	@Test
	void spacerConsumesSpace() {
		Table root = new Table();
		ParentStack.push(root);
		Table row = new Table();
		Element back = new Element();
		back.name = "back";
		Element save = new Element();
		save.name = "save";
		row.add(back);
		row.add(new Spacer().element());
		row.add(save);
		assertEquals(3, row.getChildren().size);
		ParentStack.pop();
	}

	@Test
	void gridColumnsWrapping() {
		Grid g = new Grid(3).gap(8f);
		for (int i = 0; i < 4; i++) {
			g.add(new Element());
		}
		assertEquals(4, g.table().getChildren().size);
	}

	@Test
	void stackOverlay() {
		SolimStack s = new SolimStack();
		Element bg = new Element();
		Element fg = new Element();
		s.add(bg);
		s.add(fg);
		assertEquals(2, s.stack().getChildren().size);
	}

	@Test
	void dividerRenders() {
		Divider dx = new Divider();
		assertEquals(Direction.X, dx.direction());
		assertTrue(dx.sizeConstraints().growX);
		assertFalse(dx.sizeConstraints().growY);

		Divider dy = new Divider(Direction.Y);
		assertEquals(Direction.Y, dy.direction());
		assertTrue(dy.sizeConstraints().growY);
		assertFalse(dy.sizeConstraints().growX);
	}

	@Test
	void rowDraggableRegistersListener() {
		Row r = new Row().draggable();
		assertEquals(Touchable.enabled, r.table().touchable,
				"Row drag handle must use enabled so entire surface is draggable");

		boolean hasDragListener = false;
		for (EventListener l : r.table().getListeners()) {
			if (l instanceof InputListener) {
				hasDragListener = true;
				break;
			}
		}
		assertTrue(hasDragListener, "draggable() must register an InputListener");
	}

	@Test
	void containerAddsChild() {
		Container c = new Container().padding(12f);
		Element child = new Element();
		c.add(child);
		assertEquals(1, c.table().getChildren().size);
	}

	@Test
	void scrollWrapsContent() {
		Scroll s = new Scroll();
		Element e = new Element();
		s.add(e);
		assertEquals(1, s.content().getChildren().size);
		assertSame(e, s.content().getChildren().get(0));
	}

	@Test
	void justifyAndAlignEnums() {
		assertEquals(6, Justify.values().length);
		assertTrue(Arrays.asList(Justify.values()).contains(Justify.BETWEEN));
		assertEquals(4, Align.values().length);
		assertTrue(Arrays.asList(Align.values()).contains(Align.STRETCH));
	}

	@Test
	void wrapAddsChildren() {
		Wrap w = new Wrap().gap(4f);
		for (int i = 0; i < 3; i++) {
			w.add(new Element());
		}
		assertEquals(3, w.table().getChildren().size);
	}

	@Test
	void columnAlignPreservesChildrenAndReturnsSelf() {
		Column c = new Column();
		Element child = new Element();
		c.add(child);

		assertSame(c, c.align(Align.CENTER));
		assertSame(c, c.align(Align.START));
		assertSame(c, c.align(Align.END));
		assertEquals(1, c.table().getChildren().size);
		assertSame(child, c.table().getChildren().get(0));
	}

	@Test
	void rowJustifyPreservesChildrenAndReturnsSelf() {
		for (Justify j : Justify.values()) {
			Row r = new Row();
			Element child = new Element();
			r.add(child);
			assertSame(r, r.justify(j));
			assertEquals(1, r.table().getChildren().size);
			assertSame(child, r.table().getChildren().get(0));
		}
	}

	@Test
	void declarativeColumnAndRowCellLayout() {
		Column col = Ui.column().children(() -> {
			Ui.row().children(() -> {
				Element e1 = new Element() {
					@Override
					public float getPrefWidth() {
						return 100f;
					}

					@Override
					public float getPrefHeight() {
						return 40f;
					}
				};
				ParentStack.add(e1);
			});
			Ui.scroll().grow().children(() -> {
				Element e2 = new Element() {
					@Override
					public float getPrefWidth() {
						return 200f;
					}

					@Override
					public float getPrefHeight() {
						return 200f;
					}
				};
				ParentStack.add(e2);
			});
		});

		Table t = col.table();
		assertEquals(2, t.getCells().size, "Column must have 2 cells for its 2 children");
		t.setSize(600f, 800f);
		t.layout();

		Element toolbar = t.getChildren().get(0);
		Element scroll = t.getChildren().get(1);

		assertTrue(toolbar.getWidth() > 0f, "Toolbar must have non-zero width");
		assertTrue(toolbar.getHeight() > 0f, "Toolbar must have non-zero height");
		assertTrue(scroll.getWidth() > 0f, "Scroll must have non-zero width");
		assertTrue(scroll.getHeight() > 0f, "Scroll must have non-zero height");
		assertTrue(scroll.y + scroll.getHeight() <= toolbar.y, "Scroll and Toolbar must not overlap vertically");
	}

	@Test
	void cardDeclarativeAndChainedBindings() {
		Signal<Float> widthSignal = Signal.of(250f);
		Signal<Color> colorSignal = Signal.of(Color.scarlet);

		Card c = Ui.card()
				.height(180f)
				.width(widthSignal)
				.color(colorSignal)
				.cellPadding(12f)
				.children(() -> {
					ParentStack.add(new Element());
					ParentStack.add(new Element());
				});

		assertEquals(2, c.container().getChildren().size, "Card container must have 2 children");
		assertEquals(180f, c.cardButton().getHeight(), 0.01f);
		assertEquals(250f, c.cardButton().getWidth(), 0.01f);
		assertEquals(Color.scarlet, c.cardButton().color);

		widthSignal.set(300f);
		colorSignal.set(Color.green);
		SignalDispatcher.flush();
		assertEquals(300f, c.cardButton().getWidth(), 0.01f);
		assertEquals(Color.green, c.cardButton().color);

		c.dispose();
	}

	@Test
	void cardClickAndChildEventIsolation() {
		boolean[] cardClicked = {false};
		Card c = Ui.card().onClick(() -> cardClicked[0] = true);

		InputEvent stoppedEvent = new InputEvent();
		stoppedEvent.stop();
		c.cardButton().getListeners().forEach(listener -> {
			if (listener instanceof ClickListener) {
				((ClickListener) listener).clicked(stoppedEvent, 0f, 0f);
			}
		});
		assertFalse(cardClicked[0], "Card onClick should not execute when event is stopped");

		InputEvent normalEvent = new InputEvent();
		c.cardButton().getListeners().forEach(listener -> {
			if (listener instanceof ClickListener) {
				((ClickListener) listener).clicked(normalEvent, 0f, 0f);
			}
		});
		assertTrue(cardClicked[0], "Card onClick should execute for normal events");

		c.dispose();
	}

	@Test
	void spacerExpandsInRowAndColumn() {
		Row row = Ui.row(() -> {
			Ui.spacer();
		});
		Cell<?> rowCell = row.table().getCells().first();
		assertTrue(CellAccess.expandX(rowCell) > 0, "Spacer in Row must grow horizontally");

		Column col = Ui.column(() -> {
			Ui.spacer();
		});
		Cell<?> colCell = col.table().getCells().first();
		assertTrue(CellAccess.expandY(colCell) > 0, "Spacer in Column must grow vertically");
	}

	@Test
	void childrenDoNotGrowByDefault() {
		// Column
		Column col = Ui.column().children(() -> {
			Ui.row().children(() -> {});
		});
		Cell<?> colCell = col.table().getCells().first();
		assertEquals(0, CellAccess.expandX(colCell), "Column child must not growX by default");
		assertEquals(0, CellAccess.expandY(colCell), "Column child must not growY by default");

		// Row
		Row row = Ui.row().children(() -> {
			Ui.row().children(() -> {});
			ParentStack.attachToParent(new Element());
		});
		Cell<?> rowChildCell = row.table().getCells().get(0);
		Cell<?> rowElementCell = row.table().getCells().get(1);
		assertEquals(0, CellAccess.expandX(rowChildCell), "Row child must not growX by default");
		assertEquals(0, CellAccess.expandY(rowChildCell), "Row child must not growY by default");
		assertEquals(0, CellAccess.expandX(rowElementCell), "Row element must not growX by default");
		assertEquals(0, CellAccess.expandY(rowElementCell), "Row element must not growY by default");

		// Card
		Card card = Ui.card().children(() -> {
			Ui.row().children(() -> {});
		});
		Cell<?> cardCell = card.container().getCells().first();
		assertEquals(0, CellAccess.expandX(cardCell), "Card child must not growX by default");
		assertEquals(0, CellAccess.expandY(cardCell), "Card child must not growY by default");
		card.dispose();

		// Scroll
		Scroll scroll = Ui.scroll().children(() -> {
			Ui.row().children(() -> {});
		});
		Cell<?> scrollCell = scroll.content().getCells().first();
		assertEquals(0, CellAccess.expandX(scrollCell), "Scroll child must not growX by default");
		assertEquals(0, CellAccess.expandY(scrollCell), "Scroll child must not growY by default");
	}

	@Test
	void explicitGrowExpandsCells() {
		Column col = Ui.column().children(() -> {
			Ui.row().growX().children(() -> {});
			Ui.row().growY().children(() -> {});
			Ui.row().grow().children(() -> {});
		});
		Cell<?> cellGrowX = col.table().getCells().get(0);
		Cell<?> cellGrowY = col.table().getCells().get(1);
		Cell<?> cellGrow = col.table().getCells().get(2);

		assertTrue(CellAccess.expandX(cellGrowX) > 0, "explicit growX must expand horizontally");
		assertEquals(0, CellAccess.expandY(cellGrowX), "explicit growX must not expand vertically");

		assertEquals(0, CellAccess.expandX(cellGrowY), "explicit growY must not expand horizontally");
		assertTrue(CellAccess.expandY(cellGrowY) > 0, "explicit growY must expand vertically");

		assertTrue(CellAccess.expandX(cellGrow) > 0, "explicit grow must expand horizontally");
		assertTrue(CellAccess.expandY(cellGrow) > 0, "explicit grow must expand vertically");
	}

	@Test
	void chainedGrowAfterAttachment() {
		Column col = Ui.column().children(() -> {
			// SizedImage with growX() chained
			Ui.image().growX();
			// Button with growX() chained
			Ui.button().growX();
			// Row with growX() chained AFTER children()
			Ui.row().children(() -> {}).growX();
		});

		Cell<?> imgCell = col.table().getCells().get(0);
		Cell<?> btnCell = col.table().getCells().get(1);
		Cell<?> rowCell = col.table().getCells().get(2);

		assertTrue(CellAccess.expandX(imgCell) > 0, "image.growX() after attach must expand horizontally");
		assertEquals(1f, CellAccess.fillX(imgCell), 0.001f, "image.growX() after attach must fill horizontally");

		assertTrue(CellAccess.expandX(btnCell) > 0, "button.growX() after attach must expand horizontally");
		assertEquals(1f, CellAccess.fillX(btnCell), 0.001f, "button.growX() after attach must fill horizontally");

		assertTrue(CellAccess.expandX(rowCell) > 0, "row.children().growX() after attach must expand horizontally");
		assertEquals(1f, CellAccess.fillX(rowCell), 0.001f, "row.children().growX() after attach must fill horizontally");
	}

	@Test
	void reactiveGridInsideScrollHasNoGhostCells() {
		Table root = new Table();
		root.setSize(1024, 768);

		Signal<Seq<String>> items = Signal.of(Seq.with("feat1", "feat2"));
		Scroll[] scrollRef = new Scroll[1];

		Column col = Ui.column().grow().children(() -> {
			Ui.row().growX().gap(8f).children(() -> {
				Ui.image().size(24, 24);
				Ui.button().height(40).width(200);
			});
			scrollRef[0] = Ui.scroll().grow().children(() -> {
				Ui.grid(Signal.of(2), items, x -> x, x -> Ui.card().height(160).width(300).children(() -> {}));
			});
		});

		root.add(col.element()).grow().expand();
		root.validate();
		root.layout();

		Table content = scrollRef[0].content();
		assertEquals(1, content.getCells().size, "Scroll content must only contain the grid table, with no ghost cells");
		Table gridTable = (Table) content.getChildren().first();
		assertSame(gridTable, content.getCells().first().get(), "First and only cell in scroll content must be the grid table");
	}

	@Test
	void sizedTextFieldGrowAfterAttachment() {
		Assumptions.assumeTrue(Core.scene != null, "Arc Core.scene is null; skipping skin-dependent tests");
		Table parent = new Table();
		SolimTextField field = new SolimTextField("");
		parent.add(field.element());
		Cell<?> cell = parent.getCell(field.element());
		assertEquals(0, CellAccess.expandX(cell));

		field.growX();
		assertTrue(CellAccess.expandX(cell) > 0, "field.growX() after attach must expand horizontally");
		assertEquals(1f, CellAccess.fillX(cell), 0.001f, "field.growX() after attach must fill horizontally");
	}

	@Test
	void checkboxGrowVariants() {
		Assumptions.assumeTrue(Core.scene != null, "Arc Core.scene is null; skipping skin-dependent tests");
		Table parent = new Table();
		Checkbox cb = new Checkbox("Test", Signal.of(false));
		parent.add(cb.element());
		Cell<?> cell = parent.getCell(cb.element());
		assertEquals(0, CellAccess.expandX(cell));
		assertEquals(0, CellAccess.expandY(cell));

		cb.growX();
		assertTrue(CellAccess.expandX(cell) > 0, "cb.growX() must expand horizontally");
		assertEquals(1f, CellAccess.fillX(cell), 0.001f, "cb.growX() must fill horizontally");
		assertEquals(0, CellAccess.expandY(cell));

		cb.growY();
		assertTrue(CellAccess.expandY(cell) > 0, "cb.growY() must expand vertically");
		assertEquals(1f, CellAccess.fillY(cell), 0.001f, "cb.growY() must fill vertically");

		Table parent2 = new Table();
		Checkbox checkbox = new Checkbox("Test", Signal.of(false));
		parent2.add(checkbox.element());
		Cell<?> cell2 = parent2.getCell(checkbox.element());
		checkbox.grow();
		assertTrue(CellAccess.expandX(cell2) > 0, "checkbox.grow() must expand horizontally");
		assertTrue(CellAccess.expandY(cell2) > 0, "checkbox.grow() must expand vertically");
	}

	@Test
	void columnInsideScrollCentering() {
		Table root = new Table();
		root.setSize(1000, 800);

		Scroll scroll = Ui.scroll()
				.grow()
				.center()
				.children(() -> {
					Ui.column()
							.growX()
							.center()
							.maxWidth(400f)
							.children(() -> {
								Element child = new Element();
								child.setSize(100, 50);
								Ui.column().add(child);
							});
				});

		root.add(scroll.element()).grow();
		root.validate();
		root.layout();

		Table content = scroll.content();
		Table columnTable = (Table) content.getChildren().first();

		assertEquals(1000f, content.getWidth(), 0.01f);
		assertEquals(400f, columnTable.getWidth(), 0.01f);
		assertEquals(300f, columnTable.x, 0.01f, "Column with .center() inside scroll with .center() should be centered at x = 300");
	}

	@Test
	void columnCenteringWithoutScrollCenter() {
		Table root = new Table();
		root.setSize(1000, 800);

		Scroll scroll = Ui.scroll()
				.grow()
				.children(() -> {
					Ui.column()
							.growX()
							.center()
							.maxWidth(400f)
							.children(() -> {
								Element child = new Element();
								child.setSize(100, 50);
								Ui.column().add(child);
							});
				});

		root.add(scroll.element()).grow();
		root.validate();
		root.layout();

		Table content = scroll.content();
		Table columnTable = (Table) content.getChildren().first();

		assertEquals(400f, columnTable.getWidth(), 0.01f);
		assertEquals(300f, columnTable.x, 0.01f, "Column with .center() inside scroll without .center() should still be centered at x = 300");
	}

	@Test
	void scrollCenterCentersChildren() {
		Table root = new Table();
		root.setSize(1000, 800);

		Scroll scroll = Ui.scroll()
				.grow()
				.center()
				.children(() -> {
					Ui.column()
							.growX()
							.maxWidth(400f)
							.children(() -> {
								Element child = new Element();
								child.setSize(100, 50);
								Ui.column().add(child);
							});
				});

		root.add(scroll.element()).grow();
		root.validate();
		root.layout();

		Table content = scroll.content();
		Table columnTable = (Table) content.getChildren().first();

		assertEquals(400f, columnTable.getWidth(), 0.01f);
		assertEquals(300f, columnTable.x, 0.01f, "Scroll with .center() should center child column with maxWidth at x = 300");
	}

	@Test
	void columnInsideColumnCentering() {
		Table root = new Table();
		root.setSize(1000, 800);

		Column parentCol = Ui.column().grow().children(() -> {
			Ui.column()
					.growX()
					.center()
					.maxWidth(400f)
					.children(() -> {
						Element child = new Element();
						child.setSize(100, 50);
						Ui.column().add(child);
					});
		});

		root.add(parentCol.element()).grow();
		root.validate();
		root.layout();

		Table innerTable = (Table) parentCol.table().getChildren().first();
		assertEquals(400f, innerTable.getWidth(), 0.01f);
		assertEquals(300f, innerTable.x, 0.01f, "Column with .center() inside another column should be centered at x = 300");
	}

	@Test
	void cardCenteringInsideColumn() {
		Table root = new Table();
		root.setSize(1000, 800);

		Column parentCol = Ui.column().grow().children(() -> {
			Ui.card()
					.growX()
					.center()
					.maxWidth(400f)
					.children(() -> {
					});
		});

		root.add(parentCol.element()).grow();
		root.validate();
		root.layout();

		Element cardBtn = parentCol.table().getChildren().first();
		assertEquals(400f, cardBtn.getWidth(), 0.01f);
		assertEquals(300f, cardBtn.x, 0.01f, "Card with .center() inside column should be centered at x = 300");
	}

	@Test
	void gridGapBeforeChildren() {
		Grid g = Ui.grid(2)
				.gap(16f)
				.children(() -> {
					Ui.row(() -> {});
					Ui.row(() -> {});
				});

		assertEquals(2, g.table().getCells().size);
		Cell<?> cell0 = g.table().getCells().get(0);
		Cell<?> cell1 = g.table().getCells().get(1);
		assertEquals(0f, CellAccess.padLeft(cell0), 0.01f);
		assertEquals(0f, CellAccess.padTop(cell0), 0.01f);
		assertEquals(16f, CellAccess.padLeft(cell1), 0.01f);
		assertEquals(0f, CellAccess.padTop(cell1), 0.01f);
	}

	@Test
	void gridGapAfterChildren() {
		Grid g = Ui.grid(2)
				.children(() -> {
					Ui.row(() -> {});
					Ui.row(() -> {});
				})
				.gap(20f);

		assertEquals(2, g.table().getCells().size);
		Cell<?> cell0 = g.table().getCells().get(0);
		Cell<?> cell1 = g.table().getCells().get(1);
		assertEquals(0f, CellAccess.padLeft(cell0), 0.01f);
		assertEquals(20f, CellAccess.padLeft(cell1), 0.01f);
	}

	@Test
	void gridGapZeroArgConstructor() {
		Grid g = Ui.grid()
				.gap(12f)
				.children(() -> {
					Ui.row(() -> {});
				});

		assertEquals(1, g.table().getCells().size);
		assertEquals(0f, CellAccess.padLeft(g.table().getCells().first()), 0.01f);
	}

	@Test
	void gridGapReactiveSignal() {
		Signal<Float> gapSig = Signal.of(10f);
		Grid g = Ui.grid(2)
				.gap(gapSig)
				.children(() -> {
					Ui.row(() -> {});
					Ui.row(() -> {});
				});

		Cell<?> cell0 = g.table().getCells().get(0);
		Cell<?> cell1 = g.table().getCells().get(1);
		assertEquals(0f, CellAccess.padLeft(cell0), 0.01f);
		assertEquals(10f, CellAccess.padLeft(cell1), 0.01f);

		gapSig.set(30f);
		SignalDispatcher.flush();
		assertEquals(0f, CellAccess.padLeft(cell0), 0.01f);
		assertEquals(30f, CellAccess.padLeft(cell1), 0.01f);
	}

	@Test
	void reactiveGridGapUpdatesRenderedCells() {
		Signal<Float> gapSig = Signal.of(0f);
		ReactiveGrid<String, String> rg = Ui.grid(
				Signal.of(2),
				Signal.of(Arrays.asList("A", "B")),
				s -> s,
				s -> new Row()
		).gap(gapSig);

		assertEquals(2, rg.table().getCells().size);
		for (Cell<?> cell : rg.table().getCells()) {
			assertEquals(0f, CellAccess.padTop(cell), 0.01f, "Gap 0f must apply 0 pad to all cells");
		}

		gapSig.set(16f);
		SignalDispatcher.flush();
		Cell<?> cell0 = rg.table().getCells().get(0);
		Cell<?> cell1 = rg.table().getCells().get(1);
		assertEquals(0f, CellAccess.padLeft(cell0), 0.01f);
		assertEquals(16f, CellAccess.padLeft(cell1), 0.01f);
	}

	@SuppressWarnings("unchecked")
	@Test
	void featureCardGrowXInReactiveGrid() {
		Table root = new Table();
		root.setSize(1024, 768);

		Signal<Seq<String>> items = Signal.of(Seq.with("feat1", "feat2"));
		ReactiveGrid<String, String>[] gridRef = new ReactiveGrid[1];

		Column col = Ui.column().grow().children(() -> {
			Ui.scroll().grow().children(() -> {
				gridRef[0] = Ui.grid(Signal.of(2), items, x -> x, x -> {
					return Ui.card().height(160).growX().children(() -> {});
				});
			});
		});

		root.add(col.element()).grow();
		root.validate();
		root.layout();

		Table gridTable = gridRef[0].table();
		System.out.println("Actual cells size: " + gridTable.getCells().size);
		for (int i = 0; i < gridTable.getCells().size; i++) {
			System.out.println("Cell " + i + ": " + gridTable.getCells().get(i).get());
		}
		assertEquals(2, gridTable.getCells().size);
		Cell<?> cell0 = gridTable.getCells().get(0);
		Cell<?> cell1 = gridTable.getCells().get(1);

		assertTrue(CellAccess.expandX(cell0) > 0, "Cell 0 must have expandX > 0");
		assertEquals(1f, CellAccess.fillX(cell0), 0.01f, "Cell 0 must have fillX == 1");
		assertTrue(CellAccess.expandX(cell1) > 0, "Cell 1 must have expandX > 0");
		assertEquals(1f, CellAccess.fillX(cell1), 0.01f, "Cell 1 must have fillX == 1");

		assertEquals(cell0.get().getWidth(), cell1.get().getWidth(), 0.01f, "Both cards must have equal uniform width");
		assertTrue(cell0.get().getWidth() > 400f, "Card must expand to fill grid column (was " + cell0.get().getWidth() + ")");
	}

	@SuppressWarnings("unchecked")
	@Test
	void featureCardSingleItemInMultiColumnReactiveGrid() {
		Table root = new Table();
		root.setSize(1024, 768);

		Signal<Seq<String>> items = Signal.of(Seq.with("feat1"));
		ReactiveGrid<String, String>[] gridRef = new ReactiveGrid[1];

		Column col = Ui.column().grow().children(() -> {
			Ui.scroll().grow().children(() -> {
				gridRef[0] = Ui.grid(Signal.of(3), items, x -> x, x -> {
					return Ui.card().height(160).growX().children(() -> {});
				});
			});
		});

		root.add(col.element()).grow();
		root.validate();
		root.layout();

		Table gridTable = gridRef[0].table();
		assertEquals(3, gridTable.getCells().size, "Grid table must have 3 cells (1 card + 2 padding cells)");
		Cell<?> cell0 = gridTable.getCells().get(0);
		assertTrue(cell0.get().getWidth() < 400f, "Card in 3-column grid must not stretch across entire 1024px (was " + cell0.get().getWidth() + ")");
		assertTrue(cell0.get().getWidth() > 300f, "Card in 3-column grid must take its 1/3 share (was " + cell0.get().getWidth() + ")");
	}

	@Test
	void textWrapSetsMinWidthZeroOnTableCell() {
		Table table = new Table();
		Label.LabelStyle style = new Label.LabelStyle();
		FontData fontData = new FontData() {};
		style.font = new Font(fontData, new TextureRegion(), false);

		Text text = new Text("A very long message string that wraps across multiple lines", style);
		table.add(text.element());
		text.wrap();

		Cell<?> cell = table.getCell(text.element());
		assertNotNull(cell, "Cell must exist in parent table");
		assertEquals(0f, CellAccess.minWidth(cell), 0.001f, "Cell minWidth must be 0 when wrap() is set");
		assertEquals(1, CellAccess.expandX(cell), "Cell must expandX when wrap() is set");
		assertTrue(text.isWrap(), "Text wrap flag must be true");
	}
}

