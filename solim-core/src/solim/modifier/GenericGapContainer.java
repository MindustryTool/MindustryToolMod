package solim.modifier;

import arc.scene.ui.layout.Table;
import solim.layout.Direction;
import solim.layout.GapContainer;

/**
 * Package-private generic gap container for tables that don't have a dedicated
 * GapContainer implementation (Row, Column, Grid, etc.).
 */
final class GenericGapContainer implements GapContainer {
    private final Table table;
    private float gap;

    GenericGapContainer(Table table, float gap) {
        this.table = table;
        this.gap = gap;
    }

    static void setGap(Table table, float gap) {
        if (table == null) return;
        if (table.userObject instanceof GapContainer) {
            GapContainer gc = (GapContainer) table.userObject;
            if (gc instanceof solim.layout.Row) {
                ((solim.layout.Row) gc).gap(gap);
            } else if (gc instanceof solim.layout.Column) {
                ((solim.layout.Column) gc).gap(gap);
            } else if (gc instanceof solim.layout.Grid) {
                ((solim.layout.Grid) gc).gap(gap);
            } else if (gc instanceof solim.layout.Wrap) {
                ((solim.layout.Wrap) gc).gap(gap);
            } else if (gc instanceof solim.layout.Card) {
                ((solim.layout.Card) gc).gap(gap);
            } else if (gc instanceof solim.input.Button) {
                ((solim.input.Button) gc).gap(gap);
            } else if (gc instanceof GenericGapContainer) {
                ((GenericGapContainer) gc).setGap(gap);
            } else {
                gc.respace();
            }
        } else {
            GenericGapContainer ggc = new GenericGapContainer(table, gap);
            table.userObject = ggc;
            ggc.respace();
        }
        table.invalidateHierarchy();
    }

    void setGap(float gap) {
        this.gap = gap;
        respace();
    }

    @Override
    public Direction direction() {
        return Direction.HORIZONTAL;
    }

    @Override
    public float gap() {
        return gap;
    }

    @Override
    public void respace() {
        GapContainer.applySpacing(table, Direction.HORIZONTAL, gap);
    }
}
