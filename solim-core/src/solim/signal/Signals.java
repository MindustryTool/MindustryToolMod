package solim.signal;

import arc.Core;
import arc.Events;
import mindustry.game.EventType.ResizeEvent;

public final class Signals {
    private static final Signal<Boolean> portrait = Signal.of(false);
    private static boolean initialized = false;

    static {
        init();
    }

    private Signals() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }

        Events.on(ResizeEvent.class, e -> {
            Core.app.post(() -> portrait.set(Core.graphics.isPortrait()));
        });

        initialized = true;
    }

    public static Readable<Boolean> isPortrait() {
        return portrait;
    }
}
