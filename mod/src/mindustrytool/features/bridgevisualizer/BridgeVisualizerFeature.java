package mindustrytool.features.bridgevisualizer;

import arc.Core;
import arc.Events;
import arc.func.Boolf;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Rect;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.TimeItem;
import mindustry.graphics.Layer;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.ItemBuffer;
import mindustry.world.blocks.distribution.BufferedItemBridge;
import mindustry.world.blocks.distribution.BufferedItemBridge.BufferedItemBridgeBuild;
import mindustry.world.blocks.distribution.DirectionBridge.DirectionBridgeBuild;
import mindustry.world.blocks.distribution.DirectionLiquidBridge;
import mindustry.world.blocks.distribution.DuctBridge;
import mindustry.world.blocks.distribution.DuctBridge.DuctBridgeBuild;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.blocks.distribution.ItemBridge.ItemBridgeBuild;
import mindustry.world.blocks.liquid.LiquidBridge.LiquidBridgeBuild;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.core.Provider;
import solim.overlay.SolimDialog;

import java.lang.reflect.Field;

/**
 * Feature responsible for visualizing items and fluids travelling through bridges:
 * - Item Bridges (ItemBridge)
 * - Buffered Item Bridges (BufferedItemBridge)
 * - Duct Bridges (DuctBridge)
 * - Liquid Bridges (LiquidBridge & DirectionLiquidBridge)
 *
 * Optimized for 60 FPS real-time rendering:
 * - Zero allocations in draw() loop (no capturing lambdas, no temporary arrays).
 * - Pre-allocated predicate and consumer delegates.
 * - Reactive primitive cache to eliminate boxing/unboxing and config signal overhead.
 * - Direct array/index iteration over ItemModule.
 */
public class BridgeVisualizerFeature extends Feature {

    public final ConfigGroup config;
    public final ConfigValue<Boolean> showItemBridgesConfig;
    public final ConfigValue<Boolean> showDuctBridgesConfig;
    public final ConfigValue<Boolean> showLiquidBridgesConfig;
    public final ConfigValue<Float> itemScaleConfig;
    public final ConfigValue<Float> opacityConfig;

    private final Rect viewBounds = new Rect();
    private @Nullable BridgeVisualizerSettingsDialog settingsDialog;

    // Pre-allocated functional interfaces for zero GC allocation during eachBlock
    private static final Boolf<Building> IS_BRIDGE = b -> b instanceof ItemBridgeBuild
            || b instanceof LiquidBridgeBuild
            || b instanceof DirectionBridgeBuild;
    private final Cons<Building> bridgeDrawer = this::drawBridge;

    // Cached primitive configurations to avoid repeated getter calls, boxing, and unboxing in draw()
    private boolean cachedShowItemBridges = true;
    private boolean cachedShowDuctBridges = true;
    private boolean cachedShowLiquidBridges = true;
    private float cachedItemScale = 1.0f;
    private float cachedOpacity = 1.0f;

    private static @Nullable Field bufferField;
    private static @Nullable Field itemBufferArrayField;
    private static @Nullable Field itemBufferIndexField;

    static {
        try {
            bufferField = BufferedItemBridgeBuild.class.getDeclaredField("buffer");
            bufferField.setAccessible(true);
        } catch (Throwable t) {
            bufferField = null;
        }

        try {
            itemBufferArrayField = ItemBuffer.class.getDeclaredField("buffer");
            itemBufferArrayField.setAccessible(true);
            itemBufferIndexField = ItemBuffer.class.getDeclaredField("index");
            itemBufferIndexField.setAccessible(true);
        } catch (Throwable t) {
            itemBufferArrayField = null;
            itemBufferIndexField = null;
        }
    }

