package mindustrytool.visuals;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Rect;
import arc.math.geom.Vec2;
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;

import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.BufferedItemBridge;
import mindustry.world.blocks.distribution.DuctBridge;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.blocks.distribution.Router;
import mindustry.world.blocks.liquid.LiquidBridge;
import mindustry.world.blocks.storage.Unloader;
import mindustry.world.modules.ItemModule;
import mindustry.world.modules.LiquidModule;

import static mindustry.Vars.*;

/**
 * Visualizes bridge connections with colored lines showing flow status.
 * 
 * Features:
 * - Color-coded lines: Gray (inactive), Light (blocked), Normal (active)
 * - Striped lines for multi-item bridges
 * - Arrowhead at line start indicating flow direction
 * - Bottleneck detection (red warning when near capacity)
 * - Router and Unloader visualization (showing actual flow)
 * 
 * Supports:
 * - Serpulo: Bridge Conveyor, Phase Conveyor, Bridge Conduit, Phase Conduit,
 * Router, Distributor, Unloader
 * - Erekir: Duct Bridge, Reinforced Bridge Conduit
 */
public class DistributionRevealVisualizer {

    // === Settings Keys ===
    private static final String PREFIX = "distribution.";

    // === Settings (Persisted) ===
    private boolean showItemBridges;
    private boolean showLiquidBridges;
    private boolean showRouters;
    private boolean showUnloaders;
    private boolean showArrows;
    private boolean showBottleneck;
    private float lineThickness;
    private float zoomThreshold;
    private float arrowSize;

    // === UI ===
    private BaseDialog dialog;

    // === Cache ===
    private final Rect viewBounds = new Rect();
    private final Color tmpColor = new Color();

    public DistributionRevealVisualizer() {
        Log.info("[DistributionRevealVisualizer] INITIALIZED.");

        loadSettings();
    }

    private void loadSettings() {
        showItemBridges = Core.settings.getBool(PREFIX + "showItemBridges", true);
        showLiquidBridges = Core.settings.getBool(PREFIX + "showLiquidBridges", true);
        showRouters = Core.settings.getBool(PREFIX + "showRouters", true);
        showUnloaders = Core.settings.getBool(PREFIX + "showUnloaders", true);
        showArrows = Core.settings.getBool(PREFIX + "showArrows", true);
        showBottleneck = Core.settings.getBool(PREFIX + "showBottleneck", true);
        lineThickness = Core.settings.getFloat(PREFIX + "lineThickness", 1f);
        zoomThreshold = Core.settings.getFloat(PREFIX + "zoomThreshold", 0.3f);
        arrowSize = Core.settings.getFloat(PREFIX + "arrowSize", 2.5f);

        // Force migrate old thick line to new thin line
        if (lineThickness >= 2f) {
            lineThickness = 1f;
            Core.settings.put(PREFIX + "lineThickness", lineThickness);
        }
        // Ensure arrow is visible (restore if too small)
        if (arrowSize < 2f) {
            arrowSize = 2.5f;
            Core.settings.put(PREFIX + "arrowSize", arrowSize);
        }
    }

    private void saveSettings() {
        Core.settings.put(PREFIX + "showItemBridges", showItemBridges);
        Core.settings.put(PREFIX + "showLiquidBridges", showLiquidBridges);
        Core.settings.put(PREFIX + "showRouters", showRouters);
        Core.settings.put(PREFIX + "showUnloaders", showUnloaders);
        Core.settings.put(PREFIX + "showArrows", showArrows);
        Core.settings.put(PREFIX + "showBottleneck", showBottleneck);
        Core.settings.put(PREFIX + "lineThickness", lineThickness);
        Core.settings.put(PREFIX + "zoomThreshold", zoomThreshold);
        Core.settings.put(PREFIX + "arrowSize", arrowSize);
    }

    // =============================================
    // MAIN DRAW METHOD
    // =============================================

