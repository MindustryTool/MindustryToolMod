package solim.config;

import arc.util.Nullable;
import java.util.Set;
import java.util.function.Function;
import solim.signal.Readable;

public class ConfigGroup {
	private final String namespace;

	private ConfigGroup(String namespace) {
		this.namespace = normalizeNamespace(namespace);
	}

	public static ConfigGroup of(String namespace) {
		return new ConfigGroup(namespace);
	}

	public static ConfigGroup root(String rootNamespace) {
		return new ConfigGroup(rootNamespace);
	}

	public ConfigGroup group(String subGroup) {
		if (subGroup == null || subGroup.trim().isEmpty()) {
			return this;
		}
		return new ConfigGroup(namespace.isEmpty() ? subGroup : namespace + "." + subGroup);
	}

	public String getNamespace() {
		return namespace;
	}

	public String resolveKey(String name) {
		if (name == null || name.trim().isEmpty()) {
			return namespace;
		}
		String cleanName = name.startsWith(".") ? name.substring(1) : name;
		if (namespace.isEmpty()) {
			return cleanName;
		}
		return namespace + "." + cleanName;
	}

	public <T> ConfigValue<T> value(String name, @Nullable T defaultValue, ConfigPersister<T> persister) {
		String key = resolveKey(name);
		return new ConfigValue<>(key, defaultValue, persister);
	}

	public <T, K> ContextualConfigValue<T, K> valueKeyed(
			String name,
			Readable<K> discriminant,
			Function<K, String> keySuffix,
			@Nullable T defaultValue,
			ConfigPersister<T> persister) {
		return new ContextualConfigValue<>(this, name, discriminant, keySuffix, defaultValue, persister);
	}

	public <T, K> ContextualConfigValue<T, K> valueKeyed(
			String name,
			Readable<K> discriminant,
			Function<K, String> keySuffix,
			Function<K, T> defaultFactory,
			ConfigPersister<T> persister) {
		return new ContextualConfigValue<>(this, name, discriminant, keySuffix, defaultFactory, persister);
	}

	public ConfigValue<Boolean> boolValue(String name, boolean defaultValue) {
		return value(name, defaultValue, ConfigPersister.BOOLEAN);
	}

	public ConfigValue<Integer> intValue(String name, int defaultValue) {
		return value(name, defaultValue, ConfigPersister.INTEGER);
	}

	public ConfigValue<Long> longValue(String name, long defaultValue) {
		return value(name, defaultValue, ConfigPersister.LONG);
	}

	public ConfigValue<Float> floatValue(String name, float defaultValue) {
		return value(name, defaultValue, ConfigPersister.FLOAT);
	}

	public ConfigValue<Double> doubleValue(String name, double defaultValue) {
		return value(name, defaultValue, ConfigPersister.DOUBLE);
	}

	public ConfigValue<String> stringValue(String name, @Nullable String defaultValue) {
		return value(name, defaultValue, ConfigPersister.STRING);
	}

	public ConfigValue<Set<String>> setValue(String name, Class<String> elementType, Set<String> defaultValue) {
		return value(name, defaultValue, ConfigPersister.STRING_SET);
	}

	public <K> ContextualConfigValue<Boolean, K> boolValueKeyed(
			String name,
			Readable<K> discriminant,
			Function<K, String> keySuffix,
			boolean defaultValue) {
		return valueKeyed(name, discriminant, keySuffix, defaultValue, ConfigPersister.BOOLEAN);
	}

	public <K> ContextualConfigValue<Boolean, K> boolValueKeyed(
			String name,
			Readable<K> discriminant,
			Function<K, String> keySuffix,
			Function<K, Boolean> defaultFactory) {
		return valueKeyed(name, discriminant, keySuffix, defaultFactory, ConfigPersister.BOOLEAN);
	}

	public <K> ContextualConfigValue<Integer, K> intValueKeyed(
			String name,
			Readable<K> discriminant,
			Function<K, String> keySuffix,
			int defaultValue) {
		return valueKeyed(name, discriminant, keySuffix, defaultValue, ConfigPersister.INTEGER);
	}

	public <K> ContextualConfigValue<Integer, K> intValueKeyed(
			String name,
			Readable<K> discriminant,
			Function<K, String> keySuffix,
			Function<K, Integer> defaultFactory) {
		return valueKeyed(name, discriminant, keySuffix, defaultFactory, ConfigPersister.INTEGER);
	}

	public <K> ContextualConfigValue<Float, K> floatValueKeyed(
			String name,
			Readable<K> discriminant,
			Function<K, String> keySuffix,
			float defaultValue) {
		return valueKeyed(name, discriminant, keySuffix, defaultValue, ConfigPersister.FLOAT);
	}

	public <K> ContextualConfigValue<Float, K> floatValueKeyed(
			String name,
			Readable<K> discriminant,
			Function<K, String> keySuffix,
			Function<K, Float> defaultFactory) {
		return valueKeyed(name, discriminant, keySuffix, defaultFactory, ConfigPersister.FLOAT);
	}

	public <K> ContextualConfigValue<String, K> stringValueKeyed(
			String name,
			Readable<K> discriminant,
			Function<K, String> keySuffix,
			@Nullable String defaultValue) {
		return valueKeyed(name, discriminant, keySuffix, defaultValue, ConfigPersister.STRING);
	}

	public <K> ContextualConfigValue<String, K> stringValueKeyed(
			String name,
			Readable<K> discriminant,
			Function<K, String> keySuffix,
			Function<K, String> defaultFactory) {
		return valueKeyed(name, discriminant, keySuffix, defaultFactory, ConfigPersister.STRING);
	}

	private static String normalizeNamespace(String ns) {
		if (ns == null) {
			return "";
		}
		String clean = ns.trim();
		while (clean.startsWith(".")) {
			clean = clean.substring(1);
		}
		while (clean.endsWith(".")) {
			clean = clean.substring(0, clean.length() - 1);
		}
		return clean.replaceAll("\\.{2,}", ".");
	}
}
