package mindustrytool.features.smartdrill;

import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.graphics.g2d.Draw;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import arc.util.Eachable;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mindustry.Vars;
import mindustry.core.GameState;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType.StateChangeEvent;
import mindustry.game.EventType.TapEvent;
import mindustry.game.EventType.Trigger;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.graphics.Layer;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.production.BeamDrill;
import mindustry.world.blocks.production.Drill;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.smartdrill.plan.DefaultDrillPlanner;
import mindustrytool.features.smartdrill.plan.DrillPlanRequest;
import mindustrytool.features.smartdrill.plan.DrillPlanResult;
import mindustrytool.features.smartdrill.plan.DrillWorldView;
import mindustrytool.features.smartdrill.plan.MindustryDrillWorldView;
import mindustrytool.features.smartdrill.plan.OreFlood;
import mindustrytool.features.smartdrill.plan.OreKind;
import mindustrytool.features.smartdrill.plan.PlannedBuild;
import mindustrytool.features.smartdrill.plan.Rect;
import mindustrytool.features.smartdrill.plan.SchematicData;
import mindustrytool.features.smartdrill.ui.DrillAreaOverlay;
import mindustrytool.features.smartdrill.ui.SchematicBindDialog;
import mindustrytool.features.smartdrill.ui.SmartDrillPanel;
import mindustrytool.features.smartdrill.ui.SmartDrillSettingsDialog;
import mindustrytool.utils.JsonUtils;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.overlay.SolimDialog;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class SmartDrillFeature extends Feature {

    public static final int WARNING_DRILL_COUNT = 500;

    public static final class Target {
        public static final Target NONE = new Target(0, 0, "", OreKind.FLOOR);

        public final int x;
        public final int y;
        public final String ore;
        public final OreKind kind;

        public Target(int x, int y, String ore, OreKind kind) {
            this.x = x;
            this.y = y;
            this.ore = ore;
            this.kind = kind;
        }

        public boolean valid() {
            return !ore.isEmpty();
        }
    }

    public static final class DrillBinding {
        public String block = "";
        public String file = "";

        public DrillBinding() {
        }

        public DrillBinding(String block, String file) {
            this.block = block;
            this.file = file;
        }
    }

    private static final Eachable<BuildPlan> NO_PLANS = plans -> {
    };

    public final ConfigGroup config;
    public final ConfigValue<Boolean> autoDisableConfig;
    public final ConfigValue<String> bindingsConfig;

    private final Signal<Target> target = Signal.of(Target.NONE);
    private final Signal<Rect> rect = Signal.of(new Rect(0, 0, 1, 1));
    private final Signal<Integer> facing = Signal.of(0);
    private final Signal<String> selectedDrill = Signal.of("");
    private final Signal<String> selectedRig = Signal.of("");
    private final Signal<DrillPlanResult> plan = Signal.of(DrillPlanResult.EMPTY);
    private final Signal<Boolean> panelOpen = Signal.of(false);

    private final DefaultDrillPlanner planner = new DefaultDrillPlanner();
    private final Map<String, String> bindings = new LinkedHashMap<>();
    private final Seq<BuildPlan> ghosts = new Seq<>();

    private float viewX;
    private float viewY;
    private float viewWidth;
    private float viewHeight;

    private @Nullable SmartDrillSettingsDialog settingsDialog;
    private @Nullable SmartDrillPanel panel;
    private @Nullable DrillAreaOverlay overlay;
    private boolean suppressTap;

    public SmartDrillFeature() {
        super(FeatureMetadata.builder()
                .id("smart-drill")
                .icon(FileIcon.of("pickaxe.png"))
                .order(11)
                .quickAccess(true)
                .enabledByDefault(false)
                .development(false)
                .build());

        this.config = configGroup();
        this.autoDisableConfig = config.boolValue("autoDisable", true);
        this.bindingsConfig = config.stringValue("bindings", "[]");
        loadBindings();

        Events.run(Trigger.update, this::update);
        Events.run(Trigger.draw, this::draw);
        Events.on(TapEvent.class, this::handleTap);
        Events.on(StateChangeEvent.class, event -> {
            if (event.to == GameState.State.menu) {
                closePanel();
            }
        });
    }

    public void resetToDefaults() {
        autoDisableConfig.reset();
    }

    @Override
    public void onEnable() {
        if (Vars.ui != null && Vars.ui.hudfrag != null) {
            Vars.ui.hudfrag.showToast(Core.bundle.get("feature.smart-drill.active-hint"));
        }
    }

    @Override
    public void onDisable() {
        closePanel();
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new SmartDrillSettingsDialog(this);
            }
            return settingsDialog;
        };
    }

    private void update() {
        if (!isEnabled()) {
            return;
        }
        if (overlay != null) {
            overlay.update();
        }
    }

    private void draw() {
        if (!isEnabled() || overlay == null) {
            return;
        }
        drawGhosts();
        overlay.draw();
    }

    private void drawGhosts() {
        if (ghosts.size == 0) {
            return;
        }
        if (Core.camera != null) {
            viewX = Core.camera.position.x - Core.camera.width / 2f;
            viewY = Core.camera.position.y - Core.camera.height / 2f;
            viewWidth = Core.camera.width;
            viewHeight = Core.camera.height;
        }
        float z = Draw.z();
        Draw.z(Layer.overlayUI);
        for (int i = 0; i < ghosts.size; i++) {
            BuildPlan plan = ghosts.get(i);
            Block block = plan.block;
            if (block == null) {
                continue;
            }
            if (!visible(plan, block)) {
                continue;
            }
            block.drawPlan(plan, NO_PLANS, true, 0.55f);
        }
        Draw.reset();
        Draw.z(z);
    }

    private boolean visible(BuildPlan plan, Block block) {
        if (viewWidth <= 0f || viewHeight <= 0f) {
            return true;
        }
        float wx = plan.x * Vars.tilesize + block.offset;
        float wy = plan.y * Vars.tilesize + block.offset;
        float half = block.size * Vars.tilesize / 2f;
        return wx + half >= viewX && wx - half <= viewX + viewWidth
                && wy + half >= viewY && wy - half <= viewY + viewHeight;
    }

    private void handleTap(TapEvent event) {
        if (!isEnabled() || Vars.state == null || !Vars.state.isGame() || Vars.world == null
                || Vars.player == null) {
            return;
        }
        if (suppressTap || panelOpen.get()) {
            return;
        }
        Tile tile = event.tile;
        if (tile == null || tile.build != null) {
            return;
        }
        Item drop = tile.drop();
        Item wallDrop = tile.wallDrop();
        if (drop != null) {
            begin(tile, drop.name, OreKind.FLOOR);
        } else if (wallDrop != null) {
            begin(tile, wallDrop.name, OreKind.WALL);
        }
    }

    private void begin(Tile tile, String ore, OreKind kind) {
        DrillWorldView world = worldView();
        OreFlood.Result flood = kind.isFloor()
                ? OreFlood.flood(world, tile.x, tile.y, kind, ore, DrillPlanRequest.DEFAULT_MAX_TILES,
                        DrillPlanRequest.DEFAULT_MAX_RECT_SIZE)
                : OreFlood.floodDiagonal(world, tile.x, tile.y, kind, ore,
                        DrillPlanRequest.DEFAULT_MAX_TILES, DrillPlanRequest.DEFAULT_MAX_RECT_SIZE);

        Rect initial = DrillAreaOverlay.clampToWorld(flood.rect);
        target.set(new Target(tile.x, tile.y, ore, kind));
        rect.set(initial != null ? initial : new Rect(tile.x, tile.y, 1, 1));
        facing.set(defaultFacing(rect.get(), tile.x, tile.y));
        selectedDrill.set("");
        selectedRig.set("");
        openPanel();
        recompute();
    }

    private static int defaultFacing(Rect rect, int tileX, int tileY) {
        int dx = tileX - rect.centerX();
        int dy = tileY - rect.centerY();
        if (Math.abs(dx) >= Math.abs(dy)) {
            return dx >= 0 ? 0 : 2;
        }
        return dy >= 0 ? 1 : 3;
    }

    private DrillWorldView worldView() {
        return new MindustryDrillWorldView(Vars.player.team());
    }

    private void openPanel() {
        closePanel();
        overlay = new DrillAreaOverlay(this);
        panel = new SmartDrillPanel(this);
        Element element = panel.element();
        element.name = "smart-drill-panel";
        panelOpen.set(true);
        Core.app.post(() -> {
            SmartDrillPanel current = panel;
            if (current != null && Vars.ui != null && Vars.ui.hudGroup != null) {
                Vars.ui.hudGroup.addChild(current.element());
            }
        });
    }

    public void closePanel() {
        SmartDrillPanel previous = panel;
        panel = null;
        DrillAreaOverlay previousOverlay = overlay;
        overlay = null;
        panelOpen.set(false);
        if (previousOverlay != null) {
            previousOverlay.dispose();
        }
        if (previous != null) {
            Element element = previous.element();
            Core.app.post(() -> {
                element.remove();
                previous.dispose();
            });
        }
        ghosts.clear();
        plan.set(DrillPlanResult.EMPTY);
        target.set(Target.NONE);
        selectedDrill.set("");
        selectedRig.set("");
    }

    private void recompute() {
        DrillPlanRequest request = buildRequest();
        DrillPlanResult result = request == null ? DrillPlanResult.EMPTY : planner.plan(request, worldView());
        plan.set(result);
        rebuildGhosts(result);
    }

    private void rebuildGhosts(DrillPlanResult result) {
        ghosts.clear();
        for (int i = 0; i < result.builds.size; i++) {
            PlannedBuild planned = result.builds.get(i);
            Block block = Vars.content != null ? Vars.content.block(planned.block) : null;
            if (block == null) {
                continue;
            }
            BuildPlan buildPlan = new BuildPlan(planned.x, planned.y, planned.rotation, block, planned.config);
            buildPlan.animScale = 1f;
            ghosts.add(buildPlan);
        }
    }

    private @Nullable DrillPlanRequest buildRequest() {
        Target current = target.get();
        String drill = selectedDrill.get();
        if (!current.valid() || drill.isEmpty()) {
            return null;
        }
        DrillPlanRequest.Builder builder = DrillPlanRequest.builder()
                .target(current.ore, current.kind)
                .rect(rect.get())
                .facing(facing.get())
                .drill(drill);
        Schematic schematic = selectedSchematic();
        if (schematic != null) {
            builder.schematic(toSchematicData(schematic));
        }
        return builder.build();
    }

    private static SchematicData toSchematicData(Schematic schematic) {
        Seq<SchematicData.Tile> tiles = new Seq<>();
        Seq<Schematic.Stile> source = schematic.tiles;
        for (int i = 0; i < source.size; i++) {
            Schematic.Stile stile = source.get(i);
            tiles.add(new SchematicData.Tile(stile.block.name, stile.x, stile.y, stile.rotation, stile.config));
        }
        return new SchematicData(schematic.name(), schematic.width, schematic.height, tiles);
    }

    private @Nullable Schematic selectedSchematic() {
        String name = selectedRig.get();
        if (name.isEmpty() || Vars.schematics == null) {
            return null;
        }
        Seq<Schematic> all = Vars.schematics.all();
        for (int i = 0; i < all.size; i++) {
            Schematic schematic = all.get(i);
            if (schematic != null && name.equals(schematic.name())) {
                return schematic;
            }
        }
        return null;
    }

    public void place() {
        DrillPlanResult result = plan.get();
        if (result.builds.isEmpty() || Vars.player == null || Vars.player.unit() == null) {
            closePanel();
            return;
        }
        if (Vars.control != null && Vars.control.input != null) {
            Vars.control.input.isBuilding = false;
        }
        int placed = 0;
        for (int i = 0; i < result.builds.size; i++) {
            PlannedBuild planned = result.builds.get(i);
            Block block = Vars.content != null ? Vars.content.block(planned.block) : null;
            if (block == null) {
                continue;
            }
            BuildPlan buildPlan = new BuildPlan(planned.x, planned.y, planned.rotation, block, planned.config);
            if (buildPlan.placeable(Vars.player.team())) {
                Vars.player.unit().addBuild(buildPlan);
                placed++;
            }
        }
        closePanel();
        if (placed > 0 && Vars.ui != null && Vars.ui.hudfrag != null) {
            Vars.ui.hudfrag.showToast(Core.bundle.format("feature.smart-drill.placed", placed));
        }
        if (autoDisableConfig.get()) {
            disable();
        }
    }

    public void cancel() {
        closePanel();
    }

    public void setFacing(int value) {
        facing.set(Math.floorMod(value, 4));
        recompute();
    }

    public int getFacing() {
        Integer value = facing.get();
        return value != null ? value : 0;
    }

    public void setRectFromDrag(Rect value) {
        rect.set(value);
    }

    public void onRectDragFinished() {
        recompute();
    }

    public void resetRect() {
        Target current = target.get();
        if (!current.valid()) {
            return;
        }
        Tile tile = Vars.world != null ? Vars.world.tile(current.x, current.y) : null;
        if (tile == null) {
            return;
        }
        DrillWorldView world = worldView();
        OreFlood.Result flood = current.kind.isFloor()
                ? OreFlood.flood(world, tile.x, tile.y, current.kind, current.ore,
                        DrillPlanRequest.DEFAULT_MAX_TILES, DrillPlanRequest.DEFAULT_MAX_RECT_SIZE)
                : OreFlood.floodDiagonal(world, tile.x, tile.y, current.kind, current.ore,
                        DrillPlanRequest.DEFAULT_MAX_TILES, DrillPlanRequest.DEFAULT_MAX_RECT_SIZE);
        Rect clamped = DrillAreaOverlay.clampToWorld(flood.rect);
        rect.set(clamped != null ? clamped : rect.get());
        recompute();
    }

    public void selectDrill(String name) {
        if (name.equals(selectedDrill.get())) {
            selectedRig.set("");
        } else {
            selectedDrill.set(name);
            selectedRig.set("");
        }
        recompute();
    }

    public void selectRig(String name) {
        selectedRig.set(name.equals(selectedRig.get()) ? "" : name);
        recompute();
    }

    public void openBindDialog() {
        String drill = selectedDrill.get();
        if (drill.isEmpty()) {
            if (Vars.ui != null) {
                Vars.ui.showInfo(Core.bundle.get("feature.smart-drill.panel.rig.pick-drill"));
            }
            return;
        }
        SchematicBindDialog dialog = new SchematicBindDialog(this);
        Core.app.post(dialog::show);
    }

    public void bind(String schematicFile) {
        String drill = selectedDrill.get();
        if (drill.isEmpty()) {
            return;
        }
        bindings.put(drill, schematicFile);
        bindingsConfig.set(JsonUtils.toJson(toBindingList()));
        recompute();
    }

    public void clearBinding() {
        String drill = selectedDrill.get();
        if (drill.isEmpty()) {
            return;
        }
        bindings.remove(drill);
        bindingsConfig.set(JsonUtils.toJson(toBindingList()));
        recompute();
    }

    public String getBinding() {
        String drill = selectedDrill.get();
        String file = drill.isEmpty() ? null : bindings.get(drill);
        return file != null ? file : "";
    }

    private List<DrillBinding> toBindingList() {
        List<DrillBinding> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : bindings.entrySet()) {
            list.add(new DrillBinding(entry.getKey(), entry.getValue()));
        }
        return list;
    }

    private void loadBindings() {
        bindings.clear();
        List<DrillBinding> parsed = null;
        try {
            parsed = JsonUtils.fromJsonArray(DrillBinding.class, bindingsConfig.get());
        } catch (RuntimeException ignored) {
            parsed = null;
        }
        if (parsed == null) {
            return;
        }
        for (int i = 0; i < parsed.size(); i++) {
            DrillBinding binding = parsed.get(i);
            if (binding != null && binding.block != null && !binding.block.isEmpty()
                    && binding.file != null && !binding.file.isEmpty()) {
                bindings.put(binding.block, binding.file);
            }
        }
    }

    public Readable<List<Block>> drills() {
        return Signal.computed(this::computeDrills);
    }

    private List<Block> computeDrills() {
        List<Block> result = new ArrayList<>();
        Target current = target.get();
        if (!current.valid() || Vars.content == null) {
            return result;
        }
        Item ore = Vars.content.item(current.ore);
        if (ore == null) {
            return result;
        }
        Seq<Block> blocks = Vars.content.blocks();
        for (int i = 0; i < blocks.size; i++) {
            Block block = blocks.get(i);
            if (isDrillFor(block, current.kind, ore)) {
                result.add(block);
            }
        }
        return result;
    }

    private boolean isDrillFor(Block block, OreKind kind, Item ore) {
        if (block == null || !isUnlocked(block)) {
            return false;
        }
        if (kind.isFloor()) {
            return block instanceof Drill && ((Drill) block).tier >= ore.hardness;
        }
        return block instanceof BeamDrill && ((BeamDrill) block).tier >= ore.hardness;
    }

    public boolean isUnlocked(Block block) {
        if (block == null) {
            return false;
        }
        if (Vars.state == null || Vars.state.rules == null) {
            return true;
        }
        return block.unlockedNowHost()
                && block.placeablePlayer
                && block.environmentBuildable()
                && block.supportsEnv(Vars.state.rules.env);
    }

    public Readable<List<Schematic>> rigs() {
        return Signal.computed(this::computeRigs);
    }

    private List<Schematic> computeRigs() {
        List<Schematic> result = new ArrayList<>();
        String drill = selectedDrill.get();
        String file = drill.isEmpty() ? null : bindings.get(drill);
        if (file == null || Vars.schematics == null) {
            return result;
        }
        Seq<Schematic> all = Vars.schematics.all();
        for (int i = 0; i < all.size; i++) {
            Schematic schematic = all.get(i);
            if (schematic != null && schematic.file != null && file.equals(schematic.file.name())) {
                result.add(schematic);
            }
        }
        return result;
    }

    public Readable<Boolean> hasRigs() {
        return rigs().map(list -> list != null && !list.isEmpty());
    }

    public Readable<String> readout() {
        return plan.map(result -> Core.bundle.format("feature.smart-drill.panel.readout",
                result.drillCount, result.coveredOre, result.bridgeCount, result.placedBlocks));
    }

    public Readable<String> warning() {
        return plan.map(result -> result.drillCount > WARNING_DRILL_COUNT
                ? Core.bundle.format("feature.smart-drill.panel.warning", result.drillCount)
                : "");
    }

    public Readable<Boolean> hasPlan() {
        return plan.map(result -> result != null && !result.builds.isEmpty());
    }

    public Readable<String> rectLabel() {
        return rect.map(value -> Core.bundle.format("feature.smart-drill.panel.area.size",
                value.width, value.height));
    }

    public Readable<String> targetName() {
        return target.map(value -> value.valid()
                ? Core.bundle.format("feature.smart-drill.panel.target", oreName(value.ore))
                : Core.bundle.get("feature.smart-drill.panel.no-target"));
    }

    public Readable<String> targetBadge() {
        return target.map(value -> value.valid()
                ? Core.bundle.get(value.kind.isFloor()
                        ? "feature.smart-drill.panel.target.floor"
                        : "feature.smart-drill.panel.target.wall")
                : "");
    }

    public Drawable targetIcon() {
        Target current = target.get();
        Item ore = Vars.content != null && current.valid() ? Vars.content.item(current.ore) : null;
        return ore != null && ore.fullIcon != null ? new TextureRegionDrawable(ore.fullIcon)
                : Icon.wrench;
    }

    private static String oreName(String ore) {
        Item item = Vars.content != null ? Vars.content.item(ore) : null;
        return item != null ? item.localizedName : ore;
    }

    public String getSelectedDrillName() {
        String drill = selectedDrill.get();
        Block block = Vars.content != null && !drill.isEmpty() ? Vars.content.block(drill) : null;
        return block != null ? block.localizedName : "";
    }

    public Rect getRect() {
        return rect.get();
    }

    public DrillPlanResult getPlan() {
        DrillPlanResult value = plan.get();
        return value != null ? value : DrillPlanResult.EMPTY;
    }

    public boolean isPanelOpen() {
        return Boolean.TRUE.equals(panelOpen.get());
    }

    public void setSuppressTap(boolean value) {
        this.suppressTap = value;
    }
}
