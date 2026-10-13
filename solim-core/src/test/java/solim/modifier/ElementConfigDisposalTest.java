package solim.modifier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.func.Cons2;
import arc.scene.Element;
import arc.scene.event.EventListener;
import arc.scene.ui.Label;
import arc.scene.ui.Tooltip;
import solim.core.OwnershipContext;
import solim.layout.Row;
import solim.reactive.Signal;
import solim.test.BuildScopeHost;
import solim.test.ModifierScope;
import solim.test.SolimEnv;

class ElementConfigDisposalTest extends SolimEnv {

    @BeforeEach
    void setUp() {

        newScene();
    }

    @Test
    void widthBindingStopsOnDispose() {
        forScope((row, scope) -> {
            Signal<Float> width = Signal.of(100f);
            row.width(width);
            Element element = row.element();
            assertEquals(100f, element.getWidth(), 0.01f, scope + ": initial width");

            width.set(140f);
            flushEffects();
            assertEquals(140f, element.getWidth(), 0.01f, scope + ": live width must follow the signal");

            row.dispose();
            assertDisposed(row, width, scope);

            width.set(220f);
            flushEffects();
            assertEquals(140f, element.getWidth(), 0.01f, scope + ": disposed width must freeze");
        });
    }

    @Test
    void heightBindingStopsOnDispose() {
        forScope((row, scope) -> {
            Signal<Float> height = Signal.of(40f);
            row.height(height);
            Element element = row.element();

            height.set(80f);
            flushEffects();
            assertEquals(80f, element.getHeight(), 0.01f, scope + ": live height must follow the signal");

            row.dispose();
            assertDisposed(row, height, scope);

            height.set(160f);
            flushEffects();
            assertEquals(80f, element.getHeight(), 0.01f, scope + ": disposed height must freeze");
        });
    }

    @Test
    void sizeBindingStopsOnDispose() {
        forScope((row, scope) -> {
            Signal<Float> size = Signal.of(32f);
            row.size(size);
            Element element = row.element();

            size.set(64f);
            flushEffects();
            assertEquals(64f, element.getWidth(), 0.01f, scope + ": live square width");
            assertEquals(64f, element.getHeight(), 0.01f, scope + ": live square height");

            row.dispose();
            assertDisposed(row, size, scope);

            size.set(96f);
            flushEffects();
            assertEquals(64f, element.getWidth(), 0.01f, scope + ": disposed square width must freeze");
            assertEquals(64f, element.getHeight(), 0.01f, scope + ": disposed square height must freeze");
        });
    }

    @Test
    void independentSizeBindingsStopOnDispose() {
        forScope((row, scope) -> {
            Signal<Float> width = Signal.of(20f);
            Signal<Float> height = Signal.of(30f);
            row.size(width, height);
            Element element = row.element();

            width.set(50f);
            height.set(60f);
            flushEffects();
            assertEquals(50f, element.getWidth(), 0.01f, scope + ": live width");
            assertEquals(60f, element.getHeight(), 0.01f, scope + ": live height");

            row.dispose();
            assertDisposed(row, width, scope);

            width.set(70f);
            height.set(90f);
            flushEffects();
            assertEquals(50f, element.getWidth(), 0.01f, scope + ": disposed width must freeze");
            assertEquals(60f, element.getHeight(), 0.01f, scope + ": disposed height must freeze");
        });
    }

    @Test
    void positionBindingsStopOnDispose() {
        forScope((row, scope) -> {
            Signal<Float> x = Signal.of(5f);
            Signal<Float> y = Signal.of(7f);
            row.position(x, y);
            Element element = row.element();

            x.set(15f);
            y.set(17f);
            flushEffects();
            assertEquals(15f, element.x, 0.01f, scope + ": live x");
            assertEquals(17f, element.y, 0.01f, scope + ": live y");

            row.dispose();
            assertDisposed(row, x, scope);
            assertDisposed(row, y, scope);

            x.set(25f);
            y.set(27f);
            flushEffects();
            assertEquals(15f, element.x, 0.01f, scope + ": disposed x must freeze");
            assertEquals(17f, element.y, 0.01f, scope + ": disposed y must freeze");
        });
    }

