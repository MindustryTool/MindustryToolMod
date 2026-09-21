package solim.core;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.scene.Element;
import org.junit.jupiter.api.Test;
import arc.scene.ui.layout.Table;
import solim.test.SolimEnv;

class SettingsPanelTest extends SolimEnv {

	@Test
	void reactiveStateAndComputed() {
		SettingsPanel panel = new SettingsPanel();
		assertFalse(panel.darkMode().get());
		assertFalse(panel.dirty().get());
		assertTrue(panel.saveText().get().contains("Save Changes"));

		panel.dirty().set(true);
		assertTrue(panel.dirty().get());
		assertTrue(panel.saveText().get().contains("●"));

		panel.darkMode().set(true);
		assertTrue(panel.darkMode().get());

		panel.dispose();
	}

	@Test
	void renderBuildsColumnWithChildrenIfSceneAvailable() {
		if (Core.scene != null) {
			SettingsPanel panel = new SettingsPanel();
			Element el = panel.element();
			assertTrue(el instanceof Table);
			assertTrue(((Table) el).getChildren().size > 0);
			assertSame(el, panel.element());
			panel.dispose();
		}
	}
}
