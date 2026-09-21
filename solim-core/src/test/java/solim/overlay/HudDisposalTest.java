package solim.overlay;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.Application;
import arc.Core;
import arc.Events;
import arc.Graphics;
import arc.graphics.GL20;
import arc.mock.MockGraphics;
import mindustry.game.EventType.ResizeEvent;
import solim.reactive.Signal;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class HudDisposalTest extends SolimEnv {

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

	static Application prevApp;
	static Graphics prevGraphics;
	static GL20 prevGl;
	static GL20 prevGl20;
	static ResizableMockGraphics mockGraphics;

	@BeforeAll
	static void initCore() {
		prevApp = Core.app;
		prevGraphics = Core.graphics;
		prevGl = Core.gl;
		prevGl20 = Core.gl20;
		mockGraphics = new ResizableMockGraphics();
		Core.graphics = mockGraphics;
	}

	@AfterAll
	static void tearDownCore() {
		Core.app = prevApp;
		Core.graphics = prevGraphics;
		Core.gl = prevGl;
		Core.gl20 = prevGl20;
		prevApp = null;
		prevGraphics = null;
		prevGl = null;
		prevGl20 = null;
		mockGraphics = null;
	}

	@BeforeEach
	void setUp() {
		mockGraphics.width = 1024;
		mockGraphics.height = 768;
		SignalDispatcher.resetForTests();
	}

	@AfterEach
	void tearDown() {
		SignalDispatcher.resetForTests();
	}

	@Test
	void resizeListenerUnregisteredOnDispose() {
		Hud hud = new Hud();
		hud.element().setSize(200f, 100f);
		hud.position(800f, 600f);
		hud.keepInScreen();
		assertFalse(hud.isDisposed());

		mockGraphics.width = 800;
		mockGraphics.height = 600;
		Events.fire(new ResizeEvent());
		assertEquals(600f, hud.element().x, 0.01f, "Resize must clamp while alive");
		assertEquals(500f, hud.element().y, 0.01f);

		hud.dispose();
		assertTrue(hud.isDisposed());

		hud.position(700f, 550f);
		mockGraphics.width = 640;
		mockGraphics.height = 480;
		Events.fire(new ResizeEvent());
		assertEquals(700f, hud.element().x, 0.01f, "Resize must not react after dispose");
		assertEquals(550f, hud.element().y, 0.01f);

		assertDoesNotThrow(hud::dispose, "Double dispose must be safe");
		assertTrue(hud.isDisposed());
	}

	@Test
	void signalBindingsDieOnDispose() {
		Signal<Float> opacity = Signal.of(1f);
		Signal<Float> scale = Signal.of(1f);
		Hud hud = new Hud();
		hud.opacity(opacity);
		hud.scale(scale);
		assertFalse(hud.isDisposed());

		opacity.set(0.5f);
		scale.set(2f);
		SignalDispatcher.flush();
		assertEquals(0.5f, hud.root().color.a, 0.0001f);
		assertEquals(2f, hud.container().scaleX, 0.0001f);

		hud.dispose();
		assertTrue(hud.isDisposed());

		opacity.set(0.9f);
		scale.set(3f);
		SignalDispatcher.flush();
		assertEquals(0.5f, hud.root().color.a, 0.0001f, "Opacity must stop after dispose");
		assertEquals(2f, hud.container().scaleX, 0.0001f, "Scale must stop after dispose");

		assertDoesNotThrow(hud::dispose, "Double dispose must be safe");
		assertTrue(hud.isDisposed());
	}
}
