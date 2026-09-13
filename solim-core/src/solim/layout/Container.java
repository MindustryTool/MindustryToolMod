package solim.layout;

import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.modifier.ElementConfig;
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
		ElementConfig.name(table, name);
		return this;
	}

	public Container padding(float p) {
		ElementConfig.padding(table, p);
		return this;
	}

	public Container paddingX(float x) {
		ElementConfig.paddingX(table, x);
		return this;
	}

	public Container paddingY(float y) {
		ElementConfig.paddingY(table, y);
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
		ElementConfig.rounded(table, radius);
		return this;
	}

	public Container rounded(int radius, @Nullable Color color) {
		ElementConfig.rounded(table, radius, color);
		return this;
	}

	public Container rounded(int radius, @Nullable Readable<Color> color) {
		ElementConfig.rounded(table, radius, color);
		return this;
	}

	public Container border(float stroke, @Nullable Color color) {
		ElementConfig.border(table, stroke, color);
		return this;
	}

	public Container border(float stroke, @Nullable Readable<Color> color) {
		ElementConfig.border(table, stroke, color);
		return this;
	}
}
