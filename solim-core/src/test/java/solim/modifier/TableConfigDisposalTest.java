package solim.modifier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import arc.func.Cons2;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.SolimToken;
import solim.core.OwnershipContext;
import solim.graphics.RoundedDrawable;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.test.BuildScopeHost;
import solim.test.ModifierScope;
import solim.test.OwnershipEnv;

class TableConfigDisposalTest extends OwnershipEnv {

    private final List<RoundedDrawable> drawables = new ArrayList<>();

    @AfterEach
    void disposeDrawables() {
        for (RoundedDrawable drawable : drawables) {
            drawable.dispose();
        }
        drawables.clear();
    }

    @Test
    void uniformPaddingBindingStopsOnDispose() {
        forScope((target, scope) -> {
            Signal<Float> padding = Signal.of(2f);
            target.padding(padding);
            assertMargins(target, 2f, scope + ": initial padding");

            padding.set(8f);
            flushEffects();
            assertMargins(target, 8f, scope + ": live padding");

            target.dispose();
            assertDisposed(target, padding, scope);

            padding.set(16f);
            flushEffects();
            assertMargins(target, 8f, scope + ": disposed padding must freeze");
        });
    }

    @Test
    void perEdgePaddingBindingStopsOnDispose() {
        forScope((target, scope) -> {
            Signal<Float> top = Signal.of(1f);
            Signal<Float> left = Signal.of(2f);
            Signal<Float> bottom = Signal.of(3f);
            Signal<Float> right = Signal.of(4f);
            target.padding(top, left, bottom, right);
            assertMargins(target, 1f, 2f, 3f, 4f, scope + ": initial per-edge padding");

            top.set(11f);
            left.set(12f);
            bottom.set(13f);
            right.set(14f);
            flushEffects();
            assertMargins(target, 11f, 12f, 13f, 14f, scope + ": live per-edge padding");

            target.dispose();
            assertDisposed(target, top, scope);

            top.set(21f);
            left.set(22f);
            bottom.set(23f);
            right.set(24f);
            flushEffects();
            assertMargins(target, 11f, 12f, 13f, 14f, scope + ": disposed per-edge padding must freeze");
        });
    }

    @Test
    void perEdgePaddingBindingsStopOnDispose() {
        forScope((target, scope) -> {
            Signal<Float> top = Signal.of(1f);
            Signal<Float> bottom = Signal.of(2f);
            Signal<Float> left = Signal.of(3f);
            Signal<Float> right = Signal.of(4f);
            target.paddingTop(top).paddingBottom(bottom).paddingLeft(left).paddingRight(right);
            assertMargins(target, 1f, 3f, 2f, 4f, scope + ": initial per-edge padding");

            top.set(11f);
            bottom.set(12f);
            left.set(13f);
            right.set(14f);
            flushEffects();
            assertMargins(target, 11f, 13f, 12f, 14f, scope + ": live per-edge padding");

            target.dispose();
            assertDisposed(target, top, scope);
            assertDisposed(target, bottom, scope);
            assertDisposed(target, left, scope);
            assertDisposed(target, right, scope);

            top.set(21f);
            bottom.set(22f);
            left.set(23f);
            right.set(24f);
            flushEffects();
            assertMargins(target, 11f, 13f, 12f, 14f, scope + ": disposed per-edge padding must freeze");
        });
    }

    @Test
    void axisPaddingBindingsStopOnDispose() {
        forScope((target, scope) -> {
            Signal<Float> horizontal = Signal.of(5f);
            Signal<Float> vertical = Signal.of(6f);
            target.paddingX(horizontal).paddingY(vertical);
            assertMargins(target, 6f, 5f, 6f, 5f, scope + ": initial axis padding");

            horizontal.set(15f);
            vertical.set(16f);
            flushEffects();
            assertMargins(target, 16f, 15f, 16f, 15f, scope + ": live axis padding");

            target.dispose();
            assertDisposed(target, horizontal, scope);
            assertDisposed(target, vertical, scope);

            horizontal.set(25f);
            vertical.set(26f);
            flushEffects();
            assertMargins(target, 16f, 15f, 16f, 15f, scope + ": disposed axis padding must freeze");
        });
    }

