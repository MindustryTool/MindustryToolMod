package solim.modifier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import arc.func.Cons2;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import solim.core.OwnershipContext;
import solim.layout.Row;
import solim.reactive.Signal;
import solim.test.BuildScopeHost;
import solim.test.ModifierScope;
import solim.test.SolimEnv;

/**
 * Method-by-method disposal proof for the reactive {@link CellConfig}
 * constraints.
 *
 * <p>
 * Each constraint is applied to the component's parent {@link Cell}, so the
 * proof reads the
 * cell back after disposal: a leaked effect would keep writing
 * {@code Cell.minWidth()} and friends
 * long after the component that configured them is gone.
 *
 * <p>
 * The parent is a plain Arc {@link Table}, not a gap container, so margins land
 * on
 * {@code Cell.pad*} where they are observable.
 */
class CellConfigDisposalTest extends SolimEnv {

    @Test
    void minimumSizeBindingsStopOnDispose() {
        forScope((target, scope) -> {
            Signal<Float> minWidth = Signal.of(50f);
            Signal<Float> minHeight = Signal.of(30f);
            target.component.minWidth(minWidth).minHeight(minHeight);
            assertEquals(50f, CellAccess.minWidth(target.cell), 0.01f, scope + ": initial min width");
            assertEquals(30f, CellAccess.minHeight(target.cell), 0.01f, scope + ": initial min height");

            minWidth.set(75f);
            minHeight.set(45f);
            flushEffects();
            assertEquals(75f, CellAccess.minWidth(target.cell), 0.01f, scope + ": live min width");
            assertEquals(45f, CellAccess.minHeight(target.cell), 0.01f, scope + ": live min height");

            target.component.dispose();
            assertDisposed(target, minWidth, scope);

            minWidth.set(95f);
            minHeight.set(85f);
            flushEffects();
            assertEquals(75f, CellAccess.minWidth(target.cell), 0.01f, scope + ": disposed min width must freeze");
            assertEquals(45f, CellAccess.minHeight(target.cell), 0.01f,
                    scope + ": disposed min height must freeze");
        });
    }

    @Test
    void maximumSizeBindingsStopOnDispose() {
        forScope((target, scope) -> {
            Signal<Float> maxWidth = Signal.of(200f);
            Signal<Float> maxHeight = Signal.of(150f);
            target.component.maxWidth(maxWidth).maxHeight(maxHeight);
            assertEquals(200f, CellAccess.maxWidth(target.cell), 0.01f, scope + ": initial max width");
            assertEquals(150f, CellAccess.maxHeight(target.cell), 0.01f, scope + ": initial max height");

            maxWidth.set(260f);
            maxHeight.set(240f);
            flushEffects();
            assertEquals(260f, CellAccess.maxWidth(target.cell), 0.01f, scope + ": live max width");
            assertEquals(240f, CellAccess.maxHeight(target.cell), 0.01f, scope + ": live max height");

            target.component.dispose();
            assertDisposed(target, maxWidth, scope);

            maxWidth.set(300f);
            maxHeight.set(280f);
            flushEffects();
            assertEquals(260f, CellAccess.maxWidth(target.cell), 0.01f, scope + ": disposed max width must freeze");
            assertEquals(240f, CellAccess.maxHeight(target.cell), 0.01f,
                    scope + ": disposed max height must freeze");
        });
    }

    @Test
    void marginBindingStopsOnDispose() {
        forScope((target, scope) -> {
            Signal<Float> margin = Signal.of(4f);
            target.component.margin(margin);
            assertPads(target.cell, 4f, scope + ": initial margin");

            margin.set(9f);
            flushEffects();
            assertPads(target.cell, 9f, scope + ": live margin");

            target.component.dispose();
            assertDisposed(target, margin, scope);

            margin.set(20f);
            flushEffects();
            assertPads(target.cell, 9f, scope + ": disposed margin must freeze");
        });
    }

    @Test
    void perEdgeMarginBindingsStopOnDispose() {
        forScope((target, scope) -> {
            Signal<Float> top = Signal.of(1f);
            Signal<Float> bottom = Signal.of(2f);
            Signal<Float> left = Signal.of(3f);
            Signal<Float> right = Signal.of(4f);
            target.component.marginTop(top).marginBottom(bottom).marginLeft(left).marginRight(right);
            assertPads(target.cell, 3f, 1f, 2f, 4f, scope + ": initial per-edge margins");

            top.set(11f);
            bottom.set(12f);
            left.set(13f);
            right.set(14f);
            flushEffects();
            assertPads(target.cell, 13f, 11f, 12f, 14f, scope + ": live per-edge margins");

            target.component.dispose();
            assertDisposed(target, top, scope);
            assertDisposed(target, bottom, scope);
            assertDisposed(target, left, scope);
            assertDisposed(target, right, scope);

            top.set(21f);
            bottom.set(22f);
            left.set(23f);
            right.set(24f);
            flushEffects();
            assertPads(target.cell, 13f, 11f, 12f, 14f, scope + ": disposed per-edge margins must freeze");
        });
    }

