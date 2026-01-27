package mindustrytool.features.godmode;

import arc.Events;
import mindustry.game.EventType.TapEvent;

import java.util.function.BiConsumer;

public class TapListener {
    private static BiConsumer<Float, Float> currentListener;

    public static void init() {
        Events.on(TapEvent.class, e -> {
            if (currentListener != null) {
                BiConsumer<Float, Float> listener = currentListener;

                float worldX = e.tile.worldx();
                float worldY = e.tile.worldy();

                currentListener = null;

                listener.accept(worldX, worldY);
            }
        });
    }

    public static void select(BiConsumer<Float, Float> onSelect) {
        currentListener = onSelect;
    }
}
