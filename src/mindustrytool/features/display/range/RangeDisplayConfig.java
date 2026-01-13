package mindustrytool.features.display.range;

import arc.Core;

public class RangeDisplayConfig {
    public boolean drawBlockRangeAlly = true;
    public boolean drawBlockRangeEnemy = true;
    public boolean drawUnitRangeAlly = true;
    public boolean drawUnitRangeEnemy = true;
    public boolean drawTurretRangeAlly = true;
    public boolean drawTurretRangeEnemy = true;
    public boolean drawPlayerRange = true;
    public boolean drawSpawnerRange = true;

    public void load() {
        drawBlockRangeAlly = Core.settings.getBool("range-drawBlockRangeAlly", true);
        drawBlockRangeEnemy = Core.settings.getBool("range-drawBlockRangeEnemy", true);
        drawUnitRangeAlly = Core.settings.getBool("range-drawUnitRangeAlly", true);
        drawUnitRangeEnemy = Core.settings.getBool("range-drawUnitRangeEnemy", true);
        drawTurretRangeAlly = Core.settings.getBool("range-drawTurretRangeAlly", true);
        drawTurretRangeEnemy = Core.settings.getBool("range-drawTurretRangeEnemy", true);
        drawPlayerRange = Core.settings.getBool("range-drawPlayerRange", true);
        drawSpawnerRange = Core.settings.getBool("range-drawSpawnerRange", true);
    }

    public void save() {
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
