package solim.ui;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.scene.Element;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.core.BaseComponent;
import solim.signal.Signal;
import solim.runtime.SignalDispatcher;

class DynamicComponentTest {

	@BeforeAll
	static void initArc() {
		if (Core.app == null) {
			Core.app = new arc.mock.MockApplication();
		}
		if (Core.graphics == null) {
			Core.graphics = new arc.mock.MockGraphics();
		}
	}

	static class TestComponent extends BaseComponent {
		final String id;
		boolean wasDisposed = false;

		TestComponent(String id) {
			this.id = id;
		}

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
	void dynamicCreatesWithInitialContentA() {
		Map<String, TestComponent> instances = new HashMap<>();
		Signal<Boolean> toggle = Signal.of(true);
		Dynamic<Boolean> dyn = new Dynamic<>(toggle, val -> {
			TestComponent tc = new TestComponent(val ? "A" : "B");
			instances.put(tc.id, tc);
			return tc;
		});
		dyn.element();
		assertEquals("A", instances.get("A").id);
		assertEquals(1, dyn.container().getChildren().size);
		assertSame(dyn.container(), dyn.element());
		dyn.dispose();
	}

	@Test
	void dynamicRendersContent() {
		Signal<Boolean> toggle = Signal.of(true);
		Dynamic<Boolean> dyn = new Dynamic<>(toggle, val -> new TestComponent(val ? "A" : "B"));
		dyn.element();
		assertEquals(1, dyn.container().getChildren().size);
		dyn.dispose();
	}

	@Test
	void dynamicSwitchesContent() {
		Signal<Boolean> toggle = Signal.of(true);
		Map<String, TestComponent> instances = new HashMap<>();
		Dynamic<Boolean> dyn = new Dynamic<>(toggle, val -> {
			TestComponent tc = new TestComponent(val ? "A" : "B");
			instances.put(tc.id, tc);
			return tc;
		});
		dyn.element();
		TestComponent compA = instances.get("A");
		assertEquals("A", compA.id);

		toggle.set(false);
		SignalDispatcher.flush();
		assertTrue(compA.wasDisposed, "Previous component must be disposed on change");
		TestComponent compB = instances.get("B");
		assertEquals("B", compB.id);
		assertFalse(compB.wasDisposed);
		assertEquals(1, dyn.container().getChildren().size);
		dyn.dispose();
	}

	@Test
	void dynamicDisposal() {
		Signal<Boolean> toggle = Signal.of(true);
		Map<String, TestComponent> instances = new HashMap<>();
		Dynamic<Boolean> dyn = new Dynamic<>(toggle, val -> {
			TestComponent tc = new TestComponent(val ? "A" : "B");
			instances.put(tc.id, tc);
			return tc;
		});
		dyn.element();
		dyn.dispose();
		assertTrue(instances.get("A").wasDisposed);
	}

	@Test
	void dynamicChildDoesNotGrowByDefault() {
		Signal<String> source = Signal.of("A");
		Dynamic<String> dyn = new Dynamic<>(source, val -> new TestComponent(val));
		dyn.element();
		arc.scene.ui.layout.Cell<?> cell = dyn.container().getCells().first();
		assertEquals(0, arc.scene.ui.layout.CellAccess.expandX(cell));
		assertEquals(0, arc.scene.ui.layout.CellAccess.expandY(cell));
		dyn.dispose();
	}

	@Test
	void dynamicFactoryDoesNotTrackSignalsEvaluatedDuringChildBuild() {
		Signal<Boolean> switcher = Signal.of(true);
		Signal<String> internalChildSignal = Signal.of("initial");
		int[] factoryBuildCount = new int[]{0};

		Dynamic<Boolean> dyn = new Dynamic<>(switcher, val -> {
			factoryBuildCount[0]++;
			String text = internalChildSignal.get();
			return new TestComponent(val + "-" + text);
		});
		dyn.element();
		assertEquals(1, factoryBuildCount[0]);

		internalChildSignal.set("updated");
		SignalDispatcher.flush();
		assertEquals(1, factoryBuildCount[0], "Updating signal read during child build must not re-run Dynamic factory");

		switcher.set(false);
		SignalDispatcher.flush();
		assertEquals(2, factoryBuildCount[0], "Updating switcher source must trigger Dynamic factory");
		dyn.dispose();
	}

