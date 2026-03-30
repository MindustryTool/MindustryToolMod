package mindustrytool.features.display.togglerendering;

import arc.Core;
import arc.Events;
import arc.scene.ui.Dialog;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Tile;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

import java.lang.reflect.Field;
import java.util.Optional;

public class ToggleRenderingFeature implements Feature {

    private BaseDialog dialog;

    private final Seq<Unit> hiddenUnits = new Seq<>();

    private Field unitDrawIndexField;
    private Field tileviewField;

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.toggle-rendering.name")
                .description("@feature.toggle-rendering.description")
                .icon(Icon.eye)
                .order(5)
                .enabledByDefault(false)
                .quickAccess(true)
                .build();
    }

    @Override
    public void init() {
        ToggleRenderingConfig.load();

        try {
            unitDrawIndexField = Unit.class.getDeclaredField("index__draw");
            unitDrawIndexField.setAccessible(true);
        } catch (Exception e) {
            Log.err("Failed to access unit draw index", e);
        }

        try {
            tileviewField = mindustry.graphics.BlockRenderer.class.getDeclaredField("tileview");
            tileviewField.setAccessible(true);
        } catch (Exception e) {
            Log.err("Failed to access tileview", e);
        }

        Events.run(Trigger.draw, () -> {
            if (isEnabled()) {
                updateVisibility();
            }
        });
    }

    @Override
    public void onDisable() {
        restoreAll();
    }

    @Override
    public Optional<Dialog> setting() {
        if (dialog == null) {
            dialog = new BaseDialog("@toggle-rendering.settings.title");
            dialog.name = "toggleRenderingSettingDialog";
            dialog.addCloseButton();
            dialog.buttons.button("@reset", Icon.refresh, () -> {
                ToggleRenderingConfig.reset();
                rebuild();
            }).size(250, 64);

            dialog.shown(this::rebuild);
        }
        return Optional.of(dialog);
    }

    private void rebuild() {
        Table cont = dialog.cont;
        cont.clear();
        cont.defaults().pad(6).left();

        float width = Math.min(Core.graphics.getWidth() / 1.2f, 460f);

        cont.check("@toggle-rendering.draw-blocks", ToggleRenderingConfig.drawBlocks, v -> {
            ToggleRenderingConfig.drawBlocks = v;
            ToggleRenderingConfig.save();
        }).width(width).left().row();

        cont.check("@toggle-rendering.draw-units-allies", ToggleRenderingConfig.drawUnitsAllies, v -> {
            ToggleRenderingConfig.drawUnitsAllies = v;
            ToggleRenderingConfig.save();
        }).width(width).left().row();

        cont.check("@toggle-rendering.draw-units-enemies", ToggleRenderingConfig.drawUnitsEnemies, v -> {
            ToggleRenderingConfig.drawUnitsEnemies = v;
            ToggleRenderingConfig.save();
        }).width(width).left().row();
    }

    @SuppressWarnings("unchecked")
    private void updateVisibility() {
        if (!Vars.state.isGame()) {
            return;
        }

        for (int i = hiddenUnits.size - 1; i >= 0; i--) {
            Unit unit = hiddenUnits.get(i);

            if (!unit.isValid()) {
                hiddenUnits.remove(i);
                continue;
            }

            if (!shouldHideUnit(unit)) {
                try {
                    int newIndex = Groups.draw.addIndex(unit);
                    if (unitDrawIndexField != null) {
                        unitDrawIndexField.setInt(unit, newIndex);
                    }
                } catch (Exception error) {
                    Log.err("Failed to restore unit visibility", error);
                }
                hiddenUnits.remove(i);
            }
        }

        Seq<Unit> deleted = new Seq<>();

        Groups.draw.each(entity -> {
            if (entity instanceof Unit unit) {
                if (shouldHideUnit(unit)) {
                    deleted.add(unit);
                }
            }
        });

        for (Unit unit : deleted) {
            try {
                int idx = unitDrawIndexField != null ? unitDrawIndexField.getInt(unit) : -1;
                if (idx != -1) {
                    Groups.draw.removeIndex(unit, idx);
                    unitDrawIndexField.setInt(unit, -1);
                    hiddenUnits.add(unit);
                }
            } catch (Exception error) {
                Log.err("Failed to update unit visibility", error);
            }
        }

        if (!ToggleRenderingConfig.drawBlocks) {
            try {
                if (tileviewField != null) {
                    Seq<Tile> tileview = (Seq<Tile>) tileviewField.get(Vars.renderer.blocks);
                    if (tileview != null && !tileview.isEmpty()) {
                        tileview.removeAll(tile -> tile.build != null);
                    }
                }
            } catch (Exception error) {
                Log.err("Failed to update building visibility", error);
            }
        }
    }

    private void restoreAll() {
        for (int i = hiddenUnits.size - 1; i >= 0; i--) {
            Unit unit = hiddenUnits.get(i);

            if (!unit.isValid()) {
                hiddenUnits.remove(i);
                continue;
            }

            int newIndex = Groups.draw.addIndex(unit);
            try {
                if (unitDrawIndexField != null) {
                    unitDrawIndexField.setInt(unit, newIndex);
                }
            } catch (Exception error) {
                Log.err("Failed to restore unit visibility", error);
            }
            hiddenUnits.remove(i);
        }
    }

    private boolean shouldHideUnit(Unit unit) {
        boolean isAlly = unit.team == Vars.player.team();

        if (!ToggleRenderingConfig.drawUnitsAllies && isAlly) {
            return true;
        }

        if (!ToggleRenderingConfig.drawUnitsEnemies && !isAlly) {
            return true;
        }

        return false;
    }
}
