package solim.overlay;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.Element;
import arc.scene.event.ClickListener;
import arc.scene.event.EventListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import mindustry.game.EventType.ResizeEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.reactive.Signal;
import solim.core.Ui;
import solim.runtime.SignalDispatcher;

class HudTest {

	static class ResizableMockGraphics extends MockGraphics {
		int width = 1024;
		int height = 768;

		@Override
		public int getWidth() {
			return width;
		}

		@Override
		public int getHeight() {
			return height;
		}
	}

	private static ResizableMockGraphics mockGraphics;

	@BeforeAll
	static void initCore() {
		if (Core.app == null) {
			Core.app = new MockApplication();
		}
		mockGraphics = new ResizableMockGraphics();
		Core.graphics = mockGraphics;
	}

	@BeforeEach
	void resetScreen() {
		mockGraphics.width = 1024;
		mockGraphics.height = 768;
	}

	@Test
	void hudDefaultsAndTouchable() {
		Hud hud = Ui.hud();
		assertEquals("solim-hud-root", hud.element().name);
		assertEquals(Touchable.childrenOnly, hud.element().touchable, "HUD root should allow touch pass-through");
		assertEquals(Touchable.enabled, hud.container().touchable, "HUD container should be enabled for touches");
		hud.dispose();
	}

	@Test
	void keepInScreenClamping() {
		Hud hud = new Hud();
		hud.element().setSize(200f, 100f);

		// Position far outside bottom-right
		hud.position(1200f, 900f);
		hud.keepInScreen();

		assertEquals(824f, hud.element().x, 0.01f, "X should clamp to 1024 - 200 = 824");
		assertEquals(668f, hud.element().y, 0.01f, "Y should clamp to 768 - 100 = 668");

		// Position outside top-left
		hud.position(-50f, -30f);
		hud.keepInScreen();

		assertEquals(0f, hud.element().x, 0.01f, "X should clamp to 0");
		assertEquals(0f, hud.element().y, 0.01f, "Y should clamp to 0");

		hud.dispose();
	}

	@Test
	void resizeEventClamping() {
		Hud hud = new Hud();
		hud.element().setSize(200f, 100f);
		hud.position(800f, 600f);
		hud.keepInScreen();

		assertEquals(800f, hud.element().x, 0.01f);
		assertEquals(600f, hud.element().y, 0.01f);

		// Shrink screen viewport
		mockGraphics.width = 800;
		mockGraphics.height = 600;

		Events.fire(new ResizeEvent());

		assertEquals(600f, hud.element().x, 0.01f, "X should clamp to 800 - 200 = 600 on resize");
		assertEquals(500f, hud.element().y, 0.01f, "Y should clamp to 600 - 100 = 500 on resize");

		hud.dispose();
	}

	@Test
	void draggableMovementAndClamping() {
		Hud hud = new Hud();
		hud.element().setSize(200f, 100f);
		hud.position(100f, 100f);

		Signal<Float> xSig = Signal.of(100f);
		Signal<Float> ySig = Signal.of(100f);

		Element handle = new Element();
		hud.draggable(handle, xSig, ySig);

		InputListener dragListener = null;
		for (EventListener l : handle.getListeners()) {
			if (l instanceof InputListener) {
				dragListener = (InputListener) l;
				break;
			}
		}
		assertNotNull(dragListener, "Handle should have InputListener registered");

		InputEvent event = new InputEvent();
		dragListener.touchDown(event, 10f, 10f, 0, KeyCode.mouseLeft);

		// Drag by +50 in X and +70 in Y
		dragListener.touchDragged(event, 60f, 80f, 0);

		assertEquals(150f, hud.element().x, 0.01f, "HUD X should move by +50");
		assertEquals(170f, hud.element().y, 0.01f, "HUD Y should move by +70");
		assertEquals(150f, xSig.get(), 0.01f, "X signal should sync with HUD X");
		assertEquals(170f, ySig.get(), 0.01f, "Y signal should sync with HUD Y");

		// Drag past screen edge
		dragListener.touchDragged(event, 2000f, 2000f, 0);

		assertEquals(824f, hud.element().x, 0.01f, "HUD X should clamp to 824");
		assertEquals(668f, hud.element().y, 0.01f, "HUD Y should clamp to 668");
		assertEquals(824f, xSig.get(), 0.01f, "X signal should clamp to 824");
		assertEquals(668f, ySig.get(), 0.01f, "Y signal should clamp to 668");

		hud.dispose();
	}

