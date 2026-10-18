package solim.modifier;

import arc.scene.ui.layout.Table;
import solim.core.Component;
import solim.core.SolimToken;
import solim.layout.Direction;
import solim.layout.GapContainer;
import solim.layout.GapHost;

final class GenericGapContainer extends Component implements GapContainer {
    private final Table table;
    private float gap;

    GenericGapContainer(Table table, float gap) {
        super(table);
        this.table = table;
        this.gap = gap;
    }

    static void setGap(Table table, float gap) {
        if (table == null)
            return;
        GapHost host = GapHost.find(table);
        if (host != null) {
            host.setGap(gap);
        } else {
            GenericGapContainer generic = new GenericGapContainer(table, gap);
            SolimToken.bind(table, generic);
            generic.respace();
        }
        table.invalidateHierarchy();
    }

    @Override
    public Table table() {
        return table;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return null;
    }

    @Override
    public void setGap(float g) {
        gap = g;
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
