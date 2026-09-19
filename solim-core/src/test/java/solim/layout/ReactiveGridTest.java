package solim.layout;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.scene.Element;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.core.BaseComponent;
import solim.runtime.SignalDispatcher;
import solim.reactive.Signal;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import solim.core.Ui;
import solim.runtime.ParentStack;

class ReactiveGridTest {

	@BeforeAll
	static void initArc() {
		if (Core.app == null) {
			Core.app = new MockApplication();
		}
		if (Core.graphics == null) {
			Core.graphics = new MockGraphics();
		}
	}

	static class ItemComp extends BaseComponent {
		final String text;
		boolean disposed = false;

		ItemComp(String text) {
			this.text = text;
		}

		@Override
		protected Element build() {
			Element el = new Element();
			el.name = text;
			return el;
		}

		@Override
		protected void onDispose() {
			disposed = true;
		}
	}

	@Test
	void providesContextToBiFunctionItemFactory() {
		Signal<Integer> cols = Signal.of(4);
		Signal<List<String>> items = Signal.of(Arrays.asList("A", "B"));
		AtomicReference<GridItemContext> capturedCtx = new AtomicReference<>();

		ReactiveGrid<String, String> grid = new ReactiveGrid<>(
				cols,
				items,
				s -> s,
				(item, ctx) -> {
					capturedCtx.set(ctx);
					return new ItemComp(item);
				}
		);
		grid.element();

		assertNotNull(capturedCtx.get());
		assertEquals(4, capturedCtx.get().columnCount().get().intValue());
		assertNotNull(capturedCtx.get().itemWidth().get());
	}

	@Test
	void calculatesItemWidthWhenTableWidthChanges() {
		Signal<Integer> cols = Signal.of(4);
		Signal<List<String>> items = Signal.of(Arrays.asList("A"));
		ReactiveGrid<String, String> grid = new ReactiveGrid<>(
				cols,
				items,
				s -> s,
				(item, ctx) -> new ItemComp(item)
		);
		grid.gap(10f);
		grid.element();

		// Set table width and trigger layout
		grid.table().setWidth(400f);
		grid.table().layout();
		SignalDispatcher.flush();

		// Expected width: (400 - (4 - 1) * 10) / 4 = 370 / 4 = 92.5
		assertEquals(92.5f, grid.context().itemWidth().get(), 0.1f);

		// Change column count to 2
		cols.set(2);
		SignalDispatcher.flush();

		// Expected width: (400 - (2 - 1) * 10) / 2 = 390 / 2 = 195
		assertEquals(195f, grid.context().itemWidth().get(), 0.1f);

		// Change gap to 20
		grid.gap(20f);
		SignalDispatcher.flush();

		// Expected width: (400 - (2 - 1) * 20) / 2 = 380 / 2 = 190
		assertEquals(190f, grid.context().itemWidth().get(), 0.1f);
	}

	@Test
	void backwardCompatibleWithFunctionItemFactory() {
		Signal<Integer> cols = Signal.of(2);
		Signal<List<String>> items = Signal.of(Arrays.asList("X", "Y"));

		ReactiveGrid<String, String> grid = ReactiveGrid.of(
				cols,
				items,
				s -> s,
				ItemComp::new
		);
		Element el = grid.element();
		assertNotNull(el);
		assertEquals(2, grid.table().getChildren().size);
	}

	@Test
	void disposesReconciledItemsAndItemWidth() {
		Signal<Integer> cols = Signal.of(2);
		Signal<List<String>> items = Signal.of(Arrays.asList("item1"));
		ItemComp[] created = new ItemComp[1];

		ReactiveGrid<String, String> grid = new ReactiveGrid<>(
				cols,
				items,
				s -> s,
				(item, ctx) -> {
					ItemComp c = new ItemComp(item);
					created[0] = c;
					return c;
				}
		);
		grid.element();
		assertNotNull(created[0]);
		assertFalse(created[0].disposed);

		grid.dispose();
		assertTrue(created[0].disposed);
	}