    @Test
    void gapBindingStopsOnDispose() {
        forScope((target, scope) -> {
            Element first = new Element();
            Element second = new Element();
            target.table().add(first).row();
            target.table().add(second);

            Signal<Float> gap = Signal.of(3f);
            target.gap(gap);
            assertEquals(3f, CellAccess.padLeft(target.table().getCell(second)), 0.01f,
                    scope + ": initial gap");

            gap.set(9f);
            flushEffects();
            assertEquals(9f, CellAccess.padLeft(target.table().getCell(second)), 0.01f,
                    scope + ": live gap must follow the signal");

            target.dispose();
            assertDisposed(target, gap, scope);

            gap.set(21f);
            flushEffects();
            assertEquals(9f, CellAccess.padLeft(target.table().getCell(second)), 0.01f,
                    scope + ": disposed gap must freeze");
        });
    }

    @Test
    void backgroundBindingStopsOnDispose() {
        forScope((target, scope) -> {
            Drawable first = new TextureRegionDrawable(new TextureRegion());
            Drawable second = new TextureRegionDrawable(new TextureRegion());
            Signal<Drawable> background = Signal.of(first);
            target.background(background);
            assertSame(first, target.table().getBackground(), scope + ": initial background");

            background.set(second);
            flushEffects();
            assertSame(second, target.table().getBackground(), scope + ": live background must follow the signal");

            target.dispose();
            assertDisposed(target, background, scope);

            background.set(first);
            flushEffects();
            assertSame(second, target.table().getBackground(), scope + ": disposed background must freeze");
        });
    }

    @Test
    void roundedColorBindingStopsOnDispose() {
        forScope((target, scope) -> {
            Signal<Color> color = Signal.of(Color.royal);
            target.rounded(8, color);
            RoundedDrawable rounded = roundedOf(target);
            assertEquals(Color.royal, rounded.getFillColor(), scope + ": initial rounded fill");

            color.set(Color.gold);
            flushEffects();
            assertEquals(Color.gold, rounded.getFillColor(), scope + ": live rounded fill must follow the signal");

            target.dispose();
            assertDisposed(target, color, scope);

            color.set(Color.crimson);
            flushEffects();
            assertEquals(Color.gold, rounded.getFillColor(), scope + ": disposed rounded fill must freeze");
        });
    }

    @Test
    void borderColorBindingStopsOnDispose() {
        forScope((target, scope) -> {
            Signal<Color> color = Signal.of(Color.white);
            target.border(2f, color);
            RoundedDrawable rounded = roundedOf(target);
            assertEquals(Color.white, rounded.getBorderColor(), scope + ": initial border color");

            color.set(Color.black);
            flushEffects();
            assertEquals(Color.black, rounded.getBorderColor(), scope + ": live border color must follow the signal");

            target.dispose();
            assertDisposed(target, color, scope);

            color.set(Color.red);
            flushEffects();
            assertEquals(Color.black, rounded.getBorderColor(), scope + ": disposed border color must freeze");
        });
    }

    @Test
    void bindingsInstalledInsideARealBuildScopeDisposeWithTheHost() {
        Signal<Float> padding = Signal.of(3f);
        Signal<Float> gap = Signal.of(4f);
        TableTarget[] target = new TableTarget[1];
        BuildScopeHost host = new BuildScopeHost(() -> target[0] = new TableTarget().padding(padding).gap(gap));
        host.element();
        TableTarget built = target[0];

        assertEquals(0, OwnershipContext.size(), "The build scope must be popped once host.element() returned");
        padding.set(9f);
        gap.set(14f);
        flushEffects();
        assertMargins(built, 9f, "Build-scoped padding must follow the signal");

        host.dispose();
        assertTrue(built.isDisposed(), "The build-scoped child must dispose with its host");
        assertEquals(0, padding.observerCount(), "Disposing the host must drain its modifier observers");
        assertEquals(0, gap.observerCount(), "Disposing the host must drain its modifier observers");

        padding.set(21f);
        gap.set(24f);
        flushEffects();
        assertMargins(built, 9f, "Host disposal must freeze build-scoped padding");
    }