	@Test
	void draggableStageCoordinates() {
		Hud hud = new Hud();
		hud.element().setSize(200f, 100f);
		hud.position(100f, 100f);

		Signal<Float> xSig = Signal.of(100f);
		Signal<Float> ySig = Signal.of(100f);

		Element handle = new Element();
		hud.draggable(handle, xSig, ySig);

		InputListener dragListener = (InputListener) handle.getListeners().first();

		InputEvent event = new InputEvent();
		event.stageX = 200f;
		event.stageY = 300f;
		dragListener.touchDown(event, 10f, 10f, 0, KeyCode.mouseLeft);

		event.stageX = 230f;
		event.stageY = 340f;
		dragListener.touchDragged(event, 40f, 50f, 0);

		assertEquals(130f, hud.element().x, 0.01f, "HUD X should move by delta stageX (+30)");
		assertEquals(140f, hud.element().y, 0.01f, "HUD Y should move by delta stageY (+40)");
		assertEquals(130f, xSig.get(), 0.01f);
		assertEquals(140f, ySig.get(), 0.01f);

		hud.dispose();
	}

	@Test
	void draggableContextHud() {
		Signal<Float> xSig = Signal.of(50f);
		Signal<Float> ySig = Signal.of(60f);

		Hud hud = Ui.hud(() -> {
			Ui.button()
					.draggable(xSig, ySig);
		});

		hud.element().setSize(100f, 50f);
		hud.position(50f, 60f);

		Element buttonEl = hud.container().getChildren().first();
		InputListener dragListener = null;
		for (EventListener l : buttonEl.getListeners()) {
			if (l instanceof InputListener && !(l instanceof ClickListener)) {
				dragListener = (InputListener) l;
				break;
			}
		}
		assertNotNull(dragListener, "Button inside hud() should automatically resolve context Hud and register listener");

		InputEvent event = new InputEvent();
		event.stageX = 100f;
		event.stageY = 100f;
		dragListener.touchDown(event, 5f, 5f, 0, KeyCode.mouseLeft);

		event.stageX = 150f;
		event.stageY = 180f;
		dragListener.touchDragged(event, 55f, 85f, 0);

		assertEquals(100f, hud.element().x, 0.01f);
		assertEquals(140f, hud.element().y, 0.01f);
		assertEquals(100f, xSig.get(), 0.01f);
		assertEquals(140f, ySig.get(), 0.01f);

		hud.dispose();
	}

	@Test
	void multipleIndependentHuds() {
		Signal<Float> x1 = Signal.of(10f);
		Signal<Float> y1 = Signal.of(20f);
		Signal<Float> x2 = Signal.of(100f);
		Signal<Float> y2 = Signal.of(200f);

		Hud hud1 = Ui.hud(() -> {
			Ui.button().draggable(x1, y1);
		});
		hud1.element().setSize(50f, 50f);
		hud1.position(10f, 20f);

		Hud hud2 = Ui.hud(() -> {
			Ui.button().draggable(x2, y2);
		});
		hud2.element().setSize(50f, 50f);
		hud2.position(100f, 200f);

		Element btn1 = hud1.container().getChildren().first();
		Element btn2 = hud2.container().getChildren().first();

		InputListener drag1 = null;
		for (EventListener l : btn1.getListeners()) {
			if (l instanceof InputListener && !(l instanceof ClickListener)) {
				drag1 = (InputListener) l;
				break;
			}
		}
		InputListener drag2 = null;
		for (EventListener l : btn2.getListeners()) {
			if (l instanceof InputListener && !(l instanceof ClickListener)) {
				drag2 = (InputListener) l;
				break;
			}
		}

		assertNotNull(drag1, "HUD 1 button should have drag listener");
		assertNotNull(drag2, "HUD 2 button should have drag listener");

		// Drag hud1 by +15, +25
		InputEvent ev1 = new InputEvent();
		ev1.stageX = 100f;
		ev1.stageY = 100f;
		drag1.touchDown(ev1, 5f, 5f, 0, KeyCode.mouseLeft);
		ev1.stageX = 115f;
		ev1.stageY = 125f;
		drag1.touchDragged(ev1, 20f, 30f, 0);

		// Verify hud1 moved and hud2 remained untouched
		assertEquals(25f, hud1.element().x, 0.01f);
		assertEquals(45f, hud1.element().y, 0.01f);
		assertEquals(100f, hud2.element().x, 0.01f);
		assertEquals(200f, hud2.element().y, 0.01f);

		// Drag hud2 by +30, +40
		InputEvent ev2 = new InputEvent();
		ev2.stageX = 200f;
		ev2.stageY = 200f;
		drag2.touchDown(ev2, 5f, 5f, 0, KeyCode.mouseLeft);
		ev2.stageX = 230f;
		ev2.stageY = 240f;
		drag2.touchDragged(ev2, 35f, 45f, 0);

		// Both reflect their independent movements
		assertEquals(25f, hud1.element().x, 0.01f);
		assertEquals(45f, hud1.element().y, 0.01f);
		assertEquals(130f, hud2.element().x, 0.01f);
		assertEquals(240f, hud2.element().y, 0.01f);

		hud1.dispose();
		hud2.dispose();
	}

