package mindustrytool.features.armycontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.struct.IntSeq;
import arc.struct.ObjectIntMap;
import arc.struct.Seq;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.core.Control;
import mindustry.gen.Groups;
import mindustry.gen.UnitEntity;
import mindustry.input.DesktopInput;
import mindustry.type.UnitType;
import mindustrytool.test.MindustryTestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArmyControlFeatureTest extends MindustryTestEnv {

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

    private static UnitType getOrCreateUnitType(String name) {
        UnitType existing = Vars.content.unit(name);
        return existing != null ? existing : new UnitType(name);
    }

    @BeforeEach
    void initContent() {
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
    }

    @Test
    void testControlGroupCreationAndPruning() {
        ControlGroup group = new ControlGroup(1, "Group 1");
        assertEquals(1, group.getId());
        assertEquals("Group 1", group.getName());
        assertTrue(group.isEmpty());

        IntSeq ids = new IntSeq();
        ids.add(101);
        ids.add(102);
        group.setUnits(ids);
        assertEquals(2, group.size());
        assertFalse(group.isEmpty());

        int remaining = group.pruneDeadUnits();
        assertEquals(0, remaining);
        assertTrue(group.isEmpty());
    }

    @Test
    void testArmyControlFeatureGroupManagement() {
        ArmyControlFeature feature = new ArmyControlFeature();
        assertNotNull(feature.controlGroupsSignal());
        assertNotNull(feature.activeSelectionSignal());

        IntSeq unitIds = new IntSeq();
        unitIds.add(1);
        unitIds.add(2);

        ControlGroup g1 = feature.createGroup("Alpha", unitIds);
        assertNotNull(g1);
        assertEquals(1, g1.getId());
        assertEquals("Alpha", g1.getName());

        Seq<ControlGroup> groups = feature.controlGroupsSignal().get();
        assertEquals(1, groups.size);
        assertEquals(g1, groups.first());

        feature.deleteGroup(g1);
        assertEquals(0, feature.controlGroupsSignal().get().size);
    }

    @Test
    void testActiveSelectionMapReactivity() {
        ArmyControlFeature feature = new ArmyControlFeature();
        assertEquals(0, feature.activeSelectionSignal().get().size);

        UnitType testType = new UnitType("test-unit");
        ObjectIntMap<UnitType> map = new ObjectIntMap<>();
        map.put(testType, 5);

        feature.activeSelectionSignal().set(map);
        assertEquals(5, feature.activeSelectionSignal().get().get(testType, 0));
    }

    @Test
    void testPoolOperationsToggleAllNoneInvert() {
        ArmyControlFeature feature = new ArmyControlFeature();
        UnitType typeA = getOrCreateUnitType("feature-test-type-a");
        UnitType typeB = getOrCreateUnitType("feature-test-type-b");
        UnitType typeC = getOrCreateUnitType("feature-test-type-c");

        UnitEntity u1 = new UnitEntity() {
            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public boolean allowCommand() {
                return true;
            }
        };
        u1.type = typeA;

        UnitEntity u2 = new UnitEntity() {
            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public boolean allowCommand() {
                return true;
            }
        };
        u2.type = typeA;

        UnitEntity u3 = new UnitEntity() {
            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public boolean allowCommand() {
                return true;
            }
        };
        u3.type = typeB;

        UnitEntity u4 = new UnitEntity() {
            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public boolean allowCommand() {
                return true;
            }
        };
        u4.type = typeC;

        Vars.control.input.selectedUnits.addAll(u1, u2, u3, u4);
        feature.refreshActiveSelection();

        assertEquals(2, feature.discoveredTypesSignal().get().get(typeA, 0));
        assertEquals(1, feature.discoveredTypesSignal().get().get(typeB, 0));
        assertEquals(1, feature.discoveredTypesSignal().get().get(typeC, 0));

        assertEquals(2, feature.activeSelectionSignal().get().get(typeA, 0));
        assertEquals(1, feature.activeSelectionSignal().get().get(typeB, 0));
        assertEquals(1, feature.activeSelectionSignal().get().get(typeC, 0));

        feature.selectNonePool();
        assertEquals(0, Vars.control.input.selectedUnits.size);
        assertEquals(0, feature.activeSelectionSignal().get().size);
        assertEquals(2, feature.discoveredTypesSignal().get().get(typeA, 0));
        assertEquals(1, feature.discoveredTypesSignal().get().get(typeB, 0));
        assertEquals(1, feature.discoveredTypesSignal().get().get(typeC, 0));

        feature.toggleUnitType(typeB);
        assertEquals(1, Vars.control.input.selectedUnits.size);
        assertEquals(1, feature.activeSelectionSignal().get().get(typeB, 0));
        assertEquals(0, feature.activeSelectionSignal().get().get(typeA, 0));

        feature.toggleUnitType(typeB);
        assertEquals(0, Vars.control.input.selectedUnits.size);
        assertEquals(0, feature.activeSelectionSignal().get().get(typeB, 0));
        assertEquals(3, feature.discoveredTypesSignal().get().size);

        feature.selectAllPool();
        assertEquals(4, Vars.control.input.selectedUnits.size);
        assertEquals(2, feature.activeSelectionSignal().get().get(typeA, 0));
        assertEquals(1, feature.activeSelectionSignal().get().get(typeB, 0));
        assertEquals(1, feature.activeSelectionSignal().get().get(typeC, 0));

        feature.toggleUnitType(typeA);
        assertEquals(2, Vars.control.input.selectedUnits.size);
        assertEquals(0, feature.activeSelectionSignal().get().get(typeA, 0));
        assertEquals(1, feature.activeSelectionSignal().get().get(typeB, 0));
        assertEquals(1, feature.activeSelectionSignal().get().get(typeC, 0));

        feature.invertPool();
        assertEquals(2, Vars.control.input.selectedUnits.size);
        assertEquals(2, feature.activeSelectionSignal().get().get(typeA, 0));
        assertEquals(0, feature.activeSelectionSignal().get().get(typeB, 0));
        assertEquals(0, feature.activeSelectionSignal().get().get(typeC, 0));

        feature.deselectAll();
        assertEquals(0, Vars.control.input.selectedUnits.size);
        assertEquals(0, feature.discoveredTypesSignal().get().size);
        assertEquals(0, feature.activeSelectionSignal().get().size);
    }

    @Test
    void testPoolPruningDeadUnits() {
        ArmyControlFeature feature = new ArmyControlFeature();
        UnitType typeA = getOrCreateUnitType("feature-test-type-prune");

        final boolean[] alive = new boolean[]{true};
        UnitEntity u1 = new UnitEntity() {
            @Override
            public boolean isValid() {
                return alive[0];
            }

            @Override
            public boolean allowCommand() {
                return true;
            }
        };
        u1.type = typeA;

        Vars.control.input.selectedUnits.add(u1);
        feature.refreshActiveSelection();
        assertEquals(1, feature.discoveredTypesSignal().get().get(typeA, 0));

        alive[0] = false;
        feature.refreshActiveSelection();
        assertEquals(0, feature.discoveredTypesSignal().get().get(typeA, 0));
    }
}
