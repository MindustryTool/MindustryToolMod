package solim.test;

import arc.Application;
import arc.Core;
import arc.Graphics;
import arc.Settings;
import arc.graphics.GL20;
import arc.mock.MockApplication;
import arc.mock.MockGL20;
import arc.mock.MockGraphics;
import arc.scene.Scene;
import arc.scene.ui.Label.LabelStyle;
import arc.scene.ui.TextField.TextFieldStyle;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Font.FontData;
import arc.graphics.g2d.TextureRegion;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base test environment for pure Arc-dependent tests.
 *
 * Statics are initialized idempotently once per JVM and snapshotted per class
 * ({@code @BeforeAll}) and per test ({@code @BeforeEach}); every snapshot is
 * restored at teardown. This makes reused worker JVMs safe: neither a class's
 * nor a single test's mutations of {@code Core.*} can leak into other classes
 * or modules.
 *
 * {@code Core.scene} is reset to {@code null} per test (headless fallback
 * contract). Extend {@link SolimEnv} instead for tests that exercise the
 * solim runtime (ambient state, signals, components).
 */
public class ArcTestEnv {

	// Per-class snapshot, restored in @AfterAll so class-level mutations
	// (e.g. a custom graphics mock) cannot leak into other classes/modules.
	private static Application classApp;
	private static Graphics classGraphics;
	private static GL20 classGl;
	private static GL20 classGl20;
	private static Scene classScene;
	private static Settings classSettings;

	// Per-test snapshot, restored in @AfterEach.
	private Application testApp;
	private Graphics testGraphics;
	private GL20 testGl;
	private GL20 testGl20;
	private Scene prevScene;
	private Settings prevSettings;

	@BeforeAll
	public static void initArcStatics() {
		classApp = Core.app;
		classGraphics = Core.graphics;
		classGl = Core.gl;
		classGl20 = Core.gl20;
		classScene = Core.scene;
		classSettings = Core.settings;

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
	}

	@AfterAll
	public static void restoreArcStatics() {
		Core.app = classApp;
		Core.graphics = classGraphics;
		Core.gl = classGl;
		Core.gl20 = classGl20;
		Core.scene = classScene;
		Core.settings = classSettings;
		classApp = null;
		classGraphics = null;
		classGl = null;
		classGl20 = null;
		classScene = null;
		classSettings = null;
	}

	@BeforeEach
	public void setUpArcTestEnv() {
		testApp = Core.app;
		testGraphics = Core.graphics;
		testGl = Core.gl;
		testGl20 = Core.gl20;
		prevScene = Core.scene;
		prevSettings = Core.settings;

		// Headless by default: components key fallbacks on Core.scene == null.
		// Tests needing a live scene call newScene() in their own setup.
		Core.scene = null;

		Core.settings = new Settings();
		Core.settings.clear();
	}

	@AfterEach
	public void tearDownArcTestEnv() {
		// Idempotent-safe: app/graphics/gl must never be nulled, so a repeated
		// teardown (manual + JUnit) cannot clear the initialized statics.
		if (testApp != null) {
			Core.app = testApp;
		}
		if (testGraphics != null) {
			Core.graphics = testGraphics;
		}
		if (testGl != null) {
			Core.gl = testGl;
		}
		if (testGl20 != null) {
			Core.gl20 = testGl20;
		}
		Core.scene = prevScene;
		Core.settings = prevSettings;
		testApp = null;
		testGraphics = null;
		testGl = null;
		testGl20 = null;
		prevScene = null;
		prevSettings = null;
	}

	/**
	 * Creates a fresh Scene with the minimal styles registered and installs it
	 * as {@code Core.scene}. Teardown restores the pre-setup value.
	 */
	public static Scene newScene() {
		Scene scene = new Scene();
		installMinimalStyles(scene);
		Core.scene = scene;
		return scene;
	}

	/**
	 * Registers the minimal styles required to construct basic Arc widgets in a
	 * scene. Tests needing additional styles register them locally on top of this.
	 */
	public static void installMinimalStyles(Scene scene) {
		Font font = new Font(new FontData() {
			@Override
			public boolean hasGlyph(char ch) {
				return true;
			}
		}, new TextureRegion(), false);

		try {
			scene.getStyle(LabelStyle.class);
		} catch (IllegalArgumentException missing) {
			scene.addStyle(LabelStyle.class, new LabelStyle(font, null));
		}

		try {
			scene.getStyle(TextFieldStyle.class);
		} catch (IllegalArgumentException missing) {
			TextFieldStyle style = new TextFieldStyle();
			style.font = font;
			scene.addStyle(TextFieldStyle.class, style);
		}
	}
}
