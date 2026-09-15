package solim.feedback;

import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import java.util.function.Consumer;
import solim.core.Disposable;
import solim.signal.Computed;
import solim.signal.Effect;

/** Badge - lightweight label for counts/status. */
public final class Badge implements Disposable {
	private final Table table = new Table();
	private final Label label = new Label("");
	private Effect binding;

	public Badge() {
		table.add(label);
	}

	public Badge(Computed<String> text) {
		table.add(label);
		this.binding = Effect.of((Consumer<Effect.Cleanup>) cleanup -> {
			label.setText(text.get());
		});
	}

	public static Badge of(Computed<String> text) {
		return new Badge(text);
	}

	public Table table() {
		return table;
	}

	@Override
	public void dispose() {
		if (binding != null) binding.dispose();
	}
}
