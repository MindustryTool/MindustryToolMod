package mindustrytool.visuals;

import arc.Events;
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.ctype.UnlockableContent;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.ui.DualContentSelectionTable;

import java.lang.reflect.Field;

public class EntityVisibilityManager {

    // Configuration
    private final ObjectSet<UnlockableContent> hiddenContent = new ObjectSet<>();

    // Global Toggles
    private boolean disableWreck = false;
    private boolean disableBullet = false;
    private boolean disableUnit = false;
    private boolean disableBuilding = false;
    private boolean disableAllies = false;
    private boolean disableEnemies = false;

    // Dynamic Optimization
    private int minFps = 0;
    private boolean dynamicActive = false;

    // Stats
    private int hiddenUnitCount = 0;
    private int hiddenBulletCount = 0;
    private int hiddenWreckCount = 0;

    // Optimization Flags
    private boolean hasHiddenUnits = false;
    private boolean hasHiddenBlocks = false;

    // Cache
    private final Seq<Unit> hiddenUnits = new Seq<>();

    // Reflection
    private static Field unitDrawIndexField;
    private static Field bulletDrawIndexField;
    private static Field decalDrawIndexField;
    private static Field blockTileviewField;

    private boolean enabled = true;
    private BaseDialog dialog;

    public EntityVisibilityManager() {
        initReflection();
        Events.run(EventType.Trigger.draw, this::updateVisibility);
        updateSettings();
        buildDialog();
    }

    private void initReflection() {
        try {
            unitDrawIndexField = Unit.class.getDeclaredField("index__draw");
            unitDrawIndexField.setAccessible(true);
            try {
                bulletDrawIndexField = mindustry.gen.Bullet.class.getDeclaredField("index__draw");
                bulletDrawIndexField.setAccessible(true);
            } catch (Exception e) {
            }
            try {
                decalDrawIndexField = mindustry.gen.Decal.class.getDeclaredField("index__draw");
                decalDrawIndexField.setAccessible(true);
            } catch (Exception e) {
            }
            Class<?> blockRendererClass = mindustry.graphics.BlockRenderer.class;
            blockTileviewField = blockRendererClass.getDeclaredField("tileview");
            blockTileviewField.setAccessible(true);
        } catch (Exception e) {
            enabled = false;
        }
    }

    private void recalculateFlags() {
        hasHiddenUnits = false;
        hasHiddenBlocks = false;
        for (UnlockableContent c : hiddenContent) {
            if (c instanceof mindustry.type.UnitType)
                hasHiddenUnits = true;
            if (c instanceof mindustry.world.Block)
                hasHiddenBlocks = true;
        }
    }

    private void updateSettings() {
        disableWreck = arc.Core.settings.getBool("mindustrytool.disableWreck", false);
        disableBullet = arc.Core.settings.getBool("mindustrytool.disableBullet", false);
        disableUnit = arc.Core.settings.getBool("mindustrytool.disableUnit", false);
        disableBuilding = arc.Core.settings.getBool("mindustrytool.disableBuilding", false);
        disableAllies = arc.Core.settings.getBool("mindustrytool.disableAllies", false);
        disableEnemies = arc.Core.settings.getBool("mindustrytool.disableEnemies", false);
        minFps = arc.Core.settings.getInt("mindustrytool.minFps", 0);
    }

    private void buildDialog() {
        dialog = new BaseDialog("Entity Visibility");
        dialog.addCloseButton();
        dialog.buttons.button("Reset to defaults", mindustry.gen.Icon.refresh, this::resetToDefaults).size(250, 64);

        rebuildCont();
    }