    private static final class TableTarget extends Component
            implements Configurable<TableTarget>, TableCapability, ContainerCapability<TableTarget> {

        private final Table table;
        private final PendingCellConfig constraints = new PendingCellConfig();
        private @Nullable Props<TableTarget> pending;

        TableTarget() {
            this(new Table());
        }

        private TableTarget(Table table) {
            super(table);
            this.table = table;
            SolimToken.bind(table, this, constraints);
        }

        public TableTarget padding(@Nullable Readable<Float> padding) {
            props(Values.padding(padding));
            return this;
        }

        public TableTarget padding(@Nullable Readable<Float> top, @Nullable Readable<Float> left,
                @Nullable Readable<Float> bottom, @Nullable Readable<Float> right) {
            props(Values.padding(top, left, bottom, right));
            return this;
        }

        public TableTarget paddingTop(@Nullable Readable<Float> top) {
            props(Values.paddingTop(top));
            return this;
        }

        public TableTarget paddingBottom(@Nullable Readable<Float> bottom) {
            props(Values.paddingBottom(bottom));
            return this;
        }

        public TableTarget paddingLeft(@Nullable Readable<Float> left) {
            props(Values.paddingLeft(left));
            return this;
        }

        public TableTarget paddingRight(@Nullable Readable<Float> right) {
            props(Values.paddingRight(right));
            return this;
        }

        public TableTarget paddingX(@Nullable Readable<Float> x) {
            props(Values.paddingX(x));
            return this;
        }

        public TableTarget paddingY(@Nullable Readable<Float> y) {
            props(Values.paddingY(y));
            return this;
        }

        public TableTarget gap(@Nullable Readable<Float> gap) {
            props(Values.gap(gap));
            return this;
        }

        public TableTarget background(@Nullable Readable<Drawable> background) {
            props(Values.background(background));
            return this;
        }

        public TableTarget rounded(int radius, @Nullable Readable<Color> color) {
            props(Values.rounded(radius, color));
            return this;
        }

        public TableTarget border(float stroke, @Nullable Readable<Color> color) {
            props(Values.border(stroke, color));
            return this;
        }

        @Override
        public Table table() {
            return table;
        }

        @Override
        public PendingCellConfig cellConfig() {
            return constraints;
        }

        public TableTarget children(@Nullable Runnable block) {
            applyPendingProps();
            OwnershipContext.push(this);
            try {
                if (block != null) {
                    block.run();
                }
            } finally {
                OwnershipContext.pop();
            }
            return this;
        }

        @Override
        public @Nullable Props<TableTarget> pendingProps() {
            return pending;
        }

        @Override
        public void setPendingProps(@Nullable Props<TableTarget> props) {
            this.pending = props;
        }

        public TableTarget name(String name) {
            setName(name);
            return this;
        }

        public TableTarget self() {
            return this;
        }
    }

    private void forScope(Cons2<TableTarget, ModifierScope> scenario) {
        for (ModifierScope scope : ModifierScope.values()) {
            TableTarget target = new TableTarget();
            scope.inScope(target, created -> scenario.get(created, scope));
            assertEquals(0, OwnershipContext.size(), scope + ": the scenario must leave no ownership scope open");
        }
    }

    private void assertDisposed(TableTarget target, Signal<?> signal, ModifierScope scope) {
        assertTrue(target.isDisposed(), scope + ": the component must report itself disposed");
        assertEquals(0, signal.observerCount(), scope + ": disposal must drain the bound signal's observers");
        assertDoesNotThrow(target::dispose, scope + ": double dispose must be safe");
    }

    private RoundedDrawable roundedOf(TableTarget target) {
        RoundedDrawable rounded = (RoundedDrawable) target.table().getBackground();
        drawables.add(rounded);
        return rounded;
    }

    private void assertMargins(TableTarget target, float value, String message) {
        assertMargins(target, value, value, value, value, message);
    }

    private void assertMargins(TableTarget target, float top, float left, float bottom, float right,
            String message) {
        Table table = target.table();
        assertEquals(top, table.getMarginTop(), 0.01f, message + " (top)");
        assertEquals(left, table.getMarginLeft(), 0.01f, message + " (left)");
        assertEquals(bottom, table.getMarginBottom(), 0.01f, message + " (bottom)");
        assertEquals(right, table.getMarginRight(), 0.01f, message + " (right)");
    }
}