	@Test
	void measuresItemWidthMatchesActualDirectChildWidth() {
		Signal<Integer> cols = Signal.of(3);
		Signal<List<String>> items = Signal.of(Arrays.asList("A", "B", "C"));

		List<Element> childElementsGrowX = new ArrayList<>();
		ReactiveGrid<String, String> grid = new ReactiveGrid<>(
				cols,
				items,
				s -> s,
				(item, ctx) -> {
					Column col = Ui.column().growX();
					childElementsGrowX.add(col.element());
					return col;
				}
		);
		grid.gap(10f);
		grid.element();
		grid.table().setSize(300f, 300f);
		grid.table().layout();
		SignalDispatcher.flush();

		float calculatedWidth = grid.context().itemWidth().get();
		// Formula: (tw - (cols - 1) * gap) / cols = (300 - 2 * 10) / 3 = 280 / 3 = 93.33
		assertEquals(93.33f, calculatedWidth, 0.1f);

		// Direct children with growX() in a grid with no table padding match calculated itemWidth
		for (Element child : childElementsGrowX) {
			assertEquals(calculatedWidth, child.getWidth(), 1.0f,
					"Direct child width in cell must match calculated itemWidth");
		}
	}

	@Test
	void measuresItemWidthMatchesActualChildWithExplicitWidth() {
		Signal<Integer> cols = Signal.of(3);
		Signal<List<String>> items = Signal.of(Arrays.asList("A", "B", "C"));

		List<Element> childElements = new ArrayList<>();
		ReactiveGrid<String, String> grid = new ReactiveGrid<>(
				cols,
				items,
				s -> s,
				(item, ctx) -> {
					Column col = Ui.column().width(ctx.itemWidth());
					childElements.add(col.element());
					return col;
				}
		);
		grid.gap(10f);
		grid.element();
		grid.table().setSize(300f, 300f);
		grid.table().layout();
		SignalDispatcher.flush();

		float calculatedWidth = grid.context().itemWidth().get();
		for (Element child : childElements) {
			assertEquals(calculatedWidth, child.getWidth(), 1.0f,
					"Child with explicit width(ctx.itemWidth()) must match calculated width");
		}
	}

	@Test
	void measuresItemWidthAccountsForTableMarginsAndPadding() {
		Signal<Integer> cols = Signal.of(3);
		Signal<List<String>> items = Signal.of(Arrays.asList("A", "B", "C"));

		List<Element> childElements = new ArrayList<>();
		ReactiveGrid<String, String> grid = new ReactiveGrid<>(
				cols,
				items,
				s -> s,
				(item, ctx) -> {
					Column col = Ui.column().growX();
					childElements.add(col.element());
					return col;
				}
		);
		grid.gap(10f);
		grid.element();
		grid.table().margin(20f); // Table has 20px outer margin (padLeft=20, padRight=20)
		grid.table().setSize(300f, 300f);
		grid.table().layout();
		SignalDispatcher.flush();

		float calculatedWidth = grid.context().itemWidth().get();
		float actualChildWidth = childElements.get(0).getWidth();

		// Actual available width = 300 - 40 = 260 -> (260 - 2 * 10) / 3 = 240 / 3 = 80.0
		assertEquals(80.0f, calculatedWidth, 0.5f,
				"Calculated itemWidth must deduct table margins");
		assertEquals(actualChildWidth, calculatedWidth, 1.0f,
				"Calculated itemWidth must match actual rendered child width when table has padding");
	}

