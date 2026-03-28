package mindustrytool;

import arc.Core;
import arc.Events;
import arc.input.KeyBind;
import arc.input.KeyCode;
import mindustry.game.EventType.Trigger;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;

public class MdtKeybinds {

    public static KeyBind mapBrowserKb = KeyBind.add("mapBrowser", KeyCode.unset, "MindustryTool"),
            schematicBrowserKb = KeyBind.add("schematicBrowser", KeyCode.unset, "MindustryTool"),
            autoPlay = KeyBind.add("autoPlay", KeyCode.unset, "MindustryTool"),
            chatKb = KeyBind.add("chatOverlay", KeyCode.unset, "MindustryTool");

    public static void addFeatureKeyBind(Feature feature, KeyBind keyBind) {
        Events.run(Trigger.update, () -> {
            boolean noInputFocused = !Core.scene.hasField();

            if (noInputFocused && Core.input.keyRelease(keyBind)) {
                Core.app.post(() -> FeatureManager.getInstance().toogle(feature));
            }
        });
    }
}
