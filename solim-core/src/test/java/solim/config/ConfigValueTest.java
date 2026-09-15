package solim.config;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.Settings;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigValueTest {

	@BeforeAll
	static void initSettings() {
		Core.settings = new Settings();
	}

	@BeforeEach
	void clearSettings() {
		Core.settings.clear();
	}

	@Test
	void testInitialValueAndDefaultFallback() {
		ConfigValue<String> config = new ConfigValue<>(
				"test.greeting",
				"hello",
				ConfigPersister.STRING);

		assertEquals("hello", config.getDefaultValue());
		assertEquals("hello", config.get());
		assertEquals("test.greeting", config.getKey());
		assertFalse(config.isModified());

		// When value is already in settings
		Core.settings.put("test.existing", "world");
		ConfigValue<String> existing = new ConfigValue<>(
				"test.existing",
				"hello",
				ConfigPersister.STRING);
		assertEquals("world", existing.get());
		assertTrue(existing.isModified());
	}

	@Test
	void testSetUpdatesSettingsAndSignal() {
		ConfigValue<Integer> config = new ConfigValue<>(
				"test.count",
				10,
				ConfigPersister.INTEGER);

		AtomicInteger listenerValue = new AtomicInteger(-1);
		config.signal().subscribe(listenerValue::set);

		config.set(42);

		assertEquals(42, config.get());
		assertEquals(42, Core.settings.getInt("test.count"));
		assertEquals(42, listenerValue.get());
		assertTrue(config.isModified());
	}

	@Test
	void testSignalSetUpdatesStorageBidirectionally() {
		ConfigValue<Boolean> config = new ConfigValue<>(
				"test.flag",
				false,
				ConfigPersister.BOOLEAN);

		config.signal().set(true);

		assertEquals(true, config.get());
		assertTrue(Core.settings.getBool("test.flag"));
		assertTrue(config.isModified());
	}

	@Test
	void testResetRestoresDefault() {
		ConfigValue<Float> config = new ConfigValue<>(
				"test.speed",
				1.5f,
				ConfigPersister.FLOAT);

		config.set(3.0f);
		assertTrue(config.isModified());
		assertEquals(3.0f, config.get());

		config.reset();
		assertFalse(config.isModified());
		assertEquals(1.5f, config.get());
		assertEquals(1.5f, Core.settings.getFloat("test.speed"));
	}

	@Test
	void testDisposeUnregistersInternalListener() {
		ConfigValue<String> config = new ConfigValue<>(
				"test.disposable",
				"init",
				ConfigPersister.STRING);

		config.dispose();

		// After disposing internal subscriber, setting the signal directly should not push to storage
		config.signal().set("updated");
		assertNull(Core.settings.getString("test.disposable", null));
	}
}
