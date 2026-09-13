package solim.layout;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import solim.core.Component;
import solim.modifier.ElementModifiers;

/** Wrap container: lays out children in a row that wraps. */
public final class Wrap implements Component, GapContainer {
	private final Table table = new Table();
	private float gap = 4f;

	public Wrap() {
		this.table.name = "solim-wrap-table";
		this.table.userObject = this;
		respace();
	}

	public Table table() {
		return table;
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

	public Wrap name(String name) {
		ElementModifiers.name(table, name);
		return this;
	}

	public Wrap gap(float g) {
		this.gap = g;
		respace();
		return this;
	}

	public Wrap add(Element child) {
		table.add(child);
		respace();
		return this;
	}
}
