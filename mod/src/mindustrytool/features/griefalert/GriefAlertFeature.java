package mindustrytool.features.griefalert;

import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.input.KeyCode;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Rect;
import arc.scene.Element;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Time;
import arc.util.Tmp;
import java.util.BitSet;
import mindustry.Vars;
import mindustry.game.EventType.BlockBuildBeginEvent;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEndEvent;
import mindustry.gen.Player;
import mindustry.graphics.Layer;
import mindustry.input.DesktopInput;
import mindustry.input.InputHandler;
import mindustry.world.Block;
import mindustry.world.blocks.ConstructBlock.ConstructBuild;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import mindustry.world.meta.BlockFlag;
import mindustrytool.components.FileIcon;
import mindustrytool.components.WebStyles;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.input.ModMobileInput;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.overlay.SolimDialog;
import solim.reactive.Effect;
import solim.reactive.Signal;

public class GriefAlertFeature extends Feature {
    public static final float DEFAULT_RADIUS = 10f;
    public static final float DEFAULT_DURATION = 15f;
    private static final long COOLDOWN_MILLIS = 3000L;
    private static final long HIGHLIGHT_DURATION_MILLIS = 3000L;
    private static final float HIGHLIGHT_MAX_TILES = 3f;
    private static final float HIGHLIGHT_PULSE_CYCLES = 3f;

    public final ConfigGroup config;
    public final ConfigValue<Float> radiusConfig;
    public final ConfigValue<Float> durationConfig;

    private final Signal<GriefAlertInfo> activeAlert = Signal.of(null);
    private final Color highlightColor = new Color();

    private @Nullable GriefAlertSettingsDialog settingsDialog;
    private @Nullable GriefAlertHudView hudView;
    private long lastAlertMillis = 0L;
    private long alertExpireMillis = 0L;
    private float highlightWorldX = 0f;
    private float highlightWorldY = 0f;
    private long highlightExpireMillis = 0L;

    private @Nullable BitSet blockEnabled;
    private final ObjectMap<String, Signal<Boolean>> blockSignals = new ObjectMap<>();

    public GriefAlertFeature() {
        super(FeatureMetadata.builder()
                .id("grief-alert")
                .icon(FileIcon.of("triangle-alert.png"))
                .order(23)
                .enabledByDefault(true)
                .quickAccess(false)
                .build());

        config = configGroup();
        radiusConfig = config.floatValue("radius", DEFAULT_RADIUS);
        durationConfig = config.floatValue("duration", DEFAULT_DURATION);

        bindAction("griefalertjump", KeyCode.unset, () -> {
            GriefAlertInfo info = activeAlert();
            if (info != null) {
                jumpToAlert(info);
            }
        }, true);
        bindDialog("griefalertsettings", KeyCode.unset, getSettingDialog(), false);

        Events.on(BlockBuildBeginEvent.class, this::onBuildBegin);
        Events.on(WorldLoadEndEvent.class, event -> clearTransientState());
        Events.run(Trigger.update, () -> tick(Time.millis()));
        Events.run(Trigger.draw, this::drawHighlight);
    }

    public static String blockSettingKey(Block block) {
        return "mindustrytool.features.grief-alert.block." + block.name;
    }

    public Seq<Block> explosiveBlocks() {
        Seq<Block> result = new Seq<>();
        if (Vars.content == null || Vars.content.blocks() == null) {
            return result;
        }
        for (Block block : Vars.content.blocks()) {
            if (block != null && isExplosiveReactor(block) && block.isPlaceable()) {
                result.add(block);
            }
        }
        return result;
    }

    public Signal<Boolean> getBlockSignal(Block block) {
        Signal<Boolean> signal = blockSignals.get(block.name);
        if (signal == null) {
            boolean initial = Core.settings.getBool(blockSettingKey(block), true);
            signal = Signal.of(initial);
            Effect.of(signal, enabled -> {
                Core.settings.put(blockSettingKey(block), enabled);
                if (blockEnabled == null) {
                    rebuildBlockLookup();
                } else if (block.id >= 0) {
                    blockEnabled.set(block.id, enabled);
                }
            });
            blockSignals.put(block.name, signal);
        }
        return signal;
    }

