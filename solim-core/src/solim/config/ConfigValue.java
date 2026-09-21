package solim.config;

import arc.util.Nullable;
import java.util.Objects;
import arc.func.Cons;
import arc.func.Prov;
import solim.core.Disposable;
import solim.reactive.Signal;
import solim.reactive.Subscription;

public class ConfigValue<T> implements Disposable {
	protected String key;
	protected final @Nullable T defaultValue;
	protected final Cons<T> setter;
	protected final Signal<T> signal;
	protected final Subscription signalSub;
	protected boolean updating = false;
	protected boolean disposed = false;

	public ConfigValue(
			String key,
			@Nullable T defaultValue,
			Prov<T> getter,
			Cons<T> setter) {
		this.key = key;
		this.defaultValue = defaultValue;
		this.setter = setter;

		T initial = getter != null ? getter.get() : null;
		this.signal = Signal.of(initial != null ? initial : defaultValue);

		this.signalSub = this.signal.subscribe(value -> {
			if (updating) {
				return;
			}
			updating = true;
			try {
				save(value);
			} finally {
				updating = false;
			}
		});
	}

	public ConfigValue(
			String key,
			@Nullable T defaultValue,
			ConfigPersister<T> persister) {
		this(
				key,
				defaultValue,
				() -> persister.load(key, defaultValue),
				val -> persister.save(key, val));
	}

	protected void save(@Nullable T value) {
		if (setter != null) {
			setter.get(value);
		}
	}

	public String getKey() {
		return key;
	}

	public @Nullable T getDefaultValue() {
		return defaultValue;
	}

	public @Nullable T get() {
		return signal.peek();
	}

	public void set(@Nullable T value) {
		if (disposed || updating) {
			return;
		}
		updating = true;
		try {
			save(value);
			if (!Objects.equals(signal.peek(), value)) {
				signal.set(value);
			}
		} finally {
			updating = false;
		}
	}

	public Signal<T> signal() {
		return signal;
	}

	public void reset() {
		set(defaultValue);
	}

	public boolean isModified() {
		return !Objects.equals(get(), defaultValue);
	}

	@Override
	public void dispose() {
		if (disposed) {
			return;
		}
		disposed = true;
		if (signalSub != null) {
			signalSub.dispose();
		}
	}

	@Override
	public boolean isDisposed() {
		return disposed;
	}
}
