package solim.layout;

import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.modifier.PendingCellConfig;

public final class CollapseHelper {

    private CollapseHelper() {
    }

    public static void updateParentCell(Table container, boolean visible,
            @Nullable PendingCellConfig constraints) {
        Cell<?> parentCell = container.parent instanceof Table
                ? ((Table) container.parent).getCell(container)
                : null;

        if (visible) {
            container.visible = true;
            if (parentCell != null) {
                parentCell.minWidth(Float.NEGATIVE_INFINITY).minHeight(Float.NEGATIVE_INFINITY);
                parentCell.maxWidth(Float.NEGATIVE_INFINITY).maxHeight(Float.NEGATIVE_INFINITY);
                if (constraints != null) {
                    constraints.applyToCell(parentCell);
                }
            }
        } else {
            container.visible = false;
            if (parentCell != null) {
                parentCell.size(0f).pad(0f);
            }
        }

        if (container.parent instanceof Table) {
            Table parentTable = (Table) container.parent;
            if (GapContainer.isGapContainer(parentTable)) {
                GapContainer.respace(parentTable);
            }
        }
    }
}
