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
import solim.reactive.Signal;

/** Checkbox widget bound to Signal&lt;Boolean&gt;. */
public final class Checkbox extends Component implements ElementConfig<Checkbox>, CellConfig<Checkbox> {

	private final CheckBox checkBox;
	private final PendingCellConfig constraints = new PendingCellConfig();

	@Override
	public PendingCellConfig cellConfig() {
		return constraints;
	}

	public Checkbox(String label, Signal<Boolean> signal) {
		this(label, signal, Core.scene == null ? new CheckBoxStyle() : null);
	}

	public Checkbox(String label, Signal<Boolean> signal, @Nullable CheckBoxStyle style) {
		this(create(label, style), signal);
	}

	private Checkbox(CheckBox checkBox, Signal<Boolean> signal) {
		super(checkBox);
		this.checkBox = checkBox;
		checkBox.top().left();
		checkBox.name = "solim-checkbox-checkBox";
		checkBox.setChecked(signal.peek());
		own(new TwoWayBinding<>(
			signal,
			checkBox::isChecked,
			checkBox::setChecked,
			onChange -> {
				checkBox.changed(onChange::run);
				// Arc CheckBox.changed() does not return a cleanup handle; cannot unregister
				return DisposableAction.empty();
			}
		));
	}

	private static CheckBox create(String label, @Nullable CheckBoxStyle style) {
		return style != null
				? new CheckBox(label != null ? label : "", style)
				: new CheckBox(label != null ? label : "");
	}

	public Checkbox(String label, boolean initial, Cons<Boolean> onChanged) {
		this(label, initial, Core.scene == null ? new CheckBoxStyle() : null, onChanged);
	}

	public Checkbox(String label, boolean initial, @Nullable CheckBoxStyle style, Cons<Boolean> onChanged) {
		this(create(label, style), initial, onChanged);
	}

	private Checkbox(CheckBox checkBox, boolean initial, @Nullable Cons<Boolean> onChanged) {
		super(checkBox);
		this.checkBox = checkBox;
		checkBox.top().left();
		checkBox.name = "solim-checkbox-checkBox";
		checkBox.setChecked(initial);
		if (onChanged != null) {
			checkBox.changed(() -> onChanged.get(checkBox.isChecked()));
		}
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

	public Checkbox name(String name) {
		setName(name);
		return this;
	}

	public Checkbox wrap() {
		return wrap(true);
	}

	public Checkbox wrap(boolean wrap) {
		if (checkBox.getLabel() != null) {
			checkBox.getLabel().setWrap(wrap);
		}
		if (checkBox.getLabelCell() != null && wrap) {
			checkBox.getLabelCell().growX().minWidth(0f);
		}
		return this;
	}

    @Override
    public Checkbox self() {
        return this;
    }
}
