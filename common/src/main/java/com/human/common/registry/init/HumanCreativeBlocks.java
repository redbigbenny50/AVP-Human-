package com.human.common.registry.init;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.block.creative.CreativeBatteryBlock;
import com.human.common.gameplay.block.creative.CreativeBatteryBlockEntity;
import com.human.common.gameplay.block.creative.CreativeBlockItem;
import com.human.common.gameplay.block.creative.CreativeFluidTankBlock;
import com.human.common.gameplay.block.creative.CreativeFluidTankBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * [stated] Oct 20 - the creative test blocks: the Creative Battery (always full, never fills) and the Creative Fluid
 * Tank (endless supply and void of one chosen fluid). The Creative Generator is the old infinite power generator,
 * renamed - it stays registered in HumanBlocks under its old id so placed ones keep working. All three are creative
 * only: no recipes, magenta art, and a "Creative only" tooltip (CreativeBlockItem).
 */
public final class HumanCreativeBlocks {

    private static final BLibRegistry<Block> BLOCKS = Human.MOD.registries().create(BuiltInRegistries.BLOCK);

    private static final BLibRegistry<Item> ITEMS = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    private static final BLibRegistry<BlockEntityType<?>> BLOCK_ENTITY_TYPES = Human.MOD.registries()
        .create(BuiltInRegistries.BLOCK_ENTITY_TYPE);

    public static final BLibHolder<Block> CREATIVE_BATTERY = BLOCKS.createHolder(
        "creative_battery",
        () -> new CreativeBatteryBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK))
    );

    public static final BLibHolder<Block> CREATIVE_FLUID_TANK = BLOCKS.createHolder(
        "creative_fluid_tank",
        () -> new CreativeFluidTankBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion())
    );

    public static final BLibHolder<Item> CREATIVE_BATTERY_ITEM = ITEMS.createHolder(
        "creative_battery",
        () -> new CreativeBlockItem(CREATIVE_BATTERY.get(), new Item.Properties().rarity(Rarity.EPIC))
    );

    public static final BLibHolder<Item> CREATIVE_FLUID_TANK_ITEM = ITEMS.createHolder(
        "creative_fluid_tank",
        () -> new CreativeBlockItem(CREATIVE_FLUID_TANK.get(), new Item.Properties().rarity(Rarity.EPIC))
    );

    public static final BLibHolder<BlockEntityType<CreativeBatteryBlockEntity>> CREATIVE_BATTERY_BLOCK_ENTITY = BLOCK_ENTITY_TYPES
        .createHolder(
            "creative_battery",
            () -> BlockEntityType.Builder.of(CreativeBatteryBlockEntity::new, CREATIVE_BATTERY.get()).build(null)
        );

    public static final BLibHolder<BlockEntityType<CreativeFluidTankBlockEntity>> CREATIVE_FLUID_TANK_BLOCK_ENTITY = BLOCK_ENTITY_TYPES
        .createHolder(
            "creative_fluid_tank",
            () -> BlockEntityType.Builder.of(CreativeFluidTankBlockEntity::new, CREATIVE_FLUID_TANK.get()).build(null)
        );

    private HumanCreativeBlocks() {}

    public static void initialize() {
        BLOCKS.registerAll();
        ITEMS.registerAll();
        BLOCK_ENTITY_TYPES.registerAll();
    }
}
