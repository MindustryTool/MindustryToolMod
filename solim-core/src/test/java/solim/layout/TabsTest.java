package solim.layout;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.graphics.Color;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.Element;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.layout.CellAccess;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.display.Text;
import solim.graphics.RoundedDrawable;
import solim.input.Button;
import solim.reactive.Signal;
import solim.runtime.SignalDispatcher;
import org.junit.jupiter.api.Assumptions;

class TabsTest {

	@BeforeAll
	static void checkArcContext() {
		if (Core.app == null) {
			Core.app = new MockApplication();
		}
		if (Core.graphics == null) {
			Core.graphics = new MockGraphics();
		}
	}

	private void simulateClick(Button button) {
		InputEvent event = new InputEvent();
		button.sizedButton().getListeners().forEach(l -> {
			if (l instanceof ClickListener) {
				((ClickListener) l).clicked(event, 0f, 0f);
			}
		});
	}

	@Test
	void headerBarDefaultGapUsesElementConfig() {
		Tabs tabs = new Tabs(Signal.of(0));
		Element a = new Element();
		Element b = new Element();
		tabs.headerBar().add(a);
		tabs.headerBar().add(b);
		tabs.headerGap(4f);
		assertEquals(0f, CellAccess.padLeft(tabs.headerBar().getCell(a)), 0.01f);
		assertEquals(4f, CellAccess.padLeft(tabs.headerBar().getCell(b)), 0.01f);
		assertEquals(0f, CellAccess.padTop(tabs.headerBar().getCell(b)), 0.01f);
		tabs.dispose();
	}

	@Test
	void headerBarCustomGapUpdatesHeaderBarCells() {
		Tabs tabs = new Tabs(Signal.of(0));
		Element a = new Element();
		Element b = new Element();
		tabs.headerBar().add(a);
		tabs.headerBar().add(b);
		tabs.headerGap(12f);

		assertEquals(0f, CellAccess.padLeft(tabs.headerBar().getCell(a)), 0.01f);
		assertEquals(12f, CellAccess.padLeft(tabs.headerBar().getCell(b)), 0.01f);
		assertEquals(0f, CellAccess.padTop(tabs.headerBar().getCell(b)), 0.01f);
		tabs.dispose();
	}

	@Test
	void tabTriggerDefaultStylingUsesRoundedBorderWithTransparentBackground() {
		Assumptions.assumeTrue(Core.scene != null, "Arc Core.scene is null; skipping skin-dependent tests");
		Tabs tabs = new Tabs(Signal.of(0))
				.tab("Tab A", () -> {})
				.tab("Tab B", () -> {});

		assertEquals(2, tabs.buttons().size());
		for (Button btn : tabs.buttons()) {
			assertTrue(btn.sizedButton().getBackground() instanceof RoundedDrawable);
			RoundedDrawable rd = (RoundedDrawable) btn.sizedButton().getBackground();
			assertEquals(10, rd.getRadius());
			assertEquals(Color.clear, rd.getFillColor());
			assertEquals(2f, rd.getStroke(), 0.001f);
			assertEquals(Color.gray, rd.getBorderColor());

			assertSame(rd, btn.sizedButton().getStyle().up);
			assertNotNull(btn.sizedButton().getStyle().checked);
			assertTrue(btn.sizedButton().getStyle().checked instanceof RoundedDrawable);
			assertNotNull(btn.sizedButton().getStyle().over);
			assertTrue(btn.sizedButton().getStyle().over instanceof RoundedDrawable);
			assertNotNull(btn.sizedButton().getStyle().down);
			assertTrue(btn.sizedButton().getStyle().down instanceof RoundedDrawable);
		}
		tabs.dispose();
	}

	@Test
	void tabTriggerCustomButtonStylePreservedWhenSpecified() {
		Assumptions.assumeTrue(Core.scene != null, "Arc Core.scene is null; skipping skin-dependent tests");
		ButtonStyle customStyle = new ButtonStyle();
		Tabs tabs = new Tabs(Signal.of(0))
				.tabStyle(customStyle)
				.tab("Tab A", () -> {});

		assertSame(customStyle, tabs.buttons().get(0).sizedButton().getStyle());
		tabs.dispose();
	}

	@Test
	void tabsInitialAndSignalSwitching() {
		Assumptions.assumeTrue(Core.scene != null, "Arc Core.scene is null; skipping skin-dependent tests");
		Signal<Integer> activeTab = Signal.of(0);

		Tabs tabs = new Tabs(activeTab)
				.tab("Tab 0", () -> Text.of("Content 0"))
				.tab("Tab 1", () -> Text.of("Content 1"))
				.tab("Tab 2", () -> Text.of("Content 2"));

		assertEquals(3, tabs.buttons().size());
		assertEquals(3, tabs.contents().size());

		// Initial: Tab 0 is active
		assertTrue(tabs.buttons().get(0).sizedButton().isChecked());
		assertFalse(tabs.buttons().get(1).sizedButton().isChecked());
		assertFalse(tabs.buttons().get(2).sizedButton().isChecked());

		assertTrue(tabs.contents().get(0).visible);
		assertFalse(tabs.contents().get(1).visible);
		assertFalse(tabs.contents().get(2).visible);

		// Switch to Tab 1 via signal
		activeTab.set(1);
		SignalDispatcher.flush();
		assertFalse(tabs.buttons().get(0).sizedButton().isChecked());
		assertTrue(tabs.buttons().get(1).sizedButton().isChecked());
		assertFalse(tabs.buttons().get(2).sizedButton().isChecked());

		assertFalse(tabs.contents().get(0).visible);
		assertTrue(tabs.contents().get(1).visible);
		assertFalse(tabs.contents().get(2).visible);

		// Switch to Tab 2 via click
		simulateClick(tabs.buttons().get(2));
		SignalDispatcher.flush();
		assertEquals(2, activeTab.get().intValue());

		assertFalse(tabs.buttons().get(0).sizedButton().isChecked());
		assertFalse(tabs.buttons().get(1).sizedButton().isChecked());
		assertTrue(tabs.buttons().get(2).sizedButton().isChecked());

		assertFalse(tabs.contents().get(0).visible);
		assertFalse(tabs.contents().get(1).visible);
		assertTrue(tabs.contents().get(2).visible);

		tabs.dispose();
	}

	@Test
	void tabsSupportsElementAndTableConfig() {
		Tabs tabs = new Tabs(Signal.of(0))
				.width(500f)
				.height(300f)
				.visible(false)
				.name("custom-tabs")
				.padding(12f);

		assertEquals(500f, tabs.element().getWidth());
		assertEquals(300f, tabs.element().getHeight());
		assertFalse(tabs.element().visible);
		assertEquals("custom-tabs", tabs.element().name);
		assertEquals(12f, tabs.table().getMarginTop());
		tabs.dispose();
	}
}
