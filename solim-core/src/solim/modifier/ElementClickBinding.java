package solim.modifier;

import arc.scene.Element;
import arc.scene.event.ClickListener;
import arc.scene.event.EventListener;
import arc.scene.event.InputEvent;
import arc.util.Nullable;

class ElementClickBinding extends ClickListener {

    @Nullable
    Runnable handler;

    boolean stop = false;

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
            handler.run();
        }
    }
}
