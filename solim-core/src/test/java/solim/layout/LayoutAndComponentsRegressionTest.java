package solim.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.Element;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import solim.core.DisposableAction;
import solim.display.Badge;
import solim.display.Text;
import solim.input.Button;
import solim.reactive.Signal;
import solim.reactive.TwoWayBinding;
import solim.runtime.ParentStack;
import solim.test.SolimEnv;
import solim.test.TestScheduler;

public class LayoutAndComponentsRegressionTest extends SolimEnv {

	// ==========================================
	// Phase 16: Layout Containers
	// ==========================================

	@Test
	void gridWrapsChildrenAfterColumns() {
		Grid grid = new Grid(2);
		Table table = grid.table();

		Element c1 = new Element();
		Element c2 = new Element();
		Element c3 = new Element();
		Element c4 = new Element();

		grid.add(c1);
		grid.add(c2);
		grid.add(c3);
		grid.add(c4);

		assertEquals(4, table.getChildren().size);
		assertSame(c1, table.getChildren().get(0));
		assertSame(c2, table.getChildren().get(1));
		assertSame(c3, table.getChildren().get(2));
		assertSame(c4, table.getChildren().get(3));
	}

	@Test
	void cardAppliesContentAndPadding() {
		Card card = new Card().padding(16f);
		Table container = card.container();

		Element content = new Element();
		ParentStack.push(container);
		ParentStack.add(content);
		ParentStack.pop();

		assertEquals(16f, container.getMarginLeft(), 0.01f);
		assertEquals(16f, container.getMarginRight(), 0.01f);
		assertEquals(16f, container.getMarginTop(), 0.01f);
		assertEquals(16f, container.getMarginBottom(), 0.01f);
		assertTrue(container.getChildren().contains(content, true));
	}

	// ==========================================
	// Phase 17: TwoWayBinding
	// ==========================================

	@Test
	void twoWayBindingBidirectionalSyncAndNoFeedbackLoop() {
		Signal<String> state = Signal.of("initial");
		String[] widgetValue = new String[]{"from-widget-init"};
		AtomicInteger widgetSetCalls = new AtomicInteger(0);
		new AtomicInteger(0);
		Runnable[] triggerWidgetChange = new Runnable[1];

		TwoWayBinding<String> binding = new TwoWayBinding<>(
				state,
				() -> widgetValue[0],
				val -> {
					widgetSetCalls.incrementAndGet();
					widgetValue[0] = val;
				},
				listener -> {
					triggerWidgetChange[0] = listener;
					return DisposableAction.empty();
				}
		);

		// Initial effect run applies value to widget
		assertEquals(1, widgetSetCalls.get());
		assertEquals("initial", widgetValue[0]);

		// 1. Signal -> Widget
		state.set("from-signal");
		TestScheduler.flush();

		assertEquals(2, widgetSetCalls.get());
		assertEquals("from-signal", widgetValue[0]);

		// 2. Widget -> Signal
		widgetValue[0] = "from-widget";
		triggerWidgetChange[0].run();

		assertEquals("from-widget", state.get());
		// Verify no feedback loop: widgetSetCalls must NOT increase because equality check suppresses writeback
		TestScheduler.flush();
		assertEquals(2, widgetSetCalls.get());

		// 3. Disposal stops both directions
		binding.dispose();
		assertTrue(binding.isDisposed());

		state.set("post-dispose-signal");
		TestScheduler.flush();
		assertEquals("from-widget", widgetValue[0], "Widget setter must not be called after disposal");

		widgetValue[0] = "post-dispose-widget";
		triggerWidgetChange[0].run();
		assertEquals("post-dispose-signal", state.get(), "Signal must not be updated from widget after disposal");
	}

	// ==========================================
	// Phase 18: Concrete Components
	// ==========================================

	@Test
	void textComponentUpdatesInPlaceWithoutRebuildingLabel() {
		Signal<String> labelText = Signal.of("Hello");
		Text textComp = Text.of(labelText);

		Element element = textComp.element();
		assertTrue(element instanceof Label);
		Label label = (Label) element;

		assertEquals("Hello", label.getText().toString());

		labelText.set("World");
		TestScheduler.flush();

		assertSame(element, textComp.element(), "Label instance identity must be preserved");
		assertEquals("World", label.getText().toString());

		textComp.dispose();
	}

	@Test
	void buttonComponentClickAndEnabledBinding() {
		Signal<Boolean> canClick = Signal.of(true);
		AtomicInteger clicks = new AtomicInteger(0);

		Button btn = new Button(clicks::incrementAndGet).enabled(canClick);
		arc.scene.ui.Button arcBtn = (arc.scene.ui.Button) btn.element();

		assertFalse(arcBtn.isDisabled());

		// Trigger click via ClickListener
		for (arc.scene.event.EventListener listener : arcBtn.getListeners()) {
			if (listener instanceof arc.scene.event.ClickListener) {
				arc.scene.event.InputEvent event = new arc.scene.event.InputEvent();
				((arc.scene.event.ClickListener) listener).clicked(event, 0f, 0f);
			}
		}
		assertEquals(1, clicks.get());

		// Disable
		canClick.set(false);
		TestScheduler.flush();
		assertTrue(arcBtn.isDisabled(), "Button must reflect disabled state from signal");

		btn.dispose();
	}

	@Test
	void badgeComponentCountAndVisibility() {
		Signal<Integer> count = Signal.of(3);
		Badge badge = Badge.ofCount(count);

		Element el = badge.element();
		assertTrue(el.visible);

		count.set(0);
		TestScheduler.flush();
		assertFalse(el.visible, "Badge should hide when count is 0");

		count.set(12);
		TestScheduler.flush();
		assertTrue(el.visible);

		badge.dispose();
	}
}
