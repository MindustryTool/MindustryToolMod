package mindustrytool.features.autoplay.tasks;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
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
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.blocks.production.BeamDrill;
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
    private static final int MAX_TOUR_STOPS = 4;
    private static final float ENGAGE_RANGE_FACTOR = 0.7f;
    private static final float STICKY_JOB_TIMEOUT = 300f;

    private final ConfigValue<Seq<String>> disabledBlocks;
    private final ConfigValue<Boolean> conservative;
    private final Signal<String> status = Signal.of(Core.bundle.get("feature.autoplay.status.idle"));
    private final FactorySupplyAI ai = new FactorySupplyAI();

    private int factoryCursor = 0;
    private int itemCursor = 0;

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
                if (unit.within(container, Vars.itemTransferRange * 2f)
                        && container.acceptStack(item, amount, unit) > 0) {
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
            return factory.items == null || factory.items.get(input) < requiredAmount(factory.block, input);
        } catch (Throwable ignored) {
            return true;
        }
    }

    private boolean shouldCollect(Building factory, Item output) {
        if (!isConservative()) {
            return true;
        }
        return isOutputFull(factory, output);
    }

    private static int storedOf(@Nullable Building factory, @Nullable Item item) {
        if (factory == null || item == null || factory.items == null) {
            return 0;
        }
        try {
            return Math.max(0, factory.items.get(item));
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static int spaceOf(@Nullable Building factory, @Nullable Item item) {
        if (factory == null || item == null || factory.block == null) {
            return 0;
        }
        return Math.max(0, factory.block.itemCapacity - storedOf(factory, item));
    }

    private int planSupplyTake(Unit unit, Building source, Seq<Building> factories, int primaryIndex,
            Building primary, Item input) {

        int total = spaceOf(primary, input);
        if (total < 1) {
            return 0;
        }
        int stops = 1;
        for (int e = 1; e < factories.size && stops < MAX_TOUR_STOPS; e++) {
            Building extra = factories.get((primaryIndex + e) % factories.size);
            if (!needsInput(extra, input, unit)) {
                continue;
            }
            int extraSpace = spaceOf(extra, input);
            if (extraSpace < 1) {
                continue;
            }
            total += extraSpace;
            stops++;
        }
        return Math.min(total, Math.min(unit.type.itemCapacity, storedOf(source, input)));
    }

    @Override
    public boolean update(Unit unit) {

        if (unit == null || unit.stack.amount == 0 || unit.stack.item == null) {
            ai.resetTour();
        } else if (ai.tourItem() == null || ai.tourItem() != unit.stack.item) {
            ai.resetTour();
        }

        if (unit == null || !unit.isValid() || unit.type == null || unit.type.itemCapacity <= 0) {
            status.set(Core.bundle.get("feature.autoplay.status.cannot-carry"));
            return false;
        }

        Seq<Building> cores = new Seq<>();
        Seq<Building> containers = new Seq<>();
        collectStorages(unit, cores, containers);

        if (cores.isEmpty() && containers.isEmpty()) {
            status.set(Core.bundle.get("feature.autoplay.status.no-storage"));
            return false;
        }

        Seq<Building> factories = findFactories(unit, cores, containers);
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
            ai.setJob(FactorySupplyAI.Job.DELIVER_TO_FACTORY, preferredFactory, null, held, 0);
            status.set(Core.bundle.format("feature.autoplay.status.supplying", held.localizedName));
            return true;
        }

        for (int offset = 0; offset < factories.size; offset++) {
            int index = (factoryCursor + offset) % factories.size;
            Building factory = factories.get(index);
            if (needsInput(factory, held, unit) && ai.tourStops() < MAX_TOUR_STOPS && spaceOf(factory, held) >= 1) {
                factoryCursor = index;
                ai.setJob(FactorySupplyAI.Job.DELIVER_TO_FACTORY, factory, null, held, 0);
                status.set(Core.bundle.format("feature.autoplay.status.supplying", held.localizedName));
                return true;
            }
        }

        if (ai.tourStops() < MAX_TOUR_STOPS && amount < unit.type.itemCapacity) {
            int room = unit.type.itemCapacity - amount;
            for (int offset = 0; offset < factories.size; offset++) {
                int index = (factoryCursor + offset) % factories.size;
                Building factory = factories.get(index);
                int take = Math.min(storedOf(factory, held), room);
                if (take <= 0 || !outputProducts(factory).contains(held)) {
                    continue;
                }

                if (findDepositStorageForHeld(held, amount, unit, cores, containers) == null) {
                    continue;
                }
                factoryCursor = index;
                ai.setJob(FactorySupplyAI.Job.COLLECT, factory, null, held, 0);
                status.set(Core.bundle.format("feature.autoplay.status.collecting", held.localizedName));
                return true;
            }
        }

        Building storage = findDepositStorageForHeld(held, amount, unit, cores, containers);
        if (storage != null) {
            ai.setJob(FactorySupplyAI.Job.DELIVER_TO_STORAGE, null, storage, held, 0);
            status.set(Core.bundle.format("feature.autoplay.status.collecting", held.localizedName));
            return true;
        }

        ai.setJob(FactorySupplyAI.Job.DROP_HELD, null, null, held, 0);
        status.set(Core.bundle.get("feature.autoplay.status.dropping"));
        return true;
    }

    private boolean updateEmpty(Unit unit, Seq<Building> cores, Seq<Building> containers,
            Seq<Building> factories) {
        if (reassertSupplyJob(unit, cores, containers, factories)
                || reassertCollectJob(unit, cores, containers, factories)) {
            return true;
        }
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
                        ai.setJob(FactorySupplyAI.Job.COLLECT, lastFactory, storage, output, 0);
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
                    if (source == null) {
                        continue;
                    }

                    int take = planSupplyTake(unit, source, factories, index, factory, input);
                    if (take <= 0) {
                        continue;
                    }
                    factoryCursor = index;
                    itemCursor = (itemCursor + inputOffset + 1) % inputs.size;
                    ai.setJob(FactorySupplyAI.Job.SUPPLY, factory, source, input, take);
                    status.set(Core.bundle.format("feature.autoplay.status.supplying", input.localizedName));
                    return true;
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
                        ai.setJob(FactorySupplyAI.Job.COLLECT, factory, storage, output, 0);
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

    private boolean reassertSupplyJob(Unit unit, Seq<Building> cores, Seq<Building> containers,
            Seq<Building> factories) {

        if (ai.job() != FactorySupplyAI.Job.SUPPLY || ai.targetFactory() == null || ai.targetItem() == null
                || Time.time - ai.jobStartTick() >= STICKY_JOB_TIMEOUT
                || !isFactoryStillValid(ai.targetFactory(), factories)
                || !needsInput(ai.targetFactory(), ai.targetItem(), unit)) {
            return false;
        }

        Building source = findWithdrawStorage(ai.targetFactory(), ai.targetItem(), unit, cores, containers);
        if (source == null) {
            return false;
        }

        int take = planSupplyTake(unit, source, factories, factories.indexOf(ai.targetFactory(), true),
                ai.targetFactory(), ai.targetItem());
        if (take <= 0) {
            return false;
        }

        ai.setJob(FactorySupplyAI.Job.SUPPLY, ai.targetFactory(), source, ai.targetItem(), take);
        status.set(Core.bundle.format("feature.autoplay.status.supplying", ai.targetItem().localizedName));
        return true;
    }

    private boolean reassertCollectJob(Unit unit, Seq<Building> cores, Seq<Building> containers,
            Seq<Building> factories) {

        if (ai.job() != FactorySupplyAI.Job.COLLECT || ai.targetFactory() == null || ai.targetItem() == null
                || Time.time - ai.jobStartTick() >= STICKY_JOB_TIMEOUT
                || !isFactoryStillValid(ai.targetFactory(), factories)
                || !outputProducts(ai.targetFactory()).contains(ai.targetItem())
                || storedOf(ai.targetFactory(), ai.targetItem()) <= 0) {
            return false;
        }

        Building dest = findDepositStorage(ai.targetFactory(), ai.targetItem(),
                Math.min(storedOf(ai.targetFactory(), ai.targetItem()), unit.type.itemCapacity),
                unit, cores, containers);
        if (dest == null) {
            return false;
        }

        ai.setJob(FactorySupplyAI.Job.COLLECT, ai.targetFactory(), dest, ai.targetItem(), 0);
        status.set(Core.bundle.format("feature.autoplay.status.collecting", ai.targetItem().localizedName));
        return true;
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public void buildSettings(AutoplayFeature feature) {
        column().props(growX(), gap(unit(1))).children(() -> {
            checkbox(Core.bundle.get("feature.autoplay.settings.factory-supply.conservative"), conservative.signal())
                    .props(growX());
            text(Core.bundle.get("feature.autoplay.settings.factory-supply.conservative.description"))
                    .props(color(WebStyles.Colors.GHOST_FG), growX(), left(), textWrap());

            text(Core.bundle.get("feature.autoplay.settings.factory-supply.filter"))
                    .props(color(WebStyles.Colors.GHOST_FG), growX(), left());

            row().props(growX(), gap(unit(1))).children(() -> {
                button(Core.bundle.get("feature.autoplay.settings.factory-supply.select-all"), this::selectAll)
                        .props(style(WebStyles.secondary()), height(unit(9)), prefHeight(unit(9)), growX());
                button(Core.bundle.get("feature.autoplay.settings.factory-supply.clear-all"), this::clearAll)
                        .props(style(WebStyles.secondary()), height(unit(9)), prefHeight(unit(9)), growX());
            });

            wrap().props(growX(), gap(unit(1))).children(() -> {
                for (Block block : eligibleBlocks()) {
                    Readable<Boolean> checked = disabledBlocks.signal()
                            .map(seq -> seq == null || !seq.contains(block.name));

                    button(() -> setBlockEnabled(block, !Boolean.TRUE.equals(checked.peek())))
                            .props(checked(checked), style(WebStyles.filterChipText()), paddingX(unit(2)), height(unit(8)), prefHeight(unit(8)))
                            .children(() -> {
                                icon(new TextureRegionDrawable(block.uiIcon)).props(size(unit(4)), prefWidth(unit(4)), prefHeight(unit(4)));
                                text(block.localizedName)
                                        .props(color(checked.map(c -> Boolean.TRUE.equals(c) ? Color.white : Color.gray)));
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
        private int targetAmount;
        private Job job = Job.SUPPLY;
        private @Nullable Building lastFactory;

        private int tourStops = 0;
        private @Nullable Item tourItem;
        private @Nullable Building tourFactory;

        private float cooldownTimer = 0f;
        private float pendingTimer = 0f;
        private boolean actionPending = false;
        private int prevAmount = 0;
        private @Nullable Item pendingItem;

        private @Nullable String currentKey;
        private float jobStartTick = 0f;

        private static String posOf(@Nullable Building building) {
            return building == null ? "-" : building.tileX() + "," + building.tileY();
        }

        public void setJob(Job job, @Nullable Building factory, @Nullable Building storage, @Nullable Item item,
                int amount) {
            this.job = job;
            this.targetFactory = factory;
            this.targetStorage = storage;
            this.targetItem = item;
            this.targetAmount = amount;
            if (factory != null) {
                this.lastFactory = factory;
            }
            if ((job == Job.COLLECT || job == Job.DELIVER_TO_FACTORY) && factory != null) {
                if (tourItem == null || tourItem != item) {
                    tourItem = item;
                    tourStops = 1;
                    tourFactory = factory;
                } else if (tourFactory != factory) {
                    tourStops++;
                    tourFactory = factory;
                }
            }
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

        public int tourStops() {
            return tourStops;
        }

        public Job job() {
            return job;
        }

        public @Nullable Building targetFactory() {
            return targetFactory;
        }

        public @Nullable Item targetItem() {
            return targetItem;
        }

        public float jobStartTick() {
            return jobStartTick;
        }

        public @Nullable Item tourItem() {
            return tourItem;
        }

        public void resetTour() {
            tourStops = 0;
            tourItem = null;
            tourFactory = null;
        }

        private void snapshot(Unit unit) {
            prevAmount = unit.stack.amount;
            pendingItem = unit.stack.item;
        }

        private void markActed(Unit unit) {
            actionPending = true;
            pendingTimer = PENDING_TIMEOUT;
            cooldownTimer = actionCooldown();
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
                actionPending = false;
                pendingTimer = 0f;
                return;
            }
            pendingTimer -= Time.delta;
            if (pendingTimer <= 0f) {
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

            if (unit.stack.amount > 0 && unit.stack.item != null) {
                Item held = unit.stack.item;
                if (factory != null && factory.isValid() && !factory.dead()) {
                    boolean factoryWants = false;
                    try {
                        factoryWants = factory.acceptStack(held, 1, unit) > 0;
                    } catch (Throwable ignored) {
                    }
                    if (factoryWants) {
                        moveTo(factory, Vars.itemTransferRange * ENGAGE_RANGE_FACTOR);
                        tryTransfer(unit, factory);
                        return;
                    }
                }

                if (job == Job.COLLECT && factory != null && factory.isValid() && !factory.dead()
                        && item != null && unit.stack.item == item) {
                    int room = unit.type.itemCapacity - unit.stack.amount;
                    int stored = 0;
                    try {
                        stored = factory.items != null ? factory.items.get(item) : 0;
                    } catch (Throwable ignored) {
                    }
                    if (Math.min(stored, room) > 0) {
                        moveTo(factory, Vars.itemTransferRange * ENGAGE_RANGE_FACTOR);
                        tryRequest(unit, factory, item, Math.min(stored, room));
                        return;
                    }
                }
                Building deposit = storage;
                if (deposit == null || !deposit.isValid() || deposit.dead()) {
                    deposit = findAnyDepositForHeld(unit, held);
                }
                if (deposit != null) {
                    moveTo(deposit, Vars.itemTransferRange * ENGAGE_RANGE_FACTOR);
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
                    moveTo(storage, Vars.itemTransferRange * ENGAGE_RANGE_FACTOR);
                    int available = 0;
                    try {
                        available = storage.items != null ? storage.items.get(item) : 0;
                    } catch (Throwable ignored) {
                    }
                    int planned = targetAmount > 0 ? targetAmount : unit.type.itemCapacity;
                    int request = Math.min(available, Math.min(unit.type.itemCapacity, planned));
                    if (request > 0) {
                        tryRequest(unit, storage, item, request);
                    }
                    break;
                case COLLECT:
                    if (factory == null || !factory.isValid()) {
                        return;
                    }
                    moveTo(factory, Vars.itemTransferRange * ENGAGE_RANGE_FACTOR);
                    int stored = 0;
                    try {
                        stored = factory.items != null ? factory.items.get(item) : 0;
                    } catch (Throwable ignored) {
                    }
                    int take = Math.min(stored, unit.type.itemCapacity - unit.stack.amount);
                    if (take > 0) {
                        tryRequest(unit, factory, item, take);
                    }
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
