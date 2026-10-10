package mindustrytool.features.maprules;

import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.game.Rules;
import mindustry.gen.Call;

/**
 * Applies rule changes according to the current edit mode.
 * Continuous controls (sliders) are debounced so dragging applies once the
 * value settles instead of spamming mutations or network broadcasts.
 */
public class RuleMutator {

    private static final float DEBOUNCE_SECONDS = 0.4f;

    private final ObjectMap<String, Timer.Task> pendingTasks = new ObjectMap<>();
    private final ObjectMap<String, Runnable> pendingActions = new ObjectMap<>();
    private @Nullable Runnable resyncHandler;

    public void setResyncHandler(@Nullable Runnable handler) {
        this.resyncHandler = handler;
    }

    public void scheduleResync() {
        if (resyncHandler == null) {
            return;
        }
        float[] delays = new float[]{0.5f, 1.5f, 3f};
        for (int i = 0; i < delays.length; i++) {
            final float delay = delays[i];
            Timer.schedule(() -> {
                if (resyncHandler != null) {
                    resyncHandler.run();
                }
            }, delay);
        }
    }

    public @Nullable Rules currentRules() {
        return Vars.state != null ? Vars.state.rules : null;
    }

    public void mutate(Cons<Rules> change) {
        mutate(change, true);
    }

    public void mutateVisual(Cons<Rules> change) {
        Rules rules = currentRules();
        if (rules == null || change == null) {
            return;
        }
        try {
            change.get(rules);
        } catch (Exception e) {
            Log.err("MapRules: failed to apply visual override", e);
        }
    }

    public void mutate(Cons<Rules> change, boolean broadcast) {
        Rules rules = currentRules();
        if (rules == null || change == null) {
            return;
        }
        RuleEditMode mode = RuleEditMode.resolve();
        if (mode == RuleEditMode.CLIENT_READONLY) {
            return;
        }
        if (mode == RuleEditMode.CLIENT_ADMIN) {
            return;
        }
        try {
            change.get(rules);
        } catch (Exception e) {
            Log.err("MapRules: failed to mutate rules", e);
            return;
        }
        if (broadcast && mode == RuleEditMode.HOST) {
            try {
                Call.setRules(rules);
            } catch (Exception e) {
                Log.err("MapRules: failed to broadcast rules", e);
            }
        }
    }

    public void mutateRemote(String jsStatement) {
        if (jsStatement == null || jsStatement.trim().isEmpty()) {
            return;
        }
        try {
            Call.sendChatMessage("/js " + Strings.format(jsStatement, new Object[0]));
        } catch (Exception e) {
            Log.err("MapRules: failed to send remote rule change", e);
        }
    }

    public void setFloat(RuleDescriptor def, float value) {
        if (def == null || def.floatSetter == null) {
            return;
        }
        RuleEditMode mode = RuleEditMode.resolve();
        if (mode == RuleEditMode.CLIENT_READONLY) {
            return;
        }
        if (mode == RuleEditMode.CLIENT_ADMIN) {
            final float next = value;
            debounce("js:" + def.id, () -> {
                mutateVisual(rules -> def.floatSetter.get(rules, next));
                mutateRemote("Vars.state.rules." + def.fieldName + " = " + next + "; Call.setRules(Vars.state.rules);");
                scheduleResync();
            });
            return;
        }
        final float next = value;
        debounce(def.id, () -> mutate(rules -> def.floatSetter.get(rules, next)));
    }

    public void setInt(RuleDescriptor def, int value) {
        if (def == null || def.intSetter == null) {
            return;
        }
        RuleEditMode mode = RuleEditMode.resolve();
        if (mode == RuleEditMode.CLIENT_READONLY) {
            return;
        }
        if (mode == RuleEditMode.CLIENT_ADMIN) {
            final int next = value;
            debounce("js:" + def.id, () -> {
                mutateVisual(rules -> def.intSetter.get(rules, next));
                mutateRemote("Vars.state.rules." + def.fieldName + " = " + next + "; Call.setRules(Vars.state.rules);");
                scheduleResync();
            });
            return;
        }
        final int next = value;
        debounce(def.id, () -> mutate(rules -> def.intSetter.get(rules, next)));
    }

    private void debounce(String key, Runnable action) {
        Timer.Task old = pendingTasks.remove(key);
        if (old != null) {
            old.cancel();
        }
        pendingActions.put(key, action);
        pendingTasks.put(key, Timer.schedule(() -> {
            pendingTasks.remove(key);
            Runnable run = pendingActions.remove(key);
            if (run != null) {
                run.run();
            }
        }, DEBOUNCE_SECONDS));
    }

    public void flushPending() {
        Seq<String> keys = new Seq<>();
        for (String key : pendingTasks.keys()) {
            keys.add(key);
        }
        for (int i = 0; i < keys.size; i++) {
            String key = keys.get(i);
            Timer.Task task = pendingTasks.remove(key);
            if (task != null) {
                task.cancel();
            }
            Runnable run = pendingActions.remove(key);
            if (run != null) {
                run.run();
            }
        }
    }

    public void setBool(RuleDescriptor def, boolean value) {
        if (def == null || def.boolSetter == null) {
            return;
        }
        RuleEditMode mode = RuleEditMode.resolve();
        if (mode == RuleEditMode.CLIENT_ADMIN) {
            mutateVisual(rules -> def.boolSetter.get(rules, value));
            mutateRemote("Vars.state.rules." + def.fieldName + " = " + value + "; Call.setRules(Vars.state.rules);");
            scheduleResync();
            return;
        }
        final boolean next = value;
        mutate(rules -> def.boolSetter.get(rules, next));
    }

    public void setVisualBool(RuleDescriptor def, boolean value) {
        if (def == null || def.boolSetter == null) {
            return;
        }
        final boolean next = value;
        mutateVisual(rules -> def.boolSetter.get(rules, next));
    }

}
