package mindustrytool.features.autoplay.tasks;

import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.blocks.production.BeamDrill;
import mindustry.world.blocks.production.Drill;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.production.Separator;
import mindustry.world.blocks.production.WallCrafter;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.blocks.storage.StorageBlock;
import mindustry.world.blocks.storage.Unloader;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.blocks.units.UnitAssembler;
import mindustry.world.blocks.units.UnitAssembler.AssemblerUnitPlan;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.blocks.units.UnitFactory.UnitPlan;
import mindustry.world.consumers.Consume;
import mindustry.world.consumers.ConsumeItemFilter;
import mindustry.world.consumers.ConsumeItems;

public final class TransportItemHelper {

    private TransportItemHelper() {
    }

    public static boolean isGeneralStorage(@Nullable Block block) {
        if (block == null) {
            return false;
        }
        return block instanceof CoreBlock || block instanceof StorageBlock || block instanceof Unloader;
    }

    public static Seq<Item> validReceiveItems(@Nullable Block block) {
        Seq<Item> result = new Seq<>();
        if (block == null || !block.hasItems || Vars.content == null || Vars.content.items() == null) {
            return result;
        }

        if (isGeneralStorage(block)) {
            for (Item item : Vars.content.items()) {
                if (item != null && item.unlockedNow()) {
                    result.add(item);
                }
            }
            return result;
        }

        if (block instanceof ItemTurret) {
            ItemTurret turret = (ItemTurret) block;
            if (turret.ammoTypes != null) {
                for (Item item : turret.ammoTypes.keys()) {
                    if (item != null && item.unlockedNow() && !result.contains(item)) {
                        result.add(item);
                    }
                }
            }
        }

        if (block.consumers != null) {
            for (Consume consume : block.consumers) {
                if (consume instanceof ConsumeItems) {
                    ItemStack[] stacks = ((ConsumeItems) consume).items;
                    if (stacks != null) {
                        for (ItemStack stack : stacks) {
                            if (stack != null && stack.item != null && stack.item.unlockedNow() && !result.contains(stack.item)) {
                                result.add(stack.item);
                            }
                        }
                    }
                } else if (consume instanceof ConsumeItemFilter) {
                    ConsumeItemFilter filter = (ConsumeItemFilter) consume;
                    for (Item item : Vars.content.items()) {
                        if (item != null && item.unlockedNow() && filter.filter.get(item) && !result.contains(item)) {
                            result.add(item);
                        }
                    }
                }
            }
        }

        if (block instanceof UnitFactory) {
            UnitFactory factory = (UnitFactory) block;
            if (factory.plans != null) {
                for (UnitPlan plan : factory.plans) {
                    if (plan != null && plan.requirements != null) {
                        for (ItemStack stack : plan.requirements) {
                            if (stack != null && stack.item != null && stack.item.unlockedNow() && !result.contains(stack.item)) {
                                result.add(stack.item);
                            }
                        }
                    }
                }
            }
        }

        if (block instanceof UnitAssembler) {
            UnitAssembler assembler = (UnitAssembler) block;
            if (assembler.plans != null) {
                for (AssemblerUnitPlan plan : assembler.plans) {
                    if (plan != null && plan.itemReq != null) {
                        for (ItemStack stack : plan.itemReq) {
                            if (stack != null && stack.item != null && stack.item.unlockedNow() && !result.contains(stack.item)) {
                                result.add(stack.item);
                            }
                        }
                    }
                }
            }
        }

        if (block instanceof Reconstructor) {
            Reconstructor reconstructor = (Reconstructor) block;
            if (reconstructor.requirements != null) {
                for (ItemStack stack : reconstructor.requirements) {
                    if (stack != null && stack.item != null && stack.item.unlockedNow() && !result.contains(stack.item)) {
                        result.add(stack.item);
                    }
                }
            }
        }

        if (block instanceof PowerGenerator) {
            for (Item item : Vars.content.items()) {
                if (item != null && item.unlockedNow() && block.consumesItem(item) && !result.contains(item)) {
                    result.add(item);
                }
            }
        }

        return result;
    }

    public static Seq<Item> validTransferItems(@Nullable Block block) {
        Seq<Item> result = new Seq<>();
        if (block == null || !block.hasItems || Vars.content == null || Vars.content.items() == null) {
            return result;
        }

        if (isGeneralStorage(block)) {
            for (Item item : Vars.content.items()) {
                if (item != null && item.unlockedNow()) {
                    result.add(item);
                }
            }
            return result;
        }

        if (block instanceof GenericCrafter) {
            GenericCrafter crafter = (GenericCrafter) block;
            if (crafter.outputItem != null && crafter.outputItem.item != null && crafter.outputItem.item.unlockedNow()) {
                result.add(crafter.outputItem.item);
            }
            if (crafter.outputItems != null) {
                for (ItemStack stack : crafter.outputItems) {
                    if (stack != null && stack.item != null && stack.item.unlockedNow() && !result.contains(stack.item)) {
                        result.add(stack.item);
                    }
                }
            }
        } else if (block instanceof Separator) {
            Separator separator = (Separator) block;
            if (separator.results != null) {
                for (ItemStack stack : separator.results) {
                    if (stack != null && stack.item != null && stack.item.unlockedNow() && !result.contains(stack.item)) {
                        result.add(stack.item);
                    }
                }
            }
        } else if (block instanceof WallCrafter) {
            WallCrafter wallCrafter = (WallCrafter) block;
            if (wallCrafter.output != null && wallCrafter.output.unlockedNow()) {
                result.add(wallCrafter.output);
            }
        } else if (block instanceof Drill || block instanceof BeamDrill) {
            for (Item item : Vars.content.items()) {
                if (item != null && item.unlockedNow() && !block.consumesItem(item)) {
                    result.add(item);
                }
            }
        } else {
            for (Item item : Vars.content.items()) {
                if (item != null && item.unlockedNow()) {
                    result.add(item);
                }
            }
        }

        return result;
    }
}