	@Test
	void measuresInnerCardWithMarginMatchesItemWidthAndRemainsSquare() {
		Signal<Integer> cols = Signal.of(3);
		Signal<List<String>> items = Signal.of(Arrays.asList("A", "B", "C"));

		List<Element> cardElements = new ArrayList<>();
		List<Element> outerColElements = new ArrayList<>();
		ReactiveGrid<String, String> gridCards = new ReactiveGrid<>(
				cols,
				items,
				s -> s,
				(item, ctx) -> {
					Card[] holder = new Card[1];
					Column col = Ui.column()
							.growX()
							.gap(8f)
							.children(() -> {
								holder[0] = Ui.card()
										.growX()
										.margin(4f, 0f, 4f, 0f)
										.height(ctx.itemWidth())
										.children(() -> {});
							});
					cardElements.add(holder[0].element());
					outerColElements.add(col.element());
					return col;
				}
		);
		gridCards.gap(10f);
		gridCards.element();
		gridCards.table().setSize(300f, 300f);
		gridCards.table().layout();
		SignalDispatcher.flush();

		outerColElements.get(0).validate();
		Element cardEl = cardElements.get(0);
		assertEquals(93.33f, cardEl.getWidth(), 1.0f);
		assertEquals(93.33f, cardEl.getHeight(), 1.0f);
		assertEquals(cardEl.getWidth(), cardEl.getHeight(), 1.0f,
				"Inner card is square with margin(4,0,4,0) while keeping column vertical gap");
	}

	@Test
	void reactiveGridSupportsElementAndTableConfig() {
		Signal<Integer> cols = Signal.of(2);
		Signal<List<String>> items = Signal.of(Collections.emptyList());
		ReactiveGrid<String, String> grid = new ReactiveGrid<>(cols, items, s -> s, (s, ctx) -> new ItemComp(s))
				.width(400f)
				.height(200f)
				.visible(false)
				.padding(16f);

		assertEquals(400f, grid.element().getWidth());
		assertEquals(200f, grid.element().getHeight());
		assertFalse(grid.element().visible);
		assertEquals(16f, grid.table().getMarginTop());
		grid.dispose();
	}

	static class CardWrapperProbe extends BaseComponent {
		final float contentHeight;

		CardWrapperProbe(float contentHeight) {
			this.contentHeight = contentHeight;
		}

		@Override
		protected Element build() {
			return Ui.card()
					.name("probe-card")
					.left()
					.grow()
					.minHeight(200f)
					.children(() -> {
						Element content = new Element() {
							@Override
							public float getPrefWidth() {
								return 100f;
							}

							@Override
							public float getPrefHeight() {
								return contentHeight;
							}
						};
						ParentStack.add(content);
					})
					.element();
		}
	}

	@Test
	void baseComponentChildInReactiveGridAppliesCellConstraints() {
		Signal<Integer> cols = Signal.of(2);
		Signal<List<String>> items = Signal.of(Arrays.asList("A", "B"));
		ReactiveGrid<String, String> grid = new ReactiveGrid<>(
				cols,
				items,
				s -> s,
				(item, ctx) -> new CardWrapperProbe(50f)
		);
		grid.element();

		Element firstEl = grid.table().getChildren().get(0);
		Cell<?> cell = grid.table().getCell(firstEl);
		assertNotNull(cell, "Cell must exist in grid table");

		assertEquals(1, CellAccess.expandY(cell), "Cell should have expandY = 1 from card.grow()");
		assertEquals(1f, CellAccess.fillY(cell), 0.01f, "Cell should have fillY = 1 from card.grow()");
		assertEquals(200f, CellAccess.minHeight(cell), 0.01f, "Cell should have minHeight = 200 from card.minHeight()");
		grid.dispose();
	}

	@Test
	void baseComponentChildInReactiveGridGrowsToMatchTallSibling() {
		Signal<Integer> cols = Signal.of(2);
		Signal<List<String>> items = Signal.of(Arrays.asList("short", "tall"));
		ReactiveGrid<String, String> grid = new ReactiveGrid<>(
				cols,
				items,
				s -> s,
				(item, ctx) -> "short".equals(item) ? new CardWrapperProbe(50f) : new CardWrapperProbe(350f)
		);
		grid.element();
		grid.table().setWidth(600f);
		grid.table().setHeight(grid.table().getPrefHeight());
		grid.table().validate();
		grid.table().layout();

		Element shortEl = grid.table().getChildren().get(0);
		Element tallEl = grid.table().getChildren().get(1);

		assertTrue(tallEl.getHeight() >= 350f, "Tall element should have height >= 350");
		assertEquals(tallEl.getHeight(), shortEl.getHeight(), 1.0f,
				"Short card with grow() must expand vertically to match tall sibling in the same row");
		grid.dispose();
	}
}
