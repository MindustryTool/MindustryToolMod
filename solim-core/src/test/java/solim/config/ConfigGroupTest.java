package solim.config;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.Settings;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.signal.Signal;

class ConfigGroupTest {

	@BeforeAll
	static void initSettings() {
		Core.settings = new Settings();
	}

	@BeforeEach
	void clearSettings() {
		Core.settings.clear();
	}

	@Test
	void testNamespaceAndKeyResolution() {
		ConfigGroup root = ConfigGroup.of("my.mod");
		assertEquals("my.mod", root.getNamespace());
		assertEquals("my.mod.enabled", root.resolveKey("enabled"));
		assertEquals("my.mod.enabled", root.resolveKey(".enabled"));

		ConfigGroup sub = root.group("feature");
		assertEquals("my.mod.feature", sub.getNamespace());
		assertEquals("my.mod.feature.timeout", sub.resolveKey("timeout"));

		ConfigGroup empty = ConfigGroup.of("");
		assertEquals("standalone", empty.resolveKey("standalone"));
	}

	@Test
	void testPrimitiveAndStringValues() {
		ConfigGroup group = ConfigGroup.of("settings");

		ConfigValue<Boolean> b = group.boolValue("flag", false);
		b.set(true);
		assertTrue(Core.settings.getBool("settings.flag"));

		ConfigValue<Integer> i = group.intValue("count", 1);
		i.set(99);
		assertEquals(99, Core.settings.getInt("settings.count"));

		ConfigValue<Long> l = group.longValue("timestamp", 1000L);
		l.set(2000L);
		assertEquals(2000L, Core.settings.getLong("settings.timestamp"));

		ConfigValue<Float> f = group.floatValue("ratio", 0.5f);
		f.set(0.75f);
		assertEquals(0.75f, Core.settings.getFloat("settings.ratio"), 0.001f);

		ConfigValue<Double> d = group.doubleValue("precise", 1.25);
		d.set(2.5);
		assertEquals(2.5, Core.settings.getFloat("settings.precise"), 0.001f);

		ConfigValue<String> s = group.stringValue("name", "anon");
		s.set("player");
		assertEquals("player", Core.settings.getString("settings.name"));

		s.set(null);
		assertNull(Core.settings.getString("settings.name", null));
	}

	@Test
	void testSetSerialization() {
		ConfigGroup group = ConfigGroup.of("prefs");
		Set<String> def = new HashSet<>(Arrays.asList("alpha", "beta"));

		ConfigValue<Set<String>> setConfig = group.setValue("items", String.class, def);
		assertEquals(def, setConfig.get());

		Set<String> updated = new HashSet<>(Arrays.asList("gamma", "delta"));
		setConfig.set(updated);

		String stored = Core.settings.getString("prefs.items");
		assertTrue(stored.contains("gamma"));
		assertTrue(stored.contains("delta"));

		// New config instance reading back from settings
		ConfigValue<Set<String>> reloaded = group.setValue("items", String.class, def);
		assertEquals(updated, reloaded.get());

		// Test empty set
		setConfig.set(new HashSet<>());
		assertEquals("", Core.settings.getString("prefs.items"));
		assertEquals(new HashSet<>(), setConfig.get());
	}

	@Test
	void testKeyedValues() {
		ConfigGroup group = ConfigGroup.of("ui");
		Signal<String> theme = Signal.of("dark");

		ContextualConfigValue<Boolean, String> boolKeyed = group.boolValueKeyed(
				"blur",
				theme,
				t -> t,
				false);

		boolKeyed.set(true);
		assertTrue(Core.settings.getBool("ui.blur.dark"));

		ContextualConfigValue<Integer, String> intKeyed = group.intValueKeyed(
				"pad",
				theme,
				t -> t,
				8);
		intKeyed.set(16);
		assertEquals(16, Core.settings.getInt("ui.pad.dark"));

		ContextualConfigValue<Float, String> floatKeyed = group.floatValueKeyed(
				"scale",
				theme,
				t -> t,
				1.0f);
		floatKeyed.set(1.5f);
		assertEquals(1.5f, Core.settings.getFloat("ui.scale.dark"), 0.001f);

		ContextualConfigValue<String, String> strKeyed = group.stringValueKeyed(
				"color",
				theme,
				t -> t,
				"#fff");
		strKeyed.set("#000");
		assertEquals("#000", Core.settings.getString("ui.color.dark"));
	}
}
