package solim.modifier;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Nullable;
import solim.core.HostCapability;

public interface ElementCapability extends HostCapability {

    Element element();

    @Nullable
    PendingCellConfig cellConfig();

    default void alignCoordinated(@Nullable Integer explicit, int named) {
        Table table = this instanceof TableCapability ? ((TableCapability) this).table() : null;
        PendingCellConfig constraints = cellConfig();
        if (table != null) {
            coordinateTable(table, constraints, explicit, named);
            return;
        }
        coordinateCell(constraints, element(), explicit, named);
    }

    static void coordinateTable(Table table, @Nullable PendingCellConfig constraints,
            @Nullable Integer explicit, int named) {
        int a;
        if (explicit != null) {
            a = explicit;
            if (constraints != null) {
                constraints.align = explicit;
            }
        } else if (named == Align.center) {
            a = Align.center;
            if (constraints != null) {
                constraints.alignCenter();
            }
        } else {
            if (constraints != null) {
                constraints.align(named);
                a = constraints.align != null ? constraints.align : named;
            } else {
                a = named;
            }
        }
        table.align(a);
        if (constraints != null) {
            constraints.applyAlignToParentCell(table);
        }
        table.defaults().align(a);
        for (arc.scene.ui.layout.Cell<?> c : table.getCells()) {
            if (c != null) {
                c.align(a);
            }
        }
    }

    static void coordinateCell(@Nullable PendingCellConfig constraints, @Nullable Element element,
            @Nullable Integer explicit, int named) {
        if (constraints == null) {
            return;
        }
        if (explicit != null) {
            constraints.align = explicit;
        } else if (named == Align.center) {
            constraints.alignCenter();
        } else {
            constraints.align(named);
        }
        constraints.applyAlignToParentCell(element);
    }
}
