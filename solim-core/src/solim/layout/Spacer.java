package solim.layout;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import solim.core.Component;
import solim.core.SolimToken;
import solim.runtime.ComponentContext;

/** Spacer that consumes remaining space in a row/column. */
public final class Spacer implements Component {
	private final Table table = new Table();
	private boolean disposed = false;

	public Spacer() {
		table.name = "solim-spacer-table";
		SolimToken.bind(table, this);
		SolimToken.setExpanding(table, true);
		table.add().growX().growY();
		ComponentContext.register(this);
	}

	@Override
	public Element element() {
		return table;
	}

	public Spacer name(String name) {
		table.name = name;
		return this;
	}

	@Override
	public void dispose() {
		if (disposed) {
			return;
		}
		disposed = true;
	}

	@Override
	public boolean isDisposed() {
		return disposed;
	}
}
