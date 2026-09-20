package mindustrytool.test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import arc.graphics.g2d.Font;
import mindustry.gen.Icon;
import mindustry.ui.Fonts;
import mindustrytool.features.FeatureManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import solim.test.SolimEnv;

/**
 * Test environment for mod tests: extends {@link SolimEnv} with Mindustry-side
 * global containment. Snapshots and restores the static {@link Icon} drawable
 * registry and {@link Fonts#def} around every test, and clears the shared
 * {@link FeatureManager}. This is what makes reused worker JVMs safe for mod
 * tests (replacing the old forkEvery = 1 JVM-per-class setup).
 */
public abstract class MindustryTestEnv extends SolimEnv {

	private static final Map<Field, Object> iconSnapshot = new HashMap<>();
	private static Font fontsDefSnapshot;

	@BeforeEach
	public void setUpMindustryTestEnv() {
		snapshotIcons();
		fontsDefSnapshot = Fonts.def;
		FeatureManager.clear();
	}

	@AfterEach
	public void tearDownMindustryTestEnv() {
		restoreIcons();
		Fonts.def = fontsDefSnapshot;
		fontsDefSnapshot = null;
		FeatureManager.clear();
	}

	private static void snapshotIcons() {
		iconSnapshot.clear();
		for (Field field : Icon.class.getFields()) {
			int mods = field.getModifiers();
			if (!Modifier.isStatic(mods) || Modifier.isFinal(mods)) {
				continue;
			}
			try {
				field.setAccessible(true);
				iconSnapshot.put(field, field.get(null));
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException("Failed to snapshot Icon field " + field.getName(), e);
			}
		}
	}

	private static void restoreIcons() {
		for (Map.Entry<Field, Object> entry : iconSnapshot.entrySet()) {
			try {
				entry.getKey().set(null, entry.getValue());
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException("Failed to restore Icon field " + entry.getKey().getName(), e);
			}
		}
		iconSnapshot.clear();
	}
}