	@Test
	void nullComponentCollapsesContainerAndParentCell() {
		Signal<String> source = Signal.of("show");
		Dynamic<String> dyn = new Dynamic<>(source, val -> "show".equals(val) ? new TestComponent("active") : null);

		arc.scene.ui.layout.Table parent = new arc.scene.ui.layout.Table();
		parent.defaults().padTop(8f).padBottom(8f);
		arc.scene.ui.layout.Cell<?> parentCell = parent.add(dyn.element());
		parent.pack();

		assertTrue(dyn.container().visible);
		assertEquals(1, dyn.container().getChildren().size);

		source.set("hide");
		SignalDispatcher.flush();
		parent.layout();

		assertFalse(dyn.container().visible);
		assertEquals(0, dyn.container().getChildren().size);
		assertEquals(0f, arc.scene.ui.layout.CellAccess.padTop(parentCell), 0.01f);
		assertEquals(0f, arc.scene.ui.layout.CellAccess.padBottom(parentCell), 0.01f);

		dyn.dispose();
	}

	@Test
	void expandAfterCollapseResetsCellConstraints() {
		Signal<String> source = Signal.of("show");
		Dynamic<String> dyn = new Dynamic<>(source, val -> "show".equals(val) ? new TestComponent("active") : null);

		arc.scene.ui.layout.Table parent = new arc.scene.ui.layout.Table();
		arc.scene.ui.layout.Cell<?> parentCell = parent.add(dyn.element());
		parent.pack();

		// Collapse
		source.set(null);
		SignalDispatcher.flush();
		parent.layout();

		assertFalse(dyn.container().visible);

		// Expand
		source.set("show");
		SignalDispatcher.flush();
		parent.layout();

		assertTrue(dyn.container().visible);
		assertEquals(1, dyn.container().getChildren().size);
		assertTrue(dyn.container().getChildren().first().getMinHeight() >= 0,
				"Expanded container child must have non-negative minHeight");
	}

	@Test
	void nullThenNonNullShowsVisibleContainerWithChild() {
		Signal<String> source = Signal.of(null);
		Dynamic<String> dyn = new Dynamic<>(source, val -> val != null ? new TestComponent("item") : null);

		arc.scene.ui.layout.Table parent = new arc.scene.ui.layout.Table();
		parent.add(dyn.element());
		parent.pack();

		assertFalse(dyn.container().visible);
		assertEquals(0, dyn.container().getChildren().size);

		source.set("show");
		SignalDispatcher.flush();
		parent.layout();

		assertTrue(dyn.container().visible);
		assertEquals(1, dyn.container().getChildren().size);
		dyn.dispose();
	}

	@Test
	void collapseExpandCyclePreservesContainerChild() {
		Signal<String> source = Signal.of("show");
		Dynamic<String> dyn = new Dynamic<>(source, val -> "show".equals(val) ? new TestComponent("dynamic") : null);

		arc.scene.ui.layout.Table parent = new arc.scene.ui.layout.Table();
		parent.add(dyn.element());
		parent.setSize(400f, 300f);
		parent.validate();
		parent.layout();

		assertTrue(dyn.container().visible);
		assertEquals(1, dyn.container().getChildren().size);

		// Collapse
		source.set(null);
		SignalDispatcher.flush();
		parent.layout();
		assertFalse(dyn.container().visible);
		assertEquals(0, dyn.container().getChildren().size);

		// Expand
		source.set("show");
		SignalDispatcher.flush();
		parent.layout();
		assertTrue(dyn.container().visible);
		assertEquals(1, dyn.container().getChildren().size);

		dyn.dispose();
	}

	@Test
	void dynamicPreservesTopRightAlignmentWithoutGrowX() {
		Signal<Boolean> state = Signal.of(true);
		Dynamic<Boolean> dyn = Dynamic.of(state, s -> {
			solim.layout.Row row = Ui.row();
			row.sizeConstraints().prefWidth = solim.signal.Readable.of(100f);
			row.sizeConstraints().prefHeight = solim.signal.Readable.of(40f);
			return row;
		}).top().right();

		assertFalse(dyn.sizeConstraints().growX, "Dynamic must not growX by default");

		solim.layout.Column col = Ui.column().fillParent().top().right().children(() -> {
			solim.runtime.ParentStack.add(dyn);
		});

		arc.scene.ui.layout.Table table = col.table();
		table.setSize(800f, 600f);
		table.validate();
		table.layout();

		arc.scene.ui.layout.Cell<?> cell = table.getCells().first();
		assertEquals(0, arc.scene.ui.layout.CellAccess.expandX(cell), "Cell in top-right column must not expandX");
		assertTrue(dyn.element().x > 600f, "Element must be positioned on the right side (was " + dyn.element().x + ")");

		dyn.dispose();
		col.dispose();
	}
}

