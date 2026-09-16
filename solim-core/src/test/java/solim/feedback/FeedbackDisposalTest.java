package solim.feedback;

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
import arc.scene.ui.Label;
import arc.scene.ui.Label.LabelStyle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.runtime.SignalDispatcher;
import solim.signal.Computed;
import solim.signal.Signal;

class FeedbackDisposalTest {

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

	@Test
	void badgeBindingDiesOnDispose() {
		Signal<String> source = Signal.of("a");
		Computed<String> text = source.map(v -> "[" + v + "]");
		text.peek();
		Badge badge = Badge.of(text);
		assertFalse(badge.isDisposed());

		source.set("b");
		SignalDispatcher.flush();
		assertEquals("[b]", badgeLabel(badge));

		badge.dispose();
		assertTrue(badge.isDisposed());

		source.set("c");
		SignalDispatcher.flush();
		assertEquals("[b]", badgeLabel(badge), "Badge text must freeze after dispose");

		assertDoesNotThrow(badge::dispose, "Double dispose must be safe");
		assertTrue(badge.isDisposed());
		text.dispose();
	}

	static String badgeLabel(Badge badge) {
		return ((Label) badge.table().getChildren().first()).getText().toString();
	}

	@Test
	void badgeDefaultConstructorDisposesSafely() {
		Badge badge = new Badge();
		assertFalse(badge.isDisposed());

		badge.dispose();
		assertTrue(badge.isDisposed());

		assertDoesNotThrow(badge::dispose, "Double dispose must be safe");
		assertTrue(badge.isDisposed());
	}

	@Test
	void progressBarEffectDiesOnDispose() {
		Signal<Float> progress = Signal.of(0f);
		ProgressBar bar = ProgressBar.of(progress);
		assertFalse(bar.isDisposed());

		progress.set(0.5f);
		SignalDispatcher.flush();
		int children = bar.bar().getChildren().size;

		bar.dispose();
		assertTrue(bar.isDisposed());

		progress.set(0.9f);
		SignalDispatcher.flush();
		assertEquals(children, bar.bar().getChildren().size, "Bar must freeze after dispose");

		assertDoesNotThrow(bar::dispose, "Double dispose must be safe");
		assertTrue(bar.isDisposed());
	}
}
