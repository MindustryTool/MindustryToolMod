package solim.config;

import arc.util.Nullable;
import arc.func.Func;
import java.util.Objects;
import solim.core.ReactiveObserver;
import solim.core.ReactiveSource;
import solim.reactive.Readable;

public class ContextualConfigValue<T, K> extends ConfigValue<T> {
	private final ConfigGroup group;
	private final String baseName;
	private final Readable<K> discriminant;
	private final Func<K, String> keySuffix;
	private final ConfigPersister<T> persister;
	private final @Nullable ReactiveObserver discriminantObserver;
	private @Nullable K currentDiscriminant;

	private final @Nullable Func<K, T> defaultFactory;

	public ContextualConfigValue(
			ConfigGroup group,
			String baseName,
			Readable<K> discriminant,
			Func<K, String> keySuffix,
			@Nullable T defaultValue,
			@Nullable Func<K, T> defaultFactory,
			ConfigPersister<T> persister) {
		super(
				deriveInitialKey(group, baseName, discriminant, keySuffix),
				resolveInitialDefault(defaultValue, defaultFactory, discriminant),
				() -> {
					String key = deriveInitialKey(group, baseName, discriminant, keySuffix);
					T def = resolveInitialDefault(defaultValue, defaultFactory, discriminant);
					return persister != null ? persister.load(key, def) : null;
				},
				null);
		this.group = group;
		this.baseName = baseName;
		this.discriminant = discriminant;
		this.keySuffix = keySuffix;
		this.defaultFactory = defaultFactory;
		this.persister = persister;
		this.currentDiscriminant = discriminant != null ? discriminant.peek() : null;
		if (this.discriminant instanceof ReactiveSource) {
			this.discriminantObserver = new ReactiveObserver() {
				@Override
				public void addDependency(ReactiveSource observable) {
				}

				@Override
				public void invalidate() {
					K newDisc = discriminant.peek();
					if (!Objects.equals(currentDiscriminant, newDisc)) {
						onDiscriminantChanged(newDisc);
					}
				}
			};
			((ReactiveSource) this.discriminant).addObserver(this.discriminantObserver);
		} else {
			this.discriminantObserver = null;
		}
	}

	public ContextualConfigValue(
			ConfigGroup group,
			String baseName,
			Readable<K> discriminant,
			Func<K, String> keySuffix,
			@Nullable T defaultValue,
			ConfigPersister<T> persister) {
		this(group, baseName, discriminant, keySuffix, defaultValue, null, persister);
	}

	public ContextualConfigValue(
			ConfigGroup group,
			String baseName,
			Readable<K> discriminant,
			Func<K, String> keySuffix,
			Func<K, T> defaultFactory,
			ConfigPersister<T> persister) {
		this(group, baseName, discriminant, keySuffix, null, defaultFactory, persister);
	}

	public ContextualConfigValue(
			ConfigGroup group,
			String baseName,
			Readable<K> discriminant,
			Func<K, String> keySuffix,
			@Nullable T defaultValue,
			ContextualPersister<T> persister) {
		this(group, baseName, discriminant, keySuffix, defaultValue, null, (ConfigPersister<T>) persister);
	}

	private static <T, K> T resolveInitialDefault(
			@Nullable T defaultValue,
			@Nullable Func<K, T> defaultFactory,
			@Nullable Readable<K> discriminant) {
		if (defaultFactory != null && discriminant != null) {
			T val = defaultFactory.get(discriminant.peek());
			if (val != null) {
				return val;
			}
		}
		return defaultValue;
	}

	private static <K> String deriveInitialKey(
			ConfigGroup group,
			String baseName,
			@Nullable Readable<K> discriminant,
			Func<K, String> keySuffix) {
		K initialDisc = discriminant != null ? discriminant.peek() : null;
		String suffix = keySuffix != null ? keySuffix.get(initialDisc) : "";
		return group.resolveKey(baseName + (suffix != null && !suffix.isEmpty() ? "." + suffix : ""));
	}

	@Override
	protected void save(@Nullable T value) {
		if (persister != null) {
			persister.save(this.key, value);
		}
	}

	private void onDiscriminantChanged(K newDisc) {
		if (updating) {
			return;
		}
		updating = true;
		try {
			T current = signal.peek();
			if (persister != null) {
				persister.save(this.key, current);
			}
			this.currentDiscriminant = newDisc;
			this.key = deriveKey(newDisc);
			T contextDefault = defaultFactory != null ? defaultFactory.get(newDisc) : defaultValue;
			T loaded = persister != null ? persister.load(this.key, contextDefault) : null;
			signal.set(loaded != null ? loaded : contextDefault);
		} finally {
			updating = false;
		}
	}

	private String deriveKey(K disc) {
		String suffix = keySuffix != null ? keySuffix.get(disc) : "";
		return group.resolveKey(baseName + (suffix != null && !suffix.isEmpty() ? "." + suffix : ""));
	}

	public String getCurrentKey() {
		return getKey();
	}

	public @Nullable K getCurrentDiscriminant() {
		return currentDiscriminant;
	}

	@Override
	public @Nullable T getDefaultValue() {
		return defaultFactory != null ? defaultFactory.get(currentDiscriminant) : defaultValue;
	}

	@Override
	public void reset() {
		set(getDefaultValue());
	}

	@Override
	public boolean isModified() {
		T def = getDefaultValue();
		return def != null ? !def.equals(get()) : get() != null;
	}

    @Override
    public void dispose() {
        if (isDisposed()) {
            return;
        }
        if (discriminantObserver != null && discriminant instanceof ReactiveSource) {
            ((ReactiveSource) discriminant).removeObserver(discriminantObserver);
        }
        super.dispose();
    }

	public interface ContextualPersister<T> extends ConfigPersister<T> {
		@Nullable
		T load(String key);

		@Override
		default @Nullable T load(String key, @Nullable T defaultValue) {
			T val = load(key);
			return val != null ? val : defaultValue;
		}
	}
}
