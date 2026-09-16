package solim.layout;

import static org.junit.jupiter.api.Assertions.*;

import arc.Application;
import arc.Core;
import arc.Graphics;
import arc.graphics.GL20;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Font.FontData;
import arc.graphics.g2d.TextureRegion;
import arc.mock.MockApplication;
import arc.mock.MockGL20;
import arc.mock.MockGraphics;
import arc.scene.Scene;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.Label.LabelStyle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.display.Text;
import solim.input.Button;
import solim.runtime.SignalDispatcher;
import solim.signal.Signal;

class TabsDisposalTest {

	static Application prevApp;
	static Graphics prevGraphics;
	static GL20 prevGl;
	static GL20 prevGl20;
	static Scene prevScene;
	static boolean createdScene;

	@BeforeAll
	static void initArc() {
		prevApp = Core.app;
		prevGraphics = Core.graphics;
		prevGl = Core.gl;
		prevGl20 = Core.gl20;
		prevScene = Core.scene;
		if (Core.app == null) {
			Core.app = new MockApplication();
		}
		if (Core.graphics == null) {
			Core.graphics = new MockGraphics();
		}
		if (Core.gl == null) {
			Core.gl = new MockGL20();
			Core.gl20 = (MockGL20) Core.gl;
		}
		if (Core.scene == null) {
			Core.scene = new Scene();
			createdScene = true;
		}
		try {
			Core.scene.getStyle(ButtonStyle.class);
		} catch (IllegalArgumentException missing) {
			Core.scene.addStyle(ButtonStyle.class, new ButtonStyle());
		}
		try {
			Core.scene.getStyle(LabelStyle.class);
		} catch (IllegalArgumentException missing) {
			LabelStyle style = new LabelStyle();
			FontData fontData = new FontData() {
				@Override
				public boolean hasGlyph(char ch) {
					return true;
				}
			};
			style.font = new Font(fontData, new TextureRegion(), false);
			Core.scene.addStyle(LabelStyle.class, style);
		}
	}

	@AfterAll
	static void tearDownArc() {
		Core.app = prevApp;
		Core.graphics = prevGraphics;
		Core.gl = prevGl;
		Core.gl20 = prevGl20;
		Core.scene = createdScene ? null : prevScene;
		prevApp = null;
		prevGraphics = null;
		prevGl = null;
		prevGl20 = null;
		prevScene = null;
		createdScene = false;
	}

	@BeforeEach
	void setUp() {
		SignalDispatcher.resetForTests();
	}

	@AfterEach
	void tearDown() {
		SignalDispatcher.resetForTests();
	}

	static Tabs twoTabFixture(Signal<Integer> active, Signal<String> label) {
		Tabs tabs = new Tabs(active);
		tabs.tab("One", () -> {
			Text.of(label);
		});
		tabs.tab("Two", () -> {
			Text.of("static");
		});
		return tabs;
	}

	@Test
	void tabSwitchReactsWhileAlive() {
		Signal<Integer> active = Signal.of(0);
		Signal<String> label = Signal.of("a");
		Tabs tabs = twoTabFixture(active, label);
		assertFalse(tabs.isDisposed());

		assertTrue(tabs.contents().get(0).visible);
		assertFalse(tabs.contents().get(1).visible);

		active.set(1);
		SignalDispatcher.flush();
		assertFalse(tabs.contents().get(0).visible);
		assertTrue(tabs.contents().get(1).visible);
		tabs.dispose();
	}

	@Test
	void disposeDisposesButtonsHeaderAndContentEffects() {
		Signal<Integer> active = Signal.of(0);
		Signal<String> label = Signal.of("a");
		Tabs tabs = twoTabFixture(active, label);

		active.set(1);
		SignalDispatcher.flush();
		assertTrue(tabs.contents().get(1).visible);

		tabs.dispose();
		assertTrue(tabs.isDisposed());
		for (Button button : tabs.buttons()) {
			assertTrue(button.isDisposed(), "Tab button must dispose with Tabs");
		}

		active.set(0);
		SignalDispatcher.flush();
		assertTrue(tabs.contents().get(1).visible, "Content effects must stop after dispose");
		assertFalse(tabs.contents().get(0).visible);

		label.set("b");
		SignalDispatcher.flush();

		assertDoesNotThrow(tabs::dispose, "Double dispose must be safe");
		assertTrue(tabs.isDisposed());
	}
}
