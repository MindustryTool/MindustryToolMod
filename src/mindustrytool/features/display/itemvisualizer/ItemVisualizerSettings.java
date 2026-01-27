package mindustrytool.features.display.itemvisualizer;

import arc.Core;

public class ItemVisualizerSettings {
    private static final String PREFIX = "mdt.itemvisualizer.";

    public static boolean showItemBridges = true;
    public static boolean showLiquidBridges = true;
    public static boolean showRouters = true;

    public static void load() {
        showItemBridges = Core.settings.getBool(PREFIX + "showItemBridges", true);
        showLiquidBridges = Core.settings.getBool(PREFIX + "showLiquidBridges", true);
        showRouters = Core.settings.getBool(PREFIX + "showRouters", true);
    }

    public static void save() {
        Core.settings.put(PREFIX + "showItemBridges", showItemBridges);
        Core.settings.put(PREFIX + "showLiquidBridges", showLiquidBridges);
        Core.settings.put(PREFIX + "showRouters", showRouters);
    }
}
