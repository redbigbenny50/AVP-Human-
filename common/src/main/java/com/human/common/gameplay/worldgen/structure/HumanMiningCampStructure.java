package com.human.common.gameplay.worldgen.structure;

import com.blib.api.common.worldgen.v1.JigsawBackedStructure;
import com.human.common.gameplay.worldgen.structure.util.StructureUtil;
import com.human.common.registry.init.HumanStructureTypes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * The swamp and jungle mining camps.
 * <p>
 * [stated] Oct 23 - the camps must keep out of lakes and rivers, but "swamps are normally 60% water". The marine camp's
 * whole-site dry check ({@link StructureUtil#isSiteDry}) refuses a site if ANY of its nine samples is water, which
 * would refuse almost every swamp. These camps use {@link StructureUtil#isSiteMostlyShallow} instead: shallow water
 * (marsh, puddles) is fine, deep water (a lake, river or ocean edge) is not.
 * </p>
 * <p>
 * One type for both camps - the start pool in each camp's structure file picks which camp it is. Everything else
 * matches what the camps were built for:
 * </p>
 * <ul>
 * <li>size 2: the camp, then one colonist piece on each of its colonist jigsaws;</li>
 * <li>start height 0 on WORLD_SURFACE_WG, with the camp files loaded through {@link GroundedSinglePoolElement} at a
 * ground level of 10: placement sets minY + 10 = the first free block, so the camp's grass (y 9 of the file) replaces
 * the world's top block. Over water that top block is the WATER SURFACE, so the camp's ground sits level with the water
 * around it and nothing flows onto it. The terrain blending reads the same ground, so the land around each camp is
 * shaped to the grass, not to the bottom of the file;</li>
 * <li>80 blocks from the centre, enough for the 32 x 38 camp and its colonists.</li>
 * </ul>
 */
public class HumanMiningCampStructure extends JigsawBackedStructure {

    public static final MapCodec<HumanMiningCampStructure> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter((structure) -> structure.startPool)
        )
            .apply(instance, HumanMiningCampStructure::new)
    );

    public static final String NAME = "mining_camp";

    public HumanMiningCampStructure(
        Structure.StructureSettings structureSettings,
        Holder<StructureTemplatePool> startPool
    ) {
        super(
            structureSettings,
            startPool,
            Optional.empty(),
            2,
            ConstantHeight.of(VerticalAnchor.absolute(0)),
            false,
            Optional.of(Heightmap.Types.WORLD_SURFACE_WG),
            80,
            List.of(),
            DimensionPadding.ZERO,
            LiquidSettings.APPLY_WATERLOGGING
        );
    }

    @Override
    protected @NotNull Optional<GenerationStub> findGenerationPoint(@NotNull GenerationContext generationContext) {
        if (!StructureUtil.isSiteMostlyShallow(generationContext)) {
            return Optional.empty();
        }

        return super.findGenerationPoint(generationContext);
    }

    @Override
    public @NotNull StructureType<?> type() {
        return HumanStructureTypes.MINING_CAMP.get();
    }
}
