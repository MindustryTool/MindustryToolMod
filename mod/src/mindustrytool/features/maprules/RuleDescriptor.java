package mindustrytool.features.maprules;

import arc.func.Cons2;
import arc.func.Func;
import arc.util.Nullable;
import mindustry.game.Rules;

/**
 * Single source of truth for one map rule: its identity, category, control
 * range, and typed accessors. Diff, dispatch, reset, rows, and search all
 * derive from these descriptors instead of repeating per-field code.
 */
public class RuleDescriptor {

    public static final String CAT_ALL = "all";
    public static final String CAT_MODIFIED = "modified";
    public static final String CAT_MULTIPLIERS = "multipliers";
    public static final String CAT_WAVES = "waves";
    public static final String CAT_BANS = "bans";
    public static final String CAT_ENVIRONMENT = "environment";
    public static final String CAT_MECHANICS = "mechanics";

    public enum Kind {
        FLOAT,
        INT,
        BOOL,
        BAN_BLOCKS,
        BAN_UNITS
    }

    public final String id;
    public final String fieldName;
    public final String category;
    public final Kind kind;
    public final float min;
    public final float max;
    public final float step;
    public final @Nullable Func<Rules, Float> floatGetter;
    public final @Nullable Cons2<Rules, Float> floatSetter;
    public final @Nullable Func<Rules, Integer> intGetter;
    public final @Nullable Cons2<Rules, Integer> intSetter;
    public final @Nullable Func<Rules, Boolean> boolGetter;
    public final @Nullable Cons2<Rules, Boolean> boolSetter;

    private RuleDescriptor(String id, String fieldName, String category, Kind kind,
            float min, float max, float step,
            @Nullable Func<Rules, Float> floatGetter, @Nullable Cons2<Rules, Float> floatSetter,
            @Nullable Func<Rules, Integer> intGetter, @Nullable Cons2<Rules, Integer> intSetter,
            @Nullable Func<Rules, Boolean> boolGetter, @Nullable Cons2<Rules, Boolean> boolSetter) {
        this.id = id;
        this.fieldName = fieldName;
        this.category = category;
        this.kind = kind;
        this.min = min;
        this.max = max;
        this.step = step;
        this.floatGetter = floatGetter;
        this.floatSetter = floatSetter;
        this.intGetter = intGetter;
        this.intSetter = intSetter;
        this.boolGetter = boolGetter;
        this.boolSetter = boolSetter;
    }

    public static RuleDescriptor floatRule(String id, String category, String fieldName,
            float min, float max, float step,
            Func<Rules, Float> getter, Cons2<Rules, Float> setter) {
        return new RuleDescriptor(id, fieldName, category, Kind.FLOAT, min, max, step,
                getter, setter, null, null, null, null);
    }

    public static RuleDescriptor intRule(String id, String category, String fieldName,
            int min, int max, int step,
            Func<Rules, Integer> getter, Cons2<Rules, Integer> setter) {
        return new RuleDescriptor(id, fieldName, category, Kind.INT, min, max, step,
                null, null, getter, setter, null, null);
    }

    public static RuleDescriptor boolRule(String id, String category, String fieldName,
            Func<Rules, Boolean> getter, Cons2<Rules, Boolean> setter) {
        return new RuleDescriptor(id, fieldName, category, Kind.BOOL, 0f, 0f, 0f,
                null, null, null, null, getter, setter);
    }

    public static RuleDescriptor banBlocks() {
        return new RuleDescriptor("bans", "bannedBlocks", CAT_BANS, Kind.BAN_BLOCKS,
                0f, 0f, 0f, null, null, null, null, null, null);
    }

    public static RuleDescriptor banUnits() {
        return new RuleDescriptor("bans", "bannedUnits", CAT_BANS, Kind.BAN_UNITS,
                0f, 0f, 0f, null, null, null, null, null, null);
    }

    public boolean isScalar() {
        return kind == Kind.FLOAT || kind == Kind.INT || kind == Kind.BOOL;
    }

    public String nameKey() {
        return "feature.map-rules.rule." + id;
    }

    public String descKey() {
        return "feature.map-rules.rule." + id + ".desc";
    }
}
