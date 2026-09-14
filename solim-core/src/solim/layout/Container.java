package solim.layout;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import solim.core.Component;
import solim.modifier.ElementConfig;
import solim.modifier.TableConfig;

/** Container with single child, padding, and optional background. */
public final class Container implements Component, ElementConfig<Container>, TableConfig<Container> {
	private final Table table = new Table();

	public Container() {
		this.table.name = "solim-container-table";
	}

	@Override
	public Table table() {
		return table;
	}

	@Override
	public Element element() {
		return table;
	}

	public Container add(Element child) {
		table.add(child);
		return this;
	}
}