    @Test
    void axisMarginBindingsStopOnDispose() {
        forScope((target, scope) -> {
            Signal<Float> horizontal = Signal.of(6f);
            Signal<Float> vertical = Signal.of(7f);
            target.component.marginX(horizontal).marginY(vertical);
            assertPads(target.cell, 6f, 7f, 7f, 6f, scope + ": initial axis margins");

            horizontal.set(16f);
            vertical.set(17f);
            flushEffects();
            assertPads(target.cell, 16f, 17f, 17f, 16f, scope + ": live axis margins");

            target.component.dispose();
            assertDisposed(target, horizontal, scope);
            assertDisposed(target, vertical, scope);

            horizontal.set(26f);
            vertical.set(27f);
            flushEffects();
            assertPads(target.cell, 16f, 17f, 17f, 16f, scope + ": disposed axis margins must freeze");
        });
    }

    @Test
    void bindingsInstalledInsideARealBuildScopeDisposeWithTheHost() {
        Signal<Float> minWidth = Signal.of(20f);
        Signal<Float> marginTop = Signal.of(2f);
        Table parent = new Table();
        Target[] target = new Target[1];
        BuildScopeHost host = new BuildScopeHost(() -> {
            Row row = new Row();
            target[0] = new Target(row, parent.add(row.element()));
            row.minWidth(minWidth).marginTop(marginTop);
        });
        host.element();
        Target built = target[0];

        assertEquals(0, OwnershipContext.size(), "The build scope must be popped once host.element() returned");
        minWidth.set(40f);
        marginTop.set(6f);
        flushEffects();
        assertEquals(40f, CellAccess.minWidth(built.cell), 0.01f, "Build-scoped min width must follow the signal");
        assertEquals(6f, CellAccess.padTop(built.cell), 0.01f, "Build-scoped margin must follow the signal");

        host.dispose();
        assertTrue(built.component.isDisposed(), "The build-scoped child must dispose with its host");
        assertEquals(0, minWidth.observerCount(), "Disposing the host must drain its modifier observers");
        assertEquals(0, marginTop.observerCount(), "Disposing the host must drain its modifier observers");

        minWidth.set(80f);
        marginTop.set(12f);
        flushEffects();
        assertEquals(40f, CellAccess.minWidth(built.cell), 0.01f, "Host disposal must freeze build-scoped min width");
        assertEquals(6f, CellAccess.padTop(built.cell), 0.01f, "Host disposal must freeze build-scoped margin");
    }

    // ---------- helpers ----------

    private static final class Target {
        final Row component;
        final Cell<?> cell;

        Target(Row component, Cell<?> cell) {
            this.component = component;
            this.cell = cell;
        }
    }

    private void forScope(Cons2<Target, ModifierScope> scenario) {
        for (ModifierScope scope : ModifierScope.values()) {
            Target target = parented();
            scope.inScope(target.component, created -> scenario.get(target, scope));
            assertEquals(0, OwnershipContext.size(), scope + ": the scenario must leave no ownership scope open");
        }
    }

    private static Target parented() {
        Table parent = new Table();
        Row row = new Row();
        return new Target(row, parent.add(row.element()));
    }

    private void assertDisposed(Target target, Signal<?> signal, ModifierScope scope) {
        assertTrue(target.component.isDisposed(), scope + ": the component must report itself disposed");
        assertEquals(0, signal.observerCount(), scope + ": disposal must drain the bound signal's observers");
        assertDoesNotThrow(target.component::dispose, scope + ": double dispose must be safe");
    }

    private static void assertPads(Cell<?> cell, float value, String message) {
        assertPads(cell, value, value, value, value, message);
    }

    private static void assertPads(Cell<?> cell, float left, float top, float bottom, float right,
            String message) {
        assertEquals(left, CellAccess.padLeft(cell), 0.01f, message + " (left)");
        assertEquals(top, CellAccess.padTop(cell), 0.01f, message + " (top)");
        assertEquals(bottom, CellAccess.padBottom(cell), 0.01f, message + " (bottom)");
        assertEquals(right, CellAccess.padRight(cell), 0.01f, message + " (right)");
    }
}
