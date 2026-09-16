package solim.modifier;

import arc.scene.Element;
import arc.scene.event.ClickListener;
import arc.scene.event.EventListener;
import arc.scene.event.InputEvent;
import arc.util.Log;
import arc.util.Nullable;

/**
 * Marker click listener installed by {@link ElementConfig#onClick} on an
 * element. It carries the current handler and propagation flag so the generic
 * mixin can replace or update them without a global registry.
 */
class ElementClickBinding extends ClickListener {

    @Nullable
    Runnable handler;

    boolean stop = false;

    /** Finds an existing binding among the element's listeners, if any. */
    static @Nullable ElementClickBinding find(@Nullable Element element) {
        if (element == null) {
            return null;
        }
        for (EventListener listener : element.getListeners()) {
            if (listener instanceof ElementClickBinding) {
                return (ElementClickBinding) listener;
            }
        }
        return null;
    }

    @Override
    public void clicked(InputEvent event, float x, float y) {
        if (event != null && event.stopped) {
            return;
        }
        if (stop && event != null) {
            event.stop();
        }
        if (handler != null) {
            try {
                handler.run();
            } catch (Exception e) {
                Log.err("Error executing element onClick", e);
            }
        }
    }
}
