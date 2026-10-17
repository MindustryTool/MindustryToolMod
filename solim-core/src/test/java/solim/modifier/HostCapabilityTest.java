package solim.modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static solim.modifier.Values.gap;
import static solim.modifier.Values.left;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.top;

import arc.scene.Element;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import solim.core.Component;
import solim.core.HostCapability;
import solim.display.Badge;
import solim.display.NetworkImage;
import solim.display.SolimImage;
import solim.display.Text;
import solim.display.video.SolimVideo;
import solim.input.Button;
import solim.input.Checkbox;
import solim.input.SolimSelect;
import solim.input.SolimSlider;
import solim.input.SolimTextField;
import solim.input.Switch;
import solim.layout.Card;
import solim.layout.Column;
import solim.layout.Divider;
import solim.layout.Grid;
import solim.layout.ReactiveGrid;
import solim.layout.Row;
import solim.layout.Scroll;
import solim.layout.SolimCollapser;
import solim.layout.SolimStack;
import solim.layout.Spacer;
import solim.layout.Tabs;
import solim.layout.VirtualList;
import solim.layout.Wrap;
import solim.overlay.Hud;
import solim.overlay.Popup;
import solim.overlay.SolimDialog;
import solim.reactive.Dynamic;
import solim.reactive.ForEach;
import solim.reactive.QueryView;
import solim.reactive.When;
import solim.test.SolimEnv;

public class HostCapabilityTest extends SolimEnv {

    @Test
    void hostCapabilityHierarchyIsWellFormed() {
        assertTrue(HostCapability.class.isAssignableFrom(ElementCapability.class));
        assertTrue(ElementCapability.class.isAssignableFrom(TableCapability.class));
        assertTrue(HostCapability.class.isAssignableFrom(CellCapability.class));
        assertTrue(HostCapability.class.isAssignableFrom(ContainerCapability.class));
        assertTrue(HostCapability.class.isAssignableFrom(Component.class));
    }

    @Test
    void tableContainersDeclareTableAndContainerCapabilities() {
        List<Class<?>> tableContainers = Arrays.asList(
                Column.class,
                Row.class,
                Card.class,
                Grid.class,
                ReactiveGrid.class,
                Wrap.class,
                Tabs.class,
                Scroll.class,
                SolimCollapser.class,
                Button.class,
                VirtualList.class,
                Popup.class,
                Hud.class,
                SolimDialog.class,
                Dynamic.class,
                When.class,
                ForEach.class,
                QueryView.class
        );

        for (Class<?> clazz : tableContainers) {
            assertTrue(TableCapability.class.isAssignableFrom(clazz), clazz.getSimpleName() + " must implement TableCapability");
            assertTrue(CellCapability.class.isAssignableFrom(clazz), clazz.getSimpleName() + " must implement CellCapability");
            assertTrue(ContainerCapability.class.isAssignableFrom(clazz), clazz.getSimpleName() + " must implement ContainerCapability");
            assertTrue(AlignmentCapability.class.isAssignableFrom(clazz), clazz.getSimpleName() + " must implement AlignmentCapability");
        }
    }

    @Test
    void stackDeclaresContainerAndElementCapabilityWithoutTableCapability() {
        assertTrue(ElementCapability.class.isAssignableFrom(SolimStack.class));
        assertTrue(CellCapability.class.isAssignableFrom(SolimStack.class));
        assertTrue(ContainerCapability.class.isAssignableFrom(SolimStack.class));
        assertTrue(AlignmentCapability.class.isAssignableFrom(SolimStack.class));
        assertFalse(TableCapability.class.isAssignableFrom(SolimStack.class));
    }

    @Test
    void badgeDeclaresTableCapabilityWithoutContainerCapability() {
        assertTrue(TableCapability.class.isAssignableFrom(Badge.class));
        assertTrue(CellCapability.class.isAssignableFrom(Badge.class));
        assertTrue(AlignmentCapability.class.isAssignableFrom(Badge.class));
        assertFalse(ContainerCapability.class.isAssignableFrom(Badge.class));
    }

    @Test
    void leavesDeclareElementCapabilityWithoutContainerCapability() {
        List<Class<?>> leaves = Arrays.asList(
                Text.class,
                SolimImage.class,
                Divider.class,
                Spacer.class,
                SolimTextField.class,
                SolimSlider.class,
                Switch.class,
                Checkbox.class,
                SolimSelect.class,
                NetworkImage.class,
                SolimVideo.class
        );

        for (Class<?> leaf : leaves) {
            assertTrue(ElementCapability.class.isAssignableFrom(leaf), leaf.getSimpleName() + " must implement ElementCapability");
            assertTrue(CellCapability.class.isAssignableFrom(leaf), leaf.getSimpleName() + " must implement CellCapability");
            assertTrue(AlignmentCapability.class.isAssignableFrom(leaf), leaf.getSimpleName() + " must implement AlignmentCapability");
            assertFalse(ContainerCapability.class.isAssignableFrom(leaf), leaf.getSimpleName() + " must not implement ContainerCapability");
            assertFalse(TableCapability.class.isAssignableFrom(leaf), leaf.getSimpleName() + " must not implement TableCapability");
        }
    }

    @Test
    void leavesDoNotExposeChildrenMethod() {
        List<Class<?>> leaves = Arrays.asList(
                Text.class,
                SolimImage.class,
                Badge.class,
                Divider.class,
                Spacer.class,
                SolimTextField.class,
                SolimSlider.class,
                Switch.class,
                Checkbox.class,
                SolimSelect.class,
                NetworkImage.class,
                SolimVideo.class
        );

        for (Class<?> leaf : leaves) {
            boolean hasChildrenDsl = false;
            for (Method method : leaf.getMethods()) {
                if ("children".equals(method.getName()) && method.getParameterTypes().length == 1
                        && Runnable.class.isAssignableFrom(method.getParameterTypes()[0])) {
                    hasChildrenDsl = true;
                    break;
                }
            }
            assertFalse(hasChildrenDsl, leaf.getSimpleName() + " must not declare or expose children(Runnable)");
        }
    }

    @Test
    void stackMountsChildrenWithoutTableCapability() {
        SolimStack comp = new SolimStack().children(() -> {
            new Text("inside-stack");
        });

        assertNull(comp.table());
        Element el = comp.element();
        assertNotNull(el);
        assertTrue(comp.stack().getChildren().size > 0);
        assertTrue(comp.stack().getChildren().first() instanceof Label);
    }

    @Test
    void tableContainersApplyTableProps() {
        Column col = new Column()
                .props(gap(12f), padding(8f))
                .children(() -> {
                    new Text("first");
                    new Text("second");
                });

        col.element();
        Table table = col.table();
        assertNotNull(table);
        assertEquals(8f, table.getMarginTop(), 0.01f);
        assertEquals(8f, table.getMarginBottom(), 0.01f);
        assertEquals(8f, table.getMarginLeft(), 0.01f);
        assertEquals(8f, table.getMarginRight(), 0.01f);
    }

    @Test
    void alignmentDispatchComposesCellMask() {
        Column col = new Column();
        Table hostTable = new Table();
        Cell<?> cell = hostTable.add(col.element());

        col.props(top(), left());

        PendingCellConfig config = col.cellConfig();
        int expectedMask = Align.top | Align.left;
        assertEquals(expectedMask, config.align);
        assertEquals(expectedMask, CellAccess.align(cell));
    }
}
