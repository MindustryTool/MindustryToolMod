package mindustrytool.features.godmode;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.func.Cons2;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.game.EventType.TapEvent;

public class MapPositionPicker {

    @SuppressWarnings("unchecked")
    public static void pick(@Nullable Runnable onStart, Cons2<Float, Float> onPicked) {
        if (onStart != null) {
            onStart.run();
        }

        if (Vars.ui != null && Vars.ui.hudfrag != null) {
            Vars.ui.hudfrag.showToast(Core.bundle.get("feature.god-mode.picker.tap-instruction"));
        }

        Cons<TapEvent>[] holder = (Cons<TapEvent>[]) new Cons[1];
        holder[0] = event -> {
            if (event.tile != null) {
                Events.remove(TapEvent.class, holder[0]);
                onPicked.get(event.tile.worldx(), event.tile.worldy());
            }
        };
        Events.on(TapEvent.class, holder[0]);
    }
}
