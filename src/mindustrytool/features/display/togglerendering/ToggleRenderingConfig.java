package mindustrytool.features.display.togglerendering;

import arc.Core;

public class ToggleRenderingConfig {
    public static boolean drawBlocks = true;
    public static boolean drawUnitsAllies = true;
    public static boolean drawUnitsEnemies = true;

    public static void load() {
        drawBlocks = Core.settings.getBool("mindustry-tool.toggle-rendering.draw-blocks", true);
        drawUnitsAllies = Core.settings.getBool("mindustry-tool.toggle-rendering.draw-units-allies", true);
        drawUnitsEnemies = Core.settings.getBool("mindustry-tool.toggle-rendering.draw-units-enemies", true);
    }

    public static void save() {
        Core.settings.put("mindustry-tool.toggle-rendering.draw-blocks", drawBlocks);
        Core.settings.put("mindustry-tool.toggle-rendering.draw-units-allies", drawUnitsAllies);
        Core.settings.put("mindustry-tool.toggle-rendering.draw-units-enemies", drawUnitsEnemies);
    }

    public static void reset() {
        drawBlocks = true;
        drawUnitsAllies = true;
        drawUnitsEnemies = true;
        save();
    }
}
