package solim.feedback;

import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;

/** Alert - dismissible message with type (info/warning/error/success). */
public final class Alert {
	public enum Type {
		INFO,
		WARNING,
		ERROR,
		SUCCESS
	}

	private final Table table = new Table();
	private final TextButton close = new TextButton("X");
	private boolean shown = true;

	public Alert(String message, Type type) {
		table.add(message);
		table.add(close);
		close.changed(() -> {
			shown = false;
		});
	}

	public Table table() {
		return table;
	}

	public void dismiss() {
		shown = false;
	}

	public boolean isShown() {
		return shown;
	}
}
