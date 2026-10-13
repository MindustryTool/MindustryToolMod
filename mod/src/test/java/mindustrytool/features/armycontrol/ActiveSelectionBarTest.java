package mindustrytool.features.armycontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import arc.struct.ObjectIntMap;
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

class ActiveSelectionBarTest extends MindustryTestEnv {

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
    }

    @Test
    void testActiveSelectionBarElementCreationAndDeselection() {
        ArmyControlFeature feature = new ArmyControlFeature();
        ActiveSelectionBar bar = new ActiveSelectionBar(feature);

        assertNotNull(bar.element());

        UnitType typeA = new UnitType("unit-a");
        UnitType typeB = new UnitType("unit-b");

        UnitEntity u1 = new UnitEntity() {
            @Override
            public boolean isValid() {
                return true;
            }
        };
        u1.type = typeA;

        UnitEntity u2 = new UnitEntity() {
            @Override
            public boolean isValid() {
                return true;
            }
        };
        u2.type = typeA;

        UnitEntity u3 = new UnitEntity() {
            @Override
            public boolean isValid() {
                return true;
            }
        };
        u3.type = typeB;

        Vars.control.input.selectedUnits.addAll(u1, u2, u3);
        feature.refreshActiveSelection();

        ObjectIntMap<UnitType> map = feature.activeSelectionSignal().get();
        assertEquals(2, map.get(typeA, 0));
        assertEquals(1, map.get(typeB, 0));

        feature.deselectUnitType(typeA);

        assertEquals(1, mindustry.Vars.control.input.selectedUnits.size);
        assertEquals(u3, mindustry.Vars.control.input.selectedUnits.first());

        ObjectIntMap<UnitType> mapAfter = feature.activeSelectionSignal().get();
        assertEquals(0, mapAfter.get(typeA, 0));
        assertEquals(1, mapAfter.get(typeB, 0));

        feature.deselectAll();
        assertEquals(0, mindustry.Vars.control.input.selectedUnits.size);
        assertEquals(0, feature.activeSelectionSignal().get().size);

        bar.dispose();
    }
}
