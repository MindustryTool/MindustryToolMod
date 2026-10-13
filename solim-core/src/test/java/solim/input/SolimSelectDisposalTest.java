package solim.input;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.Core;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Font.FontData;
import arc.graphics.g2d.TextureRegion;
import arc.scene.event.ChangeListener;
import arc.scene.ui.TextButton.TextButtonStyle;
import arc.scene.ui.Button.ButtonStyle;
import solim.core.OwnershipContext;
import solim.reactive.Signal;
import solim.test.BuildScopeHost;
import solim.test.SolimEnv;

/**
 * Disposal proof for the two-way binding a {@link SolimSelect} installs.
 *
 * <p>The binding owns two halves that must both die with the component: the effect that pushes the
 * signal into the widget, and the widget listener that pushes the widget back into the signal.
 * Only the second is easy to get wrong — a surviving listener keeps writing a signal whose owner
 * is gone — so detachment is asserted directly on the widget's listener list.
 */
class SolimSelectDisposalTest extends SolimEnv {

	private static final List<String> options = Arrays.asList("A", "B", "C");

	@BeforeEach
	void setUp() {
		newScene();
		// A TextButton resolves both TextButtonStyle and its ButtonStyle parent from the scene.
		Font font = new Font(new FontData() {
			@Override
			public boolean hasGlyph(char ch) {
				return true;
			}
		}, new TextureRegion(), false);
		Core.scene.addStyle(ButtonStyle.class, new ButtonStyle());
		TextButtonStyle style = new TextButtonStyle();
		style.font = font;
		Core.scene.addStyle(TextButtonStyle.class, style);
	}

	@Test
	void bindingFlowsBothWaysWhileAlive() {
		Signal<String> selection = Signal.of("A");
		SolimSelect<String> select = SolimSelect.of(selection, options);
		assertEquals("A", text(select));

		selection.set("B");
		flushEffects();
		assertEquals("B", text(select), "Signal to widget must flow while bound");

		select.selectBox().fire(new ChangeListener.ChangeEvent());
		flushEffects();
		assertEquals("C", selection.peek(), "Widget to signal must flow while bound");
	}

	@Test
	void disposeSeversBothDirectionsAndDetachesTheWidgetListener() {
		Signal<String> selection = Signal.of("A");
		SolimSelect<String> select = SolimSelect.of(selection, options);

		selection.set("B");
		flushEffects();
		assertEquals("B", text(select));

		int listenersWhileBound = select.selectBox().getListeners().size;
		select.dispose();
		assertTrue(select.isDisposed(), "The select must report itself disposed");
		assertEquals(listenersWhileBound - 1, select.selectBox().getListeners().size,
				"Disposal must unregister the widget listener that drove the binding");
		assertEquals(0, selection.observerCount(), "Disposal must drain the bound signal's observers");

		selection.set("C");
		flushEffects();
		assertEquals("B", text(select), "Signal to widget must stop after dispose");

		select.selectBox().fire(new ChangeListener.ChangeEvent());
		assertEquals("C", selection.peek(), "Widget to signal must stop after dispose");
	}

	@Test
	void disposeIsIdempotent() {
		Signal<String> selection = Signal.of("A");
		SolimSelect<String> select = SolimSelect.of(selection, options);

		select.dispose();
		assertDoesNotThrow(select::dispose, "Double dispose must be safe");
		assertDoesNotThrow(select::dispose, "Repeated dispose must stay safe");
		assertTrue(select.isDisposed());
		assertEquals(0, OwnershipContext.size(), "No ownership scope may leak");
	}

	@Test
	void selectCreatedInsideABuildScopeIsDisposedWithItsHost() {
		Signal<String> selection = Signal.of("A");
		AtomicReference<SolimSelect<String>> target = new AtomicReference<>();
		BuildScopeHost host = new BuildScopeHost(() -> target.set(SolimSelect.of(selection, options)));
		host.element();
		SolimSelect<String> select = target.get();

		selection.set("B");
		flushEffects();
		assertEquals("B", text(select), "Build-scoped select must follow the signal");

		host.dispose();
		assertTrue(select.isDisposed(), "The build-scoped select must dispose with its host");
		assertEquals(0, selection.observerCount(), "Disposing the host must drain the binding observers");

		selection.set("C");
		flushEffects();
		assertEquals("B", text(select), "Host disposal must freeze the build-scoped select");
	}

	private static String text(SolimSelect<?> select) {
		return select.selectBox().getText().toString();
	}
}