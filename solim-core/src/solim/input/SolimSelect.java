package solim.input;

import arc.scene.ui.TextButton;
import java.util.List;
import solim.core.Component;
import solim.core.Disposable;
import solim.modifier.ElementConfig;
import solim.signal.Signal;

/**
 * Select widget bound to Signal&lt;T&gt;. Uses TextButton as placeholder (Arc SelectBox unavailable
 * in this version). State held in signal; visual updates on change.
 */
public final class SolimSelect<T> implements Component {
	private final TextButton selectBox = new TextButton("");
	private final Signal<T> signal;
	private final List<T> options;
	private int selectedIndex = 0;
	private Disposable binding;

	{
		selectBox.name = "solim-select-selectBox";
	}

	public SolimSelect(Signal<T> signal, List<T> options) {
		this.signal = signal;
		this.options = options;
		if (signal.peek() != null) {
			int idx = options.indexOf(signal.peek());
			if (idx >= 0) selectedIndex = idx;
		}
		selectBox.setText(String.valueOf(signal.peek()));
		this.binding = new TwoWayBinding<>(
			signal,
			() -> options.isEmpty() ? null : options.get(selectedIndex),
			cur -> {
				int idx = options.indexOf(cur);
				if (idx >= 0) selectedIndex = idx;
				selectBox.setText(String.valueOf(cur));
			},
			onChange -> {
				selectBox.changed(() -> {
					if (!options.isEmpty()) {
						selectedIndex = (selectedIndex + 1) % options.size();
						onChange.run();
					}
				});
				return () -> {};
			}
		);
		solim.runtime.ComponentContext.register(this);
	}

	public static <T> SolimSelect<T> of(Signal<T> signal, List<T> options) {
		return new SolimSelect<>(signal, options);
	}

	public TextButton selectBox() {
		return selectBox;
	}

	@Override
	public TextButton element() {
		return selectBox;
	}

	@Override
	public SolimSelect<T> name(String name) {
		ElementConfig.name(selectBox, name);
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
