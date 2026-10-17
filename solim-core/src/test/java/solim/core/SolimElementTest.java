package solim.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static solim.modifier.Values.align;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.visible;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.util.Align;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import solim.layout.Row;
import solim.test.SolimCoreEnv;

class SolimElementTest extends SolimCoreEnv {

    private static final class Probe extends SolimElement {
        private final Element element = new Element();

        @Override
        protected Element createElement() {
            return element;
        }
    }

    @Test
    void hostedElementIsBoundToItsComponent() {
        Row row = new Row();
        AtomicReference<Probe> holder = new AtomicReference<>();
        row.children(() -> holder.set(new Probe()));

        assertSame(holder.get(), SolimToken.getComponent(holder.get().element()));
    }

    @Test
    void cellPropsDeclaredBeforeRealizationReachTheParentCell() {
        Row row = new Row();
        AtomicReference<Probe> holder = new AtomicReference<>();
        row.children(() -> {
            Probe probe = new Probe();
            probe.props(growX(), align(Align.center));
            holder.set(probe);
        });

        Cell<?> cell = row.table().getCell(holder.get().element());
        assertEquals(1, CellAccess.expandX(cell));
        assertEquals(1f, CellAccess.fillX(cell), 0.01f);
        assertEquals(Align.center, CellAccess.align(cell));
    }

    @Test
    void elementPropsDeclaredBeforeRealizationApplyToTheElement() {
        Row row = new Row();
        AtomicReference<Probe> holder = new AtomicReference<>();
        row.children(() -> {
            Probe probe = new Probe();
            probe.props(visible(false));
            holder.set(probe);
        });

        assertFalse(holder.get().element().visible,
                "props recorded while the element was still pending must apply when it is realized");
    }
}
