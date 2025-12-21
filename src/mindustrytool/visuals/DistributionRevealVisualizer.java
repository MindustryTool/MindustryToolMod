package mindustrytool.visuals;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Log;
import arc.util.Pack;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.BufferedItemBridge;
import mindustry.world.blocks.distribution.DuctBridge;
import mindustry.world.blocks.distribution.DirectionalUnloader;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.blocks.distribution.Junction;
import mindustry.world.blocks.distribution.Router;
import mindustry.world.blocks.liquid.LiquidBridge;
import mindustry.world.blocks.storage.Unloader;
import mindustry.world.blocks.storage.CoreBlock;

import java.lang.reflect.Field;

public class DistributionRevealVisualizer {

    private boolean reflectionInitialized = false;

    // Reflection Fields
    private static Field itemBridgeBuffer;
    private static Field itemBridgeBufferBuffer;
    private static Field itemBridgeBufferIndex;
    private static Field bridgeLinkField;
    private static Field directionBridgeLinkField; // lastLink field for Erekir bridges

    // Unloader
    private static Field unloaderDumpTileField; // Robust fallback for unloader target tile
    private static Field unloaderDumpingTo;
    private static Field unloaderBuilding; // ContainerStat.building

    // Junction
    // Uses jb.buffer.indexes directly (assuming accessors or public).
    // In V7, JunctionBuild.buffer is typically protected.
    // Reflection used for Junction fields to ensure access.
    private static Field junctionBufferField;
    private static Field junctionBufferBuffersField;
    private static Field junctionBufferIndexesField;

    // Settings
    private boolean revealBridge = true;
    private boolean revealJunction = true;
    private boolean revealUnloader = true;
    private boolean revealInventory = false;
    private float zoomThreshold = 3f;

    private mindustry.ui.dialogs.BaseDialog dialog;

    public DistributionRevealVisualizer() {
        loadSettings();
    }

    private void loadSettings() {
        revealBridge = Core.settings.getBool("dist_reveal_bridge", true);
        revealJunction = Core.settings.getBool("dist_reveal_junction", true);
        revealUnloader = Core.settings.getBool("dist_reveal_unloader", true);
        revealInventory = Core.settings.getBool("dist_reveal_inventory", false);
        zoomThreshold = (float) Core.settings.getInt("dist_zoom_threshold", 300) / 100f;
    }

    private void initReflection() {
        if (reflectionInitialized)
            return;

        try {
            // BufferedItemBridge.buffer
            itemBridgeBuffer = findField(BufferedItemBridge.BufferedItemBridgeBuild.class, "buffer");
            if (itemBridgeBuffer != null) {
                itemBridgeBuffer.setAccessible(true);
                Class<?> itemBufferClass = itemBridgeBuffer.getType();

                // ItemBuffer.buffer (long[])
                itemBridgeBufferBuffer = findField(itemBufferClass, "buffer");
                if (itemBridgeBufferBuffer != null)
                    itemBridgeBufferBuffer.setAccessible(true);

                // ItemBuffer.index (int)
                itemBridgeBufferIndex = findField(itemBufferClass, "index");
                if (itemBridgeBufferIndex != null)
                    itemBridgeBufferIndex.setAccessible(true);
            }

            if (bridgeLinkField != null)
                bridgeLinkField.setAccessible(true);

            // DirectionBridge lastLink field (Erekir)
            // Found via representative class since the direct class reference may not be
            // available.
            // DirectionBridgeBuild contains the lastLink field.
            // On-demand lookup attempted via available representative classes.
            try {
                // Try finding it in any block that might have it
                directionBridgeLinkField = findField(DuctBridge.DuctBridgeBuild.class, "lastLink");
                if (directionBridgeLinkField != null)
                    directionBridgeLinkField.setAccessible(true);
            } catch (Exception e) {
            }

            // Junction (Manual Reflection needed if fields strictly private)
            junctionBufferField = findField(Junction.JunctionBuild.class, "buffer");
            if (junctionBufferField != null) {
                junctionBufferField.setAccessible(true);
                Class<?> junctionBufferClass = junctionBufferField.getType();

                junctionBufferBuffersField = findField(junctionBufferClass, "buffers"); // long[][]
                if (junctionBufferBuffersField != null)
                    junctionBufferBuffersField.setAccessible(true);

                junctionBufferIndexesField = findField(junctionBufferClass, "indexes"); // int[]
                if (junctionBufferIndexesField != null)
                    junctionBufferIndexesField.setAccessible(true);
            }

            // Unloader
            unloaderDumpingTo = findField(Unloader.UnloaderBuild.class, "dumpingTo");
            if (unloaderDumpingTo != null) {
                unloaderDumpingTo.setAccessible(true);
                Class<?> containerStatClass = unloaderDumpingTo.getType();
                unloaderBuilding = findField(containerStatClass, "building");
                if (unloaderBuilding != null)
                    unloaderBuilding.setAccessible(true);
            }
            // DumpTile fallback
            unloaderDumpTileField = findFieldByType(Unloader.UnloaderBuild.class, Tile.class);
            if (unloaderDumpTileField != null)
                unloaderDumpTileField.setAccessible(true);

            reflectionInitialized = true;
            Log.info("[DistReveal] Reflection Initialized.");
        } catch (Exception e) {
            Log.err("[DistReveal] Init Error: " + e.getMessage());
        }
    }

