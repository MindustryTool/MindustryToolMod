package mindustrytool.features.armycontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import arc.graphics.g2d.Font;
import arc.graphics.g2d.Font.FontData;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.style.TextureRegionDrawable;
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

        ArmyControlCompactContent withSelection = new ArmyControlCompactContent(feature, null);
        assertNotNull(withSelection.element());
        flushEffects();
        assertEquals(1, countDividers(withSelection.element()));
        layoutTree(withSelection.element(), 260f);
        assertDividersFillWidth(withSelection.element(), 260f);
        withSelection.dispose();

        feature.deselectAll();
        ArmyControlCompactContent empty = new ArmyControlCompactContent(feature, null);
        assertNotNull(empty.element());
        flushEffects();
        assertEquals(0, countDividers(empty.element()));
        empty.dispose();

        Vars.control.input.selectedUnits.addAll(validUnit(typeA));
        feature.refreshActiveSelection();
        IntSeq ids = new IntSeq();
        ids.add(1);
        ids.add(2);
        feature.createGroup("Group 1", ids);
        ArmyControlCompactContent withGroups = new ArmyControlCompactContent(feature, null);
        assertNotNull(withGroups.element());
        flushEffects();
        assertEquals(2, countDividers(withGroups.element()));
        layoutTree(withGroups.element(), 260f);
        assertDividersFillWidth(withGroups.element(), 260f);
        withGroups.dispose();
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

    /**
     * Guards the invisible-divider regression: after laying the content out at
     * popup width, every divider must actually fill the width. Without a growX
     * cell the When container sizes to the divider's ~zero preferred width and
     * the line never shows.
     */
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
