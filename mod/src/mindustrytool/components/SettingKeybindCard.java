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

/**
 * Reusable settings card displaying feature keybinds.
 * Conforms to the project layout and spacing standards.
 * Automatically hidden on mobile devices.
 */
public final class SettingKeybindCard {

    private SettingKeybindCard() {
    }

    /**
     * Creates a standardized Keybinds section card for a single feature.
     * If on mobile or if the feature has no registered keybinds, returns null.
     */
    public static @Nullable Component forFeature(Feature feature) {
        if (Vars.mobile || feature == null || feature.getKeybinds().isEmpty()) {
            return null;
        }
        return of(Core.bundle.get("setting.section.keybinds", "Keybinds"), feature.getKeybinds());
    }

    /**
     * Creates a standardized Keybinds section card for a custom collection of keybinds.
     * If on mobile or if the keybind collection is empty, returns null.
     */
    public static @Nullable Component of(String title, Seq<FeatureKeybind> keybinds) {
        if (Vars.mobile || keybinds == null || keybinds.isEmpty()) {
            return null;
        }
        return SettingCard.section(Icon.settings, title, () -> {
            column().growX().gap(unit(2)).children(() -> {
                for (int i = 0; i < keybinds.size; i++) {
                    if (i > 0) {
                        divider().color(WebStyles.Colors.SECTION_BORDER);
                    }
                    new SettingKeybindRow(keybinds.get(i));
                }
            });
        });
    }
}
