package mindustrytool.features.display.range;

import arc.Core;

public class RangeDisplayConfig {
    public static float opacity = 1f;
    public static boolean drawBlockRangeAlly = true;
    public static boolean drawBlockRangeEnemy = true;
    public static boolean drawUnitRangeAlly = true;
    public static boolean drawUnitRangeEnemy = true;
    public static boolean drawTurretRangeAlly = true;
    public static boolean drawTurretRangeEnemy = true;
    public static boolean drawPlayerRange = true;
    public static boolean drawSpawnerRange = true;

    public static void load() {
        opacity = Core.settings.getFloat("range-opacity", 1f);
        drawBlockRangeAlly = Core.settings.getBool("range-drawBlockRangeAlly", true);
        drawBlockRangeEnemy = Core.settings.getBool("range-drawBlockRangeEnemy", true);
        drawUnitRangeAlly = Core.settings.getBool("range-drawUnitRangeAlly", true);
        drawUnitRangeEnemy = Core.settings.getBool("range-drawUnitRangeEnemy", true);
        drawTurretRangeAlly = Core.settings.getBool("range-drawTurretRangeAlly", true);
        drawTurretRangeEnemy = Core.settings.getBool("range-drawTurretRangeEnemy", true);
        drawPlayerRange = Core.settings.getBool("range-drawPlayerRange", true);
        drawSpawnerRange = Core.settings.getBool("range-drawSpawnerRange", true);
    }

    public static void save() {
        Core.settings.put("range-opacity", opacity);
        Core.settings.put("range-drawBlockRangeAlly", drawBlockRangeAlly);
        Core.settings.put("range-drawBlockRangeEnemy", drawBlockRangeEnemy);
        Core.settings.put("range-drawUnitRangeAlly", drawUnitRangeAlly);
        Core.settings.put("range-drawUnitRangeEnemy", drawUnitRangeEnemy);
        Core.settings.put("range-drawTurretRangeAlly", drawTurretRangeAlly);
        Core.settings.put("range-drawTurretRangeEnemy", drawTurretRangeEnemy);
        Core.settings.put("range-drawPlayerRange", drawPlayerRange);
        Core.settings.put("range-drawSpawnerRange", drawSpawnerRange);
    }
}
