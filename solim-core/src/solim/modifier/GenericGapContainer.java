package solim.modifier;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import solim.core.Component;
import solim.core.SolimToken;
import solim.layout.Direction;
import solim.layout.GapContainer;
import solim.input.Button;
import solim.layout.Card;
import solim.layout.Column;
import solim.layout.Grid;
import solim.layout.Row;
import solim.layout.Wrap;

/**
 * Package-private generic gap container for tables that don't have a dedicated
 * GapContainer implementation (Row, Column, Grid, etc.).
 */
final class GenericGapContainer implements GapContainer, Component {
    private final Table table;
    private float gap;

    GenericGapContainer(Table table, float gap) {
        this.table = table;
        this.gap = gap;
    }

    static void setGap(Table table, float gap) {
        if (table == null) return;
        GapContainer gc = GapContainer.find(table);
        if (gc != null) {
            if (gc instanceof Row) {
                ((Row) gc).gap(gap);
            } else if (gc instanceof Column) {
                ((Column) gc).gap(gap);
            } else if (gc instanceof Grid) {
                ((Grid) gc).gap(gap);
            } else if (gc instanceof Wrap) {
                ((Wrap) gc).gap(gap);
            } else if (gc instanceof Card) {
                ((Card) gc).gap(gap);
            } else if (gc instanceof Button) {
                ((Button) gc).gap(gap);
            } else if (gc instanceof GenericGapContainer) {
                ((GenericGapContainer) gc).setGap(gap);
            } else {
                gc.respace();
            }
        } else {
            GenericGapContainer ggc = new GenericGapContainer(table, gap);
            SolimToken.bind(table, ggc);
            ggc.respace();
        }
        table.invalidateHierarchy();
    }

    void setGap(float gap) {
        this.gap = gap;
        respace();
    }

    @Override
    public Element element() {
        return table;
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
