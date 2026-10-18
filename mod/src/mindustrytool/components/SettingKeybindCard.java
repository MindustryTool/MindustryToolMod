package mindustrytool.components;

import static solim.UI.*;

import arc.Core;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureKeybind;
import solim.core.Component;

public final class SettingKeybindCard {

    private SettingKeybindCard() {
    }

    public static @Nullable Component forFeature(Feature feature) {
        if (Vars.mobile || feature == null || feature.getKeybinds().isEmpty()) {
            return null;
        }
        return of(Core.bundle.get("setting.section.keybinds", "Keybinds"), feature.getKeybinds());
    }

    public static @Nullable Component of(String title, Seq<FeatureKeybind> keybinds) {
        if (Vars.mobile || keybinds == null || keybinds.isEmpty()) {
            return null;
        }
        return SettingCard.section(Icon.settings, title, () -> {
            column().props(growX(), gap(unit(2))).children(() -> {
                for (int i = 0; i < keybinds.size; i++) {
                    if (i > 0) {
                        divider().props(color(WebStyles.Colors.SECTION_BORDER));
                    }
                    new SettingKeybindRow(keybinds.get(i));
                }
            });
        });
    }
}
