package mindustrytool.features.logicvisualizer;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.func.Prov;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.input.KeyCode;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.ai.types.LogicAI;
import mindustry.entities.units.UnitController;
import mindustry.game.EventType.Trigger;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.overlay.SolimDialog;

public class LogicVisualizerFeature extends Feature {

    public final ConfigGroup config;
    public final ConfigValue<Boolean> showAllyUnitsConfig;
    public final ConfigValue<Boolean> showEnemyUnitsConfig;

    private boolean cachedShowAllyUnits = true;
    private boolean cachedShowEnemyUnits = false;

    private final Cons<Unit> unitDrawer = this::drawUnitLine;
    private @Nullable LogicVisualizerSettingsDialog settingsDialog;

    private @Nullable Team framePlayerTeam;
    private boolean frameShowAlly;
    private boolean frameShowEnemy;

    public LogicVisualizerFeature() {
        super(FeatureMetadata.builder()
                .id("logic-visualizer")
                .icon(Icon.logic)
                .order(5)
                .enabledByDefault(false)
                .quickAccess(false)
                .displayGroup(true)
                .build());

        config = configGroup();
        showAllyUnitsConfig = config.boolValue("logic-unit-ally", true);
        showEnemyUnitsConfig = config.boolValue("logic-unit-enemy", false);

        showAllyUnitsConfig.signal().subscribe(v -> cachedShowAllyUnits = v != null ? v : true);
        showEnemyUnitsConfig.signal().subscribe(v -> cachedShowEnemyUnits = v != null ? v : false);

        syncConfigCache();

        bindToggle("logicvisualizer", KeyCode.unset);
        bindDialog("logicvisualizersettings", KeyCode.unset, getSettingDialog(), false);

        Events.run(Trigger.draw, this::draw);
    }

    public void syncConfigCache() {
        Boolean ally = showAllyUnitsConfig.get();
        cachedShowAllyUnits = ally != null ? ally : true;
        Boolean enemy = showEnemyUnitsConfig.get();
        cachedShowEnemyUnits = enemy != null ? enemy : false;
    }

    public void resetToDefaults() {
        showAllyUnitsConfig.reset();
        showEnemyUnitsConfig.reset();
        syncConfigCache();
    }

    public boolean isShowAllyUnits() {
        return cachedShowAllyUnits;
    }

    public boolean isShowEnemyUnits() {
        return cachedShowEnemyUnits;
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new LogicVisualizerSettingsDialog(this);
            }
            return settingsDialog;
        };
    }

    private void draw() {
        if (!isEnabled() || Vars.state == null || !Vars.state.isGame() || Core.camera == null) {
            return;
        }

        framePlayerTeam = Vars.player != null ? Vars.player.team() : null;
        frameShowAlly = cachedShowAllyUnits;
        frameShowEnemy = cachedShowEnemyUnits;

        float prevZ = Draw.z();
        Draw.z(Layer.overlayUI);
        Lines.stroke(1.5f);

        Groups.unit.each(unitDrawer);

        Draw.reset();
        Draw.z(prevZ);
    }

    private void drawUnitLine(Unit unit) {
        if (unit == null || !unit.isValid()) {
            return;
        }

        UnitController controller = unit.controller();
        if (!(controller instanceof LogicAI)) {
            return;
        }

        LogicAI ai = (LogicAI) controller;
        Building block = ai.controller;
        if (block == null || !block.isValid()) {
            return;
        }

        boolean isAlly = framePlayerTeam == null || unit.team == framePlayerTeam;
        if (isAlly ? !frameShowAlly : !frameShowEnemy) {
            return;
        }

        Draw.color(block.team.color, 0.7f);
        Lines.line(unit.x, unit.y, block.x, block.y);
    }
}