    public void draw() {
        if (!state.isGame())
            return;

        // Zoom threshold check
        float zoom = renderer.getScale();
        if (zoomThreshold > 0 && zoom < zoomThreshold)
            return;

        // Get visible area
        Core.camera.bounds(viewBounds);

        float z = Draw.z();
        Draw.z(Layer.overlayUI);

        // Iterate visible tiles
        int minX = Math.max((int) ((viewBounds.x) / tilesize) - 1, 0);
        int minY = Math.max((int) ((viewBounds.y) / tilesize) - 1, 0);
        int maxX = Math.min((int) ((viewBounds.x + viewBounds.width) / tilesize) + 1, world.width() - 1);
        int maxY = Math.min((int) ((viewBounds.y + viewBounds.height) / tilesize) + 1, world.height() - 1);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                Tile tile = world.tile(x, y);
                if (tile == null || tile.build == null)
                    continue;

                Building build = tile.build;

                // Skip if not our team in fog
                if (state.rules.fog && build.inFogTo(player.team()))
                    continue;

                // Item Bridges
                if (showItemBridges) {
                    if (build instanceof BufferedItemBridge.BufferedItemBridgeBuild bb) {
                        drawItemBridgeLine(bb);
                    } else if (build instanceof ItemBridge.ItemBridgeBuild ib) {
                        drawItemBridgeLine(ib);
                    } else if (build instanceof DuctBridge.DuctBridgeBuild db) {
                        drawDuctBridgeLine(db);
                    }
                }

                // Liquid Bridges
                if (showLiquidBridges) {
                    if (build instanceof LiquidBridge.LiquidBridgeBuild lb) {
                        drawLiquidBridgeLine(lb);
                    }
                    // Reinforced Bridge Conduit (Erekir) - check by class hierarchy
                    if (build.block.name.contains("reinforced-bridge-conduit") ||
                            build.block.name.contains("phase-conduit")) {
                        // These extend LiquidBridge, handled above
                    }
                }

                // Routers & Distributors
                if (showRouters && build instanceof Router.RouterBuild rb) {
                    drawRouter(rb);
                }

                // Unloaders
                if (showUnloaders && build instanceof Unloader.UnloaderBuild ub) {
                    drawUnloader(ub);
                }
            }
        }

        Draw.z(z);
        Draw.reset();
    }

    // =============================================
    // ITEM BRIDGE DRAWING
    // =============================================

    private void drawItemBridgeLine(ItemBridge.ItemBridgeBuild build) {
        // Get linked building
        Building linked = world.build(build.link);
        if (linked == null)
            return;

        // Validate link
        ItemBridge block = (ItemBridge) build.block;
        if (!block.linkValid(build.tile, linked.tile))
            return;

        // Get items
        ItemModule items = build.items;
        boolean hasItems = items != null && items.total() > 0;

        // Check if can transport
        boolean canTransport = false;
        if (hasItems && linked.team == build.team) {
            if (linked instanceof ItemBridge.ItemBridgeBuild) {
                canTransport = true;
            } else {
                // Check if target can accept
                Item firstItem = getFirstItem(items);
                if (firstItem != null) {
                    canTransport = linked.acceptItem(build, firstItem);
                }
            }
        }

        // Check bottleneck
        boolean isBottleneck = hasItems && items.total() >= build.block.itemCapacity * 0.9f;

        // Draw
        float x1 = build.x, y1 = build.y;
        float x2 = linked.x, y2 = linked.y;

        if (hasItems && items.total() > 0) {
            Item firstItem = getFirstItem(items);
            Color color = new Color(firstItem != null ? firstItem.color : Color.gray);
            color.a(canTransport ? 0.8f : 0.35f);

            // Draw main line
            Draw.color(color);
            Lines.stroke(lineThickness);
            Lines.line(x1, y1, x2, y2);

            // Draw arrow at start
            if (showArrows) {
                Color arrowColor = (isBottleneck && showBottleneck)
                        ? new Color(Pal.remove).a(0.8f)
                        : color;
                drawArrowhead(x1, y1, x2, y2, arrowColor);
            }

            // Draw integrated output extensions from destination bridge (same color,
            // continuous)
            if (linked instanceof ItemBridge.ItemBridgeBuild linkedBridge) {
                drawIntegratedOutputs(linkedBridge, color);
            }
        } else {
            // Empty - gray line
            Draw.color(Color.gray, 0.4f);
            Lines.stroke(lineThickness);
            Lines.line(x1, y1, x2, y2);

            // Still draw gray output branches when empty
            if (linked instanceof ItemBridge.ItemBridgeBuild linkedBridge) {
                Color grayColor = new Color(Color.gray).a(0.4f);
                drawIntegratedOutputs(linkedBridge, grayColor);
            }
        }
    }

    private void drawDuctBridgeLine(DuctBridge.DuctBridgeBuild build) {
        // Duct bridge uses different link mechanism
        Building linked = build.findLink();
        if (linked == null)
            return;

        ItemModule items = build.items;
        boolean hasItems = items != null && items.total() > 0;

        boolean canTransport = hasItems && linked.team == build.team;
        boolean isBottleneck = hasItems && items.total() >= build.block.itemCapacity * 0.9f;

        float x1 = build.x, y1 = build.y;
        float x2 = linked.x, y2 = linked.y;

        if (hasItems && items.total() > 0) {
            Item firstItem = getFirstItem(items);
            Color color = new Color(firstItem != null ? firstItem.color : Color.gray);
            color.a(canTransport ? 0.8f : 0.35f);

            // Draw main line
            Draw.color(color);
            Lines.stroke(lineThickness);
            Lines.line(x1, y1, x2, y2);

            // Draw arrow at start
            if (showArrows) {
                Color arrowColor = (isBottleneck && showBottleneck)
                        ? new Color(Pal.remove).a(0.8f)
                        : color;
                drawArrowhead(x1, y1, x2, y2, arrowColor);
            }

            // Draw integrated output extensions from destination bridge
            if (linked instanceof DuctBridge.DuctBridgeBuild linkedBridge) {
                drawDuctIntegratedOutputs(linkedBridge, color);
            }
        } else {
            // Empty - gray line
            Draw.color(Color.gray, 0.4f);
            Lines.stroke(lineThickness);
            Lines.line(x1, y1, x2, y2);

            // Still draw gray output branches when empty
            if (linked instanceof DuctBridge.DuctBridgeBuild linkedBridge) {
                Color grayColor = new Color(Color.gray).a(0.4f);
                drawDuctIntegratedOutputs(linkedBridge, grayColor);
            }
        }
    }

    // =============================================
    // LIQUID BRIDGE DRAWING
    // =============================================

    private void drawLiquidBridgeLine(LiquidBridge.LiquidBridgeBuild build) {
        Building linked = world.build(build.link);
        if (linked == null)
            return;

        LiquidBridge block = (LiquidBridge) build.block;
        if (!block.linkValid(build.tile, linked.tile))
            return;

        LiquidModule liquids = build.liquids;
        Liquid currentLiquid = liquids != null ? liquids.current() : null;
        float liquidAmount = currentLiquid != null ? liquids.get(currentLiquid) : 0f;
        boolean hasLiquid = liquidAmount > 0.01f;

        boolean canTransport = hasLiquid && linked.team == build.team &&
                currentLiquid != null && linked.acceptLiquid(build, currentLiquid);

        boolean isBottleneck = hasLiquid && liquidAmount >= block.liquidCapacity * 0.9f;

        float x1 = build.x, y1 = build.y;
        float x2 = linked.x, y2 = linked.y;

        if (hasLiquid) {
            Color color = new Color(currentLiquid != null ? currentLiquid.color : Color.gray);
            // Use fixed alpha to prevent flickering (was: canTransport ? 0.8f : 0.35f)
            color.a(0.7f);

            // Draw main line
            Draw.color(color);
            Lines.stroke(lineThickness);
            Lines.line(x1, y1, x2, y2);

            // Draw arrow at start
            if (showArrows) {
                Color arrowColor = (isBottleneck && showBottleneck)
                        ? new Color(Pal.remove).a(0.8f)
                        : color;
                drawArrowhead(x1, y1, x2, y2, arrowColor);
            }

            // Draw integrated output extensions from destination bridge
            if (linked instanceof LiquidBridge.LiquidBridgeBuild linkedBridge) {
                drawLiquidIntegratedOutputs(linkedBridge, color);
            }
        } else {
            // Empty - gray line
            Draw.color(Color.gray, 0.4f);
            Lines.stroke(lineThickness);
            Lines.line(x1, y1, x2, y2);

            // Still draw gray output branches when empty to prevent flickering
            if (linked instanceof LiquidBridge.LiquidBridgeBuild linkedBridge) {
                Color grayColor = new Color(Color.gray).a(0.4f);
                drawLiquidIntegratedOutputs(linkedBridge, grayColor);
            }
        }
    }

    // =============================================
    // OUTPUT BRANCH DRAWING
    // =============================================

    /**
     * Draw integrated output extensions from bridge to adjacent output buildings
     * Uses same color as main line for seamless continuous appearance
     */
    /**
     * Draw integrated output extensions from bridge to adjacent output buildings
     * Uses same color as main line for seamless continuous appearance
     */
    private void drawIntegratedOutputs(ItemBridge.ItemBridgeBuild bridge, Color color) {
        // Skip if bridge has a valid link (items go to linked bridge, not dumped)
        Tile linkedTile = world.tile(bridge.link);
        ItemBridge block = (ItemBridge) bridge.block;
        if (linkedTile != null && block.linkValid(bridge.tile, linkedTile)) {
            return;
        }

        // CRITICAL FIX: Only show outputs if there is visibly something to output!
        // If bridge is empty, show NOTHING.
        Item item = bridge.items != null ? getFirstItem(bridge.items) : null;
        if (item == null) {
            return;
        }

        // Draw extensions to adjacent output buildings
        for (Building adj : bridge.proximity) {
            if (adj == null)
                continue;
            if (adj instanceof ItemBridge.ItemBridgeBuild)
                continue;
            if (adj.team != bridge.team)
                continue;

            // Strict check: Can the neighbor accept THIS item?
            // This handles filters, directionality, and static capability.
            if (!canBlockAcceptItem(bridge, adj, item)) {
                continue;
            }

            // Double check directionality to preventing drawing "backwards" to input
            // sources
            // The canBlockAcceptItem already checks if target.front() == source.
            // But we should also check if the bridge thinks this neighbor is an input?
            // (bridge.incoming list might be useful but sometimes unreliable for visual
            // sync)

            // Draw integrated output line - minimal, just the arrow base
            float branchLength = arrowSize * 0.81f;
            float rawAngle = Angles.angle(bridge.x, bridge.y, adj.x, adj.y);
            float angle = Math.round(rawAngle / 90f) * 90f;

            float endX = bridge.x + Angles.trnsx(angle, branchLength);
            float endY = bridge.y + Angles.trnsy(angle, branchLength);

            // Draw line (always colored, never gray/empty)
            Draw.color(color);
            Lines.stroke(lineThickness);
            Lines.line(bridge.x, bridge.y, endX, endY);

            // Draw arrow at end of branch
            if (showArrows) {
                drawArrowhead(bridge.x, bridge.y, endX, endY, color);
            }
        }
    }

    /**
     * Draw integrated output extensions from duct bridge to adjacent output
     * buildings
     */
    /**
     * Draw integrated output extensions from duct bridge to adjacent output
     * buildings
     */
    private void drawDuctIntegratedOutputs(DuctBridge.DuctBridgeBuild bridge, Color color) {
        // STRICT: If no items, show nothing.
        Item item = bridge.items != null ? getFirstItem(bridge.items) : null;
        if (item == null) {
            return;
        }

        for (Building adj : bridge.proximity) {
            if (adj == null)
                continue;
            if (adj instanceof DuctBridge.DuctBridgeBuild)
                continue;
            if (adj.team != bridge.team)
                continue;

            // Strict check: Can the neighbor accept THIS item?
            if (!canBlockAcceptItem(bridge, adj, item)) {
                continue;
            }

            float branchLength = arrowSize * 0.9f;
            float rawAngle = Angles.angle(bridge.x, bridge.y, adj.x, adj.y);
            // Snap to 90 degree increments (perpendicular)
            float angle = Math.round(rawAngle / 90f) * 90f;

            float endX = bridge.x + Angles.trnsx(angle, branchLength);
            float endY = bridge.y + Angles.trnsy(angle, branchLength);

            Draw.color(color);
            Lines.stroke(lineThickness);
            Lines.line(bridge.x, bridge.y, endX, endY);

            // Draw arrow at end of branch
            if (showArrows) {
                drawArrowhead(bridge.x, bridge.y, endX, endY, color);
            }
        }
    }

    /**
     * Draw integrated output extensions from liquid bridge to adjacent output
     * buildings
     */
    /**
     * Draw integrated output extensions from liquid bridge to adjacent output
     * buildings
     */
    private void drawLiquidIntegratedOutputs(LiquidBridge.LiquidBridgeBuild bridge, Color color) {
        Liquid liq = bridge.liquids != null ? bridge.liquids.current() : null;
        // STRICT: If no liquid, show nothing.
        if (liq == null || bridge.liquids.get(liq) <= 0.01f) {
            return;
        }

        for (Building adj : bridge.proximity) {
            if (adj == null)
                continue;
            if (adj instanceof LiquidBridge.LiquidBridgeBuild)
                continue;
            if (adj.team != bridge.team)
                continue;

            // Check if building CAN accept liquids
            // Also strictness check: target must accept THIS specific liquid if known?
            // Usually hasLiquids is enough, but to be safe and avoid showing flow to
            // non-acceptors:
            if (!adj.acceptLiquid(bridge, liq)) {
                // However, acceptLiquid might return false if full.
                // We want to visualize potential flow.
                // Revert to: does it have liquids capability AND is not facing us (if
                // transport)?

                if (!adj.block.hasLiquids)
                    continue;

                // Directionality check for conduits pointing AT us
                // Similar to canBlockAcceptItem logic but for liquids
                if (adj.block instanceof mindustry.world.blocks.liquid.Conduit ||
                        adj.block instanceof mindustry.world.blocks.liquid.LiquidBridge) { // etc
                    if (adj.front() == bridge)
                        continue;
                }
            }

            float branchLength = arrowSize * 0.9f;
            float rawAngle = Angles.angle(bridge.x, bridge.y, adj.x, adj.y);
            // Snap to 90 degree increments (perpendicular)
            float angle = Math.round(rawAngle / 90f) * 90f;

            float endX = bridge.x + Angles.trnsx(angle, branchLength);
            float endY = bridge.y + Angles.trnsy(angle, branchLength);

            Draw.color(color);
            Lines.stroke(lineThickness);
            Lines.line(bridge.x, bridge.y, endX, endY);

            // Draw arrow at end of branch
            if (showArrows) {
                drawArrowhead(bridge.x, bridge.y, endX, endY, color);
            }
        }
    }

    // =============================================
    // DRAWING UTILITIES
    // =============================================

    /**
     * Draw a simple line with arrowhead at start
     * 
     * @param isBottleneck If true, arrow will be red to indicate bottleneck
     */
    private void drawLine(float x1, float y1, float x2, float y2, Color color, boolean isBottleneck) {
        Draw.color(color);
        Lines.stroke(lineThickness);
        Lines.line(x1, y1, x2, y2);

        // Draw arrowhead at start (pointing toward x2, y2)
        if (showArrows) {
            // Arrow is solid red when bottleneck, otherwise same as line
            Color arrowColor = (isBottleneck && showBottleneck)
                    ? tmpColor.set(Pal.remove).a(0.8f)
                    : color;
            drawArrowhead(x1, y1, x2, y2, arrowColor);
        }
    }

    /**
     * Draw striped line for multi-item bridges
     * Each item gets its own colored segment
     */
    private void drawStripedItemLine(float x1, float y1, float x2, float y2,
            ItemModule items, boolean canTransport, boolean isBottleneck) {
        // Collect all items (with duplicates for count)
        Seq<Item> itemList = new Seq<>();
        for (int i = 0; i < content.items().size; i++) {
            int count = items.get(i);
            if (count > 0) {
                Item item = content.item(i);
                // Add proportionally (but cap to avoid too many segments)
                int addCount = Math.min(count, 3); // Max 3 segments per item type
                for (int j = 0; j < addCount; j++) {
                    itemList.add(item);
                }
            }
        }

        if (itemList.isEmpty()) {
            Draw.color(Color.gray, 0.4f);
            Lines.stroke(lineThickness);
            Lines.line(x1, y1, x2, y2);
            return;
        }

        // Calculate segment parameters
        float totalLength = Mathf.dst(x1, y1, x2, y2);
        float segmentLength = totalLength / itemList.size;
        float angle = Angles.angle(x1, y1, x2, y2);

        // Alpha based on transport status
        float alpha = canTransport ? 0.8f : 0.35f;

        // Draw each segment
        float currentX = x1, currentY = y1;
        for (int i = 0; i < itemList.size; i++) {
            Item item = itemList.get(i);

            float nextX = currentX + Angles.trnsx(angle, segmentLength);
            float nextY = currentY + Angles.trnsy(angle, segmentLength);

            // Bottleneck: solid red for last segment
            if (isBottleneck && showBottleneck && i == itemList.size - 1) {
                Draw.color(Pal.remove, 0.8f);
            } else {
                Draw.color(item.color, alpha);
            }

            Lines.stroke(lineThickness);
            Lines.line(currentX, currentY, nextX, nextY);

            currentX = nextX;
            currentY = nextY;
        }

        // Draw arrowhead at start
        if (showArrows) {
            Item firstItem = itemList.first();
            tmpColor.set(firstItem.color).a(alpha);
            drawArrowhead(x1, y1, x2, y2, tmpColor);
        }
    }

    /**
     * Draw arrowhead at start of line (right after source block)
     * Format: A▸─────────────B
     */
    private void drawArrowhead(float x1, float y1, float x2, float y2, Color color) {
        float angle = Angles.angle(x1, y1, x2, y2);

        // Position: slightly offset from start point toward target
        float offset = arrowSize * 1.5f;
        float tipX = x1 + Angles.trnsx(angle, offset);
        float tipY = y1 + Angles.trnsy(angle, offset);

        Draw.color(color);

        // Draw small triangle pointing in flow direction
        Fill.tri(
                tipX + Angles.trnsx(angle, arrowSize), // Tip (pointing forward)
                tipY + Angles.trnsy(angle, arrowSize),
                tipX + Angles.trnsx(angle + 135f, arrowSize * 0.7f), // Left corner
                tipY + Angles.trnsy(angle + 135f, arrowSize * 0.7f),
                tipX + Angles.trnsx(angle - 135f, arrowSize * 0.7f), // Right corner
                tipY + Angles.trnsy(angle - 135f, arrowSize * 0.7f));
    }

    /**
     * Get flow color based on status
     * Line always shows item/liquid color - bottleneck is indicated by arrow
     * 
     * @param hasContent   Whether bridge has items/liquid
     * @param canTransport Whether items can be transported
     * @param baseColor    The item/liquid color
     */
    private Color getFlowColor(boolean hasContent, boolean canTransport, Color baseColor) {
        if (!hasContent) {
            // Gray - inactive
            return tmpColor.set(Color.gray).a(0.4f);
        } else if (!canTransport) {
            // Light color - blocked
            return tmpColor.set(baseColor).a(0.35f);
        } else {
            // Normal color - active
            return tmpColor.set(baseColor).a(0.8f);
        }
    }

    /**
     * Draw connections for Routers and Distributors
     */
    /**
     * Draw connections for Routers and Distributors
     */
    private void drawRouter(Router.RouterBuild rb) {
        // Input line: from lastInput to this router
        Building from = (rb.lastInput != null) ? rb.lastInput.build : null;
        if (from != null) {
            drawConnection(from.x, from.y, rb.x, rb.y, Pal.placing, false);
        }

        // Output line: from this router to current target
        // Mindustry router logic: target = proximity.get(rotation % proximity.size)
        if (rb.proximity.size > 0) {
            int index = (rb.rotation) % rb.proximity.size;
            // Handle negative modulo if any
            if (index < 0)
                index += rb.proximity.size;

            Building to = rb.proximity.get(index);
            if (to != null) {
                // Get the item currently in the router, or null if empty
                Item item = (rb.items != null && rb.items.total() > 0) ? getFirstItem(rb.items) : null;

                // STRICT CHECK: Can the target accept this item?
                // If item is null (router empty), we check if it accepts ANY item (null check).
                if (canBlockAcceptItem(rb, to, item)) {
                    drawConnection(rb.x, rb.y, to.x, to.y, Pal.remove, true);
                }
            }
        }
    }

    /**
     * Draw connections for Unloader
     * Visualizes the potential flow: Input from neighbors -> Unloader -> Output
     * (Front)
     */
    /**
     * Draw connections for Unloader
     * Visualizes the potential flow: Input from neighbors -> Unloader -> Output
     * (Front)
     */
    private void drawUnloader(Unloader.UnloaderBuild ub) {
        // Output: The block the unloader is facing
        Building to = ub.front();
        if (to != null) {
            // Unloader output validity check
            // Pass null item (generic check) because unloader can select different items
            if (canBlockAcceptItem(ub, to, null)) {
                drawConnection(ub.x, ub.y, to.x, to.y, Pal.remove, true);
            }
        }

        // Inputs: All other neighbors are potential sources
        for (Building other : ub.proximity) {
            if (other != to) {
                // Input validity check:
                // 1. Source must have items capability
                // 2. Source must NOT be a transport block pointing elsewhere (handled by
                // canBlockAcceptItem direction check)
                // 3. We check if Unloader can "Accept" from other? No, Unloader PULLS.
                // Reverse check: Can 'other' provide to 'ub'?
                // canBlockAcceptItem(other, ub, null) checks if 'ub' accepts from 'other'.
                // But unloader logic is unique. It pulls.
                // We just need to check if 'other' has items and is a valid storage/source.

                if (other.block.hasItems && other.team == ub.team) {
                    // Don't draw input from a conveyor that is facing away or sideways,
                    // unless it's a junction/router/bridge that exposes items?
                    // Actually, unloaders generally pull from STORAGE or FACTORY.
                    // Pulling from a conveyor is weird but possible if it's a specific type?
                    // Standard unloader pulls from ANY block with hasItems.

                    // Filter out walls/batteries etc (hasItems=false usually).
                    // Filter out blocks that don't actually store items (e.g. some logic blocks).

                    drawConnection(other.x, other.y, ub.x, ub.y, Pal.placing, false);
                }
            }
        }
    }

    /**
     * Helper to draw a simple connection line with optional arrow
     */
    private void drawConnection(float x1, float y1, float x2, float y2, Color color, boolean arrowAtEnd) {
        Draw.color(color, 0.6f); // Slightly transparent
        Lines.stroke(lineThickness);
        Lines.line(x1, y1, x2, y2);

        if (showArrows) {
            if (arrowAtEnd) {
                // Arrow at x2, y2
                drawArrowhead(x1, y1, x2, y2, color);
            } else {
                // Arrow mid-way or at end? Router input usually implies flow towards router.
                // Let's draw arrow at destination (router) for input line.
                drawArrowhead(x1, y1, x2, y2, color);
            }
        }
    }

    private Item getFirstItem(ItemModule items) {
        if (items == null)
            return null;
        for (int i = 0; i < content.items().size; i++) {
            if (items.get(i) > 0) {
                return content.item(i);
            }
        }
        return null;
    }

    private int countUniqueItems(ItemModule items) {
        if (items == null)
            return 0;
        int count = 0;
        for (int i = 0; i < content.items().size; i++) {
            if (items.get(i) > 0)
                count++;
        }
        return count;
    }

    /**
     * Check if a building can accept a specific item type.
     * Uses block type checks for transport/storage, and checks item requirements
     * for crafters.
     * Also checks if transport blocks are facing the right way (not outputting TO
     * the bridge).
     */
    /**
     * Check if a building can accept a specific item type.
     * Uses block type checks for transport/storage, and checks item requirements
     * for crafters.
     * Also checks if transport blocks are facing the right way (not outputting TO
     * the bridge).
     *
     * @param item The item to check, or null for a generic "can accept any item"
     *             check
     */
    private boolean canBlockAcceptItem(Building source, Building target, Item item) {
        if (source == null || target == null)
            return false;

        // 1. Basic Capability Check
        // If block statically says it doesn't accept items, we trust it.
        // This handles Drills, Unloaders (usually), Solar Panels, etc.
        if (!target.block.acceptsItems) {
            return false;
        }

        // 2. Team Check
        if (target.team != source.team) {
            return false;
        }

        var block = target.block;

        // 3. Transport Direction Check
        // If target is a transport block, it must not be pointing AT the source
        // (This prevents visualizing flow "backwards" into a conveyor)
        if (block instanceof mindustry.world.blocks.distribution.Conveyor ||
                block instanceof mindustry.world.blocks.distribution.StackConveyor ||
                block instanceof mindustry.world.blocks.distribution.Duct ||
                block instanceof mindustry.world.blocks.distribution.Junction ||
                block instanceof mindustry.world.blocks.distribution.Router ||
                block instanceof mindustry.world.blocks.distribution.OverflowGate ||
                block instanceof mindustry.world.blocks.distribution.ItemBridge ||
                block instanceof mindustry.world.blocks.distribution.DuctBridge) {

            // If target is facing SOURCE, it is outputting to source, not accepting from it
            if (target.front() == source) {
                return false;
            }
        }

        // 4. Item Specific Checks
        // If we know the component item, we can check filters.
        if (item != null) {
            // Factories/Crafters with strict item filters
            if (block.itemFilter != null) {
                // Return false if item ID is outside filter bounds or filter is false
                if (item.id >= block.itemFilter.length || !block.itemFilter[item.id]) {
                    return false;
                }
            }
        }

        // We intentionally DO NOT check target.acceptItem(source, item)
        // because that often returns false if the block is full, causing flickering.
        // We only want to visualize the *connection*, not the momentary accept state.

        return true;
    }

    // =============================================
    // SETTINGS DIALOG
    // =============================================

    public void showSettings() {
        if (dialog == null) {
            dialog = new BaseDialog(Core.bundle.get("distribution.settings.title", "Distribution Reveal Settings"));
            dialog.addCloseButton();

            dialog.buttons.button("@settings.reset", mindustry.gen.Icon.refresh, () -> {
                resetToDefaults();
                rebuildDialog();
            }).size(210, 64);

            dialog.shown(this::rebuildDialog);
        }
        dialog.show();
    }

    private void resetToDefaults() {
        showItemBridges = true;
        showLiquidBridges = true;
        showArrows = true;
        showBottleneck = true;
        lineThickness = 1f;
        zoomThreshold = 0.3f;
        arrowSize = 2.5f;
        saveSettings();
    }

    private void rebuildDialog() {
        Table cont = dialog.cont;
        cont.clear();
        cont.defaults().pad(6).left();

        float width = Math.min(Core.graphics.getWidth() / 1.2f, 460f);

        // Title
        cont.add(Core.bundle.get("distribution.settings.title", "Distribution Reveal Settings"))
                .color(Color.white).padBottom(10).row();

        // === Toggles ===
        cont.check(Core.bundle.get("distribution.showItemBridges", "Show Item Bridges"), showItemBridges, val -> {
            showItemBridges = val;
            saveSettings();
        }).left().padTop(4).row();

        cont.check(Core.bundle.get("distribution.showLiquidBridges", "Show Liquid Bridges"), showLiquidBridges, val -> {
            showLiquidBridges = val;
            saveSettings();
        }).left().padTop(4).row();

        cont.check(Core.bundle.get("distribution.showRouters", "Show Routers & Distributors"), showRouters, val -> {
            showRouters = val;
            saveSettings();
        }).left().padTop(4).row();

        cont.check(Core.bundle.get("distribution.showUnloaders", "Show Unloaders"), showUnloaders, val -> {
            showUnloaders = val;
            saveSettings();
        }).left().padTop(4).row();

        cont.check(Core.bundle.get("distribution.showArrows", "Show Direction Arrows"), showArrows, val -> {
            showArrows = val;
            saveSettings();
        }).left().padTop(4).row();

        cont.check(Core.bundle.get("distribution.showBottleneck", "Highlight Bottlenecks (Red)"), showBottleneck,
                val -> {
                    showBottleneck = val;
                    saveSettings();
                }).left().padTop(4).row();

        cont.add("").padTop(10).row(); // Spacer

        // === Line Thickness Slider ===
        Slider thicknessSlider = new Slider(0.5f, 5f, 0.5f, false);
        thicknessSlider.setValue(lineThickness);

        Label thicknessValue = new Label(String.format("%.1f", lineThickness), Styles.outlineLabel);

        Table thicknessContent = new Table();
        thicknessContent.touchable = arc.scene.event.Touchable.disabled;
        thicknessContent.margin(3f, 33f, 3f, 33f);
        thicknessContent.add(Core.bundle.get("distribution.lineThickness", "Line Thickness"), Styles.outlineLabel)
                .left().growX();
        thicknessContent.add(thicknessValue).padLeft(10f).right();

        thicknessSlider.changed(() -> {
            lineThickness = thicknessSlider.getValue();
            thicknessValue.setText(String.format("%.1f", lineThickness));
            saveSettings();
        });

        cont.stack(thicknessSlider, thicknessContent).width(width).left().padTop(4).row();

        // === Arrow Size Slider ===
        Slider arrowSlider = new Slider(1f, 5f, 0.5f, false);
        arrowSlider.setValue(arrowSize);

        Label arrowValue = new Label(String.format("%.1f", arrowSize), Styles.outlineLabel);

        Table arrowContent = new Table();
        arrowContent.touchable = arc.scene.event.Touchable.disabled;
        arrowContent.margin(3f, 33f, 3f, 33f);
        arrowContent.add(Core.bundle.get("distribution.arrowSize", "Arrow Size"), Styles.outlineLabel).left().growX();
        arrowContent.add(arrowValue).padLeft(10f).right();

        arrowSlider.changed(() -> {
            arrowSize = arrowSlider.getValue();
            arrowValue.setText(String.format("%.1f", arrowSize));
            saveSettings();
        });

        cont.stack(arrowSlider, arrowContent).width(width).left().padTop(4).row();

        // === Zoom Threshold Slider ===
        Slider zoomSlider = new Slider(0f, 1f, 0.1f, false);
        zoomSlider.setValue(zoomThreshold);

        Label zoomValue = new Label(
                zoomThreshold <= 0.01f ? "Off" : String.format("%.1fx", zoomThreshold),
                Styles.outlineLabel);

        Table zoomContent = new Table();
        zoomContent.touchable = arc.scene.event.Touchable.disabled;
        zoomContent.margin(3f, 33f, 3f, 33f);
        zoomContent.add(Core.bundle.get("distribution.zoomThreshold", "Min Zoom"), Styles.outlineLabel).left().growX();
        zoomContent.add(zoomValue).padLeft(10f).right();

        zoomSlider.changed(() -> {
            zoomThreshold = zoomSlider.getValue();
            zoomValue.setText(zoomThreshold <= 0.01f ? "Off" : String.format("%.1fx", zoomThreshold));
            saveSettings();
        });

        cont.stack(zoomSlider, zoomContent).width(width).left().padTop(4).row();
    }
}
