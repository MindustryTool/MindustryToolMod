package solim.input;

import static org.junit.jupiter.api.Assertions.*;

import arc.Application;
import arc.Core;
import arc.Graphics;
import arc.graphics.GL20;
import arc.mock.MockApplication;
import arc.mock.MockGL20;
import arc.mock.MockGraphics;
import arc.scene.Scene;
import arc.scene.event.ChangeListener;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.CheckBox.CheckBoxStyle;
import arc.scene.ui.Label.LabelStyle;
import arc.scene.ui.Slider.SliderStyle;
import arc.scene.ui.TextButton.TextButtonStyle;
import arc.scene.ui.TextField.TextFieldStyle;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Font.FontData;
import arc.graphics.g2d.TextureRegion;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.reactive.Signal;
import solim.reactive.TwoWayBinding;
import solim.runtime.SignalDispatcher;

class TwoWayBindingDisposalTest {

	static Application prevApp;
	static Graphics prevGraphics;
	static GL20 prevGl;
	static GL20 prevGl20;
	static Scene prevScene;
	static boolean createdScene;

	@SuppressWarnings({"rawtypes", "unchecked"})
	static void addStyleIfMissing(Class type, Object style) {
		try {
			Core.scene.getStyle(type);
		} catch (IllegalArgumentException missing) {
			Core.scene.addStyle(type, style);
		}
	}

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
		addStyleIfMissing(ButtonStyle.class, new ButtonStyle());
		addStyleIfMissing(SliderStyle.class, new SliderStyle());
		addFontBackedStyles();
	}

	static Font testFont() {
		FontData fontData = new FontData() {
			@Override
			public boolean hasGlyph(char ch) {
				return true;
			}
		};
		return new Font(fontData, new TextureRegion(), false);
	}

	static void addFontBackedStyles() {
		Font font = testFont();
		try {
			Core.scene.getStyle(CheckBoxStyle.class);
		} catch (IllegalArgumentException missing) {
			CheckBoxStyle style = new CheckBoxStyle();
			style.font = font;
			Core.scene.addStyle(CheckBoxStyle.class, style);
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
		addTextFieldStyleIfMissing(font);
	}

	static void addTextFieldStyleIfMissing(Font font) {
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

	static final class FakeWidget {
		String value = "";
		final List<Runnable> listeners = new ArrayList<>();
	}

	static void fireAll(FakeWidget widget) {
		for (Runnable listener : new ArrayList<>(widget.listeners)) {
			listener.run();
		}
	}

	@Test
	void seversBothDirectionsAndRunsListenerCleanup() {
		Signal<String> signal = Signal.of("a");
		FakeWidget widget = new FakeWidget();
		widget.value = "a";
		AtomicBoolean cleanupRan = new AtomicBoolean(false);
		TwoWayBinding<String> binding = new TwoWayBinding<>(
			signal,
			() -> widget.value,
			v -> widget.value = v,
			onChange -> {
				widget.listeners.add(onChange);
				return () -> {
					cleanupRan.set(true);
					widget.listeners.remove(onChange);
				};
			});
		assertFalse(binding.isDisposed());

		signal.set("b");
		SignalDispatcher.flush();
		assertEquals("b", widget.value, "Signal to widget must flow while bound");

		widget.value = "c";
		fireAll(widget);
		assertEquals("c", signal.peek(), "Widget to signal must flow while bound");

		binding.dispose();
		assertTrue(binding.isDisposed());
		assertTrue(cleanupRan.get(), "Listener cleanup must run on dispose");

		signal.set("d");
		SignalDispatcher.flush();
		assertEquals("c", widget.value, "Signal to widget must stop after dispose");

		widget.value = "e";
		fireAll(widget);
		assertEquals("d", signal.peek(), "Widget to signal must stop after dispose");

		assertDoesNotThrow(binding::dispose, "Double dispose must be safe");
		assertTrue(binding.isDisposed());
	}

	@Test
	void sliderSeversBothDirections() {
		Signal<Float> value = Signal.of(0.3f);
		SolimSlider slider = new SolimSlider(value, 0f, 1f, 0.1f);
		assertFalse(slider.isDisposed());

		slider.slider().setValue(0.7f);
		slider.slider().fire(new ChangeListener.ChangeEvent());
		assertEquals(0.7f, value.get(), 0.0001f);

		slider.dispose();
		assertTrue(slider.isDisposed());

		value.set(0.2f);
		SignalDispatcher.flush();
		assertEquals(0.7f, slider.slider().getValue(), 0.0001f, "Signal to widget must stop");

		slider.slider().setValue(0.9f);
		slider.slider().fire(new ChangeListener.ChangeEvent());
		assertEquals(0.2f, value.get(), 0.0001f, "Widget to signal must stop");

		assertDoesNotThrow(slider::dispose, "Double dispose must be safe");
		slider.dispose();
	}

	@Test
	void checkboxSeversBothDirections() {
		Signal<Boolean> enabled = Signal.of(false);
		Checkbox checkbox = new Checkbox("Enable", enabled);
		assertFalse(checkbox.isDisposed());

		checkbox.checkBox().setChecked(true);
		checkbox.checkBox().fire(new ChangeListener.ChangeEvent());
		assertTrue(enabled.peek());

		checkbox.dispose();
		assertTrue(checkbox.isDisposed());

		enabled.set(false);
		SignalDispatcher.flush();
		assertTrue(checkbox.checkBox().isChecked(), "Signal to widget must stop");

		checkbox.checkBox().setChecked(false);
		checkbox.checkBox().fire(new ChangeListener.ChangeEvent());
		assertFalse(enabled.peek(), "Widget to signal must stop");

		assertDoesNotThrow(checkbox::dispose, "Double dispose must be safe");
		checkbox.dispose();
	}

	@Test
	void switchSeversBothDirections() {
		Signal<Boolean> on = Signal.of(false);
		Switch switchWidget = new Switch(on);
		assertFalse(switchWidget.isDisposed());

		on.set(true);
		SignalDispatcher.flush();
		assertEquals("ON", switchWidget.button().getText().toString(), "Signal to widget must flow while bound");

		// Manual fire reliably dispatches the installer's change listener (programmatic
		// toggle() does not raise change events in this Arc version). The installer
		// syncs state without touching text, so only the signal is asserted here.
		switchWidget.button().fire(new ChangeListener.ChangeEvent());
		assertFalse(on.peek(), "Widget toggle must reach the signal while bound");
		SignalDispatcher.flush();

		switchWidget.dispose();
		assertTrue(switchWidget.isDisposed());

		// Text is frozen at the last signal-driven value; the installer never writes
		// text itself, so post-dispose signal changes must leave it untouched.
		on.set(false);
		SignalDispatcher.flush();
		assertEquals("ON", switchWidget.button().getText().toString(), "Signal to widget must stop");

		// Pre-fire state matches the signal, so an undisposed binding would write;
		// staying false proves the disposed guard holds.
		on.set(false);
		switchWidget.button().fire(new ChangeListener.ChangeEvent());
		assertFalse(on.peek(), "Widget to signal must stop");

		assertDoesNotThrow(switchWidget::dispose, "Double dispose must be safe");
		switchWidget.dispose();
	}

	@Test
	void selectSeversBothDirections() {
		Signal<String> selection = Signal.of("A");
		List<String> options = Arrays.asList("A", "B", "C");
		SolimSelect<String> select = new SolimSelect<>(selection, options);
		assertFalse(select.isDisposed());

		selection.set("B");
		SignalDispatcher.flush();
		assertEquals("B", select.selectBox().getText().toString(), "Signal to widget must flow while bound");

		select.selectBox().fire(new ChangeListener.ChangeEvent());
		assertEquals("C", selection.peek(), "Widget cycle must reach the signal while bound");
		SignalDispatcher.flush();

		select.dispose();
		assertTrue(select.isDisposed());

		selection.set("A");
		SignalDispatcher.flush();
		assertEquals("B", select.selectBox().getText().toString(), "Signal to widget must stop");

		select.selectBox().fire(new ChangeListener.ChangeEvent());
		assertEquals("A", selection.peek(), "Widget to signal must stop");

		assertDoesNotThrow(select::dispose, "Double dispose must be safe");
		select.dispose();
	}

	@Test
	void textFieldSeversBothDirections() {
		Signal<String> input = Signal.of("a");
		TextFieldStyle fieldStyle = new TextFieldStyle();
		fieldStyle.font = testFont();
		SolimTextField field = new SolimTextField(input, fieldStyle);
		assertFalse(field.isDisposed());

		field.field().setText("b");
		field.field().fire(new ChangeListener.ChangeEvent());
		assertEquals("b", input.peek());

		field.dispose();
		assertTrue(field.isDisposed());

		input.set("c");
		SignalDispatcher.flush();
		assertEquals("b", field.field().getText(), "Signal to widget must stop");

		field.field().setText("d");
		field.field().fire(new ChangeListener.ChangeEvent());
		assertEquals("c", input.peek(), "Widget to signal must stop");

		assertDoesNotThrow(field::dispose, "Double dispose must be safe");
		field.dispose();
	}
}
