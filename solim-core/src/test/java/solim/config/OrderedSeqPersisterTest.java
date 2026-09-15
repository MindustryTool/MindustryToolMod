package solim.config;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.Settings;
import arc.struct.Seq;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderedSeqPersisterTest {

	private OrderedSeqPersister persister;

	@BeforeAll
	static void initSettings() {
		Core.settings = new Settings();
	}

	@BeforeEach
	void setUp() {
		Core.settings.clear();
		persister = new OrderedSeqPersister();
	}

	@Test
	void testMissingKeyReturnsDefault() {
		Seq<String> defaultSeq = Seq.with("alpha", "beta");
		Seq<String> loaded = persister.load("non.existent.key", defaultSeq);

		assertEquals(defaultSeq, loaded);
	}

	@Test
	void testEmptyListRoundTrip() {
		Seq<String> empty = new Seq<>();
		persister.save("test.empty", empty);

		Seq<String> loaded = persister.load("test.empty", Seq.with("fallback"));
		assertNotNull(loaded);
		assertTrue(loaded.isEmpty());
	}

	@Test
	void testOrderPreservedAcrossSaveAndLoad() {
		Seq<String> original = Seq.with("feature-c", "feature-a", "feature-b");
		persister.save("test.ordered", original);

		Seq<String> loaded = persister.load("test.ordered", new Seq<>());
		assertNotNull(loaded);
		assertEquals(3, loaded.size);
		assertEquals("feature-c", loaded.get(0));
		assertEquals("feature-a", loaded.get(1));
		assertEquals("feature-b", loaded.get(2));
	}

	@Test
	void testCorruptJsonReturnsDefault() {
		Core.settings.put("test.corrupt", "{not valid json");
		Seq<String> defaultSeq = Seq.with("safe-default");

		Seq<String> loaded = persister.load("test.corrupt", defaultSeq);
		assertEquals(defaultSeq, loaded);
	}

	@Test
	void testSaveNullRemovesKey() {
		persister.save("test.remove", Seq.with("val"));
		assertTrue(Core.settings.has("test.remove"));

		persister.save("test.remove", null);
		assertFalse(Core.settings.has("test.remove"));
	}

	@Test
	void testConfigValueIntegration() {
		ConfigGroup group = ConfigGroup.of("test.group");
		ConfigValue<Seq<String>> config = group.value("order", Seq.with("first"), persister);

		assertEquals(1, config.get().size);
		assertEquals("first", config.get().get(0));

		config.set(Seq.with("third", "second", "first"));
		assertEquals(3, config.get().size);
		assertEquals("third", config.get().get(0));

		// Reload through a new ConfigValue instance on the same key
		ConfigValue<Seq<String>> reloaded = group.value("order", new Seq<>(), persister);
		assertEquals(3, reloaded.get().size);
		assertEquals("third", reloaded.get().get(0));
		assertEquals("second", reloaded.get().get(1));
		assertEquals("first", reloaded.get().get(2));
	}
}
