package solim.input;

import arc.scene.ui.TextButton;
import solim.core.Component;
import solim.core.Disposable;
import solim.signal.Signal;
import solim.runtime.ComponentContext;

/**
 * Switch widget bound to Signal&lt;Boolean&gt;. Uses TextButton as a visual toggle; state held in
 * signal.
 */
public final class Switch implements Component {
	private final TextButton button = new TextButton("");
	private boolean state;
	private Disposable binding;

	{
		button.name = "solim-switch-switchBox";
	}

	public Switch(Signal<Boolean> signal) {
		this.state = Boolean.TRUE.equals(signal.peek());
		button.setText(state ? "ON" : "OFF");
		this.binding = new TwoWayBinding<>(
			signal,
			() -> state,
			val -> {
				state = Boolean.TRUE.equals(val);
				button.setText(state ? "ON" : "OFF");
			},
			onChange -> {
				button.changed(() -> {
					state = !state;
					onChange.run();
				});
				return () -> {};
			}
		);
		ComponentContext.register(this);
	}

	public static Switch of(Signal<Boolean> signal) {
		return new Switch(signal);
	}

	public TextButton button() {
		return button;
	}

	@Override
	public TextButton element() {
		return button;
	}

	public Switch name(String name) {
		button.name = name;
		return this;
	}

	@Override
	public void dispose() {
		if (binding != null) {
			binding.dispose();
			binding = null;
		}
	}
}
