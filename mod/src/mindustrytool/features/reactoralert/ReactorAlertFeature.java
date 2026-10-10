package mindustrytool.features.reactoralert;

import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Time;
import java.util.BitSet;
import mindustry.Vars;
import mindustry.game.EventType.BlockBuildBeginEvent;
import mindustry.gen.Player;
import mindustry.world.Block;
import mindustry.world.blocks.ConstructBlock.ConstructBuild;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import mindustry.world.meta.BlockFlag;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.overlay.SolimDialog;
import solim.reactive.Signal;

public class ReactorAlertFeature extends Feature {
    private static final float DEFAULT_RADIUS = 10f;
    private static final long COOLDOWN_MILLIS = 3000L;

    public final ConfigGroup config;
    public final ConfigValue<Float> radiusConfig;

    private @Nullable ReactorAlertSettingsDialog settingsDialog;
    private long lastAlertMillis = 0L;

    private @Nullable BitSet blockEnabled;
    private final ObjectMap<String, Signal<Boolean>> blockSignals = new ObjectMap<>();

    public ReactorAlertFeature() {
        super(FeatureMetadata.builder()
                .id("reactor-alert")
                .icon(FileIcon.of("triangle-alert.png"))
                .order(23)
                .enabledByDefault(true)
                .quickAccess(false)
                .build());

        config = configGroup();
        radiusConfig = config.floatValue("radius", DEFAULT_RADIUS);

        Events.on(BlockBuildBeginEvent.class, this::onBuildBegin);
    }

    public static String blockSettingKey(Block block) {
        return "mindustrytool.features.reactor-alert.block." + block.name;
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
            signal.subscribe(enabled -> {
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
        if (block == null || !isExplosiveReactor(block) || !isBlockEnabled(block)) {
            return;
        }

        Player builder = event.unit != null ? event.unit.getPlayer() : null;
        if (builder != null && builder == Vars.player) {
            return;
        }

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
        
        for (int i = 0; i < cores.size; i++) {
            CoreBuild core = cores.get(i);
            if (core == null) {
                continue;
            }
            float distance = Mathf.dst(worldX, worldY, core.x, core.y) / Vars.tilesize;
            nearest = nearest < distance ? nearest : distance;
        }

        if (nearest == Float.MAX_VALUE) {
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
        String message = Core.bundle.format("feature.reactor-alert.alert", builderName + "[white]",
                String.format("%.1f", nearest), block.localizedName);
        String colored = "[scarlet]" + message + "[]";
        
        if (Vars.net.active() && Vars.ui != null && Vars.ui.chatfrag != null) {
            Vars.ui.chatfrag.addMessage(colored);
        } else if (Vars.ui != null && Vars.ui.hudfrag != null) {
            Vars.ui.hudfrag.showToast(colored);
        }
    }

    private boolean isExplosiveReactor(Block block) {
        return block.flags != null && block.flags.contains(BlockFlag.reactor)
                || (block instanceof PowerGenerator && ((PowerGenerator) block).explosionRadius > 0);
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new ReactorAlertSettingsDialog(this);
            }
            return settingsDialog;
        };
    }
}
