package mindustrytool.features.blockstats;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import arc.util.Nullable;
import java.util.Locale;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.world.Block;
import mindustry.world.blocks.payloads.BlockProducer;
import mindustry.world.blocks.payloads.PayloadDeconstructor;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.blocks.production.BeamDrill;
import mindustry.world.blocks.production.Drill;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.production.Pump;
import mindustry.world.blocks.production.Separator;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.blocks.units.UnitAssembler;
import mindustry.world.blocks.units.UnitFactory;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingKeybindCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import solim.config.ConfigValue;
import solim.core.Component;
import solim.reactive.Computed;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class BlockStatsDisplaySettingsView extends Component {

    private static final int chipColumns = 8;

    private final BlockStatsDisplayFeature feature;
    private final Signal<BlockGroup> progressFilter = Signal.of(BlockGroup.all);
    private final Signal<BlockGroup> efficiencyFilter = Signal.of(BlockGroup.all);

    public BlockStatsDisplaySettingsView(BlockStatsDisplayFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            globalSettingsCard();

            blockSelectionCard(
                    Icon.production,
                    Core.bundle.get("feature.blockstats-display.settings.section.progress"),
                    BlockStatsDisplayFeature.progressBlocks(),
                    progressFilter,
                    true);

            blockSelectionCard(
                    Icon.power,
                    Core.bundle.get("feature.blockstats-display.settings.section.efficiency"),
                    BlockStatsDisplayFeature.efficiencyBlocks(),
                    efficiencyFilter,
                    false);

            keybindsCard();
            actionsCard();
        });
    }

    private void keybindsCard() {
        SettingKeybindCard.forFeature(feature);
    }

    private Component headerCard() {
        return SettingCard.header(feature, "LOGISTICS");
    }

    private Component globalSettingsCard() {
        return SettingCard.section(Icon.settings, Core.bundle.get("feature.blockstats-display.settings.section.global"), () -> {
            SettingSlider.of(
                    Core.bundle.get("feature.blockstats-display.settings.zoom-threshold"),
                    feature.zoomThresholdConfig.signal(),
                    0f,
                    2f,
                    0.1f,
                    feature.zoomThresholdConfig.signal().map(v -> {
                        float val = v != null ? v : 0.5f;
                        return val <= 0.01f
                                ? Core.bundle.get("feature.blockstats-display.settings.off")
                                : String.format(Locale.US, "%.1fx", val);
                    })
            );

            SettingSlider.of(
                    Core.bundle.get("feature.blockstats-display.settings.opacity"),
                    feature.opacityConfig.signal(),
                    0f,
                    1f,
                    0.05f,
                    feature.opacityConfig.signal().map(v ->
                            String.format(Locale.US, "%.0f%%", (v != null ? v : 1f) * 100))
            );

            SettingSlider.of(
                    Core.bundle.get("feature.blockstats-display.settings.scale"),
                    feature.scaleConfig.signal(),
                    0.5f,
                    1.5f,
                    0.1f,
                    feature.scaleConfig.signal().map(v ->
                            String.format(Locale.US, "%.0f%%", (v != null ? v : 1f) * 100))
            );
        });
    }

    private Component blockSelectionCard(
            @Nullable Drawable iconDrawable,
            String title,
            Seq<Block> blocks,
            Signal<BlockGroup> filter,
            boolean progress) {

        Seq<BlockGroup> groups = groupsOf(blocks);
        Computed<Seq<Block>> visibleBlocks = Signal.computed(() -> {
            BlockGroup group = filter.get();
            if (group == null || group == BlockGroup.all) {
                return blocks;
            }
            return blocks.select(block -> groupOf(block) == group);
        });

        return SettingCard.section(iconDrawable, title, () -> {
            wrap().props(growX(), gap(unit(1.5f))).children(() -> {
                for (BlockGroup group : groups) {
                    button(Core.bundle.get(group.bundleKey()), () -> filter.set(group))
                            .props(checked(filter.map(current -> current == group)),
                                    style(WebStyles.filterChipText()), height(unit(8)), prefHeight(unit(8)), paddingX(unit(2.5f)));
                }
            });

            row().props(growX(), gap(unit(1.5f))).children(() -> {
                bulkButton(Icon.add, Core.bundle.get("feature.blockstats-display.settings.select-all"),
                        () -> setAllBlocksEnabled(progress, true));
                bulkButton(Icon.cancel, Core.bundle.get("feature.blockstats-display.settings.deselect-all"),
                        () -> setAllBlocksEnabled(progress, false));
            });

            reactiveGrid(visibleBlocks)
                    .props(growX(), gap(unit(1.5f)))
                    .columns(chipColumns)
                    .key(block -> block.name)
                    .items(block -> new BlockChip(block, progress));
        });
    }

    private Component bulkButton(Drawable iconDrawable, String label, Runnable action) {
        return button(action)
                .props(style(WebStyles.outlineText()), height(unit(11)), prefHeight(unit(11)), growX(), gap(unit(2)))
                .children(() -> {
                    icon(iconDrawable).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                    text(label);
                });
    }

    private Component actionsCard() {
        return SettingCard.card().children(() -> {
            button(feature::resetToDefaults)
                    .props(style(WebStyles.outline()), growX(), height(unit(11)), prefHeight(unit(11)), gap(unit(2)))
                    .children(() -> {
                        icon(Icon.refresh).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                        text(Core.bundle.get("feature.blockstats-display.settings.reset"));
                    });
        });
    }

    private void setAllBlocksEnabled(boolean progress, boolean value) {
        if (progress) {
            feature.setAllProgressBlocksEnabled(value);
        } else {
            feature.setAllEfficiencyBlocksEnabled(value);
        }
    }

    private ConfigValue<Boolean> blockValue(Block block, boolean progress) {
        return progress ? feature.progressBlockValue(block) : feature.efficiencyBlockValue(block);
    }

    private boolean isBlockEnabled(Block block, boolean progress) {
        return Boolean.TRUE.equals(blockValue(block, progress).get());
    }

    private void setBlockEnabled(Block block, boolean progress, boolean value) {
        blockValue(block, progress).set(value);
    }

    private static Seq<BlockGroup> groupsOf(Seq<Block> blocks) {
        Seq<BlockGroup> groups = new Seq<>();
        groups.add(BlockGroup.all);
        for (BlockGroup group : BlockGroup.values()) {
            if (group != BlockGroup.all && hasGroup(blocks, group)) {
                groups.add(group);
            }
        }
        return groups;
    }

    private static boolean hasGroup(Seq<Block> blocks, BlockGroup group) {
        for (int i = 0; i < blocks.size; i++) {
            if (groupOf(blocks.get(i)) == group) {
                return true;
            }
        }
        return false;
    }

    private static BlockGroup groupOf(Block block) {
        if (block instanceof UnitFactory || block instanceof Reconstructor || block instanceof UnitAssembler) {
            return BlockGroup.units;
        }
        if (block instanceof Separator || block instanceof BlockProducer || block instanceof PayloadDeconstructor) {
            return BlockGroup.payload;
        }
        if (block instanceof GenericCrafter) {
            return BlockGroup.crafters;
        }
        if (block instanceof Drill || block instanceof BeamDrill) {
            return BlockGroup.drills;
        }
        if (block instanceof Pump) {
            return BlockGroup.pumps;
        }
        if (block instanceof PowerGenerator) {
            return BlockGroup.power;
        }
        return BlockGroup.all;
    }

    private enum BlockGroup {
        all("feature.blockstats-display.settings.filter.all"),
        crafters("feature.blockstats-display.settings.filter.crafters"),
        units("feature.blockstats-display.settings.filter.units"),
        payload("feature.blockstats-display.settings.filter.payload"),
        drills("feature.blockstats-display.settings.filter.drills"),
        pumps("feature.blockstats-display.settings.filter.pumps"),
        power("feature.blockstats-display.settings.filter.power");

        private final String bundleKey;

        BlockGroup(String bundleKey) {
            this.bundleKey = bundleKey;
        }

        String bundleKey() {
            return bundleKey;
        }
    }

    private final class BlockChip extends Component {

        private final Block block;
        private final boolean progress;

        BlockChip(Block block, boolean progress) {
            this.block = block;
            this.progress = progress;
        }

        @Override
        protected Element build() {
            TextureRegion region = block.uiIcon != null ? block.uiIcon : block.fullIcon;
            Drawable drawable = region != null ? new TextureRegionDrawable(region) : Tex.clear;

            Readable<Boolean> enabled = blockValue(block, progress).signal();

            return button()
                    .props(checked(enabled), style(WebStyles.filterChip()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), padding(unit(1)), tooltip(block.localizedName))
                    .onClick(() -> setBlockEnabled(block, progress, !isBlockEnabled(block, progress)))
                    .children(() -> {
                        icon(drawable)
                                .props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)), color(enabled.map(on ->
                                        Boolean.TRUE.equals(on) ? Color.white : new Color(1f, 1f, 1f, 0.35f))));
                    }).element();
        }
    }
}