    private static Field findField(Class<?> type, String name) {
        try {
            return type.getDeclaredField(name);
        } catch (Exception ignored) {
        }
        try {
            return type.getField(name);
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Field findFieldByType(Class<?> type, Class<?> fieldType) {
        for (Field f : type.getDeclaredFields()) {
            if (f.getType().equals(fieldType))
                return f;
        }
        for (Field f : type.getFields()) {
            if (f.getType().equals(fieldType))
                return f;
        }
        return null;
    }

    public void draw() {
        if (!Vars.state.isGame())
            return;
        if (!reflectionInitialized)
            initReflection();
        if (Vars.renderer.getScale() < zoomThreshold)
            return;

        Draw.z(Layer.power + 1);

        arc.math.geom.Rect view = Core.camera.bounds(Tmp.r1);
        int top = (int) (view.y + view.height) / 8;
        int bottom = (int) view.y / 8;
        int left = (int) view.x / 8;
        int right = (int) (view.x + view.width) / 8;

        for (int x = left; x <= right; x++) {
            for (int y = bottom; y <= top; y++) {
                Tile tile = Vars.world.tile(x, y);
                if (tile == null)
                    continue;
                Building build = tile.build;
                if (build == null || build.tile != tile)
                    continue;
                if (build instanceof ItemBridge.ItemBridgeBuild && revealBridge) {
                    ItemBridge.ItemBridgeBuild ib = (ItemBridge.ItemBridgeBuild) build;
                    if (ib.block.hasLiquids) {
                        drawLiquidBridge(build);
                    } else if (ib instanceof BufferedItemBridge.BufferedItemBridgeBuild) {
                        drawBufferedItemBridge((BufferedItemBridge.BufferedItemBridgeBuild) ib);
                    } else {
                        drawItemBridge(ib);
                    }
                } else if (build instanceof DuctBridge.DuctBridgeBuild && revealBridge) {
                    DuctBridge.DuctBridgeBuild db = (DuctBridge.DuctBridgeBuild) build;
                    if (db.block.hasLiquids) {
                        drawLiquidBridge(build);
                    } else {
                        drawDuctBridge(db);
                    }
                } else if (build instanceof Junction.JunctionBuild && revealJunction) {
                    drawJunction((Junction.JunctionBuild) build);
                } else if (build instanceof Unloader.UnloaderBuild && revealUnloader) {
                    drawUnloader((Unloader.UnloaderBuild) build);
                } else if (build instanceof Router.RouterBuild && revealJunction) {
                    drawRouter((Router.RouterBuild) build);
                } else if (build instanceof DirectionalUnloader.DirectionalUnloaderBuild && revealUnloader) {
                    drawDirectionalUnloader((DirectionalUnloader.DirectionalUnloaderBuild) build);
                } // End of Distribution checks

                if (revealInventory) {
                    drawItemStack(build);
                }
            }
        }
        Draw.reset();

    }

    // --- Core Distribution Logic ---

    private void drawBufferedItemBridge(BufferedItemBridge.BufferedItemBridgeBuild bb) {
        Draw.reset();

        // Reflection Get
        Object buffer = null;
        try {
            if (itemBridgeBuffer != null)
                buffer = itemBridgeBuffer.get(bb);
        } catch (Exception e) {
        }
        if (buffer == null)
            return;

        long[] bufferbuffer = null;
        try {
            if (itemBridgeBufferBuffer != null)
                bufferbuffer = (long[]) itemBridgeBufferBuffer.get(buffer);
        } catch (Exception e) {
        }
        if (bufferbuffer == null)
            return;

        int index = 0;
        try {
            if (itemBridgeBufferIndex != null)
                index = itemBridgeBufferIndex.getInt(buffer);
        } catch (Exception e) {
        }

        // Vectors
        Vec2 vline = Tmp.v1;
        Vec2 voffset = Tmp.v2;

        if (((ItemBridge) bb.block).linkValid(bb.tile, Vars.world.tile(bb.link))) {
            vline.set(Vars.world.tile(bb.link)).sub(bb);
            voffset.set(bb);
        } else {
            vline.set(Vars.tilesize, 0f);
            voffset.set(bb).sub(Vars.tilesize / 2f, Vars.tilesize / 2f);
        }

        int cap = ((BufferedItemBridge) bb.block).bufferCapacity;
        float speed = ((BufferedItemBridge) bb.block).speed;
        Draw.alpha(0.9f); // Set specific transparency for visibility

        for (int idi = 0; idi < bufferbuffer.length && idi < index; idi++) {
            float time = Float.intBitsToFloat(Pack.leftInt(bufferbuffer[idi]));
            Item item = Vars.content.item(Pack.leftShort(Pack.rightInt(bufferbuffer[idi])));

            if (item != null) {
                // Determine item progress based on speed and capacity
                float progress = Math.min(((Time.time - time) * bb.timeScale() / speed) * cap, cap - idi);

                float drawX = voffset.x + (vline.x / bufferbuffer.length * progress);
                float drawY = voffset.y + (vline.y / bufferbuffer.length * progress);

                // In user code:
                // voffset.x + (vline.x / bufferbuffer.length * Math.min(...) )
                // Note that 'bufferbuffer.length' IS 'cap' usually (bufferCapacity).

                Draw.color(Color.white); // FORCE WHITE for visibility
                Draw.rect(item.fullIcon, drawX, drawY, 4f, 4f);
                Draw.reset();
            }
        }

        drawItemBridge(bb);
    }

    private void drawItemBridge(ItemBridge.ItemBridgeBuild ib) {
        Draw.reset();

        // Draw connection line to linked tile
        Tile other = Vars.world.tile(ib.link);
        if (other != null && ((ItemBridge) ib.block).linkValid(ib.tile, other)) {
            // Dim line if inactive/unpowered
            float alpha = ib.efficiency > 0 ? 0.6f : 0.2f;
            Color itemColor = getItemColor(ib);
            Draw.color(itemColor, alpha);
            Lines.stroke(1f);
            Lines.line(ib.x, ib.y, other.drawx(), other.drawy());

            // Animate items "teleporting" - show items moving along the line
            // ONLY if:
            // 1. Has items
            // 2. Is Active (powered/efficient)
            if (ib.items != null && ib.items.total() > 0 && ib.efficiency > 0) {
                // Use FASTER cyclic animation based on time to simulate speed
                // 20 ticks = ~0.33 seconds
                float cycle = (Time.time % 20f) / 20f;

                // Show 1-3 items moving along the path based on total items
                int showCount = Math.min(3, ib.items.total());
                for (int i = 0; i < showCount; i++) {
                    float offset = i / (float) showCount;
                    float progress = (cycle + offset) % 1f;

                    float x = ib.x + (other.drawx() - ib.x) * progress;
                    float y = ib.y + (other.drawy() - ib.y) * progress;

                    // Get the most abundant item to show
                    Item showItem = null;
                    int maxAmount = 0;
                    for (int iid = 0; iid < ib.items.length(); iid++) {
                        if (ib.items.get(iid) > maxAmount) {
                            maxAmount = ib.items.get(iid);
                            showItem = Vars.content.item(iid);
                        }
                    }

                    if (showItem != null) {
                        Draw.color(Color.white, 0.7f * ib.efficiency);
                        Draw.rect(showItem.fullIcon, x, y, 3f, 3f);
                    }
                }
            }
        }

        // Draw items at the bridge itself (inventory)
        if (ib.items != null) {
            Draw.color(Color.white, 0.8f);
            int loti = 0;
            for (int iid = 0; iid < ib.items.length(); iid++) {
                if (ib.items.get(iid) > 0) {
                    for (int itemid = 1; itemid <= ib.items.get(iid); itemid++) {
                        Draw.rect(Vars.content.item(iid).fullIcon, ib.x,
                                ib.y + Vars.tilesize * (-0.5f + 0.8f * loti / (float) ib.block.itemCapacity) + 1f, 4f,
                                4f);
                        loti++;
                    }
                }
            }
        }
        Draw.reset();
    }

    private void drawJunction(Junction.JunctionBuild jb) {
        if (junctionBufferField == null)
            return;

        try {
            Object bufferObj = junctionBufferField.get(jb);
            if (bufferObj == null)
                return;

            long[][] buffers = (long[][]) junctionBufferBuffersField.get(bufferObj);
            int[] indexes = (int[]) junctionBufferIndexesField.get(bufferObj);

            float cap = ((Junction) jb.block).capacity;
            float speed = ((Junction) jb.block).speed;

            for (int rot = 0; rot < 4; rot++) {
                for (int i = 0; i < indexes[rot]; i++) {
                    // Helper to extract Time and Item from JunctionBuffer (which is just long)
                    long val = buffers[rot][i];
                    float time = Float.intBitsToFloat(Pack.leftInt(val));
                    // int itemId = (int) val & 0xFFFF; // Removed unused var

                    // Junction Buffer handling
                    Item item = Vars.content.item((short) val);
                    if (item == null)
                        continue;

                    Draw.alpha(0.9f);

                    // Physical position calculation for junctioned items
                    float prog = Math.min((Time.time - time) * jb.timeScale() / speed, 1f - i / cap);

                    Vec2 pos = Tmp.v1.set(-0.25f + 0.75f * prog, 0.25f).rotate(90 * rot).scl(Vars.tilesize).add(jb);

                    Draw.color(Color.white);
                    Draw.rect(item.fullIcon, pos.x, pos.y, 4f, 4f);
                    Draw.reset();
                }
            }
        } catch (Exception e) {
        }
    }

    private void drawUnloader(Unloader.UnloaderBuild ub) {
        try {
            Unloader block = (Unloader) ub.block;
            Item drawItem = Vars.content.item(ub.rotations);

            // Need 'possibleBlocks'? UnloaderBuild has it public usually?
            // V7: public Seq<Building> possibleBlocks = new Seq<>();

            // Fallback to robust line drawing if primary target resolution fails.
            // Uses ContainerStat objects for precise targeting where available.

            // Reflection for dumpingTo/From
            // Employs intersection logic for visual representation.

            Object toCont = unloaderDumpingTo != null ? unloaderDumpingTo.get(ub) : null;
            Building tob = null;
            if (toCont != null && unloaderBuilding != null)
                tob = (Building) unloaderBuilding.get(toCont);

            // Manual fallback for tile
            if (tob == null && unloaderDumpTileField != null) {
                Tile t = (Tile) unloaderDumpTileField.get(ub);
                if (t != null)
                    tob = t.build;
            }

            // Logic: Draw line to 'dumpingTo' target or 'dumpTile'
            if (tob != null) {
                DrawLineIntersection(ub, tob, block.speed);
            }

            if (drawItem != null) { // sortItem
                Draw.color();
                Draw.rect(drawItem.fullIcon, ub.x, ub.y, 4f, 4f);
            }

        } catch (Exception e) {
        }
    }

    // Simplified intersection helper from user code
    private void DrawLineIntersection(Building source, Building target, float speed) {
        Vec2 off = Tmp.v1;
        Vec2 end = Tmp.v2;

        end.x = Math.min(source.x + source.block.size * Vars.tilesize / 2f,
                target.x + target.block.size * Vars.tilesize / 2f);
        end.y = Math.min(source.y + source.block.size * Vars.tilesize / 2f,
                target.y + target.block.size * Vars.tilesize / 2f);
        off.x = Math.max(source.x - source.block.size * Vars.tilesize / 2f,
                target.x - target.block.size * Vars.tilesize / 2f);
        off.y = Math.max(source.y - source.block.size * Vars.tilesize / 2f,
                target.y - target.block.size * Vars.tilesize / 2f);

        // UnloadTimer logic? source is UnloaderBuild. unloadTimer is public.
        float timer = 0;
        if (source instanceof Unloader.UnloaderBuild)
            timer = ((Unloader.UnloaderBuild) source).unloadTimer;

        Draw.color(Pal.placing, timer < speed ? 1f : 0.25f);
        Lines.stroke(1.5f);
        Lines.line(off.x, off.y, end.x, end.y);
        Draw.reset();
    }

    private void drawDuctBridge(DuctBridge.DuctBridgeBuild ib) {
        Draw.reset();

        int link = -1;
        // Try getting link from config() which is standard API
        try {
            Object conf = ib.config();
            if (conf instanceof Integer) {
                link = (Integer) conf;
            } else if (conf instanceof Float) { // Sometimes stored as float in save?
                link = ((Float) conf).intValue();
            } else {
                // Fallback to reflection if config isn't the link (e.g. logic controlled)
                if (bridgeLinkField != null)
                    link = bridgeLinkField.getInt(ib);
            }
        } catch (Exception e) {
        }

        Tile other = Vars.world.tile(link);

        // Fallback: If link is invalid (-1), scan for target in facing direction
        // DuctBridge typically auto-connects to the first valid bridge in range
        if (other == null && ib.block instanceof DuctBridge) {
            DuctBridge dbBlock = (DuctBridge) ib.block;
            // Range is in tiles
            int range = (int) dbBlock.range;
            int rot = ib.rotation;

            // Standard Mindustry cardinal directions: 0=right, 1=up, 2=left, 3=down
            // Geometry.d4 gives point delta
            arc.math.geom.Point2 delta = arc.math.geom.Geometry.d4(rot);

            for (int i = 1; i <= range; i++) {
                // Calculate target tile
                // ib.tile.x/y are integers
                Tile t = Vars.world.tile(ib.tile.x + delta.x * i, ib.tile.y + delta.y * i);
                if (t != null && t.build != null && t.block() == ib.block && t.team() == ib.team) {
                    other = t;
                    break;
                }
            }
        }

        // Manual validation since linkValid might be unavailable/protected
        // Use accessor methods: .block() and .team()
        boolean active = other != null && other.block() == ib.block && other.team() == ib.team;

        if (active) {
            // Dim line if inactive/unpowered
            float alpha = ib.efficiency > 0 ? 0.6f : 0.2f;
            Color itemColor = getItemColor(ib);
            Draw.color(itemColor, alpha);
            Lines.stroke(1f);
            Lines.line(ib.x, ib.y, other.drawx(), other.drawy());

            // Animate items "teleporting"
            if (ib.items != null && ib.items.total() > 0 && ib.efficiency > 0) {
                // Faster cycle for instant transport feel
                float cycle = (Time.time % 20f) / 20f;
                int showCount = Math.min(3, ib.items.total());
                for (int i = 0; i < showCount; i++) {
                    float offset = i / (float) showCount;
                    float progress = (cycle + offset) % 1f;

                    float x = ib.x + (other.drawx() - ib.x) * progress;
                    float y = ib.y + (other.drawy() - ib.y) * progress;

                    // Get the most abundant item to show
                    Item showItem = null;
                    int maxAmount = 0;
                    for (Item it : Vars.content.items()) {
                        if (ib.items.has(it) && ib.items.get(it) > maxAmount) {
                            maxAmount = ib.items.get(it);
                            showItem = it;
                        }
                    }

                    if (showItem != null) {
                        Draw.color(Color.white, 0.7f * ib.efficiency);
                        Draw.rect(showItem.fullIcon, x, y, 3f, 3f);
                    }
                }
            }
        }
        Draw.reset();
    }

    private void drawLiquidBridge(Building lb) {
        Draw.reset();

        int link = -1;
        // Link Logic (Robust mix of config, reflection, and scan)
        try {
            Object conf = lb.config();
            if (conf instanceof Integer) {
                link = (Integer) conf;
            } else if (conf instanceof Float) {
                link = ((Float) conf).intValue();
            } else {
                if (bridgeLinkField != null && lb instanceof ItemBridge.ItemBridgeBuild)
                    link = bridgeLinkField.getInt(lb);
            }
        } catch (Exception e) {
        }

        Tile other = null;

        // Try lastLink reflection (DirectionBridge style)
        if (directionBridgeLinkField != null) {
            try {
                Object lastLinkObj = directionBridgeLinkField.get(lb);
                if (lastLinkObj instanceof Building) {
                    other = ((Building) lastLinkObj).tile;
                }
            } catch (Exception e) {
            }
        }
        if (link != -1) {
            // Try absolute first (Serpulo)
            other = Vars.world.tile(link);

            // If absolute looks wrong (too far or null or not the same block),
            // try relative packed (Erekir style)
            // Relative links in Mindustry are usually small deltas packed via Pack.pos
            if (other == null || other.block() != lb.block || Mathf.dst(lb.tile.x, lb.tile.y, other.x, other.y) > 20) {
                int px = (short) (link >>> 16);
                int py = (short) (link & 0xFFFF);
                // Position unpacking for relatives often involves signedness correction if
                // needed,
                // but Mindustry's Pack usually handles 16-bit signed shorts.
                Tile relTile = Vars.world.tile(lb.tile.x + px, lb.tile.y + py);
                if (relTile != null && relTile.build != null && relTile.block() == lb.block) {
                    other = relTile;
                }
            }
        }

        // Fallback: Directional Scan (for Erekir Reinforced Conduits if link still
        // fails)
        if (other == null || other.block() != lb.block) {
            // ReinforcedBridgeConduit might be DirectionalLiquidBridge
            // Try to find range field via reflection
            int range = 10; // Default fallback
            try {
                // Try to get range from block
                Field f = lb.block.getClass().getField("range");
                if (f != null) {
                    Object r = f.get(lb.block);
                    if (r instanceof Integer)
                        range = (Integer) r;
                    else if (r instanceof Float)
                        range = ((Float) r).intValue();
                }
            } catch (Exception e) {
                // Fallback to LiquidBridge interface if available
                if (lb.block instanceof LiquidBridge) {
                    range = ((LiquidBridge) lb.block).range;
                }
            }

            int rot = lb.rotation;
            arc.math.geom.Point2 delta = arc.math.geom.Geometry.d4(rot);

            for (int i = 1; i <= range; i++) {
                Tile t = Vars.world.tile(lb.tile.x + delta.x * i, lb.tile.y + delta.y * i);
                if (t != null && t.build != null && t.block() == lb.block && t.team() == lb.team) {
                    other = t;
                    break;
                }
            }
        }

        // Validity Check
        boolean active = other != null && other.block() == lb.block && other.team() == lb.team;

        if (active) {
            // Use current liquid color or default blue if empty
            mindustry.type.Liquid currentLiquid = lb.liquids.current();
            Color liquidColor = currentLiquid != null ? currentLiquid.color : Color.royal;
            float liquidAmount = currentLiquid != null ? lb.liquids.get(currentLiquid) : 0f;

            if (liquidAmount < 0.01f)
                liquidColor = Color.gray; // Gray if empty/insignificant

            // Dim line if inactive/unpowered
            float alpha = lb.efficiency > 0 ? 0.8f : 0.3f;
            Draw.color(liquidColor, alpha);
            Lines.stroke(1f); // Match item bridge thickness
            Lines.line(lb.x, lb.y, other.drawx(), other.drawy());

            // Animate Liquid Flow (Icon + Bubbles)
            if (liquidAmount > 0.1f && lb.efficiency > 0) {
                // Slower, smoother flow
                float cycle = (Time.time % 60f) / 60f;

                // Show 2 icons/bubbles for clarity
                for (int i = 0; i < 2; i++) {
                    float offset = i / 2f;
                    float progress = (cycle + offset) % 1f;

                    float x = lb.x + (other.drawx() - lb.x) * progress;
                    float y = lb.y + (other.drawy() - lb.y) * progress;

                    // Draw liquid icon
                    Draw.color(Color.white, 0.8f * lb.efficiency);
                    Draw.rect(currentLiquid.fullIcon, x, y, 3f, 3f);

                }
            }
        }
        Draw.reset();
    }

    private Color getItemColor(Building build) {
        if (build.items == null || build.items.total() == 0)
            return Pal.accent;
        Item showItem = null;
        int maxAmount = 0;
        for (Item it : Vars.content.items()) {
            if (build.items.has(it) && build.items.get(it) > maxAmount) {
                maxAmount = build.items.get(it);
                showItem = it;
            }
        }
        return showItem != null ? showItem.color : Pal.accent;
    }

    private void drawRouter(Router.RouterBuild rb) {
        Building fromb = rb.lastInput == null ? null : rb.lastInput.build;
        // Logic for target block selection (rotation based)
        // rb.proximity is Seq<Building>
        Building tob = rb.proximity.size == 0 ? null
                : rb.proximity.get(((rb.rotation) % rb.proximity.size - 1 + rb.proximity.size) % rb.proximity.size);

        Draw.color();
        if (tob != null) {
            Vec2 off = Tmp.v1, end = Tmp.v2;
            // line length: sum of block sizes sub xy distance
            // Distance calculation based on block geometry
            end.set(rb).sub(tob);
            end.x = (rb.block.size + tob.block.size) * Vars.tilesize / 2f - Math.abs(end.x);
            end.y = (rb.block.size + tob.block.size) * Vars.tilesize / 2f - Math.abs(end.y);
            // line offset
            off.x = rb.x > tob.x ? rb.x - rb.block.size * Vars.tilesize / 2f
                    : tob.x - tob.block.size * Vars.tilesize / 2f;
            off.y = rb.y > tob.y ? rb.y - rb.block.size * Vars.tilesize / 2f
                    : tob.y - tob.block.size * Vars.tilesize / 2f;
            end.add(off);

            Draw.color(Pal.placing, 1 - rb.time);
            Lines.stroke(1.5f);
            Lines.line(off.x, off.y, end.x, end.y);
        }

        if (fromb != null) {
            Vec2 off = Tmp.v1, end = Tmp.v2;
            end.set(rb).sub(fromb);
            end.x = (rb.block.size + fromb.block.size) * Vars.tilesize / 2f - Math.abs(end.x);
            end.y = (rb.block.size + fromb.block.size) * Vars.tilesize / 2f - Math.abs(end.y);

            off.x = rb.x > fromb.x ? rb.x - rb.block.size * Vars.tilesize / 2f
                    : fromb.x - fromb.block.size * Vars.tilesize / 2f;
            off.y = rb.y > fromb.y ? rb.y - rb.block.size * Vars.tilesize / 2f
                    : fromb.y - fromb.block.size * Vars.tilesize / 2f;

            // margin logic using Geometry.d4
            // Geometry.d4 returns Point2 (x,y) for 0,1,2,3
            arc.math.geom.Point2 p = arc.math.geom.Geometry
                    .d4(Mathf.mod(((int) arc.math.Angles.angle(fromb.x - rb.x, fromb.y - rb.y) + 45) / 90, 4));

            // Adjust off and end
            off.add(p.x * -2f, p.y * -2f).add(p.y == 0f ? 0f : 2f, p.x == 0f ? 0f : 2f);
            end.add(off).sub(p.y == 0f ? 0f : 4f, p.x == 0f ? 0f : 4f);

            Draw.color(Pal.remove, 1 - rb.time);
            Lines.stroke(1.5f);
            Lines.line(off.x, off.y, end.x, end.y);
        }

        if (rb.lastItem != null) {
            Draw.color(Color.white);
            Draw.rect(rb.lastItem.fullIcon, rb.x, rb.y, 4f, 4f);
        }
        Draw.reset();
    }

    private void drawDirectionalUnloader(DirectionalUnloader.DirectionalUnloaderBuild db) {
        Draw.color();
        Building front = db.front(), back = db.back();
        if (front == null || back == null || back.items == null || front.team != db.team || back.team != db.team
                || !back.canUnload()
                || !(((DirectionalUnloader) db.block).allowCoreUnload || !(back instanceof CoreBlock.CoreBuild)))
            return;

        if (db.unloadItem != null) {
            Draw.alpha(db.unloadTimer / ((DirectionalUnloader) db.block).speed < 1f && back.items.has(db.unloadItem)
                    && front.acceptItem(db, db.unloadItem) ? 0.8f : 0f);
            Draw.rect(db.unloadItem.fullIcon, db.x, db.y, 4f, 4f);
        } else {
            var itemseq = Vars.content.items();
            for (int i = 0; i < itemseq.size; i++) {
                Item item = itemseq.get((i + db.offset) % itemseq.size);
                if (back.items.has(item) && front.acceptItem(db, item)) {
                    Draw.alpha(0.8f);
                    Draw.rect(item.fullIcon, db.x, db.y, 4f, 4f);
                    break;
                }
            }
        }
        Draw.color();
        Draw.reset();
    }

    private void drawItemStack(Building b) {
        if (b.items == null || b.items.total() == 0)
            return;

        // Simple generic inventory visualization
        // Draw a small dot or bar for each item type present
        float startX = b.x - b.block.size * Vars.tilesize / 2f + 2f;
        float startY = b.y + b.block.size * Vars.tilesize / 2f - 2f;

        int rows = 0;
        int cols = 0;

        if (b.block.itemCapacity > 0) {
            Draw.color(Color.white, 0.7f);
            // Sort of a mini-bar chart or grid
            // For now just draw the most abundant item icon?
            // Or iterate and draw small icons.

            // Simplest approach: Draw icon of most abundant item and a bar?
            // MI2U generic is likely more complex.
            // Let's implement a cycle display of items

            int total = b.items.total();
            if (total > 0) {
                Item item = b.items.first(); // Just get one for now? No order in ItemModule
                // Find max
                int max = 0;
                for (Item it : Vars.content.items()) {
                    if (b.items.has(it) && b.items.get(it) > max) {
                        max = b.items.get(it);
                        item = it;
                    }
                }

                if (item != null) {
                    Draw.rect(item.fullIcon, b.x, b.y, 4f, 4f);
                    // Maybe a small text for amount?
                    // Font rendering in world is expensive/complex with transform.
                }
            }
        }
        Draw.reset();
    }

    public void showSettings() {
        if (dialog == null) {
            dialog = new mindustry.ui.dialogs.BaseDialog("Distribution Reveal Settings");
            dialog.addCloseButton();
            rebuildDialog();
        }
        dialog.show();
    }

    private void rebuildDialog() {
        dialog.cont.clear();
        dialog.cont.add("Scale Threshold").color(Pal.accent).row();
        dialog.cont.slider(1f, 10f, 0.5f, zoomThreshold, v -> {
            zoomThreshold = v;
            Core.settings.put("dist_zoom_threshold", (int) (v * 100));
        }).row();
        dialog.cont.label(() -> String.format("%.1fx", zoomThreshold)).row();

        dialog.cont.check("Reveal Bridge", revealBridge, v -> {
            revealBridge = v;
            Core.settings.put("dist_reveal_bridge", v);
        }).row();
        dialog.cont.check("Reveal Junction", revealJunction, v -> {
            revealJunction = v;
            Core.settings.put("dist_reveal_junction", v);
        }).row();
        dialog.cont.check("Reveal Unloader", revealUnloader, v -> {
            revealUnloader = v;
            Core.settings.put("dist_reveal_unloader", v);
        }).row();
    }
}
