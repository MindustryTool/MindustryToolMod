package mindustrytool.features.armycontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import arc.Core;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Font.FontData;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Button;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.struct.IntSeq;
import arc.struct.Seq;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.core.Control;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.UnitEntity;
import mindustry.input.DesktopInput;
import mindustry.type.UnitType;
import mindustry.ui.Fonts;
import mindustrytool.test.MindustryTestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.reactive.Readable;

class ArmyControlCompactContentTest extends MindustryTestEnv {

    private static Control createMockControl() {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field f = unsafeClass.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object unsafe = f.get(null);
            Method allocate = unsafeClass.getMethod("allocateInstance", Class.class);
            return (Control) allocate.invoke(unsafe, Control.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void init() {
        if (Vars.content == null) {
            Vars.content = new ContentLoader();
        }
        if (Groups.unit == null) {
            Groups.init();
        }
        if (Vars.control == null) {
            Vars.control = createMockControl();
        }
        Vars.control.input = new DesktopInput();
        Vars.control.input.selectedUnits.clear();
        Icon.map = new TextureRegionDrawable(new TextureRegion());
        Icon.zoom = new TextureRegionDrawable(new TextureRegion());
        Icon.exit = new TextureRegionDrawable(new TextureRegion());
        Icon.add = new TextureRegionDrawable(new TextureRegion());
        Icon.trash = new TextureRegionDrawable(new TextureRegion());
        Fonts.def = new Font(new FontData() {
            @Override
            public boolean hasGlyph(char ch) {
                return true;
            }
        }, new TextureRegion(), false);
    }

    @Test
    void testDividersCollapseWithSections() {
        ArmyControlFeature feature = new ArmyControlFeature();

        UnitType typeA = new UnitType("compact-unit-a");
        typeA.uiIcon = new TextureRegion();
        Vars.control.input.selectedUnits.addAll(validUnit(typeA), validUnit(typeA));
        feature.refreshActiveSelection();

        ArmyControlCompactContent withSelection = new ArmyControlCompactContent(feature, Readable.of(1f));
        assertNotNull(withSelection.element());
        flushEffects();
        assertEquals(1, countDividers(withSelection.element()));
        layoutTree(withSelection.element(), 260f);
        assertDividersFillWidth(withSelection.element(), 260f);
        assertEquals(false, containsGhostHint(withSelection.element()));
        assertChipHeightsMatchRemoveButton(withSelection.element(), 1);
        withSelection.dispose();

        feature.deselectAll();
        ArmyControlCompactContent empty = new ArmyControlCompactContent(feature, Readable.of(1f));
        assertNotNull(empty.element());
        flushEffects();
        assertEquals(0, countDividers(empty.element()));
        assertEquals(true, containsGhostHint(empty.element()));
        assertEquals(4, countButtons(empty.element()));
        empty.dispose();

        Vars.control.input.selectedUnits.addAll(validUnit(typeA));
        feature.refreshActiveSelection();
        IntSeq ids = new IntSeq();
        ids.add(1);
        ids.add(2);
        feature.createGroup("Group 1", ids);
        ArmyControlCompactContent withGroups = new ArmyControlCompactContent(feature, Readable.of(1f));
        assertNotNull(withGroups.element());
        flushEffects();
        assertEquals(2, countDividers(withGroups.element()));
        layoutTree(withGroups.element(), 260f);
        assertDividersFillWidth(withGroups.element(), 260f);
        assertEquals(false, containsGhostHint(withGroups.element()));
        assertChipHeightsMatchRemoveButton(withGroups.element(), 1);
        withGroups.dispose();

        feature.deselectAll();
        ArmyControlCompactContent groupsOnly = new ArmyControlCompactContent(feature, Readable.of(1f));
        assertNotNull(groupsOnly.element());
        flushEffects();
        assertEquals(1, countDividers(groupsOnly.element()));
        assertEquals(false, containsGhostHint(groupsOnly.element()));
        groupsOnly.dispose();
    }

    private static UnitEntity validUnit(UnitType type) {
        UnitEntity u = new UnitEntity() {
            @Override
            public boolean isValid() {
                return true;
            }
        };
        u.type = type;
        return u;
    }

    private static int countDividers(Element root) {
        Seq<String> names = new Seq<>();
        collectNames(root, names);
        int dividers = 0;
        for (int i = 0; i < names.size; i++) {
            if ("solim-divider".equals(names.get(i))) {
                dividers++;
            }
        }
        return dividers;
    }

    private static int countButtons(Element root) {
        Seq<Element> buttons = new Seq<>();
        collectButtons(root, buttons);
        return buttons.size;
    }

    private static void collectButtons(Element element, Seq<Element> out) {
        if (element instanceof Button) {
            out.add(element);
        }
        if (element instanceof Group) {
            for (Element child : ((Group) element).getChildren()) {
                collectButtons(child, out);
            }
        }
    }

    /**
     * Chips (icon + count) must match the remove button height (unit 9).
     * Identifies chips as buttons containing both an Image and a Label:
     * toolbar and remove buttons carry icons only, and recall rows render
     * label-only headless with no living units to summarize.
     */
    private static void assertChipHeightsMatchRemoveButton(Element root, int expectedChips) {
        Seq<Element> buttons = new Seq<>();
        collectButtons(root, buttons);
        int chips = 0;
        for (int i = 0; i < buttons.size; i++) {
            Element button = buttons.get(i);
            if (containsType(button, Image.class) && containsType(button, Label.class)) {
                chips++;
                float height = button.getHeight();
                if (!(Math.abs(height - 36f) < 1f)) {
                    throw new AssertionError("Chip height " + height + " does not match remove button height 36");
                }
            }
        }
        assertEquals(expectedChips, chips);
    }

    private static boolean containsType(Element root, Class<?> type) {
        if (type.isInstance(root)) {
            return true;
        }
        if (root instanceof Group) {
            for (Element child : ((Group) root).getChildren()) {
                if (containsType(child, type)) {
                    return true;
                }
            }
        }
        return false;
    }


    private static void assertDividersFillWidth(Element root, float contentWidth) {
        Seq<Element> dividers = new Seq<>();
        collectByName(root, dividers, "solim-divider");
        for (int i = 0; i < dividers.size; i++) {
            float w = dividers.get(i).getWidth();
            if (!(w > contentWidth / 2f)) {
                throw new AssertionError("Divider width " + w + " does not fill content width " + contentWidth);
            }
        }
    }

    private static void layoutTree(Element root, float width) {
        if (root instanceof Table) {
            ((Table) root).setWidth(width);
        }
        Seq<Table> tables = new Seq<>();
        collectTables(root, tables);
        for (int i = 0; i < tables.size; i++) {
            tables.get(i).validate();
        }
    }

    private static void collectTables(Element element, Seq<Table> out) {
        if (element instanceof Table) {
            out.add((Table) element);
        }
        if (element instanceof Group) {
            for (Element child : ((Group) element).getChildren()) {
                collectTables(child, out);
            }
        }
    }

    private static boolean containsGhostHint(Element root) {
        String expected = Core.bundle.get("feature.army-control.empty.all", "Nothing selected yet");
        Seq<String> texts = new Seq<>();
        collectLabelTexts(root, texts);
        for (int i = 0; i < texts.size; i++) {
            if (expected.equals(texts.get(i))) {
                return true;
            }
        }
        return false;
    }

    private static void collectLabelTexts(Element element, Seq<String> out) {
        if (element instanceof Label) {
            CharSequence text = ((Label) element).getText();
            out.add(text != null ? text.toString() : "");
        }
        if (element instanceof Group) {
            for (Element child : ((Group) element).getChildren()) {
                collectLabelTexts(child, out);
            }
        }
    }

    private static void collectByName(Element element, Seq<Element> out, String name) {
        if (name.equals(element.name)) {
            out.add(element);
        }
        if (element instanceof Group) {
            for (Element child : ((Group) element).getChildren()) {
                collectByName(child, out, name);
            }
        }
    }

    private static void collectNames(Element element, Seq<String> out) {        if (element.name != null) {
            out.add(element.name);
        }
        if (element instanceof Group) {
            for (Element child : ((Group) element).getChildren()) {
                collectNames(child, out);
            }
        }
    }
}
