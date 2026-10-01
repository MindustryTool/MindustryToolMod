package mindustrytool.features.togglerendering;

import arc.Events;
import arc.func.Prov;
import arc.input.KeyCode;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import java.lang.reflect.Field;
import mindustry.Vars;
import mindustry.game.EventType.ResetEvent;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEndEvent;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.graphics.BlockRenderer;
import mindustry.world.Tile;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.overlay.SolimDialog;

/**
 * Allows toggling rendering layers such as allied/enemy units and buildings
 * to improve performance or capture clean screenshots.
 */
public class ToggleRenderingFeature extends Feature {

    public final ConfigGroup config;
    public final ConfigValue<Boolean> drawUnitsAlliesConfig;
    public final ConfigValue<Boolean> drawUnitsEnemiesConfig;
    public final ConfigValue<Boolean> drawBlocksConfig;

    private final Seq<Unit> hiddenUnits = new Seq<>();
    private final Seq<Unit> toHide = new Seq<>();

    private @Nullable Field unitDrawIndexField;
    private @Nullable Field tileviewField;
    private @Nullable ToggleRenderingSettingsDialog settingsDialog;

    public ToggleRenderingFeature() {
        super(FeatureMetadata.builder()
                .id("toggle-rendering")
                .icon(Icon.eye)
                .order(5)
                .enabledByDefault(false)
                .quickAccess(true)
                .build());

        config = configGroup();
        drawUnitsAlliesConfig = config.boolValue("draw-units-allies", true);
        drawUnitsEnemiesConfig = config.boolValue("draw-units-enemies", true);
        drawBlocksConfig = config.boolValue("draw-blocks", true);

        try {
            unitDrawIndexField = Unit.class.getDeclaredField("index__draw");
            unitDrawIndexField.setAccessible(true);
        } catch (Exception e) {
            Log.err("ToggleRendering: Failed to access unit draw index", e);
        }

        try {
            tileviewField = BlockRenderer.class.getDeclaredField("tileview");
            tileviewField.setAccessible(true);
        } catch (Exception e) {
            Log.err("ToggleRendering: Failed to access tileview", e);
        }

        bindToggle("toggleRendering", KeyCode.unset);
        bindDialog("toggleRenderingSettings", KeyCode.unset, getSettingDialog(), false);

        Events.run(Trigger.draw, this::updateVisibility);
        Events.on(WorldLoadEndEvent.class, e -> hiddenUnits.clear());
        Events.on(ResetEvent.class, e -> hiddenUnits.clear());
    }

    @Override
    public void onDisable() {
        restoreAll();
    }

    public void resetToDefaults() {
        drawUnitsAlliesConfig.reset();
        drawUnitsEnemiesConfig.reset();
        drawBlocksConfig.reset();
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new ToggleRenderingSettingsDialog(this);
            }
            return settingsDialog;
        };
    }

    public boolean shouldHideUnit(Unit unit) {
        if (Vars.player == null) {
            return false;
        }
        boolean isAlly = unit.team == Vars.player.team();
        return isAlly ? !drawUnitsAlliesConfig.get() : !drawUnitsEnemiesConfig.get();
    }

    public void updateVisibility() {
        if (!isEnabled() || Vars.state == null || !Vars.state.isGame()) {
            return;
        }

        for (int i = hiddenUnits.size - 1; i >= 0; i--) {
            Unit unit = hiddenUnits.get(i);
            if (!unit.isValid()) {
                hiddenUnits.remove(i);
                continue;
            }
            if (!shouldHideUnit(unit)) {
                restoreUnit(unit);
                hiddenUnits.remove(i);
            }
        }

        toHide.clear();
        Groups.draw.each(entity -> {
            if (entity instanceof Unit) {
                Unit unit = (Unit) entity;
                if (shouldHideUnit(unit)) {
                    toHide.add(unit);
                }
            }
        });

        for (int i = 0; i < toHide.size; i++) {
            Unit unit = toHide.get(i);
            try {
                int idx = unitDrawIndexField != null ? unitDrawIndexField.getInt(unit) : -1;
                if (idx != -1) {
                    Groups.draw.removeIndex(unit, idx);
                    unitDrawIndexField.setInt(unit, -1);
                    hiddenUnits.add(unit);
                }
            } catch (Exception error) {
                Log.err("ToggleRendering: Failed to hide unit", error);
            }
        }
        toHide.clear();

        if (!drawBlocksConfig.get() && tileviewField != null && Vars.renderer != null && Vars.renderer.blocks != null) {
            try {
                @SuppressWarnings("unchecked")
                Seq<Tile> tileview = (Seq<Tile>) tileviewField.get(Vars.renderer.blocks);
                if (tileview != null && !tileview.isEmpty()) {
                    tileview.removeAll(tile -> tile.build != null);
                }
            } catch (Exception error) {
                Log.err("ToggleRendering: Failed to update building visibility", error);
            }
        }
    }

    public void restoreAll() {
        for (int i = hiddenUnits.size - 1; i >= 0; i--) {
            Unit unit = hiddenUnits.get(i);
            if (unit.isValid()) {
                restoreUnit(unit);
            }
        }
        hiddenUnits.clear();
    }

    private void restoreUnit(Unit unit) {
        try {
            int newIndex = Groups.draw.addIndex(unit);
            if (unitDrawIndexField != null) {
                unitDrawIndexField.setInt(unit, newIndex);
            }
        } catch (Exception error) {
            Log.err("ToggleRendering: Failed to restore unit visibility", error);
        }
    }

    public int getHiddenUnitsCount() {
        return hiddenUnits.size;
    }
}
