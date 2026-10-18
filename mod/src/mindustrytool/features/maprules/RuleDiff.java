package mindustrytool.features.maprules;

import arc.util.Nullable;
import mindustry.game.Rules;

public class RuleDiff {

    private static final float EPSILON = 0.0001f;

    private final Rules baseline;

    public RuleDiff() {
        this(new Rules());
    }

    public RuleDiff(Rules baseline) {
        this.baseline = baseline != null ? baseline : new Rules();
    }

    public Rules baseline() {
        return baseline;
    }

    private static boolean differs(float a, float b) {
        return Math.abs(a - b) > EPSILON;
    }

    public boolean isModified(RuleDescriptor def, @Nullable Rules rules) {
        if (def == null || rules == null) {
            return false;
        }
        switch (def.kind) {
            case FLOAT:
                return differs(def.floatGetter.get(rules), def.floatGetter.get(baseline));
            case INT:
                return def.intGetter.get(rules).intValue() != def.intGetter.get(baseline).intValue();
            case BOOL:
                return def.boolGetter.get(rules).booleanValue() != def.boolGetter.get(baseline).booleanValue();
            case BAN_BLOCKS:
                return rules.bannedBlocks != null && baseline.bannedBlocks != null
                        && rules.bannedBlocks.size != baseline.bannedBlocks.size;
            case BAN_UNITS:
                return rules.bannedUnits != null && baseline.bannedUnits != null
                        && rules.bannedUnits.size != baseline.bannedUnits.size;
            default:
                return false;
        }
    }

    public boolean anyModified(@Nullable Rules rules) {
        if (rules == null) {
            return false;
        }
        for (RuleDescriptor def : RuleDescriptors.ALL) {
            if (isModified(def, rules)) {
                return true;
            }
        }
        return false;
    }

    public int modifiedCount(@Nullable Rules rules) {
        if (rules == null) {
            return 0;
        }
        int count = 0;
        for (RuleDescriptor def : RuleDescriptors.ALL) {
            if (isModified(def, rules)) {
                count++;
            }
        }
        return count;
    }
}
