package mindustrytool.features.display.itemvisualizer;

import arc.Core;
import arc.Events;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Rect;
import arc.scene.ui.Dialog;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Icon;
import mindustry.graphics.Layer;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.ItemBuffer;
import mindustry.world.blocks.distribution.BufferedItemBridge.BufferedItemBridgeBuild;
import mindustry.world.blocks.distribution.Duct.DuctBuild;
import mindustry.world.blocks.distribution.DuctBridge.DuctBridgeBuild;
import mindustry.world.blocks.distribution.DuctRouter.DuctRouterBuild;
import mindustry.world.blocks.distribution.ItemBridge.ItemBridgeBuild;
import mindustry.world.blocks.distribution.Junction.JunctionBuild;
import mindustry.world.blocks.distribution.Router.RouterBuild;
import mindustry.world.blocks.liquid.LiquidBridge.LiquidBridgeBuild;
import mindustry.world.modules.ItemModule;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

import java.lang.reflect.Field;
import java.util.Optional;

public class ItemVisualizerFeature implements Feature {

    private boolean active = false;
    private final Rect viewBounds = new Rect();
    private final Field bufferField;
    {
        try {
            bufferField = BufferedItemBridgeBuild.class.getDeclaredField("buffer");
            bufferField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("Item Visualizer")
                .description("Visualizes item and liquid flow in bridges, routers and unloaders.")
                .icon(Icon.distribution)
                .build();
    }

    @Override
    public void init() {
        ItemVisualizerSettings.load();
        Events.run(Trigger.draw, this::draw);
    }

    @Override
    public void onEnable() {
        active = true;
    }

    @Override
    public void onDisable() {
        active = false;
    }

    @Override
    public Optional<Dialog> setting() {
        BaseDialog dialog = new BaseDialog("Item Visualizer Settings");
        dialog.addCloseButton();

        dialog.cont.table(t -> {
            t.defaults().pad(6).left();

            t.check("Show Item Bridges", ItemVisualizerSettings.showItemBridges, val -> {
                ItemVisualizerSettings.showItemBridges = val;
                ItemVisualizerSettings.save();
            }).row();

            t.check("Show Liquid Bridges", ItemVisualizerSettings.showLiquidBridges, val -> {
                ItemVisualizerSettings.showLiquidBridges = val;
                ItemVisualizerSettings.save();
            }).row();

            t.check("Show Routers & Distributors", ItemVisualizerSettings.showRouters, val -> {
                ItemVisualizerSettings.showRouters = val;
                ItemVisualizerSettings.save();
            }).row();
        });

        return Optional.of(dialog);
    }

    private void draw() {
        if (!active || !Vars.state.isGame())
            return;

        Core.camera.bounds(viewBounds);

        float z = Draw.z();
        Draw.z(Layer.overlayUI);

        float cx = viewBounds.x + viewBounds.width / 2f;
        float cy = viewBounds.y + viewBounds.height / 2f;
        float range = Math.max(viewBounds.width, viewBounds.height) * 0.75f;
        ItemModule temp = new ItemModule();

        Vars.indexer.eachBlock(Vars.player.team(), cx, cy, range, b -> true, build -> {
            if (build == null) {
                return;
            }

            if (ItemVisualizerSettings.showItemBridges) {
                if (build instanceof BufferedItemBridgeBuild bufferedItemBridgeBuild) {
                    if (bufferedItemBridgeBuild.link == -1) {
                        return;
                    }

                    var linked = Vars.world.build(bufferedItemBridgeBuild.link);

                    if (linked == null) {
                        return;
                    }

                    try {
                        // final var buffer = (ItemBuffer) bufferField.get(bufferedItemBridgeBuild);

                        temp.set(bufferedItemBridgeBuild.items);
                        var item = temp.take();
                        var total = temp.total();
                        var index = total;

                        while (item != null) {
                            drawFlow(build.x, build.y, linked.x, linked.y, item.uiIcon, (float) index / total);
                            index--;
                            item = temp.take();
                        }
                    } catch (Exception e) {
                        Log.err(e);
                    }
                } else if (build instanceof ItemBridgeBuild itemBridgeBuild) {
                    if (itemBridgeBuild.link == -1) {
                        return;
                    }

                    var linked = Vars.world.build(itemBridgeBuild.link);

                    if (linked == null) {
                        return;
                    }

                    temp.set(itemBridgeBuild.items);
                    var item = temp.take();
                    var total = temp.total();
                    var index = total;

                    while (item != null) {
                        drawFlow(build.x, build.y, linked.x, linked.y, item.uiIcon, (float) index / total);
                        index--;
                        item = temp.take();
                    }
                } else if (build instanceof ItemBridgeBuild) {
                } else if (build instanceof DuctBridgeBuild) {
                }
            }

            if (ItemVisualizerSettings.showLiquidBridges) {
                if (build instanceof LiquidBridgeBuild) {
                }
            }

            if (ItemVisualizerSettings.showRouters) {
                if (build instanceof RouterBuild) {
                }
                if (build instanceof DuctRouterBuild) {
                }
                if (build instanceof JunctionBuild) {
                }
                if (build instanceof DuctBuild) {
                }
            }
        });

        Draw.z(z);
        Draw.reset();
    }

    private void drawFlow(float x1, float y1, float x2, float y2, TextureRegion icon, float progress) {
        float lx = Mathf.lerp(x1, x2, progress * 0.9f);
        float ly = Mathf.lerp(y1, y2, progress * 0.9f);

        Draw.color();
        Draw.rect(icon, lx, ly, 5f, 5f);
    }
}
