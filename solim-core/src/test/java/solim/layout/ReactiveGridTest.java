package solim.layout;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.scene.Element;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.runtime.SignalDispatcher;
import solim.signal.Signal;

class ReactiveGridTest {

	@BeforeAll
	static void initArc() {
		if (Core.app == null) {
			Core.app = new arc.mock.MockApplication();
		}
		if (Core.graphics == null) {
			Core.graphics = new arc.mock.MockGraphics();
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

		// Expected width: (400 / 4) - 10 = 90
		assertEquals(90f, grid.context().itemWidth().get(), 0.1f);

		// Change column count to 2
		cols.set(2);
		SignalDispatcher.flush();

		// Expected width: (400 / 2) - 10 = 190
		assertEquals(190f, grid.context().itemWidth().get(), 0.1f);

		// Change gap to 20
		grid.gap(20f);
		SignalDispatcher.flush();

		// Expected width: (400 / 2) - 20 = 180
		assertEquals(180f, grid.context().itemWidth().get(), 0.1f);
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
}
