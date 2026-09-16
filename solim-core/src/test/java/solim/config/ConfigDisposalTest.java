package solim.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import solim.signal.Signal;

class ConfigDisposalTest {

	static final class MapPersister implements ConfigPersister<String> {
		final Map<String, String> stored = new HashMap<>();
		final AtomicInteger saves = new AtomicInteger(0);

		@Override
		public String load(String key, String defaultValue) {
			return stored.containsKey(key) ? stored.get(key) : defaultValue;
		}

		@Override
		public void save(String key, String value) {
			saves.incrementAndGet();
			stored.put(key, value);
		}
	}

	@Test
	void configValueDisposeStopsPersistence() {
		MapPersister persister = new MapPersister();
		ConfigValue<String> config = new ConfigValue<>("name", "anon", persister);
		assertFalse(config.isDisposed());
		assertEquals("anon", config.get());

		config.set("grace");
		assertEquals("grace", persister.stored.get("name"));
		int saves = persister.saves.get();

		config.dispose();
		assertTrue(config.isDisposed());

		config.set("heidi");
		assertEquals(saves, persister.saves.get(), "Setter must not persist after dispose");
		assertEquals("grace", persister.stored.get("name"));

		config.signal().set("ivan");
		assertEquals(saves, persister.saves.get(), "Signal must not persist after dispose");

		assertDoesNotThrow(config::dispose, "Double dispose must be safe");
		assertTrue(config.isDisposed());
	}

	@Test
	void contextualDisposeStopsDiscriminantReloads() {
		MapPersister persister = new MapPersister();
		ConfigGroup group = ConfigGroup.of("test");
		Signal<String> env = Signal.of("dev");

		ContextualConfigValue<String, String> endpoint = new ContextualConfigValue<>(
			group,
			"endpoint",
			env,
			disc -> disc,
			"http://localhost",
			persister);
		assertFalse(endpoint.isDisposed());

		endpoint.set("http://dev");
		assertEquals("http://dev", persister.stored.get("test.endpoint.dev"));
		int saves = persister.saves.get();

		endpoint.dispose();
		assertTrue(endpoint.isDisposed());

		env.set("prod");
		assertEquals("test.endpoint.dev", endpoint.getCurrentKey(), "Key must freeze after dispose");
		assertEquals(saves, persister.saves.get(), "Discriminant change must not persist after dispose");

		endpoint.set("http://prod");
		assertEquals(saves, persister.saves.get(), "Setter must not persist after dispose");

		assertDoesNotThrow(endpoint::dispose, "Double dispose must be safe");
		assertTrue(endpoint.isDisposed());
	}
}