    public void rebuildBlockLookup() {
        if (Vars.content == null || Vars.content.blocks() == null) {
            return;
        }
        int max = Vars.content.blocks().size;
        BitSet lookup = new BitSet(Math.max(max, 256));
        for (Block block : Vars.content.blocks()) {
            if (block != null && isExplosiveReactor(block)) {
                lookup.set(block.id, Core.settings.getBool(blockSettingKey(block), true));
            }
        }
        blockEnabled = lookup;
    }

    public boolean isBlockEnabled(Block block) {
        if (block == null) {
            return false;
        }
        if (blockEnabled == null) {
            rebuildBlockLookup();
        }
        return blockEnabled != null && block.id >= 0 && block.id < blockEnabled.size()
                ? blockEnabled.get(block.id)
                : Core.settings.getBool(blockSettingKey(block), true);
    }

    public void setBlockEnabled(Block block, boolean enabled) {
        if (block == null) {
            return;
        }
        getBlockSignal(block).set(enabled);
    }

    public void setAllBlocksEnabled(boolean enabled) {
        for (Block block : explosiveBlocks()) {
            getBlockSignal(block).set(enabled);
        }
    }

    public Signal<GriefAlertInfo> activeAlertSignal() {
        return activeAlert;
    }

    public @Nullable GriefAlertInfo activeAlert() {
        return activeAlert.peek();
    }

    public boolean hasActiveAlert() {
        return activeAlert.peek() != null;
    }

    public void raiseAlert(@Nullable GriefAlertInfo alert) {
        if (alert == null) {
            return;
        }

        activeAlert.set(alert);
        Float durationSec = durationConfig.get();
        long durationMillis = (long) ((durationSec != null ? durationSec : DEFAULT_DURATION) * 1000f);
        alertExpireMillis = Time.millis() + durationMillis;
    }

    public void dismissAlert() {
        activeAlert.set(null);
        alertExpireMillis = 0L;
    }

    public void jumpToAlert(@Nullable GriefAlertInfo alert) {
        if (alert == null || Core.camera == null) {
            return;
        }

        Core.camera.position.set(alert.worldX, alert.worldY);
        detachCameraFromPlayer();
        startHighlight(alert);
        dismissAlert();
    }

    public boolean hasActiveHighlight() {
        return Time.millis() < highlightExpireMillis;
    }

    @Override
    public void onEnable() {
        mountHud();
    }

    @Override
    public void onDisable() {
        unmountHud();
        clearTransientState();
    }

    void tick(long now) {
        if (isEnabled() && hasActiveAlert() && now >= alertExpireMillis) {
            dismissAlert();
        }
    }

    private void mountHud() {
        if (hudView != null) {
            return;
        }

        GriefAlertHudView view = new GriefAlertHudView(this);
        hudView = view;
        Element element = view.element();
        element.name = "grief-alert-hud";

        Core.app.post(() -> {
            if (hudView == view && Vars.ui != null && Vars.ui.hudGroup != null) {
                Vars.ui.hudGroup.addChild(element);
            }
        });
    }

    private void unmountHud() {
        GriefAlertHudView view = hudView;
        hudView = null;
        if (view == null) {
            return;
        }

        Core.app.post(() -> {
            view.element().remove();
            view.dispose();
        });
    }

    private void clearTransientState() {
        dismissAlert();
        highlightExpireMillis = 0L;
    }

    private void detachCameraFromPlayer() {
        InputHandler input = Vars.control != null ? Vars.control.input : null;
        if (input == null) {
            return;
        }

        input.spectating = null;
        if (input instanceof DesktopInput) {
            ((DesktopInput) input).panning = true;
        }
        if (input instanceof ModMobileInput) {
            ((ModMobileInput) input).cancelPanDelay();
        }
    }

