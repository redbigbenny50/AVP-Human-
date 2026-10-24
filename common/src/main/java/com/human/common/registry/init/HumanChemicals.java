package com.human.common.registry.init;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.acid.AcidFluidFactory;
import com.human.common.gameplay.acid.AcidKind;
import com.human.common.gameplay.acid.AcidLiquidBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;

/**
 * The chem line: [stated] Oct 17 - the three xeno acids (fluid, liquid block, bucket), and the gas tanks the vaporizer
 * fills for the chem thrower and the distributors.
 * <ul>
 * <li>Aluminum Tank - the empty tank, crafted like the fuel tank with no magma cream</li>
 * <li>Cryo Tank (cryofluid), Infernium Tank (lava)</li>
 * <li>Acidium Tank (xeno acid), Nether Acidium Tank (nether xeno acid), Irradium Tank (irradiated xeno acid) - locked
 * behind avp_alien, see {@link com.human.common.gameplay.acid.AcidAvailability}</li>
 * </ul>
 * [stated] None of the tanks stack, like the fuel tank (which stays the flamethrower's ammo, unchanged).
 * <p>
 * ⚠ ORDER: fluids, then blocks, then items - a liquid block and a bucket both take the fluid in their constructor.
 * </p>
 */
public final class HumanChemicals {

    private static final BLibRegistry<Fluid> FLUIDS = Human.MOD.registries().create(BuiltInRegistries.FLUID);

    private static final BLibRegistry<Block> BLOCKS = Human.MOD.registries().create(BuiltInRegistries.BLOCK);

    private static final BLibRegistry<Item> ITEMS = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    // ------------------------------------------------------------------------------------------------------------
    // The acids
    // ------------------------------------------------------------------------------------------------------------

    public static final BLibHolder<Fluid> XENO_ACID = FLUIDS.createHolder("xeno_acid", () -> AcidFluidFactory.source(AcidKind.XENO));

    public static final BLibHolder<Fluid> FLOWING_XENO_ACID = FLUIDS.createHolder(
        "flowing_xeno_acid",
        () -> AcidFluidFactory.flowing(AcidKind.XENO)
    );

    public static final BLibHolder<Fluid> NETHER_XENO_ACID = FLUIDS.createHolder(
        "nether_xeno_acid",
        () -> AcidFluidFactory.source(AcidKind.NETHER)
    );

    public static final BLibHolder<Fluid> FLOWING_NETHER_XENO_ACID = FLUIDS.createHolder(
        "flowing_nether_xeno_acid",
        () -> AcidFluidFactory.flowing(AcidKind.NETHER)
    );

    public static final BLibHolder<Fluid> IRRADIATED_XENO_ACID = FLUIDS.createHolder(
        "irradiated_xeno_acid",
        () -> AcidFluidFactory.source(AcidKind.IRRADIATED)
    );

    public static final BLibHolder<Fluid> FLOWING_IRRADIATED_XENO_ACID = FLUIDS.createHolder(
        "flowing_irradiated_xeno_acid",
        () -> AcidFluidFactory.flowing(AcidKind.IRRADIATED)
    );

    public static final BLibHolder<LiquidBlock> XENO_ACID_BLOCK = BLOCKS.createHolder(
        "xeno_acid",
        () -> new AcidLiquidBlock((FlowingFluid) XENO_ACID.get(), AcidKind.XENO, acidProperties(MapColor.COLOR_YELLOW))
    );

    public static final BLibHolder<LiquidBlock> NETHER_XENO_ACID_BLOCK = BLOCKS.createHolder(
        "nether_xeno_acid",
        () -> new AcidLiquidBlock((FlowingFluid) NETHER_XENO_ACID.get(), AcidKind.NETHER, acidProperties(MapColor.COLOR_BLUE))
    );

    public static final BLibHolder<LiquidBlock> IRRADIATED_XENO_ACID_BLOCK = BLOCKS.createHolder(
        "irradiated_xeno_acid",
        () -> new AcidLiquidBlock(
            (FlowingFluid) IRRADIATED_XENO_ACID.get(),
            AcidKind.IRRADIATED,
            acidProperties(MapColor.COLOR_CYAN)
        )
    );

    public static final BLibHolder<Item> XENO_ACID_BUCKET = ITEMS.createHolder(
        "xeno_acid_bucket",
        () -> new BucketItem(XENO_ACID.get(), bucketProperties())
    );

    public static final BLibHolder<Item> NETHER_XENO_ACID_BUCKET = ITEMS.createHolder(
        "nether_xeno_acid_bucket",
        () -> new BucketItem(NETHER_XENO_ACID.get(), bucketProperties())
    );

    public static final BLibHolder<Item> IRRADIATED_XENO_ACID_BUCKET = ITEMS.createHolder(
        "irradiated_xeno_acid_bucket",
        () -> new BucketItem(IRRADIATED_XENO_ACID.get(), bucketProperties())
    );

    // ------------------------------------------------------------------------------------------------------------
    // The tanks - none of them stack
    // ------------------------------------------------------------------------------------------------------------

    public static final BLibHolder<Item> ALUMINUM_TANK = tank("aluminum_tank");

    public static final BLibHolder<Item> CRYO_TANK = tank("cryo_tank");

    public static final BLibHolder<Item> INFERNIUM_TANK = tank("infernium_tank");

    public static final BLibHolder<Item> ACIDIUM_TANK = tank("acidium_tank");

    public static final BLibHolder<Item> NETHER_ACIDIUM_TANK = tank("nether_acidium_tank");

    public static final BLibHolder<Item> IRRADIUM_TANK = tank("irradium_tank");

    private HumanChemicals() {}

    private static BLibHolder<Item> tank(String id) {
        return ITEMS.createHolder(id, () -> new Item(new Item.Properties().stacksTo(1)));
    }

    /** Water's own liquid-block properties, as every avp_human liquid uses (liquid(), no loot table, replaceable). */
    private static BlockBehaviour.Properties acidProperties(MapColor color) {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).mapColor(color);
    }

    private static Item.Properties bucketProperties() {
        return new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1);
    }

    public static Fluid acid(AcidKind kind) {
        return switch (kind) {
            case XENO -> XENO_ACID.get();
            case NETHER -> NETHER_XENO_ACID.get();
            case IRRADIATED -> IRRADIATED_XENO_ACID.get();
        };
    }

    public static Fluid flowingAcid(AcidKind kind) {
        return switch (kind) {
            case XENO -> FLOWING_XENO_ACID.get();
            case NETHER -> FLOWING_NETHER_XENO_ACID.get();
            case IRRADIATED -> FLOWING_IRRADIATED_XENO_ACID.get();
        };
    }

    public static LiquidBlock acidBlock(AcidKind kind) {
        return switch (kind) {
            case XENO -> XENO_ACID_BLOCK.get();
            case NETHER -> NETHER_XENO_ACID_BLOCK.get();
            case IRRADIATED -> IRRADIATED_XENO_ACID_BLOCK.get();
        };
    }

    public static Item acidBucket(AcidKind kind) {
        return switch (kind) {
            case XENO -> XENO_ACID_BUCKET.get();
            case NETHER -> NETHER_XENO_ACID_BUCKET.get();
            case IRRADIATED -> IRRADIATED_XENO_ACID_BUCKET.get();
        };
    }

    public static void initialize() {
        FLUIDS.registerAll();
        BLOCKS.registerAll();
        ITEMS.registerAll();
    }
}
