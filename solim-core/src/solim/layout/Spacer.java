package solim.layout;

import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.SolimToken;
import solim.modifier.Configurable;
import solim.modifier.PendingCellConfig;
import solim.modifier.TableCapability;
import solim.modifier.Props;

public final class Spacer extends Component
        implements Configurable<Spacer>, TableCapability {

    private final Table table;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private @Nullable Props<Spacer> pending;

    public Spacer() {
        this(new Table());
    }

    private Spacer(Table table) {
        super(table);
        this.table = table;
        table.name = "solim-spacer-table";
        constraints.growX = true;
        constraints.growY = true;
        SolimToken.bind(table, this, constraints);
        SolimToken.setExpanding(table, true);
        table.add().growX().growY();
    }

    public Spacer name(String name) {
        table.name = name;
        return this;
    }

    @Override
    public Spacer self() {
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

    @Override
    public @Nullable Props<Spacer> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<Spacer> props) {
        this.pending = props;
    }
}