	@Test
	void hudWithConsumerBuilder() {
		Signal<Float> x = Signal.of(30f);
		Signal<Float> y = Signal.of(40f);

		Hud hud = Ui.hud(h -> {
			Ui.button().draggable(h, x, y);
		});
		hud.element().setSize(50f, 50f);
		hud.position(30f, 40f);

		Element btn = hud.container().getChildren().first();
		InputListener drag = null;
		for (EventListener l : btn.getListeners()) {
			if (l instanceof InputListener && !(l instanceof ClickListener)) {
				drag = (InputListener) l;
				break;
			}
		}
		assertNotNull(drag);

		InputEvent ev = new InputEvent();
		ev.stageX = 50f;
		ev.stageY = 50f;
		drag.touchDown(ev, 5f, 5f, 0, KeyCode.mouseLeft);
		ev.stageX = 70f;
		ev.stageY = 80f;
		drag.touchDragged(ev, 25f, 35f, 0);

		assertEquals(50f, hud.element().x, 0.01f);
		assertEquals(70f, hud.element().y, 0.01f);

		hud.dispose();
	}

	@Test
	void reactiveSignalsBinding() {
		Hud hud = new Hud();
		Signal<Float> x = Signal.of(40f);
		Signal<Float> y = Signal.of(60f);
		Signal<Float> opacity = Signal.of(0.7f);

		hud.x(x).y(y).opacity(opacity);

		assertEquals(40f, hud.element().x, 0.01f);
		assertEquals(60f, hud.element().y, 0.01f);
		assertEquals(0.7f, hud.container().color.a, 0.01f);

		x.set(150f);
		y.set(250f);
		opacity.set(0.3f);
		SignalDispatcher.flush();

		assertEquals(150f, hud.element().x, 0.01f);
		assertEquals(250f, hud.element().y, 0.01f);
		assertEquals(0.3f, hud.container().color.a, 0.01f);

		hud.dispose();
	}

	@Test
	void resizeEventClampingWithSignals() {
		Hud hud = new Hud();
		hud.element().setSize(200f, 100f);
		Signal<Float> xSig = Signal.of(800f);
		Signal<Float> ySig = Signal.of(600f);
		hud.position(xSig, ySig);
		hud.keepInScreen();

		mockGraphics.width = 800;
		mockGraphics.height = 600;

		Events.fire(new ResizeEvent());

		assertEquals(600f, hud.element().x, 0.01f);
		assertEquals(500f, hud.element().y, 0.01f);
		assertEquals(600f, xSig.get(), 0.01f, "X signal should clamp on resize");
		assertEquals(500f, ySig.get(), 0.01f, "Y signal should clamp on resize");

		hud.dispose();
	}

	@Test
	void draggableTableHandleUsesEnabled() {
		Hud hud = new Hud();
		Table handle = new Table();
		assertEquals(Touchable.childrenOnly, handle.touchable,
				"Table default touchable should be childrenOnly before draggable");

		hud.draggable(handle);
		assertEquals(Touchable.enabled, handle.touchable,
				"Table handle touchable should be enabled so entire surface is draggable");
		hud.dispose();
	}

	@Test
	void draggableNonTableHandleUsesEnabled() {
		Hud hud = new Hud();
		Element handle = new Element();
		assertEquals(Touchable.enabled, handle.touchable,
				"Non-Table element default is enabled");

		hud.draggable(handle);
		assertEquals(Touchable.enabled, handle.touchable,
				"Non-Table handle touchable should be enabled");
		hud.dispose();
	}


	@Test
	void keepInScreenOversizedAndClampingBothBounds() {
		Hud hud = new Hud();
		hud.element().setSize(1200f, 900f);
		hud.position(500f, 500f);
		hud.keepInScreen();

		// Screen is 1024x768, oversized element should clamp to (0, 0)
		assertEquals(0f, hud.element().x, 0.01f);
		assertEquals(0f, hud.element().y, 0.01f);

		// Now resize element to fit screen and place near edge
		hud.element().setSize(500f, 300f);
		hud.position(800f, 600f);
		hud.keepInScreen();

		assertEquals(524f, hud.element().x, 0.01f, "X should clamp to 1024 - 500 = 524");
		assertEquals(468f, hud.element().y, 0.01f, "Y should clamp to 768 - 300 = 468");

		hud.dispose();
	}

