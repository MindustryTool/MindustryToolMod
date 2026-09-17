package solim.modifier;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Font.FontData;
import arc.graphics.g2d.TextureRegion;
import arc.mock.MockApplication;
import arc.mock.MockGL20;
import arc.mock.MockGraphics;
import arc.scene.Element;
import arc.scene.Scene;
import arc.scene.event.EventListener;
import arc.scene.ui.Label;
import arc.scene.ui.Label.LabelStyle;
import arc.scene.ui.Tooltip;
import arc.scene.ui.layout.Table;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.input.Button;
import solim.runtime.SignalDispatcher;
import solim.reactive.Signal;

class ElementConfigTooltipTest {

    private static Scene prevScene;
    private static boolean createdScene;

    @BeforeAll
    static void initArc() {
        if (Core.app == null) {
            Core.app = new MockApplication();
        }
        if (Core.graphics == null) {
            Core.graphics = new MockGraphics();
        }
        if (Core.gl == null) {
            Core.gl = new MockGL20();
            Core.gl20 = (MockGL20) Core.gl;
        }
        prevScene = Core.scene;
        if (Core.scene == null) {
            Core.scene = new Scene();
            createdScene = true;
        }
        try {
            Core.scene.getStyle(LabelStyle.class);
        } catch (IllegalArgumentException missing) {
            LabelStyle style = new LabelStyle();
            FontData fontData = new FontData() {
                @Override
                public boolean hasGlyph(char ch) {
                    return true;
                }
            };
            style.font = new Font(fontData, new TextureRegion(), false);
            Core.scene.addStyle(LabelStyle.class, style);
        }
    }

    @AfterAll
    static void tearDownArc() {
        if (createdScene) {
            Core.scene = null;
        } else {
            Core.scene = prevScene;
        }
    }

    @Test
    void attachesStaticTooltip() {
        TestTarget target = new TestTarget();
        target.tooltip("Hello World");

        Tooltip tooltip = findTooltip(target.element());
        assertNotNull(tooltip, "Tooltip listener should be attached");
        assertTrue(tooltip.container.getChildren().size > 0, "Tooltip container should have children");
    }

    @Test
    void repeatedTooltipReplacesPrevious() {
        TestTarget target = new TestTarget();
        target.tooltip("First");
        target.tooltip("Second");

        assertEquals(1, tooltipCount(target.element()), "Repeated tooltip call must not stack listeners");
    }

    @Test
    void nullOrEmptyClearsTooltip() {
        TestTarget target = new TestTarget();
        target.tooltip("Temporary");
        assertEquals(1, tooltipCount(target.element()));

        target.tooltip((String) null);
        assertEquals(0, tooltipCount(target.element()), "Null string should clear tooltip");

        target.tooltip("Temporary again");
        assertEquals(1, tooltipCount(target.element()));

        target.tooltip("");
        assertEquals(0, tooltipCount(target.element()), "Empty string should clear tooltip");
    }

    @Test
    void reactiveTooltipUpdatesDynamically() {
        TestTarget target = new TestTarget();
        Signal<String> tip = Signal.of("First message");
        target.tooltip(tip);

        Tooltip tooltip = findTooltip(target.element());
        assertNotNull(tooltip, "Reactive tooltip should be attached");

        Element child = tooltip.container.getChildren().first();
        assertTrue(child instanceof Label, "Tooltip content should be a Label");
        assertEquals("First message", ((Label) child).getText().toString());

        tip.set("Second message");
        SignalDispatcher.flush();
        assertEquals("Second message", ((Label) child).getText().toString());
    }

    @Test
    void customTooltipBuilderAttachesConfiguredTooltip() {
        TestTarget target = new TestTarget();
        boolean[] built = { false };
        target.tooltip(t -> {
            built[0] = true;
            t.add("Custom content");
        });

        assertTrue(built[0], "Custom builder should be invoked");
        assertEquals(1, tooltipCount(target.element()), "Custom tooltip listener should be attached");
    }

    @Test
    void buttonInheritsElementConfigTooltip() {
        Button btn = new Button();
        btn.tooltip("Button tooltip");

        assertEquals(1, tooltipCount(btn.element()), "Button should inherit tooltip from ElementConfig");
        btn.dispose();
    }

    private static int tooltipCount(Element element) {
        int count = 0;
        for (EventListener listener : element.getListeners()) {
            if (listener instanceof Tooltip) {
                count++;
            }
        }
        return count;
    }

    private static Tooltip findTooltip(Element element) {
        for (EventListener listener : element.getListeners()) {
            if (listener instanceof Tooltip) {
                return (Tooltip) listener;
            }
        }
        return null;
    }

    private static final class TestTarget implements ElementConfig<TestTarget> {
        private final Element element = new Table();

        @Override
        public Element element() {
            return element;
        }

        @Override
        public TestTarget self() {
            return this;
        }
    }
}
