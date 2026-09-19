package mindustrytool.features.godmode;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.func.Cons2;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.game.EventType.TapEvent;

public final class MapPositionPicker {

    static @Nullable Cons<TapEvent> currentListener;

    private MapPositionPicker() {
    }

    public static synchronized void pick(@Nullable Runnable onStart, Cons2<Float, Float> onPicked) {
        cancel();

        if (onStart != null) {
            onStart.run();
        }

        if (Vars.ui != null && Vars.ui.hudfrag != null && Core.bundle != null) {
            Vars.ui.hudfrag.showToast(Core.bundle.get("feature.god-mode.picker.tap-instruction"));
        }

        Cons<TapEvent> listener = new Cons<TapEvent>() {
            @Override
            public void get(TapEvent event) {
                if (currentListener != this) {
                    return;
                }
                if (event.tile != null) {
                    cancel();
                    onPicked.get(event.tile.worldx(), event.tile.worldy());
                }
            }
        };

        currentListener = listener;
        Events.on(TapEvent.class, listener);
    }

    public static synchronized void cancel() {
        if (currentListener != null) {
            Cons<TapEvent> listener = currentListener;
            currentListener = null;
            removeListener(listener);
        }
    }

    private static void removeListener(Cons<TapEvent> listener) {
        if (Core.app != null) {
            Core.app.post(() -> Events.remove(TapEvent.class, listener));
        } else {
            Events.remove(TapEvent.class, listener);
        }
    }
}
