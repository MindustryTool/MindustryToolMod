package mindustrytool.features.gameplay.autoconveyor;

import arc.Events;
import arc.graphics.g2d.Draw;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType.TapEvent;
import mindustry.game.EventType.Trigger;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.Conveyor;
import mindustry.world.blocks.distribution.Duct;
import mindustrytool.features.gameplay.autoconveyor.ConveyorPathfinder.PathNode;

public class AutoConveyorInput {

    private boolean enabled = false;
    private Tile startTile;
    private Seq<BuildPlan> previewPlans = new Seq<>();

    private final ConveyorPathfinder pathfinder = new ConveyorPathfinder();
    private AutoConveyorOverlay overlay;

    public AutoConveyorInput() {
        this.enabled = true;
        Events.on(TapEvent.class, this::onTap);
        Events.run(Trigger.draw, this::draw);

        overlay = new AutoConveyorOverlay(this::confirmBuild, this::cancelBuild);
    }

    public void dispose() {
        this.enabled = false;
        if (overlay != null)
            overlay.remove();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            cancelBuild();
        }
    }

    private void onTap(TapEvent e) {
        if (!enabled || e.player != Vars.player || e.tile == null)
            return;

        // Prevent tapping through UI
        if (overlay.visible && overlay.hasMouse())
            return;

        if (startTile == null) {
            startTile = e.tile;
            overlay.showStart(startTile, getHeldBlock());
        } else {
            // End point selected -> Generate Preview
            Tile endTile = e.tile;
            generatePreview(endTile);
        }
    }

    private Block getHeldBlock() {
        Block b = Vars.control.input.block;
        if (b == null)
            return Blocks.conveyor;
        if (b instanceof Conveyor || b instanceof Duct)
            return b;
        return Blocks.conveyor;
    }

    private void generatePreview(Tile endTile) {
        Block mainBlock = getHeldBlock();
        Seq<PathNode> path = pathfinder.findPath(startTile, endTile, mainBlock);

        if (path.isEmpty()) {
            Vars.ui.showInfoToast("No Path Found!", 2f);
            return;
        }

        previewPlans.clear();
        for (PathNode node : path) {
            if (node.blockType == null)
                continue;
            previewPlans.add(new BuildPlan(node.tile.x, node.tile.y, node.rotation, node.blockType));
        }

        overlay.showPreview(mainBlock);
    }

    private void confirmBuild() {
        if (Vars.player.unit() == null)
            return;

        // Submit Plans
        for (BuildPlan plan : previewPlans) {
            Tile t = Vars.world.tile(plan.x, plan.y);
            if (t.build != null && t.build.team == Vars.player.team() && t.block() != plan.block) {
                // Destructive cleanup
                if (AutoConveyorSettings.isDestructive() && t.block().size <= 2) {
                    Vars.player.unit().addBuild(new BuildPlan(t.x, t.y)); // Break
                }
            }
            Vars.player.unit().addBuild(plan);
        }

        Vars.ui.showInfoToast("Building...", 1f);
        cancelBuild(); // Reset state
    }

    public void cancelBuild() {
        startTile = null;
        previewPlans.clear();
        if (overlay != null)
            overlay.hide();
    }

    private void draw() {
        if (!enabled || previewPlans.isEmpty())
            return;

        Draw.reset();
        for (BuildPlan plan : previewPlans) {
            Draw.alpha(0.5f);
            plan.block.drawPlanRegion(plan, previewPlans);
        }
        Draw.reset();
    }
}