    private void rebuildCont() {
        Table cont = dialog.cont;
        cont.clear();
        cont.defaults().pad(6).left();

        float width = Math.min(arc.Core.graphics.getWidth() / 1.2f, 460f);

        // --- FPS Slider ---
        Slider fpsSlider = new Slider(0, 60, 5, false);
        fpsSlider.setValue(minFps);

        Label fpsValueLabel = new Label(minFps == 0 ? "Off" : minFps + " FPS", Styles.outlineLabel);

        Table fpsContent = new Table();
        fpsContent.add("Dynamic Optimization", Styles.outlineLabel).left().growX();
        fpsContent.add(fpsValueLabel).padLeft(10f).right();
        fpsContent.margin(3f, 33f, 3f, 33f);
        fpsContent.touchable = arc.scene.event.Touchable.disabled;

        fpsSlider.changed(() -> {
            minFps = (int) fpsSlider.getValue();
            fpsValueLabel.setText(minFps == 0 ? "Off" : minFps + " FPS");
            arc.Core.settings.put("mindustrytool.minFps", minFps);
        });

        cont.stack(fpsSlider, fpsContent).width(width).left().padTop(4f).row();

        // --- Checkboxes ---
        cont.check("Hide Wrecks", disableWreck, b -> {
            disableWreck = b;
            arc.Core.settings.put("mindustrytool.disableWreck", disableWreck);
        }).left().padTop(4).row();

        cont.check("Hide Bullets", disableBullet, b -> {
            disableBullet = b;
            arc.Core.settings.put("mindustrytool.disableBullet", disableBullet);
        }).left().padTop(4).row();

        cont.check("Hide Units", disableUnit, b -> {
            disableUnit = b;
            arc.Core.settings.put("mindustrytool.disableUnit", disableUnit);
        }).left().padTop(4).row();

        cont.check("Hide Buildings", disableBuilding, b -> {
            disableBuilding = b;
            arc.Core.settings.put("mindustrytool.disableBuilding", disableBuilding);
        }).left().padTop(4).row();

        cont.check("Hide All Allied Units", disableAllies, b -> {
            disableAllies = b;
            arc.Core.settings.put("mindustrytool.disableAllies", disableAllies);
        }).left().padTop(4).row();

        cont.check("Hide All Enemy Units", disableEnemies, b -> {
            disableEnemies = b;
            arc.Core.settings.put("mindustrytool.disableEnemies", disableEnemies);
        }).left().padTop(4).row();

        // --- Advanced Filters Button ---
        cont.button("Advanced Filters", this::showFilterDialog)
                .center().size(220, 50).padTop(20).row();
    }

    private void resetToDefaults() {
        disableWreck = false;
        disableBullet = false;
        disableUnit = false;
        disableBuilding = false;
        disableAllies = false;
        disableEnemies = false;
        minFps = 0;

        hiddenContent.clear();
        hiddenUnits.clear();
        hiddenUnitCount = 0;
        hiddenBulletCount = 0;
        hiddenWreckCount = 0;

        // Persist defaults
        arc.Core.settings.put("mindustrytool.disableWreck", false);
        arc.Core.settings.put("mindustrytool.disableBullet", false);
        arc.Core.settings.put("mindustrytool.disableUnit", false);
        arc.Core.settings.put("mindustrytool.disableBuilding", false);
        arc.Core.settings.put("mindustrytool.disableAllies", false);
        arc.Core.settings.put("mindustrytool.disableEnemies", false);
        arc.Core.settings.put("mindustrytool.minFps", 0);

        recalculateFlags();
        rebuildCont();
    }

    private void showFilterDialog() {
        BaseDialog filterDialog = new BaseDialog("Advanced Filters");
        filterDialog.addCloseButton();
        Table fCont = filterDialog.cont;

        Table tabs = new Table();
        Table unitTable = new Table();
        unitTable.add(new DualContentSelectionTable(Vars.content.units(), hiddenContent, "Banned Units",
                "Unbanned Units", this::recalculateFlags)).grow();
        Table blockTable = new Table();
        blockTable.add(new DualContentSelectionTable(Vars.content.blocks(), hiddenContent, "Banned Blocks",
                "Unbanned Blocks", this::recalculateFlags)).grow();
        tabs.add(unitTable).grow();

        arc.scene.ui.ButtonGroup<arc.scene.ui.TextButton> group = new arc.scene.ui.ButtonGroup<>();
        fCont.table(t -> {
            t.button("Units", Styles.togglet, () -> {
                tabs.clear();
                tabs.add(unitTable).grow();
            }).group(group).checked(true).growX().height(50);
            t.button("Blocks", Styles.togglet, () -> {
                tabs.clear();
                tabs.add(blockTable).grow();
            }).group(group).growX().height(50);
        }).growX().pad(5).row();
        fCont.add(tabs).grow().row();
        filterDialog.show();
    }

    public void showDialog() {
        if (dialog != null)
            dialog.show();
    }

    /** Returns the shared hidden content set for other components to use. */
    public ObjectSet<UnlockableContent> getHiddenContent() {
        return hiddenContent;
    }

