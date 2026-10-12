package solim.layout;

import solim.test.FixedComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import arc.func.Prov;
import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.display.NetworkImage;
import solim.display.SolimImage;
import solim.display.Text;
import solim.input.Button;
import solim.modifier.PendingCellConfig;
import solim.reactive.Dynamic;
import solim.reactive.ForEach;
import solim.reactive.QueryView;
import solim.reactive.Signal;
import solim.reactive.When;

/**
 * Fixture utilities and component supplier providers for unified layout testing.
 */
public final class LayoutTestFixtures {

    private LayoutTestFixtures() {
    }

    public static final class ComponentAdapter {
        public final String name;
        public final Prov<Component> factory;

        public ComponentAdapter(String name, Prov<Component> factory) {
            this.name = name;
            this.factory = factory;
        }

        @Override
        public String toString() {
            return name;
        }

        public Component create() {
            return factory.get();
        }

        public static @Nullable PendingCellConfig getCellConfig(Component comp) {
            return PendingCellConfig.find(comp);
        }

        public static int getParentCellAlign(Table parent, Component comp) {
            Cell<?> cell = parent.getCell(comp.element());
            return cell != null ? CellAccess.align(cell) : 0;
        }

        public static int getInnerTableAlign(Component comp) {
            Element el = comp.element();
            if (el instanceof Table) {
                return ((Table) el).getAlign();
            }
            if (comp instanceof When) {
                return ((When) comp).container().getAlign();
            }
            if (comp instanceof Scroll) {
                return ((Scroll) comp).table().getAlign();
            }
            return 0;
        }
    }

    /** Returns representative containers implementing TableConfig & GapContainer. */
    public static List<ComponentAdapter> containers() {
        return Arrays.asList(
                new ComponentAdapter("Column", Column::new),
                new ComponentAdapter("Row", Row::new),
                new ComponentAdapter("Card", Card::new),
                new ComponentAdapter("Grid", () -> new Grid(2)),
                new ComponentAdapter("Wrap", Wrap::new),
                new ComponentAdapter("SolimCollapser", SolimCollapser::new),
                new ComponentAdapter("Scroll", Scroll::new)
        );
    }

    /** Returns reactive containers. */
    public static List<ComponentAdapter> reactiveContainers() {
        return Arrays.asList(
                new ComponentAdapter("When", () -> When.of(Signal.of(true)).thenDo(() -> new Element())),
                new ComponentAdapter("Dynamic", () -> Dynamic.of(Signal.of("item"), val -> new Element())),
                new ComponentAdapter("ForEach", () -> {
                    ForEach<String> fe = ForEach.of(Signal.of(Arrays.asList("a", "b")));
                    fe.children(item -> new FixedComponent());
                    return fe;
                }),
                new ComponentAdapter("QueryView", () -> QueryView.of(solim.reactive.Query.noKey(() -> CompletableFuture.completedFuture("data"))).data(data -> new FixedComponent()))
        );
    }

    /** Returns interactive controls and display primitives. */
    public static List<ComponentAdapter> controlsAndPrimitives() {
        return Arrays.asList(
                new ComponentAdapter("Button", Button::new),
                new ComponentAdapter("Text", () -> new Text("hello")),
                new ComponentAdapter("SolimImage", SolimImage::new),
                new ComponentAdapter("NetworkImage", () -> new NetworkImage("http://localhost/test.png"))
        );
    }

    /** Returns all components that support direct top/bottom/left/right/center chaining. */
    public static List<ComponentAdapter> allAlignableComponents() {
        List<ComponentAdapter> all = new ArrayList<>();
        all.addAll(containers());
        all.addAll(reactiveContainers());
        all.addAll(controlsAndPrimitives());
        return all;
    }
}
