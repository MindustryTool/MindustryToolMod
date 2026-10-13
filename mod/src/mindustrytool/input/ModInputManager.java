package mindustrytool.input;

import arc.Core;
import arc.Events;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.input.DesktopInput;
import mindustry.input.MobileInput;

public final class ModInputManager {

    private ModInputManager() {
    }

    public static void init() {
        Events.on(ClientLoadEvent.class, e -> Core.app.post(ModInputManager::ensureCustomInput));
        Events.on(WorldLoadEvent.class, e -> Core.app.post(ModInputManager::ensureCustomInput));
    }

    public static void ensureCustomInput() {
        if (Vars.control == null) {
            return;
        }

        if (Vars.control.input instanceof MobileInput && !(Vars.control.input instanceof ModMobileInput)) {
            Vars.control.setInput(new ModMobileInput());
        } else if (Vars.control.input instanceof DesktopInput && !(Vars.control.input instanceof ModDesktopInput)) {
            Vars.control.setInput(new ModDesktopInput());
        } else if (Vars.control.input == null) {
            Vars.control.input = Vars.mobile ? new ModMobileInput() : new ModDesktopInput();
        }
    }
}
