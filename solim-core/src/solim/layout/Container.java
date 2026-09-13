package solim.layout;

import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.modifier.ElementModifiers;
import solim.signal.Readable;

/** Container with single child, padding, and optional background. */
public final class Container implements Component {
	private final Table table = new Table();

	public Container() {
		this.table.name = "solim-container-table";
	}

	public Table table() {
		return table;
	}

	@Override
	public Element element() {
		return table;
	}

	public Container name(String name) {
		ElementModifiers.name(table, name);
		return this;
	}

	public Container padding(float p) {
		ElementModifiers.padding(table, p);
		return this;
	}

	public Container paddingX(float x) {
		ElementModifiers.paddingX(table, x);
		return this;
	}

	public Container paddingY(float y) {
		ElementModifiers.paddingY(table, y);
		return this;
	}

	public Container background(Drawable d) {
		table.setBackground(d);
		return this;
	}

	public Container add(Element child) {
		table.add(child);
		return this;
	}

	public Container rounded(int radius) {
		ElementModifiers.rounded(table, radius);
		return this;
	}

	public Container rounded(int radius, @Nullable Color color) {
		ElementModifiers.rounded(table, radius, color);
		return this;
	}

	public Container rounded(int radius, @Nullable Readable<Color> color) {
		ElementModifiers.rounded(table, radius, color);
		return this;
	}

	public Container border(float stroke, @Nullable Color color) {
		ElementModifiers.border(table, stroke, color);
		return this;
	}

	public Container border(float stroke, @Nullable Readable<Color> color) {
		ElementModifiers.border(table, stroke, color);
		return this;
	}
}
