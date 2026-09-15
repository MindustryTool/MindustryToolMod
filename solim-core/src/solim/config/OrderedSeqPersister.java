package solim.config;

import arc.Core;
import arc.struct.Seq;
import arc.util.Nullable;

/**
 * Persister for an ordered {@code Seq<String>}. The sequence is stored as a comma-separated
 * string in Mindustry's {@code Core.settings}. Order is preserved on load and save.
 */
public class OrderedSeqPersister implements ConfigPersister<Seq<String>> {
	@SuppressWarnings("unchecked")
    @Override
	public @Nullable Seq<String> load(String key, @Nullable Seq<String> defaultValue) {
		if (!Core.settings.has(key)) {
			return defaultValue;
		}
		try {
			Seq<String> seq = Core.settings.getJson(key, Seq.class, String.class, () -> null);
			return seq != null ? seq : defaultValue;
		} catch (Exception e) {
			return defaultValue;
		}
	}

	@Override
	public void save(String key, @Nullable Seq<String> value) {
		if (value == null) {
			Core.settings.remove(key);
		} else {
			Core.settings.putJson(key, String.class, value);
		}
	}
}
