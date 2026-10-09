package com.human.common.registry.init.block;

import com.blib.api.common.block.v1.BlockPropertyBuilder;
import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.block.AshBlock;
import com.human.common.gameplay.block.CryofluidLiquidBlock;
import com.human.common.gameplay.block.HoneyLiquidBlock;
import com.human.common.gameplay.block.LatexBlock;
import com.human.common.gameplay.block.LatexCauldronBlock;
import com.human.common.gameplay.block.LatexLiquidBlock;
import com.human.common.gameplay.block.LatexTapBlock;
import com.human.common.gameplay.block.LithiumBlock;
import com.human.common.gameplay.block.RadiatedBlock;
import com.human.common.gameplay.block.property.HumanBlockProperties;
import com.human.common.registry.init.HumanFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Supplier;

public class CoreBlocks {

    public static final BLibRegistry<Block> REGISTRY = Human.MOD.registries().create(BuiltInRegistries.BLOCK);

    public static final BLibHolder<Block> ALUMINUM_BLOCK = create("aluminum_block", HumanBlockProperties.ALUMINUM);

    public static final BLibHolder<Block> ASH_BLOCK = create(
        "ash_block",
        () -> new AshBlock(HumanBlockProperties.ASH_BLOCK.build())
    );

    public static final BLibHolder<LiquidBlock> LATEX = create(
        "latex",
        // ⚠ Water's own properties, copied wholesale. A liquid block needs liquid() and noLootTable(), which
        // BlockPropertyBuilder does not expose - copying water is both shorter and guaranteed correct.
        () -> new LatexLiquidBlock(
            (FlowingFluid) HumanFluids.LATEX.get(),
            BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).mapColor(MapColor.SAND)
        )
    );

    /** Liquid honey, placed. Water's properties for the same reason as latex. */
    public static final BLibHolder<LiquidBlock> HONEY = create(
        "honey",
        () -> new HoneyLiquidBlock(
            (FlowingFluid) HumanFluids.HONEY.get(),
            BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).mapColor(MapColor.COLOR_ORANGE)
        )
    );

    /**
     * [stated] Oct 1: placed cryofluid. Water's properties like the other liquids; it gives off no light, unlike lava.
     */
    public static final BLibHolder<LiquidBlock> CRYOFLUID = create(
        "cryofluid",
        () -> new CryofluidLiquidBlock(
            (FlowingFluid) HumanFluids.CRYOFLUID.get(),
            BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).mapColor(MapColor.ICE)
        )
    );

    /**
     * [stated] Oct 1: a hay-bale of kelp. 9 kelp make one, it cooks into a dried kelp block, composts at 100% per layer
     * and grinds into a slimeball - an alternative slime source for cryogel. Hay bale's properties, so it is as easy to
     * break and as soft to land on.
     */
    public static final BLibHolder<Block> KELP_BLOCK = create(
        "kelp_block",
        () -> new net.minecraft.world.level.block.RotatedPillarBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.HAY_BLOCK).mapColor(MapColor.COLOR_GREEN)
        )
    );

    public static final BLibHolder<LatexTapBlock> LATEX_TAP = create(
        "latex_tap",
        () -> new LatexTapBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).noOcclusion())
    );

    public static final BLibHolder<LatexCauldronBlock> LATEX_CAULDRON = create(
        "latex_cauldron",
        () -> new LatexCauldronBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON))
    );

    public static final BLibHolder<Block> LATEX_BLOCK = create(
        "latex_block",
        () -> new LatexBlock(HumanBlockProperties.LATEX_BLOCK.build())
    );

    public static final BLibHolder<Block> RUBBER_BLOCK = create(
        "rubber_block",
        () -> new Block(HumanBlockProperties.RUBBER_BLOCK.build())
    );

    public static final BLibHolder<Block> AUTUNITE_BLOCK = create(
        "autunite_block",
        () -> new RadiatedBlock(HumanBlockProperties.AUTUNITE_ORE.build())
    );

    public static final BLibHolder<Block> AUTUNITE_ORE = create(
        "autunite_ore",
        () -> new RadiatedBlock(HumanBlockProperties.AUTUNITE_ORE.build())
    );

    public static final BLibHolder<Block> BAUXITE_ORE = create("bauxite_ore", HumanBlockProperties.BAUXITE_ORE);

    public static final BLibHolder<Block> BRASS_BLOCK = create("brass_block", HumanBlockProperties.BRASS);

    public static final BLibHolder<Block> DEEPSLATE_TITANIUM_ORE = create(
        "deepslate_titanium_ore",
        HumanBlockProperties.DEEPSLATE_TITANIUM_ORE
    );

    public static final BLibHolder<Block> DEEPSLATE_ZINC_ORE = create(
        "deepslate_zinc_ore",
        HumanBlockProperties.DEEPSLATE_ZINC_ORE
    );

    public static final BLibHolder<Block> GALENA_ORE = create("galena_ore", HumanBlockProperties.GALENA_ORE);

    public static final BLibHolder<Block> LEAD_BLOCK = create("lead_block", HumanBlockProperties.LEAD);

    public static final BLibHolder<Block> LITHIUM_BLOCK = create(
        "lithium_block",
        () -> new LithiumBlock(HumanBlockProperties.LITHIUM_ORE.build())
    );

    public static final BLibHolder<Block> LITHIUM_ORE = create(
        "lithium_ore",
        () -> new LithiumBlock(HumanBlockProperties.LITHIUM_ORE.build())
    );

    public static final BLibHolder<Block> MONAZITE_ORE = create("monazite_ore", HumanBlockProperties.MONAZITE_ORE);

    public static final BLibHolder<Block> RAW_BAUXITE_BLOCK = create("raw_bauxite_block", HumanBlockProperties.BAUXITE_ORE);

    public static final BLibHolder<Block> RAW_GALENA_BLOCK = create("raw_galena_block", HumanBlockProperties.GALENA_ORE);

    public static final BLibHolder<Block> RAW_MONAZITE_BLOCK = create(
        "raw_monazite_block",
        HumanBlockProperties.MONAZITE_ORE
    );

    public static final BLibHolder<Block> RAW_TITANIUM_BLOCK = create(
        "raw_titanium_block",
        HumanBlockProperties.TITANIUM_ORE
    );

    public static final BLibHolder<Block> RAW_ZINC_BLOCK = create("raw_zinc_block", HumanBlockProperties.ZINC_ORE);

    public static final BLibHolder<Block> SILICA_GRAVEL = create(
        "silica_gravel",
        () -> new ColoredFallingBlock(new ColorRGBA(-8356741), BlockBehaviour.Properties.ofFullCopy(Blocks.GRAVEL))
    );

    public static final BLibHolder<Block> SILICON_BLOCK = create(
        // TODO: Change this to "silicon_block" with 0.2.0.
        "raw_silica_block",
        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GRAVEL).strength(0.9F))
    );

    public static final BLibHolder<Block> TRINITITE_BLOCK = create(
        "trinitite_block",
        () -> new RadiatedBlock(HumanBlockProperties.TRINITITE.build())
    );

    public static final BLibHolder<Block> URANIUM_BLOCK = create(
        "uranium_block",
        () -> new RadiatedBlock(HumanBlockProperties.URANIUM.build())
    );

    public static final BLibHolder<Block> ZINC_BLOCK = create("zinc_block", HumanBlockProperties.ZINC);

    public static final BLibHolder<Block> ZINC_ORE = create("zinc_ore", HumanBlockProperties.ZINC_ORE);

    private static BLibHolder<Block> create(String path, BlockPropertyBuilder blockPropertyBuilder) {
        return create(path, () -> new Block(blockPropertyBuilder.build()));
    }

    private static <T extends Block> BLibHolder<T> create(String path, Supplier<T> blockSupplier) {
        return REGISTRY.createHolder(path, blockSupplier);
    }

    public static void initialize() {
        REGISTRY.registerAll();
    }
}
