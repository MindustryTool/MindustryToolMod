package mindustrytool.features.autoplay.tasks;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.power.PowerGenerator;import mindustry.world.blocks.production.BeamDrill;
import mindustry.world.blocks.production.Drill;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.production.Separator;
import mindustry.world.blocks.production.WallCrafter;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import mindustry.world.blocks.storage.StorageBlock.StorageBuild;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.blocks.units.UnitAssembler;
import mindustry.world.blocks.units.UnitAssembler.AssemblerUnitPlan;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.blocks.units.UnitFactory.UnitPlan;
import mindustry.world.consumers.Consume;
import mindustry.world.consumers.ConsumeItems;
import mindustrytool.components.FileIcon;
import mindustrytool.components.WebStyles;
import mindustrytool.features.autoplay.AutoplayFeature;
import solim.config.ConfigValue;
import solim.config.OrderedSeqPersister;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class FactorySupplyTask implements AutoplayTask {

    public static final String ID = "factory-supply";

    private static final float PAIR_MARGIN = 20f;
    private static final float PENDING_TIMEOUT = 150f;
    private static final float MIN_ACTION_SPACING = 24f;

    private final ConfigValue<Seq<String>> disabledBlocks;
    private final ConfigValue<Boolean> conservative;
    private final Signal<String> status = Signal.of(Core.bundle.get("feature.autoplay.status.idle"));
    private final FactorySupplyAI ai = new FactorySupplyAI();

    private int factoryCursor = 0;
    private int itemCursor = 0;

    // TEMP measurement logs (remove after diagnosing slow take/drop).
    private float lastDecisionLogTick = -1000f;
    private @Nullable String lastLoggedJob;
    private int lastCoreCount = 0;
    private int lastContainerCount = 0;
    private int lastFactoryCount = 0;
    private int skippedConservativeInput = 0;
    private int skippedNotFull = 0;

    public FactorySupplyTask(AutoplayFeature feature) {
        this.disabledBlocks = feature.configGroup().value("factory-supply.disabled-blocks", new Seq<>(),
                new OrderedSeqPersister());
        this.conservative = feature.configGroup().boolValue("factory-supply.conservative", true);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return Core.bundle.get("feature.autoplay.task.factory-supply");
    }

    @Override
    public TextureRegionDrawable getIcon() {
        return FileIcon.of("factory.png", Icon.production);
    }

    @Override
    public Readable<String> status() {
        return status;
    }

    @Override
    public BaseAutoplayAI getAI() {
        return ai;
    }

    public float cooldownSeconds() {
        return ai.cooldownSeconds();
    }

    public boolean isBlockEnabled(Block block) {
        if (block == null) {
            return false;
        }
        Seq<String> disabled = disabledBlocks.get();
        return disabled == null || !disabled.contains(block.name);
    }

    public void setBlockEnabled(Block block, boolean enabled) {
        if (block == null) {
            return;
        }
        Seq<String> current = new Seq<>();
        Seq<String> stored = disabledBlocks.get();
        if (stored != null) {
            current.addAll(stored);
        }
        if (enabled) {
            current.remove(block.name);
        } else if (!current.contains(block.name)) {
            current.add(block.name);
        }
        disabledBlocks.set(current);
    }

    public void selectAll() {
        disabledBlocks.set(new Seq<>());
    }

    public void clearAll() {
        Seq<String> all = new Seq<>();
        for (Block block : eligibleBlocks()) {
            all.add(block.name);
        }
        disabledBlocks.set(all);
    }

    public static Seq<Block> eligibleBlocks() {
        Seq<Block> result = new Seq<>();
        if (Vars.content == null || Vars.content.blocks() == null) {
            return result;
        }
        for (Block block : Vars.content.blocks()) {
            if (isEligibleBlock(block)) {
                result.add(block);
            }
        }
        result.sort((a, b) -> a.name.compareTo(b.name));
        return result;
    }

    public static boolean isEligibleBlock(@Nullable Block block) {
        if (block == null || !block.hasItems) {
            return false;
        }
        if (block instanceof GenericCrafter) {
            return true;
        }
        if (block instanceof Separator) {
            return true;
        }
        if (block instanceof UnitFactory) {
            return true;
        }
        if (block instanceof Reconstructor) {
            return true;
        }
        if (block instanceof UnitAssembler) {
            return true;
        }
        if (block instanceof Drill) {
            return true;
        }
        if (block instanceof BeamDrill) {
            return true;
        }
        if (block instanceof WallCrafter) {
            return true;
        }
        if (block instanceof PowerGenerator) {
            return consumesAnyItem(block);
        }
        return false;
    }

    private static boolean consumesAnyItem(Block block) {
        if (Vars.content == null || Vars.content.items() == null) {
            return false;
        }
        for (Item item : Vars.content.items()) {
            if (item != null && block.consumesItem(item)) {
                return true;
            }
        }
        return false;
    }

    public static Seq<Item> neededInputs(Building factory, Unit unit) {
        Seq<Item> result = new Seq<>();
        if (factory == null || !factory.isValid() || unit == null || Vars.content == null) {
            return result;
        }
        for (Item item : Vars.content.items()) {
            if (item == null) {
                continue;
            }
            try {
                if (factory.acceptStack(item, 1, unit) > 0) {
                    result.add(item);
                }
            } catch (Throwable ignored) {
            }
        }
        return result;
    }

    public static Seq<Item> outputProducts(Building factory) {
        Seq<Item> result = new Seq<>();
        if (factory == null || !factory.isValid() || factory.items == null || factory.block == null) {
            return result;
        }
        Block block = factory.block;
        if (block instanceof GenericCrafter) {
            GenericCrafter crafter = (GenericCrafter) block;
            if (crafter.outputItem != null) {
                addIfStored(result, factory, crafter.outputItem.item);
            }
            if (crafter.outputItems != null) {
                for (ItemStack stack : crafter.outputItems) {
                    if (stack != null) {
                        addIfStored(result, factory, stack.item);
                    }
                }
            }
        } else if (block instanceof Separator) {
            Separator separator = (Separator) block;
            if (separator.results != null) {
                for (ItemStack stack : separator.results) {
                    if (stack != null) {
                        addIfStored(result, factory, stack.item);
                    }
                }
            }
        } else if (block instanceof WallCrafter) {
            WallCrafter wallCrafter = (WallCrafter) block;
            addIfStored(result, factory, wallCrafter.output);
        } else if (block instanceof Drill || block instanceof BeamDrill) {
            if (Vars.content.items() == null) {
                return result;
            }
            for (Item item : Vars.content.items()) {
                if (item == null || block.consumesItem(item)) {
                    continue;
                }
                addIfStored(result, factory, item);
            }
        }
        return result;
    }

    private static void addIfStored(Seq<Item> result, Building factory, @Nullable Item item) {
        if (item == null || result.contains(item)) {
            return;
        }
        try {
            if (factory.items.get(item) > 0) {
                result.add(item);
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean isValidStorage(@Nullable Building building, @Nullable Unit unit) {
        if (building == null || unit == null || !building.isValid() || building.dead()) {
            return false;
        }
        if (building.team != unit.team || building.block == null || !building.block.hasItems) {
            return false;
        }
        return building.items != null;
    }

    private static boolean isStandaloneStorage(Building building) {
        if (building instanceof CoreBuild) {
            return false;
        }
        if (building instanceof StorageBuild) {
            return ((StorageBuild) building).linkedCore == null;
        }
        return false;
    }

    public static boolean canPair(Building storage, Building factory) {
        if (storage == null || factory == null) {
            return false;
        }
        float maxPairDst = Vars.itemTransferRange * 2f - PAIR_MARGIN * 2f;
        try {
            return storage.dst(factory) <= Math.max(Vars.itemTransferRange, maxPairDst);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void collectStorages(Unit unit, Seq<Building> cores, Seq<Building> containers) {
        cores.clear();
        containers.clear();
        if (unit == null || unit.team == null) {
            return;
        }
        try {
            for (Building building : Groups.build) {
                if (!isValidStorage(building, unit)) {
                    continue;
                }
                if (building instanceof CoreBuild) {
                    cores.add(building);
                } else if (isStandaloneStorage(building)) {
                    containers.add(building);
                }
            }
        } catch (Throwable ignored) {
            return;
        }
        cores.sort(b -> unit.dst2(b));
        containers.sort(b -> unit.dst2(b));
    }

    private Seq<Building> findFactories(Unit unit, Seq<Building> cores, Seq<Building> containers) {
        Seq<Building> result = new Seq<>();
        if (unit == null) {
            return result;
        }
        try {
            for (Building building : Groups.build) {
                if (building == null || !building.isValid() || building.dead()) {
                    continue;
                }
                if (building.team != unit.team || building.block == null) {
                    continue;
                }
                if (!isEligibleBlock(building.block) || !isBlockEnabled(building.block)) {
                    continue;
                }
                if (building.items == null) {
                    continue;
                }
                if (!hasPairableStorage(building, cores, containers)) {
                    continue;
                }
                result.add(building);
            }
        } catch (Throwable ignored) {
            return result;
        }
        result.sort(b -> b.pos());
        return result;
    }

    private static boolean hasPairableStorage(Building factory, Seq<Building> cores, Seq<Building> containers) {
        for (Building core : cores) {
            if (canPair(core, factory)) {
                return true;
            }
        }
        for (Building container : containers) {
            if (canPair(container, factory)) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable Building findWithdrawStorage(Building factory, Item item, Unit unit,
            Seq<Building> cores, Seq<Building> containers) {
        for (Building core : cores) {
            if (!canPair(core, factory)) {
                continue;
            }
            try {
                if (core.items != null && core.items.get(item) > 0) {
                    return core;
                }
            } catch (Throwable ignored) {
            }
        }
        for (Building container : containers) {
            if (!canPair(container, factory)) {
                continue;
            }
            try {
                if (container.items != null && container.items.get(item) > 0) {
                    return container;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static @Nullable Building findDepositStorage(Building factory, Item item, int amount, Unit unit,
            Seq<Building> cores, Seq<Building> containers) {
        for (Building core : cores) {
            if (!canPair(core, factory)) {
                continue;
            }
            try {
                if (core.acceptStack(item, amount, unit) > 0) {
                    return core;
                }
            } catch (Throwable ignored) {
            }
        }
        for (Building container : containers) {
            if (!canPair(container, factory)) {
                continue;
            }
            try {
                if (!container.allowDeposit()) {
                    continue;
                }
                if (container.acceptStack(item, amount, unit) > 0) {
                    return container;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static @Nullable Building findDepositStorageForHeld(Item item, int amount, Unit unit,
            Seq<Building> cores, Seq<Building> containers) {
        for (Building core : cores) {
            try {
                if (unit.within(core, Vars.itemTransferRange * 2f) && core.acceptStack(item, amount, unit) > 0) {
                    return core;
                }
            } catch (Throwable ignored) {
            }
        }
        for (Building container : containers) {
            try {
                if (!container.allowDeposit()) {
                    continue;
                }
                if (unit.within(container, Vars.itemTransferRange * 2f) && container.acceptStack(item, amount, unit) > 0) {
                    return container;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static boolean canDepositInto(Building factory, Item item, Unit unit) {
        if (Vars.state != null && Vars.state.rules != null && Vars.state.rules.onlyDepositCore) {
            try {
                if (!factory.allowDeposit()) {
                    return false;
                }
            } catch (Throwable ignored) {
                return false;
            }
        }
        try {
            return factory.acceptStack(item, 1, unit) > 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public boolean isConservative() {
        Boolean value = conservative.get();
        return value == null || value;
    }

    public static int requiredAmount(@Nullable Block block, @Nullable Item item) {
        if (block == null || item == null) {
            return 1;
        }
        int min = Integer.MAX_VALUE;
        if (block.consumers != null) {
            for (Consume consume : block.consumers) {
                if (consume instanceof ConsumeItems) {
                    ItemStack[] stacks = ((ConsumeItems) consume).items;
                    if (stacks != null) {
                        for (ItemStack stack : stacks) {
                            if (stack != null && stack.item == item) {
                                min = Math.min(min, stack.amount);
                            }
                        }
                    }
                }
            }
        }
        if (block instanceof UnitFactory) {
            Seq<UnitPlan> plans = ((UnitFactory) block).plans;
            if (plans != null) {
                for (UnitPlan plan : plans) {
                    if (plan != null && plan.requirements != null) {
                        for (ItemStack stack : plan.requirements) {
                            if (stack != null && stack.item == item) {
                                min = Math.min(min, stack.amount);
                            }
                        }
                    }
                }
            }
        }
        if (block instanceof UnitAssembler) {
            Seq<AssemblerUnitPlan> plans = ((UnitAssembler) block).plans;
            if (plans != null) {
                for (AssemblerUnitPlan plan : plans) {
                    if (plan != null && plan.itemReq != null) {
                        for (ItemStack stack : plan.itemReq) {
                            if (stack != null && stack.item == item) {
                                min = Math.min(min, stack.amount);
                            }
                        }
                    }
                }
            }
        }
        return min == Integer.MAX_VALUE ? Math.max(1, block.itemCapacity) : min;
    }

    public static boolean isOutputFull(@Nullable Building factory, @Nullable Item output) {
        if (factory == null || output == null || factory.items == null || factory.block == null) {
            return false;
        }
        try {
            return factory.items.get(output) >= Math.max(1, factory.block.itemCapacity);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean needsInput(Building factory, Item input, Unit unit) {
        if (!canDepositInto(factory, input, unit)) {
            return false;
        }
        if (!isConservative()) {
            return true;
        }
        try {
            // TEMP measurement: count factories skipped because they already hold enough input.
            boolean need = factory.items == null || factory.items.get(input) < requiredAmount(factory.block, input);
            if (!need) {
                skippedConservativeInput++;
            }
            return need;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private boolean shouldCollect(Building factory, Item output) {
        if (!isConservative()) {
            return true;
        }
        // TEMP measurement: count outputs skipped because they are not full yet.
        boolean full = isOutputFull(factory, output);
        if (!full) {
            skippedNotFull++;
        }
        return full;
    }

    @Override
    public boolean update(Unit unit) {
        // TEMP measurement: wall-clock the whole decision (scan + scheduling).
        long t0 = Time.millis();
        skippedConservativeInput = 0;
        skippedNotFull = 0;
        boolean result = false;
        try {
            result = updateInner(unit);
        } finally {
            long dt = Time.millis() - t0;
            String job = ai.debugJob();
            boolean changed = lastLoggedJob == null || !lastLoggedJob.equals(job);
            if (changed || Time.time - lastDecisionLogTick >= 120f || dt >= 5) {
                lastDecisionLogTick = Time.time;
                lastLoggedJob = job;
                Log.info("[FactorySupply] decide dt=" + dt + "ms active=" + result
                        + " cores=" + lastCoreCount + " containers=" + lastContainerCount
                        + " factories=" + lastFactoryCount + " job=" + job
                        + " skipLow=" + skippedConservativeInput + " skipNotFull=" + skippedNotFull);
            }
        }
        return result;
    }

    private boolean updateInner(Unit unit) {
        if (unit == null || !unit.isValid() || unit.type == null || unit.type.itemCapacity <= 0) {
            status.set(Core.bundle.get("feature.autoplay.status.cannot-carry"));
            return false;
        }
        Seq<Building> cores = new Seq<>();
        Seq<Building> containers = new Seq<>();
        collectStorages(unit, cores, containers);
        lastCoreCount = cores.size;
        lastContainerCount = containers.size;
        if (cores.isEmpty() && containers.isEmpty()) {
            status.set(Core.bundle.get("feature.autoplay.status.no-storage"));
            return false;
        }
        Seq<Building> factories = findFactories(unit, cores, containers);
        lastFactoryCount = factories.size;
        if (factories.isEmpty()) {
            status.set(Core.bundle.get("feature.autoplay.status.no-factories"));
            return false;
        }
        if (factoryCursor >= factories.size) {
            factoryCursor = 0;
        }

        if (unit.stack.amount > 0 && unit.stack.item != null) {
            return updateCarrying(unit, cores, containers, factories);
        }
        return updateEmpty(unit, cores, containers, factories);
    }

    private boolean updateCarrying(Unit unit, Seq<Building> cores, Seq<Building> containers,
            Seq<Building> factories) {
        Item held = unit.stack.item;
        int amount = unit.stack.amount;

        Building preferredFactory = ai.targetFactory;
        if (isFactoryStillValid(preferredFactory, factories) && needsInput(preferredFactory, held, unit)) {
            ai.setJob(FactorySupplyAI.Job.DELIVER_TO_FACTORY, preferredFactory, null, held);
            status.set(Core.bundle.format("feature.autoplay.status.supplying", held.localizedName));
            return true;
        }

        for (int offset = 0; offset < factories.size; offset++) {
            int index = (factoryCursor + offset) % factories.size;
            Building factory = factories.get(index);
            if (needsInput(factory, held, unit)) {
                factoryCursor = index;
                ai.setJob(FactorySupplyAI.Job.DELIVER_TO_FACTORY, factory, null, held);
                status.set(Core.bundle.format("feature.autoplay.status.supplying", held.localizedName));
                return true;
            }
        }

        Building storage = findDepositStorageForHeld(held, amount, unit, cores, containers);
        if (storage != null) {
            ai.setJob(FactorySupplyAI.Job.DELIVER_TO_STORAGE, null, storage, held);
            status.set(Core.bundle.format("feature.autoplay.status.collecting", held.localizedName));
            return true;
        }

        ai.setJob(FactorySupplyAI.Job.DROP_HELD, null, null, held);
        status.set(Core.bundle.get("feature.autoplay.status.dropping"));
        return true;
    }

    private boolean updateEmpty(Unit unit, Seq<Building> cores, Seq<Building> containers,
            Seq<Building> factories) {
        Building lastFactory = ai.lastFactory;
        if (isFactoryStillValid(lastFactory, factories)) {
            Seq<Item> outputs = outputProducts(lastFactory);
            if (!outputs.isEmpty()) {
                itemCursor = itemCursor % outputs.size;
                for (int offset = 0; offset < outputs.size; offset++) {
                    Item output = outputs.get((itemCursor + offset) % outputs.size);
                    if (!shouldCollect(lastFactory, output)) {
                        continue;
                    }
                    Building storage = findDepositStorage(lastFactory, output,
                            Math.min(lastFactory.items.get(output), unit.type.itemCapacity), unit, cores, containers);
                    if (storage != null) {
                        itemCursor = (itemCursor + offset + 1) % outputs.size;
                        ai.setJob(FactorySupplyAI.Job.COLLECT, lastFactory, storage, output);
                        status.set(Core.bundle.format("feature.autoplay.status.collecting", output.localizedName));
                        return true;
                    }
                }
            }
        }

        for (int offset = 0; offset < factories.size; offset++) {
            int index = (factoryCursor + offset + 1) % factories.size;
            Building factory = factories.get(index);

            Seq<Item> inputs = neededInputs(factory, unit);
            if (!inputs.isEmpty()) {
                itemCursor = itemCursor % inputs.size;
                for (int inputOffset = 0; inputOffset < inputs.size; inputOffset++) {
                    Item input = inputs.get((itemCursor + inputOffset) % inputs.size);
                    if (!needsInput(factory, input, unit)) {
                        continue;
                    }
                    Building source = findWithdrawStorage(factory, input, unit, cores, containers);
                    if (source != null) {
                        factoryCursor = index;
                        itemCursor = (itemCursor + inputOffset + 1) % inputs.size;
                        ai.setJob(FactorySupplyAI.Job.SUPPLY, factory, source, input);
                        status.set(Core.bundle.format("feature.autoplay.status.supplying", input.localizedName));
                        return true;
                    }
                }
            }

            Seq<Item> outputs = outputProducts(factory);
            if (!outputs.isEmpty()) {
                for (int outputOffset = 0; outputOffset < outputs.size; outputOffset++) {
                    Item output = outputs.get(outputOffset);
                    if (!shouldCollect(factory, output)) {
                        continue;
                    }
                    Building storage = findDepositStorage(factory, output,
                            Math.min(factory.items.get(output), unit.type.itemCapacity), unit, cores, containers);
                    if (storage != null) {
                        factoryCursor = index;
                        ai.setJob(FactorySupplyAI.Job.COLLECT, factory, storage, output);
                        status.set(Core.bundle.format("feature.autoplay.status.collecting", output.localizedName));
                        return true;
                    }
                }
            }
        }

        status.set(Core.bundle.get("feature.autoplay.status.waiting-factories"));
        return false;
    }

    private static boolean isFactoryStillValid(@Nullable Building factory, Seq<Building> factories) {
        if (factory == null || !factory.isValid() || factory.dead()) {
            return false;
        }
        return factories.contains(factory);
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public void buildSettings(AutoplayFeature feature) {
        column().growX().gap(unit(1)).children(() -> {
            checkbox(Core.bundle.get("feature.autoplay.settings.factory-supply.conservative"), conservative.signal())
                    .growX();
            text(Core.bundle.get("feature.autoplay.settings.factory-supply.conservative.description")).growX().left()
                    .wrap(true)
                    .color(WebStyles.Colors.GHOST_FG);

            text(Core.bundle.get("feature.autoplay.settings.factory-supply.filter")).growX().left()
                    .color(WebStyles.Colors.GHOST_FG);

            row().growX().gap(unit(1)).children(() -> {
                button(Core.bundle.get("feature.autoplay.settings.factory-supply.select-all"), this::selectAll)
                        .style(WebStyles.secondary())
                        .height(unit(9))
                        .growX();
                button(Core.bundle.get("feature.autoplay.settings.factory-supply.clear-all"), this::clearAll)
                        .style(WebStyles.secondary())
                        .height(unit(9))
                        .growX();
            });

            wrap().growX().gap(unit(1)).children(() -> {
                for (Block block : eligibleBlocks()) {
                    Readable<Boolean> checked = disabledBlocks.signal()
                            .map(seq -> seq == null || !seq.contains(block.name));

                    button(() -> setBlockEnabled(block, !Boolean.TRUE.equals(checked.peek())))
                            .style(WebStyles.filterChipText())
                            .checked(checked)
                            .paddingX(unit(2))
                            .height(unit(8))
                            .children(() -> {
                                icon(new TextureRegionDrawable(block.uiIcon)).size(unit(4));
                                text(block.localizedName)
                                        .color(checked.map(c -> Boolean.TRUE.equals(c) ? Color.white : Color.gray));
                            });
                }
            });
        });
    }

    public static class FactorySupplyAI extends BaseAutoplayAI {

        public enum Job {
            SUPPLY,
            COLLECT,
            DELIVER_TO_FACTORY,
            DELIVER_TO_STORAGE,
            DROP_HELD
        }

        private @Nullable Building targetFactory;
        private @Nullable Building targetStorage;
        private @Nullable Item targetItem;
        private Job job = Job.SUPPLY;
        private @Nullable Building lastFactory;

        private float cooldownTimer = 0f;
        private float pendingTimer = 0f;
        private boolean actionPending = false;
        private int prevAmount = 0;
        private @Nullable Item pendingItem;

        // TEMP measurement logs (remove after diagnosing slow take/drop).
        private @Nullable String currentKey;
        private float jobStartTick = 0f;
        private float actionSentTick = 0f;
        private @Nullable String lastMoveLogKey;

        private static String posOf(@Nullable Building building) {
            return building == null ? "-" : building.tileX() + "," + building.tileY();
        }

        String debugJob() {
            return currentKey != null ? currentKey : job + " (no job set yet)";
        }

        public void setJob(Job job, @Nullable Building factory, @Nullable Building storage, @Nullable Item item) {
            this.job = job;
            this.targetFactory = factory;
            this.targetStorage = storage;
            this.targetItem = item;
            if (factory != null) {
                this.lastFactory = factory;
            }
            // TEMP measurement: track when the current job started.
            String key = job + " f=" + posOf(factory) + " s=" + posOf(storage)
                    + " i=" + (item != null ? item.name : "-");
            if (currentKey == null || !currentKey.equals(key)) {
                currentKey = key;
                jobStartTick = Time.time;
            }
        }

        private float actionCooldown() {
            float rulesCooldown = MIN_ACTION_SPACING;
            try {
                if (Vars.state != null && Vars.state.rules != null) {
                    rulesCooldown = Math.max(MIN_ACTION_SPACING, Vars.state.rules.itemDepositCooldown * 40f + 2f);
                }
            } catch (Throwable ignored) {
            }
            return rulesCooldown;
        }

        private boolean canAct() {
            return !actionPending && cooldownTimer <= 0f;
        }

        public float cooldownSeconds() {
            return Math.max(0f, cooldownTimer) / 60f;
        }

        private void snapshot(Unit unit) {
            prevAmount = unit.stack.amount;
            pendingItem = unit.stack.item;
        }

        private void markActed(Unit unit) {
            actionPending = true;
            pendingTimer = PENDING_TIMEOUT;
            cooldownTimer = actionCooldown();
            // TEMP measurement: timestamp the Call for ack/timeout elapsed reporting.
            actionSentTick = Time.time;
        }

        private void updatePending(Unit unit) {
            if (cooldownTimer > 0f) {
                cooldownTimer -= Time.delta;
            }
            if (!actionPending) {
                return;
            }
            boolean acknowledged = unit.stack.amount != prevAmount || unit.stack.item != pendingItem;
            if (acknowledged) {
                // TEMP measurement: how long the server took to confirm the Call.
                float elapsed = Time.time - actionSentTick;
                String kind = unit.stack.amount < prevAmount ? "deposit-acked" : "withdraw-acked";
                Log.info("[FactorySupply] " + kind + " after " + (int) elapsed + "t stack="
                        + unit.stack.amount + "x" + (unit.stack.item != null ? unit.stack.item.name : "-")
                        + " job=" + debugJob());
                actionPending = false;
                pendingTimer = 0f;
                return;
            }
            pendingTimer -= Time.delta;
            if (pendingTimer <= 0f) {
                // TEMP measurement: ack never arrived — suspected server rejection.
                Log.info("[FactorySupply] pending TIMEOUT after " + (int) (Time.time - actionSentTick)
                        + "t (suspected server reject) job=" + debugJob());
                actionPending = false;
            }
        }

        private boolean tryRequest(Unit unit, Building from, Item item, int amount) {
            if (unit == null || from == null || item == null || amount <= 0) {
                return false;
            }
            if (!canAct()) {
                return false;
            }
            if (!unit.within(from, Vars.itemTransferRange)) {
                return false;
            }
            try {
                if (from.items == null || from.items.get(item) <= 0) {
                    return false;
                }
            } catch (Throwable ignored) {
                return false;
            }
            snapshot(unit);
            Call.requestItem(Vars.player, from, item, amount);
            markActed(unit);
            // TEMP measurement: a take just fired.
            Log.info("[FactorySupply] requestItem " + item.name + " x" + amount + " from "
                    + from.block.name + "@" + posOf(from) + " jobAge=" + (int) (Time.time - jobStartTick) + "t");
            return true;
        }

        private boolean tryTransfer(Unit unit, Building to) {
            if (unit == null || to == null || unit.stack.amount <= 0 || unit.stack.item == null) {
                return false;
            }
            if (!canAct()) {
                return false;
            }
            if (!unit.within(to, Vars.itemTransferRange)) {
                return false;
            }
            try {
                if (to.acceptStack(unit.stack.item, unit.stack.amount, unit) <= 0) {
                    return false;
                }
            } catch (Throwable ignored) {
                return false;
            }
            snapshot(unit);
            Call.transferInventory(Vars.player, to);
            markActed(unit);
            // TEMP measurement: a drop just fired.
            Log.info("[FactorySupply] transferInventory " + unit.stack.item.name + " x" + unit.stack.amount + " to "
                    + to.block.name + "@" + posOf(to) + " jobAge=" + (int) (Time.time - jobStartTick) + "t");
            return true;
        }

        private boolean tryDrop(Unit unit) {
            if (unit == null || unit.stack.amount <= 0) {
                return false;
            }
            if (!canAct()) {
                return false;
            }
            snapshot(unit);
            Call.dropItem(0f);
            markActed(unit);
            // TEMP measurement: excess stack dropped.
            Log.info("[FactorySupply] dropItem x" + unit.stack.amount + " jobAge="
                    + (int) (Time.time - jobStartTick) + "t");
            return true;
        }

        @Override
        public void updateMovement() {
            if (unit == null) {
                return;
            }
            updatePending(unit);

            Building factory = targetFactory;
            Building storage = targetStorage;
            Item item = targetItem;

            // TEMP measurement: log each new job with travel distances.
            if (currentKey != null && !currentKey.equals(lastMoveLogKey)) {
                lastMoveLogKey = currentKey;
                Log.info("[FactorySupply] start " + currentKey + " stack=" + unit.stack.amount + "x"
                        + (unit.stack.item != null ? unit.stack.item.name : "-")
                        + " dFactory=" + (factory != null ? (int) unit.dst(factory) : -1)
                        + " dStorage=" + (storage != null ? (int) unit.dst(storage) : -1));
            }

            if (unit.stack.amount > 0 && unit.stack.item != null) {
                Item held = unit.stack.item;
                if (factory != null && factory.isValid() && !factory.dead()) {
                    boolean factoryWants = false;
                    try {
                        factoryWants = factory.acceptStack(held, 1, unit) > 0;
                    } catch (Throwable ignored) {
                    }
                    if (factoryWants) {
                        moveTo(factory, Vars.itemTransferRange * 0.7f);
                        tryTransfer(unit, factory);
                        return;
                    }
                }
                Building deposit = storage;
                if (deposit == null || !deposit.isValid() || deposit.dead()) {
                    deposit = findAnyDepositForHeld(unit, held);
                }
                if (deposit != null) {
                    moveTo(deposit, Vars.itemTransferRange * 0.7f);
                    tryTransfer(unit, deposit);
                    return;
                }
                tryDrop(unit);
                return;
            }

            if (item == null) {
                return;
            }

            switch (job) {
                case SUPPLY:
                    if (factory == null || storage == null || !factory.isValid() || !storage.isValid()) {
                        return;
                    }
                    moveTo(storage, Vars.itemTransferRange * 0.7f);
                    int available = 0;
                    try {
                        available = storage.items != null ? storage.items.get(item) : 0;
                    } catch (Throwable ignored) {
                    }
                    int request = Math.min(available, unit.type.itemCapacity);
                    tryRequest(unit, storage, item, request);
                    break;
                case COLLECT:
                    if (factory == null || !factory.isValid()) {
                        return;
                    }
                    moveTo(factory, Vars.itemTransferRange * 0.7f);
                    int stored = 0;
                    try {
                        stored = factory.items != null ? factory.items.get(item) : 0;
                    } catch (Throwable ignored) {
                    }
                    int take = Math.min(stored, unit.type.itemCapacity);
                    tryRequest(unit, factory, item, take);
                    break;
                case DELIVER_TO_FACTORY:
                case DELIVER_TO_STORAGE:
                case DROP_HELD:
                    break;
            }
        }

        private @Nullable Building findAnyDepositForHeld(Unit unit, Item held) {
            try {
                for (Building building : Groups.build) {
                    if (building == null || !building.isValid() || building.dead()) {
                        continue;
                    }
                    if (building.team != unit.team || building.block == null || !building.block.hasItems) {
                        continue;
                    }
                    if (!unit.within(building, Vars.itemTransferRange * 2f)) {
                        continue;
                    }
                    try {
                        if (building.acceptStack(held, unit.stack.amount, unit) > 0) {
                            if (building instanceof CoreBuild) {
                                return building;
                            }
                            if (building instanceof StorageBuild) {
                                StorageBuild storageBuild = (StorageBuild) building;
                                if (storageBuild.linkedCore == null && building.allowDeposit()) {
                                    return building;
                                }
                            } else if (building.allowDeposit()) {
                                return building;
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            } catch (Throwable ignored) {
            }
            return null;
        }
    }
}