    @Test
    void visibleBindingStopsOnDispose() {
        forScope((row, scope) -> {
            Signal<Boolean> visible = Signal.of(true);
            row.visible(visible);
            Element element = row.element();

            visible.set(false);
            flushEffects();
            assertFalse(element.visible, scope + ": live visibility must follow the signal");

            row.dispose();
            assertDisposed(row, visible, scope);

            visible.set(true);
            flushEffects();
            assertFalse(element.visible, scope + ": disposed visibility must freeze");
        });
    }

    @Test
    void opacityAndAlphaBindingsStopOnDispose() {
        forScope((row, scope) -> {
            Signal<Float> opacity = Signal.of(0.25f);
            Signal<Float> alpha = Signal.of(0.5f);
            row.opacity(opacity).alpha(alpha);
            Element element = row.element();
            assertEquals(0.5f, element.color.a, 0.01f, scope + ": alpha wins as the later binding");

            opacity.set(0.75f);
            alpha.set(0.8f);
            flushEffects();
            assertEquals(0.8f, element.color.a, 0.01f, scope + ": live alpha must follow the signal");

            row.dispose();
            assertDisposed(row, opacity, scope);
            assertDisposed(row, alpha, scope);

            opacity.set(1f);
            alpha.set(1f);
            flushEffects();
            assertEquals(0.8f, element.color.a, 0.01f, scope + ": disposed alpha must freeze");
        });
    }

    @Test
    void reactiveTooltipStopsOnDispose() {
        forScope((row, scope) -> {
            Signal<String> tip = Signal.of("first");
            row.tooltip(tip);
            Element element = row.element();
            Label label = tooltipLabel(element);
            assertEquals("first", label.getText().toString(), scope + ": initial tooltip text");

            tip.set("second");
            flushEffects();
            assertEquals("second", label.getText().toString(), scope + ": live tooltip must follow the signal");

            row.dispose();
            assertDisposed(row, tip, scope);

            tip.set("third");
            flushEffects();
            assertEquals("second", label.getText().toString(), scope + ": disposed tooltip must freeze");
        });
    }

    @Test
    void bindingsInstalledInsideARealBuildScopeDisposeWithTheHost() {
        Signal<Float> width = Signal.of(10f);
        Signal<Boolean> visible = Signal.of(true);
        Row[] target = new Row[1];
        BuildScopeHost host = new BuildScopeHost(() -> target[0] = new Row().width(width).visible(visible));
        host.element();

        Element element = target[0].element();
        assertEquals(0, OwnershipContext.size(), "The build scope must be popped once host.element() returned");
        assertEquals(10f, element.getWidth(), 0.01f);

        width.set(20f);
        visible.set(false);
        flushEffects();
        assertEquals(20f, element.getWidth(), 0.01f, "Build-scoped width must follow the signal");
        assertFalse(element.visible, "Build-scoped visibility must follow the signal");

        host.dispose();
        assertTrue(target[0].isDisposed(), "The build-scoped child must dispose with its host");
        assertEquals(0, width.observerCount(), "Disposing the host must drain its modifier observers");
        assertEquals(0, visible.observerCount(), "Disposing the host must drain its modifier observers");

        width.set(30f);
        visible.set(true);
        flushEffects();
        assertEquals(20f, element.getWidth(), 0.01f, "Host disposal must freeze build-scoped width");
        assertFalse(element.visible, "Host disposal must freeze build-scoped visibility");
    }

    private void forScope(Cons2<Row, ModifierScope> scenario) {
        for (ModifierScope scope : ModifierScope.values()) {
            Row row = new Row();
            scope.inScope(row, created -> scenario.get(created, scope));
            assertEquals(0, OwnershipContext.size(), scope + ": the scenario must leave no ownership scope open");
        }
    }

    private void assertDisposed(Row row, Signal<?> signal, ModifierScope scope) {
        assertTrue(row.isDisposed(), scope + ": the component must report itself disposed");
        assertEquals(0, signal.observerCount(), scope + ": disposal must drain the bound signal's observers");
        assertDoesNotThrow(row::dispose, scope + ": double dispose must be safe");
    }

    private static Label tooltipLabel(Element element) {
        for (EventListener listener : element.getListeners()) {
            if (listener instanceof Tooltip) {
                return (Label) ((Tooltip) listener).container.getChildren().first();
            }
        }
        throw new AssertionError("No Tooltip listener attached to " + element);
    }
}