    public BridgeVisualizerFeature() {
        super(FeatureMetadata.builder()
                .id("bridge-visualizer")
                .icon(FileIcon.of("network.png"))
                .order(5)
                .enabledByDefault(true)
                .quickAccess(true)
                .build());

        config = configGroup();
        showItemBridgesConfig = config.boolValue("show-item-bridges", true);
        showDuctBridgesConfig = config.boolValue("show-duct-bridges", true);
        showLiquidBridgesConfig = config.boolValue("show-liquid-bridges", true);
        itemScaleConfig = config.floatValue("item-scale", 1.0f);
        opacityConfig = config.floatValue("opacity", 1.0f);

        // Synchronize primitive caches on reactive signal changes
        showItemBridgesConfig.signal().subscribe(v -> cachedShowItemBridges = v != null ? v : true);
        showDuctBridgesConfig.signal().subscribe(v -> cachedShowDuctBridges = v != null ? v : true);
        showLiquidBridgesConfig.signal().subscribe(v -> cachedShowLiquidBridges = v != null ? v : true);
        itemScaleConfig.signal().subscribe(v -> cachedItemScale = v != null ? v : 1.0f);
        opacityConfig.signal().subscribe(v -> cachedOpacity = v != null ? v : 1.0f);

        // Initialize cache values
        syncConfigCache();

        Events.run(Trigger.draw, this::draw);
    }

    private void syncConfigCache() {
        Boolean showItems = showItemBridgesConfig.get();
        cachedShowItemBridges = showItems != null ? showItems : true;

        Boolean showDucts = showDuctBridgesConfig.get();
        cachedShowDuctBridges = showDucts != null ? showDucts : true;

        Boolean showLiquids = showLiquidBridgesConfig.get();
        cachedShowLiquidBridges = showLiquids != null ? showLiquids : true;

        Float scale = itemScaleConfig.get();
        cachedItemScale = scale != null ? scale : 1.0f;

        Float op = opacityConfig.get();
        cachedOpacity = op != null ? op : 1.0f;
    }

    public void resetToDefaults() {
        showItemBridgesConfig.reset();
        showDuctBridgesConfig.reset();
        showLiquidBridgesConfig.reset();
        itemScaleConfig.reset();
        opacityConfig.reset();
        syncConfigCache();
    }

