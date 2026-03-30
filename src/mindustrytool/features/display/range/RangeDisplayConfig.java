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
        opacity = Core.settings.getFloat("mindustrytool.range.opacity", 1f);
        drawBlockRangeAlly = Core.settings.getBool("mindustrytool.range.draw-block-range-ally", true);
        drawBlockRangeEnemy = Core.settings.getBool("mindustrytool.range.draw-block-range-enemy", true);
        drawUnitRangeAlly = Core.settings.getBool("mindustrytool.range.draw-unit-range-ally", true);
        drawUnitRangeEnemy = Core.settings.getBool("mindustrytool.range.draw-unit-range-enemy", true);
        drawTurretRangeAlly = Core.settings.getBool("mindustrytool.range.draw-turret-range-ally", true);
        drawTurretRangeEnemy = Core.settings.getBool("mindustrytool.range.draw-turret-range-enemy", true);
        drawPlayerRange = Core.settings.getBool("mindustrytool.range.draw-player-range", true);
        drawSpawnerRange = Core.settings.getBool("mindustrytool.range.draw-spawner-range", true);
    }

    public static void save() {
        Core.settings.put("mindustrytool.range.opacity", opacity);
        Core.settings.put("mindustrytool.range.draw-block-range-ally", drawBlockRangeAlly);
        Core.settings.put("mindustrytool.range.draw-block-range-enemy", drawBlockRangeEnemy);
        Core.settings.put("mindustrytool.range.draw-unit-range-ally", drawUnitRangeAlly);
        Core.settings.put("mindustrytool.range.draw-unit-range-enemy", drawUnitRangeEnemy);
        Core.settings.put("mindustrytool.range.draw-turret-range-ally", drawTurretRangeAlly);
        Core.settings.put("mindustrytool.range.draw-turret-range-enemy", drawTurretRangeEnemy);
        Core.settings.put("mindustrytool.range.draw-player-range", drawPlayerRange);
        Core.settings.put("mindustrytool.range.draw-spawner-range", drawSpawnerRange);
    }
}
