package mindustrytool.features.armycontrol;

import arc.struct.IntSeq;
import arc.struct.ObjectIntMap;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.UnitType;

/**
 * Represents a saved RTS army control group of unit IDs.
 */
public class ControlGroup {

    private final int id;
    private String name;
    private final IntSeq unitIds = new IntSeq();

    public ControlGroup(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IntSeq getUnitIds() {
        return unitIds;
    }

    public void setUnits(IntSeq ids) {
        unitIds.clear();
        unitIds.addAll(ids);
    }

    public void clear() {
        unitIds.clear();
    }

    public boolean isEmpty() {
        return unitIds.isEmpty();
    }

    public int size() {
        return unitIds.size;
    }

    /**
     * Prunes dead or missing units from this group and returns the number of living units remaining.
     */
    public int pruneDeadUnits() {
        for (int i = unitIds.size - 1; i >= 0; i--) {
            int uid = unitIds.get(i);
            Unit u = Groups.unit != null ? Groups.unit.getByID(uid) : null;
            if (u == null || !u.isValid()) {
                unitIds.removeIndex(i);
            }
        }
        return unitIds.size;
    }

    /**
     * Calculates a summary mapping each UnitType in this group to its living count.
     */
    public ObjectIntMap<UnitType> summarizeUnits() {
        ObjectIntMap<UnitType> summary = new ObjectIntMap<>();
        for (int i = 0; i < unitIds.size; i++) {
            int uid = unitIds.get(i);
            Unit u = Groups.unit != null ? Groups.unit.getByID(uid) : null;
            if (u != null && u.isValid() && u.type != null) {
                summary.put(u.type, summary.get(u.type, 0) + 1);
            }
        }
        return summary;
    }
}
