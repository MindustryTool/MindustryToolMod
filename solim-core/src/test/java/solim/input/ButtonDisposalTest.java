package solim.input;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.Button.ButtonStyle;
import solim.core.OwnershipContext;
import solim.reactive.Signal;
import solim.test.BuildScopeHost;
import solim.test.SolimEnv;

/**
 * Disposal proof for every reactive modifier on {@link Button}.
 *
 * <p>A {@link Button} writes each binding straight into its Arc widget, so a surviving effect is
 * observable as a widget that keeps tracking a signal nobody is looking at any more. Each case
 * therefore asserts the live behaviour first, then that disposal freezes it and drains the
 * signal's observers.
 */
class ButtonDisposalTest extends SolimEnv {

	@BeforeEach
	void setUp() {
		newScene();
	}

	@Test
	void enabledBindingStopsOnDispose() {
		Signal<Boolean> enabled = Signal.of(true);
		Button button = new Button().enabled(enabled);
		assertFalse(button.button().isDisabled());

		enabled.set(false);
		flushEffects();
		assertTrue(button.button().isDisabled(), "Live enabled binding must drive the disabled state");

		button.dispose();
		assertDisposed(button, enabled);

		enabled.set(true);
		flushEffects();
		assertTrue(button.button().isDisabled(), "Disposed enabled binding must freeze");
	}

	@Test
	void checkedBindingStopsOnDispose() {
		Signal<Boolean> checked = Signal.of(false);
		Button button = new Button().checked(checked);
		assertFalse(button.button().isChecked());

		checked.set(true);
		flushEffects();
		assertTrue(button.button().isChecked(), "Live checked binding must drive the checked state");

		button.dispose();
		assertDisposed(button, checked);

		checked.set(false);
		flushEffects();
		assertTrue(button.button().isChecked(), "Disposed checked binding must freeze");
	}

	@Test
	void visibleBindingStopsOnDispose() {
		Signal<Boolean> visible = Signal.of(true);
		Button button = new Button().visible(visible);
		assertTrue(button.button().visible);

		visible.set(false);
		flushEffects();
		assertFalse(button.button().visible, "Live visible binding must drive the widget visibility");

		button.dispose();
		assertDisposed(button, visible);

		visible.set(true);
		flushEffects();
		assertFalse(button.button().visible, "Disposed visible binding must freeze");
	}

	@Test
	void styleBindingStopsOnDispose() {
		ButtonStyle first = new ButtonStyle();
		ButtonStyle second = new ButtonStyle();
		Signal<ButtonStyle> style = Signal.of(first);
		Button button = new Button().style(style);
		assertSame(first, button.button().getStyle());

		style.set(second);
		flushEffects();
		assertSame(second, button.button().getStyle(), "Live style binding must swap the Arc style");

		button.dispose();
		assertDisposed(button, style);

		style.set(first);
		flushEffects();
		assertSame(second, button.button().getStyle(), "Disposed style binding must freeze");
	}

	@Test
	void iconBindingStopsOnDispose() {
		Drawable first = new TextureRegionDrawable(new TextureRegion());
		Drawable second = new TextureRegionDrawable(new TextureRegion());
		Signal<Drawable> icon = Signal.of(first);
		Button button = new Button().icon(icon);
		Image image = firstChildImage(button);
		Drawable applied = image.getDrawable();

		icon.set(second);
		flushEffects();
		assertNotSame(applied, image.getDrawable(), "Live icon binding must replace the image drawable");

		Drawable live = image.getDrawable();
		button.dispose();
		assertDisposed(button, icon);

		icon.set(first);
		flushEffects();
		assertSame(live, image.getDrawable(), "Disposed icon binding must freeze");
	}

	@Test
	void colorBindingStopsOnDispose() {
		Signal<Color> color = Signal.of(Color.red);
		Button button = new Button().color(color);
		assertEquals(1f, button.button().color.r, 0.0001f);

		color.set(Color.blue);
		flushEffects();
		assertEquals(1f, button.button().color.b, 0.0001f, "Live color binding must tint the widget");

		button.dispose();
		assertDisposed(button, color);

		color.set(Color.black);
		flushEffects();
		assertEquals(1f, button.button().color.b, 0.0001f, "Disposed color binding must freeze");
	}

	@Test
	void gapBindingStopsOnDispose() {
		Signal<Float> gap = Signal.of(4f);
		Button button = new Button().gap(gap);
		Element first = new Element();
		Element second = new Element();
		button.children(() -> {
			button.button().add(first);
			button.button().add(second);
		});
		assertEquals(4f, CellAccess.padLeft(button.button().getCell(second)), 0.01f);

		gap.set(12f);
		flushEffects();
		assertEquals(12f, CellAccess.padLeft(button.button().getCell(second)), 0.01f,
				"Live gap binding must re-space the button");

		button.dispose();
		assertDisposed(button, gap);

		gap.set(24f);
		flushEffects();
		assertEquals(12f, CellAccess.padLeft(button.button().getCell(second)), 0.01f,
				"Disposed gap binding must freeze");
	}

	@Test
	void bindingsInstalledInsideABuildScopeDisposeWithTheHost() {
		Signal<Boolean> enabled = Signal.of(true);
		Signal<Boolean> checked = Signal.of(false);
		Button[] target = new Button[1];
		BuildScopeHost host = new BuildScopeHost(
				() -> target[0] = new Button().enabled(enabled).checked(checked));
		host.element();
		Button button = target[0];

		enabled.set(false);
		flushEffects();
		checked.set(true);
		flushEffects();
		assertTrue(button.button().isDisabled(), "Build-scoped enabled binding must follow the signal");
		assertTrue(button.button().isChecked(), "Build-scoped checked binding must follow the signal");

		host.dispose();
		assertTrue(button.isDisposed(), "The build-scoped button must dispose with its host");
		assertEquals(0, enabled.observerCount(), "Disposing the host must drain the enabled observers");
		assertEquals(0, checked.observerCount(), "Disposing the host must drain the checked observers");

		enabled.set(true);
		flushEffects();
		checked.set(false);
		flushEffects();
		assertTrue(button.button().isDisabled(), "Host disposal must freeze the enabled binding");
		assertTrue(button.button().isChecked(), "Host disposal must freeze the checked binding");
	}

	// ---------- helpers ----------

	/** Asserts the shared disposal contract: disposed, idempotent, no surviving observer. */
	private void assertDisposed(Button button, Signal<?> signal) {
		assertTrue(button.isDisposed(), "The button must report itself disposed");
		assertEquals(0, signal.observerCount(), "Disposal must drain the bound signal's observers");
		assertDoesNotThrow(button::dispose, "Double dispose must be safe");
		assertTrue(button.isDisposed());
		assertEquals(0, OwnershipContext.size(), "No ownership scope may leak");
	}

	private static Image firstChildImage(Button button) {
		Element child = button.button().getChildren().first();
		return (Image)child;
	}
}