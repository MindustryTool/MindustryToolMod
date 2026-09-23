package solim.input;
import solim.reactive.TwoWayBinding;

import arc.Core;
import arc.scene.ui.CheckBox;
import arc.scene.ui.CheckBox.CheckBoxStyle;
import arc.util.Nullable;
import arc.func.Cons;
import solim.core.Component;
import solim.core.DisposableAction;
import solim.modifier.ElementConfig;
import solim.modifier.CellConfig;
import solim.modifier.PendingCellConfig;
import solim.runtime.OwnershipContext;
import solim.reactive.Signal;

/** Checkbox widget bound to Signal&lt;Boolean&gt;. */
public final class Checkbox implements Component, ElementConfig<Checkbox>, CellConfig<Checkbox> {

	private final CheckBox checkBox;
	private final PendingCellConfig constraints = new PendingCellConfig();
	private @Nullable TwoWayBinding<Boolean> binding;
	private boolean disposed = false;

	@Override
	public PendingCellConfig cellConfig() {
		return constraints;
	}

	public Checkbox(String label, Signal<Boolean> signal) {
		this(label, signal, Core.scene == null ? new CheckBoxStyle() : null);
	}

	public Checkbox(String label, Signal<Boolean> signal, @Nullable CheckBoxStyle style) {
		this.checkBox = style != null
				? new CheckBox(label != null ? label : "", style)
				: new CheckBox(label != null ? label : "");
		checkBox.top().left();
		checkBox.name = "solim-checkbox-checkBox";
		checkBox.setChecked(signal.peek());
		this.binding = new TwoWayBinding<>(
			signal,
			checkBox::isChecked,
			checkBox::setChecked,
			onChange -> {
				checkBox.changed(onChange::run);
				// Arc CheckBox.changed() does not return a cleanup handle; cannot unregister
				return DisposableAction.empty();
			}
		);
		OwnershipContext.register(this);
	}

	public Checkbox(String label, boolean initial, Cons<Boolean> onChanged) {
		this(label, initial, Core.scene == null ? new CheckBoxStyle() : null, onChanged);
	}

	public Checkbox(String label, boolean initial, @Nullable CheckBoxStyle style, Cons<Boolean> onChanged) {
		this.checkBox = style != null
				? new CheckBox(label != null ? label : "", style)
				: new CheckBox(label != null ? label : "");
		checkBox.top().left();
		checkBox.name = "solim-checkbox-checkBox";
		checkBox.setChecked(initial);
		checkBox.changed(() -> {
			if (onChanged != null) {
				onChanged.get(checkBox.isChecked());
			}
		});
		OwnershipContext.register(this);
	}

	public static Checkbox of(String label, Signal<Boolean> signal) {
		return new Checkbox(label, signal);
	}

	public static Checkbox of(String label, boolean initial, Cons<Boolean> onChanged) {
		return new Checkbox(label, initial, onChanged);
	}

	public CheckBox checkBox() {
		return checkBox;
	}

	@Override
	public CheckBox element() {
		return checkBox;
	}

	public Checkbox name(String name) {
		checkBox.name = name;
		return this;
	}

	@Override
	public void dispose() {
		if (disposed) {
			return;
		}
		disposed = true;
		if (binding != null) {
			binding.dispose();
			binding = null;
		}
	}

	@Override
	public boolean isDisposed() {
		return disposed;
	}

    @Override
    public Checkbox self() {
        return this;
    }
}
