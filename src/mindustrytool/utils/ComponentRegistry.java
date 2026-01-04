package mindustrytool.utils;

import arc.struct.Seq;
import mindustrytool.features.content.browser.BrowserFeature;
import mindustrytool.features.content.browser.LazyComponent;
import mindustrytool.features.gameplay.GameplayFeature;
import mindustrytool.features.gameplay.controls.TouchFeature;
import mindustrytool.features.social.multiplayer.PlayerConnectFeature;

/**
 * Central registry for all LazyComponents.
 * Ensures consistent list population across UI entry points.
 */
public class ComponentRegistry {

    public static Seq<LazyComponent<?>> getAllComponents() {
        Seq<LazyComponent<?>> all = new Seq<>();

        // Browser & Content
        all.addAll(BrowserFeature.getLazyComponents());

        // Multiplayer
        all.addAll(PlayerConnectFeature.getLazyComponents());

        // Gameplay (Visuals, Helpers)
        all.addAll(GameplayFeature.getLazyComponents());

        // Controls
        all.addAll(TouchFeature.getLazyComponents());

        return all;
    }
}
