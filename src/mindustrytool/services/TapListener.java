package mindustrytool.services;

import arc.Core;
import arc.Events;
import arc.math.geom.Vec2;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType.TapEvent;
import mindustry.game.EventType.Trigger;
import mindustry.world.Tile;

import java.util.function.BiConsumer;

public class TapListener {
    private static final TapListener instance = new TapListener();

    public static TapListener getInstance() {
        return instance;
    }

    private BiConsumer<Float, Float> currentListener;

    private Tile touchTile;
    private long touchTime;
    private boolean wasTouched;
    private final ObjectSet<HoldRegistration<?>> triggeredListeners = new ObjectSet<>();
    private final Seq<HoldRegistration<?>> holdListeners = new Seq<>();

    public static class HoldRegistration<T> implements Comparable<HoldRegistration<?>> {
        public long duration;
        public int order;
        public T data;
        public BiConsumer<Tile, T> callback;

        public HoldRegistration(long duration, int order, T data, BiConsumer<Tile, T> callback) {
            this.duration = duration;
            this.order = order;
            this.data = data;
            this.callback = callback;
        }

        @Override
        public int compareTo(HoldRegistration<?> o) {
            return Integer.compare(this.order, o.order);
        }
    }

    public <T> void registerHoldListener(long duration, int order, T data, BiConsumer<Tile, T> callback) {
        holdListeners.add(new HoldRegistration<>(duration, order, data, callback));
        holdListeners.sort();
    }

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

        Events.run(Trigger.update, () -> {
            if (Vars.state == null || Vars.state.isMenu() || Core.scene.hasMouse()) {
                resetHold();
                return;
            }

            if (Core.input.isTouched()) {
                Vec2 pos = Core.input.mouseWorld();
                Tile currentTile = Vars.world.tileWorld(pos.x, pos.y);

                if (!wasTouched) {
                    wasTouched = true;
                    touchTime = Time.millis();
                    touchTile = currentTile;
                    triggeredListeners.clear();
                } else if (currentTile != touchTile) {
                    // Reset if dragged to a different tile
                    touchTile = currentTile;
                    touchTime = Time.millis();
                    triggeredListeners.clear();
                }

                if (touchTile != null) {
                    long holdDuration = Time.timeSinceMillis(touchTime);
                    for (HoldRegistration<?> listener : holdListeners) {
                        if (holdDuration >= listener.duration && !triggeredListeners.contains(listener)) {
                            triggeredListeners.add(listener);
                            invokeCallback(listener, touchTile);
                        }
                    }
                }
            } else {
                resetHold();
            }
        });
    }

    private void resetHold() {
        wasTouched = false;
        touchTile = null;
        triggeredListeners.clear();
    }

    private <T> void invokeCallback(HoldRegistration<T> listener, Tile tile) {
        listener.callback.accept(tile, listener.data);
    }

    public void select(BiConsumer<Float, Float> onSelect) {
        currentListener = onSelect;
    }
}
