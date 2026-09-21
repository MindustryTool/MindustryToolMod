package mindustrytool.input;

import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Time;
import mindustry.gen.Unit;

/**
 * Shared fault tolerance for copied vanilla movement logic.
 * Stale third-party units compiled against an older Mindustry may lack newer
 * {@code Weaponsc} methods, throwing {@link LinkageError} (e.g.
 * {@code AbstractMethodError} on {@code aim}) on the render thread.
 * The latch records such unit classes once and skips their aim/weapon section
 * proactively; transient failures are throttled and retried.
 */
public final class MovementAimGuard {

    private static final long THROTTLE_MILLIS = 5000L;

    private static final ObjectSet<String> brokenAimTypes = new ObjectSet<>();
    private static final ObjectMap<String, Long> lastLogMillis = new ObjectMap<>();

    private MovementAimGuard() {
    }

    public static boolean shouldSkipWeapons(@Nullable Unit unit) {
        String key = key(unit);
        return key != null && brokenAimTypes.contains(key);
    }

    public static void reportMovementFailure(@Nullable Unit unit, Throwable t) {
        if (t instanceof LinkageError) {
            latchBroken(unit, t);
            return;
        }
        String key = key(unit);
        long now = Time.millis();
        Long last = key == null ? null : lastLogMillis.get(key);
        boolean due = last == null || now - last >= THROTTLE_MILLIS;
        if (due) {
            if (key != null) {
                lastLogMillis.put(key, now);
            }
            Log.err("Error in movement update for " + describe(unit), t);
        }
    }

    public static void latchBroken(@Nullable Unit unit, Throwable t) {
        String key = key(unit);
        boolean first = key == null || !brokenAimTypes.contains(key);
        if (key != null) {
            brokenAimTypes.add(key);
        }
        if (first) {
            Log.err("Stale unit aim API, skipping aim for " + describe(unit), t);
        }
    }

    static void clearForTesting() {
        brokenAimTypes.clear();
        lastLogMillis.clear();
    }

    private static @Nullable String key(@Nullable Unit unit) {
        return unit == null ? null : unit.getClass().getName();
    }

    private static String describe(@Nullable Unit unit) {
        String cls = unit == null ? "null" : unit.getClass().getName();
        String type = unit == null || unit.type == null ? "unknown-type" : unit.type.name;
        return cls + "[type=" + type + "]";
    }
}