	@Test
	void tableHandleDraggableAllowsChildButtonClicks() {
		Hud hud = new Hud();
		hud.element().setSize(200f, 100f);
		hud.position(100f, 100f);

		Signal<Float> xSig = Signal.of(100f);
		Signal<Float> ySig = Signal.of(100f);

		Table handle = new Table();
		// Add an interactive child with ClickListener (simulating a button)
		Element childBtn = new Element();
		int[] clickFired = {0};
		childBtn.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				clickFired[0]++;
			}
		});
		handle.add(childBtn);

		// Add a non-button child (simulating label/title/spacer)
		Element label = new Element();
		handle.add(label);

		hud.draggable(handle, xSig, ySig);

		// Core assertion: handle must use enabled
		assertEquals(Touchable.enabled, handle.touchable,
				"Table drag handle must use enabled so background and non-button elements are draggable");

		// Locate drag listener on handle
		InputListener dragListener = null;
		for (EventListener l : handle.getListeners()) {
			if (l instanceof InputListener && !(l instanceof ClickListener)) {
				dragListener = (InputListener) l;
				break;
			}
		}
		assertNotNull(dragListener, "Drag InputListener must be registered on the Table handle");

		// When touching childBtn, dragListener.touchDown should return false (delegating to button)
		InputEvent btnEvent = new InputEvent();
		btnEvent.targetActor = childBtn;
		assertFalse(dragListener.touchDown(btnEvent, 5f, 5f, 0, KeyCode.mouseLeft),
				"Touching a button must NOT capture drag");

		// The child button's click listener remains directly executable
		InputEvent ev = new InputEvent();
		for (EventListener l : childBtn.getListeners()) {
			if (l instanceof ClickListener) {
				((ClickListener) l).clicked(ev, 1f, 1f);
				break;
			}
		}
		assertEquals(1, clickFired[0], "Child element click listener must fire");

		// When touching label, dragListener.touchDown should return true (drags)
		InputEvent labelEvent = new InputEvent();
		labelEvent.targetActor = label;
		assertTrue(dragListener.touchDown(labelEvent, 5f, 5f, 0, KeyCode.mouseLeft),
				"Touching non-button label inside handle must initiate drag");

		// When touching empty handle background, dragListener.touchDown returns true (drags)
		InputEvent bgEvent = new InputEvent();
		bgEvent.targetActor = handle;
		assertTrue(dragListener.touchDown(bgEvent, 10f, 10f, 0, KeyCode.mouseLeft),
				"Touching empty handle background must initiate drag");

		// Drag via background moves the HUD
		dragListener.touchDragged(bgEvent, 60f, 80f, 0);
		assertEquals(150f, hud.element().x, 0.01f, "HUD must move by +50 on drag");
		assertEquals(170f, hud.element().y, 0.01f, "HUD must move by +70 on drag");

		hud.dispose();
	}

	@Test
	void makeDraggablePureClickDoesNotOverwriteSignal() {
		Hud hud = new Hud();
		hud.element().setSize(100f, 50f);
		hud.element().setPosition(200f, 100f);

		Signal<Float> xSignal = Signal.of(500f);
		Signal<Float> ySignal = Signal.of(400f);

		Table handle = new Table();
		hud.container().addChild(handle);
		Hud.makeDraggable(handle, hud, xSignal, ySignal);

		InputListener dragListener = null;
		for (EventListener l : handle.getListeners()) {
			if (l instanceof InputListener && !(l instanceof ClickListener)) {
				dragListener = (InputListener) l;
				break;
			}
		}
		assertNotNull(dragListener);

		// Touch down on handle (without dragging)
		InputEvent touchEvent = new InputEvent();
		touchEvent.targetActor = handle;
		assertTrue(dragListener.touchDown(touchEvent, 10f, 10f, 0, KeyCode.mouseLeft));

		// Touch up without dragging (pure click)
		dragListener.touchUp(touchEvent, 10f, 10f, 0, KeyCode.mouseLeft);

		// Signals must NOT be overwritten with hud.element().x/y
		assertEquals(500f, xSignal.get(), "xSignal must remain unchanged on pure click");
		assertEquals(400f, ySignal.get(), "ySignal must remain unchanged on pure click");

		// Now simulate an actual drag (> 0.5f movement)
		assertTrue(dragListener.touchDown(touchEvent, 10f, 10f, 0, KeyCode.mouseLeft));
		dragListener.touchDragged(touchEvent, 30f, 40f, 0); // moved by +20, +30
		dragListener.touchUp(touchEvent, 30f, 40f, 0, KeyCode.mouseLeft);

		// Now signals must be updated to the new position
		assertEquals(220f, xSignal.get(), 0.01f, "xSignal must be updated after drag");
		assertEquals(130f, ySignal.get(), 0.01f, "ySignal must be updated after drag");

		hud.dispose();
	}
}