    private void updateVisibility() {
        if (!enabled || !Vars.state.isGame())
            return;

        hiddenBulletCount = 0;
        hiddenWreckCount = 0;
        hiddenUnitCount = hiddenUnits.size;

        if (minFps > 0) {
            float fps = arc.Core.graphics.getFramesPerSecond();
            if (fps < minFps)
                dynamicActive = true;
            else if (fps > minFps + 5)
                dynamicActive = false;
        } else {
            dynamicActive = false;
        }

        if (!hasHiddenUnits && !hasHiddenBlocks && hiddenUnits.isEmpty() && !disableWreck && !disableBullet
                && !disableUnit && !disableAllies && !disableEnemies && !dynamicActive)
            return;

        if (!disableUnit && !dynamicActive) {
            for (int i = hiddenUnits.size - 1; i >= 0; i--) {
                Unit u = hiddenUnits.get(i);
                if (!u.isValid()) {
                    hiddenUnits.remove(i);
                    continue;
                }
                boolean shouldBeHidden = hiddenContent.contains(u.type);
                if (!shouldBeHidden) {
                    int newIndex = Groups.draw.addIndex(u);
                    setIndex(u, newIndex);
                    hiddenUnits.remove(i);
                }
            }
        }

        if (hasHiddenUnits || disableBullet || disableWreck || disableUnit || dynamicActive) {
            Groups.draw.each(entity -> {
                boolean isWreck = entity instanceof mindustry.gen.Decal;
                boolean isBullet = entity instanceof mindustry.gen.Bullet;
                boolean isUnit = entity instanceof Unit;

                if (isBullet) {
                    if (disableBullet || (dynamicActive && minFps > 0)) {
                        mindustry.gen.Bullet b = (mindustry.gen.Bullet) entity;
                        if (disableBullet && (disableAllies || disableEnemies)) {
                            mindustry.game.Team bulletTeam = b.team;
                            if (bulletTeam != null) {
                                boolean isAlly = bulletTeam == Vars.player.team();
                                if (disableAllies && !isAlly)
                                    return;
                                if (disableEnemies && isAlly)
                                    return;
                            }
                        }
                        int idx = getIndex(b, bulletDrawIndexField);
                        if (idx != -1) {
                            Groups.draw.removeIndex(b, idx);
                            setIndex(b, bulletDrawIndexField, -1);
                            hiddenBulletCount++;
                        }
                    }
                    return;
                }

                if (isWreck) {
                    if (disableWreck || (dynamicActive && minFps > 0)) {
                        mindustry.gen.Decal d = (mindustry.gen.Decal) entity;
                        int idx = getIndex(d, decalDrawIndexField);
                        if (idx != -1) {
                            Groups.draw.removeIndex(d, idx);
                            setIndex(d, decalDrawIndexField, -1);
                            hiddenWreckCount++;
                        }
                    }
                    return;
                }

                if (isUnit) {
                    Unit u = (Unit) entity;
                    boolean shouldHide = false;
                    if (dynamicActive) {
                        shouldHide = true;
                    } else if (disableUnit && hasHiddenUnits && hiddenContent.contains(u.type)) {
                        if (disableAllies || disableEnemies) {
                            boolean isAlly = u.team == Vars.player.team();
                            if (disableAllies && isAlly)
                                shouldHide = true;
                            if (disableEnemies && !isAlly)
                                shouldHide = true;
                        } else {
                            shouldHide = true;
                        }
                    }

                    if (shouldHide) {
                        int idx = getIndex(u);
                        if (idx != -1) {
                            Groups.draw.removeIndex(u, idx);
                            setIndex(u, -1);
                            hiddenUnits.add(u);
                            hiddenUnitCount++;
                        }
                    }
                }
            });
        }

        if (hasHiddenBlocks || disableBuilding || (dynamicActive && minFps > 0)) {
            try {
                if (blockTileviewField != null) {
                    Seq<mindustry.world.Tile> tileview = (Seq<mindustry.world.Tile>) blockTileviewField
                            .get(Vars.renderer.blocks);
                    if (tileview != null && !tileview.isEmpty()) {
                        tileview.removeAll(t -> {
                            if (t.build == null)
                                return false;
                            if (dynamicActive)
                                return true;
                            if (disableBuilding && hiddenContent.contains(t.build.block)) {
                                if (disableAllies || disableEnemies) {
                                    boolean isAlly = t.build.team == Vars.player.team();
                                    if (disableAllies && isAlly)
                                        return true;
                                    if (disableEnemies && !isAlly)
                                        return true;
                                    return false;
                                }
                                return true;
                            }
                            return false;
                        });
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    private int getIndex(Unit u) {
        try {
            return unitDrawIndexField.getInt(u);
        } catch (Exception e) {
            return -1;
        }
    }

    private void setIndex(Unit u, int val) {
        try {
            unitDrawIndexField.setInt(u, val);
        } catch (Exception e) {
        }
    }

    private int getIndex(Object obj, Field field) {
        try {
            return field != null ? field.getInt(obj) : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private void setIndex(Object obj, Field field, int val) {
        try {
            if (field != null)
                field.setInt(obj, val);
        } catch (Exception e) {
        }
    }
}
