package mindustrytool.features.display.itemvisualizer;

import arc.Core;

public class ItemVisualizerSettings {
    private static final String PREFIX = "mindustrytool.item-visualizer.";

    public static boolean showItemBridges = true;
    public static boolean showLiquidBridges = true;
    public static boolean showRouters = true;

    public static void load() {
        showItemBridges = Core.settings.getBool(PREFIX + "show-item-bridges", true);
        showLiquidBridges = Core.settings.getBool(PREFIX + "show-liquid-bridges", true);
        showRouters = Core.settings.getBool(PREFIX + "show-routers", true);
    }

    public static void save() {
        Core.settings.put(PREFIX + "show-item-bridges", showItemBridges);
        Core.settings.put(PREFIX + "show-liquid-bridges", showLiquidBridges);
        Core.settings.put(PREFIX + "show-routers", showRouters);
    }
}
