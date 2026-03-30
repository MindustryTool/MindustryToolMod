package mindustrytool.features.godmode;

import arc.Events;
import mindustry.game.EventType.TapEvent;

import java.util.function.BiConsumer;

public class TapListener {
    private static final TapListener instance = new TapListener();

    public static TapListener getInstance() {
        return instance;
    }

    private BiConsumer<Float, Float> currentListener;

    public void init() {
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

    public void select(BiConsumer<Float, Float> onSelect) {
        currentListener = onSelect;
    }
}
