package solim.layout;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import solim.core.Component;
import solim.modifier.ElementConfig;

/** Spacer that consumes remaining space in a row/column. */
public final class Spacer implements Component {
	private final Table table = new Table();

	public Spacer() {
		table.name = "solim-spacer-table";
		table.userObject = "expanding";
		table.add().growX().growY();
	}

	@Override
	public Element element() {
		return table;
	}

	public Spacer name(String name) {
		ElementConfig.name(table, name);
		return this;
	}
}