    @Override
    public @Nullable Provider<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new BridgeVisualizerSettingsDialog(this);
            }
            return settingsDialog;
        };
    }

    private void draw() {
        if (!isEnabled() || Vars.state == null || !Vars.state.isGame() || Vars.ui == null || Core.camera == null) {
            return;
        }

        Core.camera.bounds(viewBounds);

        float z = Draw.z();
        Draw.z(Layer.overlayUI);

        float cx = viewBounds.x + viewBounds.width / 2f;
        float cy = viewBounds.y + viewBounds.height / 2f;
        float range = Math.max(viewBounds.width, viewBounds.height) * 0.75f + 160f;

        Team team = Vars.player != null ? Vars.player.team() : null;

        Vars.indexer.eachBlock(team, cx, cy, range, IS_BRIDGE, bridgeDrawer);

        Draw.z(z);
        Draw.reset();
    }

    private void drawBridge(Building build) {
        if (build == null) {
            return;
        }

        if (build instanceof BufferedItemBridgeBuild) {
            if (cachedShowItemBridges) {
                drawBufferedItemBridge((BufferedItemBridgeBuild) build);
            }
        } else if (build instanceof LiquidBridgeBuild) {
            if (cachedShowLiquidBridges) {
                drawLiquidBridge((LiquidBridgeBuild) build);
            }
        } else if (build instanceof ItemBridgeBuild) {
            if (cachedShowItemBridges) {
                drawItemBridge((ItemBridgeBuild) build);
            }
        } else if (build instanceof DirectionBridgeBuild) {
            DirectionBridgeBuild dirBuild = (DirectionBridgeBuild) build;
            if (build.block instanceof DirectionLiquidBridge) {
                if (cachedShowLiquidBridges) {
                    drawDirectionLiquidBridge(dirBuild);
                }
            } else if (build instanceof DuctBridgeBuild && cachedShowDuctBridges) {
                drawDuctBridge((DuctBridgeBuild) build);
            }
        }
    }

    private void drawBufferedItemBridge(BufferedItemBridgeBuild build) {
        if (build.link == -1) {
            return;
        }

        Building linked = Vars.world.build(build.link);
        if (linked == null) {
            return;
        }

        float x1 = build.x;
        float y1 = build.y;
        float x2 = linked.x;
        float y2 = linked.y;

        boolean drawnFromBuffer = false;

        if (bufferField != null && itemBufferArrayField != null && itemBufferIndexField != null) {
            try {
                ItemBuffer itemBuffer = (ItemBuffer) bufferField.get(build);
                if (itemBuffer != null) {
                    long[] rawBuffer = (long[]) itemBufferArrayField.get(itemBuffer);
                    int count = itemBufferIndexField.getInt(itemBuffer);

                    if (rawBuffer != null && count > 0) {
                        drawnFromBuffer = true;
                        float speed = ((BufferedItemBridge) build.block).speed;
                        float safeSpeed = speed > 0.001f ? speed : 1f;

                        float prevProgress = 1.0f;
                        for (int i = 0; i < count; i++) {
                            long entry = rawBuffer[i];
                            short itemId = TimeItem.item(entry);
                            Item item = Vars.content.item(itemId);
                            if (item == null) {
                                continue;
                            }

                            float entryTime = TimeItem.time(entry);
                            float elapsed = Time.time - entryTime;
                            float rawProgress = Mathf.clamp(elapsed / safeSpeed, 0f, 1f);

                            float progress = i == 0 ? rawProgress : Math.min(rawProgress, prevProgress - 0.12f);
                            progress = Mathf.clamp(progress, 0f, 1f);
                            prevProgress = progress;

                            drawItemAt(x1, y1, x2, y2, item.fullIcon != null ? item.fullIcon : item.uiIcon, progress);
                        }
                    }
                }
            } catch (Throwable t) {
                Log.err("BufferedItemBridge reflection failed", t);
            }
        }

        if (!drawnFromBuffer && build.items != null && !build.items.empty()) {
            drawInventoryFlow(build, linked);
        }
    }

    private void drawItemBridge(ItemBridgeBuild build) {
        if (build.link == -1 || build.items == null) {
            return;
        }

        int total = build.items.total();
        if (total <= 0) {
            return;
        }

        Building linked = Vars.world.build(build.link);
        if (linked == null) {
            return;
        }

        ItemBridge block = (ItemBridge) build.block;
        float transportTime = block.transportTime > 0.001f ? block.transportTime : 1f;
        float progress = Mathf.clamp(build.transportCounter / transportTime, 0f, 1f);

        float x1 = build.x;
        float y1 = build.y;
        float x2 = linked.x;
        float y2 = linked.y;

        int itemTypes = build.items.length();
        int index = 0;
        for (int id = 0; id < itemTypes; id++) {
            int amount = build.items.get(id);
            if (amount <= 0) {
                continue;
            }
            Item item = Vars.content.item(id);
            if (item == null) {
                continue;
            }
            TextureRegion icon = item.fullIcon != null ? item.fullIcon : item.uiIcon;
            for (int i = 0; i < amount; i++) {
                float p = Math.max(0f, progress - (index * 0.18f));
                drawItemAt(x1, y1, x2, y2, icon, p);
                index++;
            }
        }
    }

    private void drawDuctBridge(DuctBridgeBuild build) {
        if (build.items == null) {
            return;
        }

        int total = build.items.total();
        if (total <= 0) {
            return;
        }

        Building linked = build.findLink();
        if (linked == null) {
            return;
        }

        float speed = ((DuctBridge) build.block).speed;
        float safeSpeed = speed > 0.001f ? speed : 1f;
        float progress = Mathf.clamp(build.progress / safeSpeed, 0f, 1f);

        float x1 = build.x;
        float y1 = build.y;
        float x2 = linked.x;
        float y2 = linked.y;

        int itemTypes = build.items.length();
        int index = 0;
        for (int id = 0; id < itemTypes; id++) {
            int amount = build.items.get(id);
            if (amount <= 0) {
                continue;
            }
            Item item = Vars.content.item(id);
            if (item == null) {
                continue;
            }
            TextureRegion icon = item.fullIcon != null ? item.fullIcon : item.uiIcon;
            for (int i = 0; i < amount; i++) {
                float p = Math.max(0f, progress - (index * 0.2f));
                drawItemAt(x1, y1, x2, y2, icon, p);
                index++;
            }
        }
    }

    private void drawLiquidBridge(LiquidBridgeBuild build) {
        if (build.link == -1 || build.liquids == null) {
            return;
        }

        Building linked = Vars.world.build(build.link);
        if (linked == null) {
            return;
        }

        Liquid current = build.liquids.current();
        float amount = build.liquids.currentAmount();
        if (current == null || amount < 0.01f) {
            return;
        }

        if (build.warmup < 0.05f && !build.moved) {
            return;
        }

        drawFluidFlow(build.x, build.y, linked.x, linked.y, current);
    }

    private void drawDirectionLiquidBridge(DirectionBridgeBuild build) {
        Building linked = build.findLink();
        if (linked == null || build.liquids == null) {
            return;
        }

        Liquid current = build.liquids.current();
        float amount = build.liquids.currentAmount();
        if (current == null || amount < 0.01f) {
            return;
        }

        drawFluidFlow(build.x, build.y, linked.x, linked.y, current);
    }

    private void drawInventoryFlow(Building build, Building linked) {
        if (build.items == null) {
            return;
        }

        int total = build.items.total();
        if (total <= 0) {
            return;
        }

        int itemTypes = build.items.length();
        int index = total;
        for (int id = 0; id < itemTypes; id++) {
            int amount = build.items.get(id);
            if (amount <= 0) {
                continue;
            }
            Item item = Vars.content.item(id);
            if (item == null) {
                continue;
            }
            TextureRegion icon = item.fullIcon != null ? item.fullIcon : item.uiIcon;
            for (int i = 0; i < amount; i++) {
                float progress = (float) index / total;
                drawItemAt(build.x, build.y, linked.x, linked.y, icon, progress);
                index--;
            }
        }
    }

    private void drawFluidFlow(float x1, float y1, float x2, float y2, Liquid liquid) {
        float dist = Mathf.dst(x1, y1, x2, y2);

        // Continuous fluid flow stream in the background using authentic liquid color
        Color fluidColor = liquid.gas && liquid.gasColor != null ? liquid.gasColor : liquid.color;
        if (fluidColor != null) {
            Draw.color(fluidColor, cachedOpacity * 0.7f);
            Lines.stroke(3.2f * cachedItemScale);
            Lines.line(x1, y1, x2, y2, false);
        }

        int drops = Math.max(1, (int) (dist / 10f));
        float timeOffset = (Time.time * 0.035f) % 1f;

        for (int d = 0; d < drops; d++) {
            float progress = (timeOffset + (float) d / drops) % 1f;
            drawLiquidAt(x1, y1, x2, y2, liquid, progress);
        }
    }

    private void drawItemAt(float x1, float y1, float x2, float y2, TextureRegion icon, float progress) {
        if (icon == null) {
            return;
        }
        float p = Mathf.clamp(progress, 0f, 1f);
        float lx = Mathf.lerp(x1, x2, p);
        float ly = Mathf.lerp(y1, y2, p);

        float baseSize = 5.5f * cachedItemScale;

        Draw.color(1f, 1f, 1f, cachedOpacity);
        Draw.rect(icon, lx, ly, baseSize, baseSize);
    }

    private void drawLiquidAt(float x1, float y1, float x2, float y2, Liquid liquid, float progress) {
        TextureRegion icon = liquid.uiIcon != null ? liquid.uiIcon : liquid.fullIcon;
        if (icon == null) {
            return;
        }

        float p = Mathf.clamp(progress, 0f, 1f);
        float lx = Mathf.lerp(x1, x2, p);
        float ly = Mathf.lerp(y1, y2, p);

        float baseSize = 5.2f * cachedItemScale;

        // Preserve authentic aspect ratio from game database (e.g. 24x32 for water droplets)
        float aspect = icon.height > 0 ? (float) icon.width / (float) icon.height : 1.0f;
        float width = aspect >= 1.0f ? baseSize : baseSize * aspect;
        float height = aspect >= 1.0f ? baseSize / aspect : baseSize;

        // Draw authentic icon with pure white color (preserving official database colors and shading)
        Draw.color(1f, 1f, 1f, cachedOpacity);
        Draw.rect(icon, lx, ly, width, height);
    }
}
