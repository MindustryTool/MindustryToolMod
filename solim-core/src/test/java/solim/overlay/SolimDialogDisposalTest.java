package solim.overlay;

import static org.junit.jupiter.api.Assertions.*;

import arc.Application;
import arc.Core;
import arc.Events;
import arc.Graphics;
import arc.audio.Audio;
import arc.graphics.GL20;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Font.FontData;
import arc.graphics.g2d.TextureRegion;
import arc.mock.MockApplication;
import arc.mock.MockAudio;
import arc.mock.MockGL20;
import arc.mock.MockGraphics;
import arc.scene.Scene;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.Dialog.DialogStyle;
import arc.scene.ui.Label.LabelStyle;
import arc.scene.ui.TextButton.TextButtonStyle;
import arc.scene.ui.TextField.TextFieldStyle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import mindustry.game.EventType.ResizeEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.display.Text;
import solim.runtime.SignalDispatcher;
import solim.signal.Signal;

class SolimDialogDisposalTest {

	static Application prevApp;
	static Graphics prevGraphics;
	static GL20 prevGl;
	static GL20 prevGl20;
	static Audio prevAudio;
	static Scene prevScene;
	static boolean createdScene;

	static Font testFont() {
		FontData fontData = new FontData() {
			@Override
			public boolean hasGlyph(char ch) {
				return true;
			}
		};
		return new Font(fontData, new TextureRegion(), false);
	}

	@BeforeAll
	static void initArc() {
		prevApp = Core.app;
		prevGraphics = Core.graphics;
		prevGl = Core.gl;
		prevGl20 = Core.gl20;
		prevAudio = Core.audio;
		prevScene = Core.scene;
		if (Core.app == null) {
			Core.app = new MockApplication();
		}
		if (Core.audio == null) {
			Core.audio = new MockAudio();
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
		Font font = testFont();
		try {
			Core.scene.getStyle(ButtonStyle.class);
		} catch (IllegalArgumentException missing) {
			Core.scene.addStyle(ButtonStyle.class, new ButtonStyle());
		}
		try {
			Core.scene.getStyle(DialogStyle.class);
		} catch (IllegalArgumentException missing) {
			DialogStyle style = new DialogStyle();
			style.titleFont = font;
			Core.scene.addStyle(DialogStyle.class, style);
		}
		try {
			Core.scene.getStyle(TextButtonStyle.class);
		} catch (IllegalArgumentException missing) {
			TextButtonStyle style = new TextButtonStyle();
			style.font = font;
			Core.scene.addStyle(TextButtonStyle.class, style);
		}
		try {
			Core.scene.getStyle(LabelStyle.class);
		} catch (IllegalArgumentException missing) {
			LabelStyle style = new LabelStyle();
			style.font = font;
			Core.scene.addStyle(LabelStyle.class, style);
		}
		try {
			Core.scene.getStyle(TextFieldStyle.class);
		} catch (IllegalArgumentException missing) {
			TextFieldStyle style = new TextFieldStyle();
			style.font = font;
			Core.scene.addStyle(TextFieldStyle.class, style);
		}
	}

	@AfterAll
	static void tearDownArc() {
		Core.app = prevApp;
		Core.graphics = prevGraphics;
		Core.gl = prevGl;
		Core.gl20 = prevGl20;
		Core.audio = prevAudio;
		Core.scene = createdScene ? null : prevScene;
		prevApp = null;
		prevGraphics = null;
		prevGl = null;
		prevGl20 = null;
		prevAudio = null;
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
	void disposeHidesDialogAndMarksDisposed() {
		SolimDialog dialog = new SolimDialog("Test");
		assertFalse(dialog.isDisposed());
		assertFalse(dialog.isShown());

		AtomicBoolean hidden = new AtomicBoolean(false);
		dialog.hidden(() -> hidden.set(true));
		dialog.show();
		assertTrue(dialog.isShown());

		dialog.dispose();
		assertTrue(dialog.isDisposed());
		assertTrue(hidden.get(), "Dispose must hide the dialog");

		assertDoesNotThrow(dialog::dispose, "Double dispose must be safe");
		assertTrue(dialog.isDisposed());
	}

	@Test
	void eventListenerUnregisteredOnDispose() {
		SolimDialog dialog = new SolimDialog("Test");
		AtomicInteger calls = new AtomicInteger(0);
		dialog.listen(ResizeEvent.class, event -> calls.incrementAndGet());

		Events.fire(new ResizeEvent());
		assertEquals(1, calls.get());

		dialog.dispose();
		assertTrue(dialog.isDisposed());

		Events.fire(new ResizeEvent());
		assertEquals(1, calls.get(), "Event listener must be removed on dispose");
	}

	@Test
	void disposalIsErrorIsolated() {
		SolimDialog dialog = new SolimDialog("Test");
		AtomicBoolean cleaned = new AtomicBoolean(false);
		dialog.registerDisposable(() -> {
			throw new IllegalStateException("Simulated disposal failure");
		});
		dialog.registerDisposable(() -> cleaned.set(true));

		assertDoesNotThrow(dialog::dispose, "Disposal must not throw when a disposable fails");
		assertTrue(cleaned.get(), "Remaining disposables must run despite failures");
		assertTrue(dialog.isDisposed());
	}

	@Test
	void reactiveContentQuietsOnDispose() {
		Signal<String> label = Signal.of("a");
		Text[] holder = new Text[1];
		SolimDialog dialog = new SolimDialog("Test");
		dialog.children(() -> holder[0] = Text.of(label));
		dialog.ensureContentBuilt();
		assertEquals("a", holder[0].label().getText().toString());

		dialog.dispose();
		assertTrue(dialog.isDisposed());
		assertTrue(holder[0].isDisposed(), "Content component must dispose with the dialog");

		label.set("b");
		SignalDispatcher.flush();
		assertEquals("a", holder[0].label().getText().toString());
	}
}