    private void startHighlight(GriefAlertInfo alert) {
        highlightWorldX = alert.worldX;
        highlightWorldY = alert.worldY;
        highlightExpireMillis = Time.millis() + HIGHLIGHT_DURATION_MILLIS;
    }

    private void onBuildBegin(BlockBuildBeginEvent event) {
        if (!isEnabled()) {
            return;
        }

        if (event.breaking) {
            return;
        }

        if (event.tile == null || !(event.tile.build instanceof ConstructBuild)) {
            return;
        }

        ConstructBuild construct = (ConstructBuild) event.tile.build;
        Block block = construct.current;
        if (block == null) {
            return;
        }
        if (!isExplosiveReactor(block)) {
            return;
        }
        if (!isBlockEnabled(block)) {
            return;
        }

        Player builder = event.unit != null ? event.unit.getPlayer() : null;

        String builderName = builder != null ? builder.name : event.team.coloredName();

        if (Vars.state == null || Vars.state.teams == null) {
            return;
        }

        Seq<CoreBuild> cores = Vars.state.teams.cores(event.team);
        if (cores == null || cores.isEmpty()) {
            return;
        }

        float worldX = event.tile.worldx();
        float worldY = event.tile.worldy();
        float nearest = Float.MAX_VALUE;
        CoreBuild nearestCore = null;

        for (int i = 0; i < cores.size; i++) {
            CoreBuild core = cores.get(i);
            if (core == null) {
                continue;
            }
            float distance = Mathf.dst(worldX, worldY, core.x, core.y) / Vars.tilesize;
            if (distance < nearest) {
                nearest = distance;
                nearestCore = core;
            }
        }

        if (nearestCore == null) {
            return;
        }

        Float configured = radiusConfig.get();
        float radius = configured != null ? configured : DEFAULT_RADIUS;
        if (nearest > radius) {
            return;
        }

        long now = Time.millis();
        if (now - lastAlertMillis < COOLDOWN_MILLIS) {
            return;
        }

        lastAlertMillis = now;
        raiseAlert(new GriefAlertInfo(
                builderName,
                block.localizedName,
                event.tile.x,
                event.tile.y,
                worldX,
                worldY,
                nearestCore.tileX(),
                nearestCore.tileY()));
    }

    private void drawHighlight() {
        if (!isEnabled() || Vars.state == null || !Vars.state.isGame() || Core.camera == null) {
            return;
        }
        if (Vars.ui == null || Vars.ui.hudfrag == null || !Vars.ui.hudfrag.shown) {
            return;
        }

        long remaining = highlightExpireMillis - Time.millis();
        if (remaining <= 0L) {
            return;
        }

        float maxRadius = Vars.tilesize * HIGHLIGHT_MAX_TILES;
        Rect bounds = Core.camera.bounds(Tmp.r1);
        if (highlightWorldX + maxRadius < bounds.x || highlightWorldX - maxRadius > bounds.x + bounds.width
                || highlightWorldY + maxRadius < bounds.y || highlightWorldY - maxRadius > bounds.y + bounds.height) {
            return;
        }

        float progress = Mathf.clamp(1f - remaining / (float) HIGHLIGHT_DURATION_MILLIS);
        float fade = 1f - progress;
        float pulse = Mathf.sin(progress * Mathf.PI * HIGHLIGHT_PULSE_CYCLES);

        highlightColor.set(WebStyles.Colors.DANGER);
        highlightColor.a = fade;

        float z = Draw.z();
        Draw.z(Layer.overlayUI);
        Lines.stroke(2f * fade, highlightColor);
        Lines.circle(highlightWorldX, highlightWorldY, maxRadius * (0.5f + 0.5f * pulse));
        Draw.z(z);
        Draw.reset();
    }

    private boolean isExplosiveReactor(Block block) {
        return block.flags != null && block.flags.contains(BlockFlag.reactor)
                || (block instanceof PowerGenerator && ((PowerGenerator) block).explosionRadius > 0);
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new GriefAlertSettingsDialog(this);
            }
            return settingsDialog;
        };
    }
}
