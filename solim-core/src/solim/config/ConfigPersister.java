package solim.config;

import arc.Core;
import arc.util.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public interface ConfigPersister<T> {

	@Nullable
	T load(String key, @Nullable T defaultValue);

	void save(String key, @Nullable T value);

	ConfigPersister<Boolean> BOOLEAN = new ConfigPersister<Boolean>() {
		@Override
		public Boolean load(String key, @Nullable Boolean defaultValue) {
			return Core.settings.getBool(key, defaultValue != null && defaultValue);
		}

		@Override
		public void save(String key, @Nullable Boolean value) {
			if (value != null) {
				Core.settings.put(key, value);
			}
		}
	};

	ConfigPersister<Integer> INTEGER = new ConfigPersister<Integer>() {
		@Override
		public Integer load(String key, @Nullable Integer defaultValue) {
			return Core.settings.getInt(key, defaultValue != null ? defaultValue : 0);
		}

		@Override
		public void save(String key, @Nullable Integer value) {
			if (value != null) {
				Core.settings.put(key, value);
			}
		}
	};

	ConfigPersister<Long> LONG = new ConfigPersister<Long>() {
		@Override
		public Long load(String key, @Nullable Long defaultValue) {
			return Core.settings.getLong(key, defaultValue != null ? defaultValue : 0L);
		}

		@Override
		public void save(String key, @Nullable Long value) {
			if (value != null) {
				Core.settings.put(key, value);
			}
		}
	};

	ConfigPersister<Float> FLOAT = new ConfigPersister<Float>() {
		@Override
		public Float load(String key, @Nullable Float defaultValue) {
			return Core.settings.getFloat(key, defaultValue != null ? defaultValue : 0f);
		}

		@Override
		public void save(String key, @Nullable Float value) {
			if (value != null) {
				Core.settings.put(key, value);
			}
		}
	};

	ConfigPersister<Double> DOUBLE = new ConfigPersister<Double>() {
		@Override
		public Double load(String key, @Nullable Double defaultValue) {
			return (double) Core.settings.getFloat(key, defaultValue != null ? defaultValue.floatValue() : 0f);
		}

		@Override
		public void save(String key, @Nullable Double value) {
			if (value != null) {
				Core.settings.put(key, value.floatValue());
			}
		}
	};

	ConfigPersister<String> STRING = new ConfigPersister<String>() {
		@Override
		public @Nullable String load(String key, @Nullable String defaultValue) {
			return Core.settings.getString(key, defaultValue);
		}

		@Override
		public void save(String key, @Nullable String value) {
			if (value == null) {
				Core.settings.remove(key);
			} else {
				Core.settings.put(key, value);
			}
		}
	};

	ConfigPersister<Set<String>> STRING_SET = new ConfigPersister<Set<String>>() {
		@Override
		public @Nullable Set<String> load(String key, @Nullable Set<String> defaultValue) {
			String str = Core.settings.getString(key, null);
			if (str == null) {
				return defaultValue;
			}
			if (str.trim().isEmpty()) {
				return new HashSet<>();
			}
			return new HashSet<>(Arrays.asList(str.split(",")));
		}

		@Override
		public void save(String key, @Nullable Set<String> value) {
			if (value == null || value.isEmpty()) {
				Core.settings.put(key, "");
			} else {
				Core.settings.put(key, String.join(",", value));
			}
		}
	};
}
