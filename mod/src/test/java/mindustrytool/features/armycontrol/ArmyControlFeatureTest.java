package mindustrytool.features.armycontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.struct.IntSeq;
import arc.struct.ObjectIntMap;
import arc.struct.Seq;
import mindustry.core.ContentLoader;
import mindustry.type.UnitType;
import mindustrytool.test.MindustryTestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArmyControlFeatureTest extends MindustryTestEnv {

    @BeforeEach
    void initContent() {
        if (mindustry.Vars.content == null) {
            mindustry.Vars.content = new ContentLoader();
        }
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
}
