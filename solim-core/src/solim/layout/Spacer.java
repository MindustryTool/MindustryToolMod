package solim.layout;

import arc.scene.ui.layout.Table;
import solim.core.Component;
import solim.core.SolimToken;

/** Spacer that consumes remaining space in a row/column. */
public final class Spacer extends Component {
	private final Table table;

	public Spacer() {
		this(new Table());
	}

	private Spacer(Table table) {
		super(table);
		this.table = table;
		table.name = "solim-spacer-table";
		SolimToken.bind(table, this);
		SolimToken.setExpanding(table, true);
		table.add().growX().growY();
	}


	public Spacer name(String name) {
		table.name = name;
		return this;
	}


}
