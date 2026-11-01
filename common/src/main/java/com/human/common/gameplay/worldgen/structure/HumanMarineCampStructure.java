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

public class HumanMarineCampStructure extends JigsawBackedStructure {

    public static final MapCodec<HumanMarineCampStructure> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter((structure) -> structure.startPool)
        )
            .apply(instance, HumanMarineCampStructure::new)
    );

    public static final String NAME = "marine_camp_grass";

    public HumanMarineCampStructure(
        Structure.StructureSettings structureSettings,
        Holder<StructureTemplatePool> startPool
    ) {
        super(
            structureSettings,
            startPool,
            Optional.empty(),
            1,
            ConstantHeight.of(VerticalAnchor.absolute(-1)),
            false,
            Optional.of(Heightmap.Types.WORLD_SURFACE_WG),
            116,
            List.of(),
            DimensionPadding.ZERO,
            LiquidSettings.APPLY_WATERLOGGING
        );
    }

    @Override
    protected @NotNull Optional<GenerationStub> findGenerationPoint(@NotNull GenerationContext generationContext) {
        // [stated] Oct 4: the whole site must be dry, not just one corner block.
        if (!StructureUtil.isSiteDry(generationContext)) {
            return Optional.empty();
        }

        // [stated] Oct 28 - no hanging off cliffs or floating: the whole footprint must be within 8 blocks of level,
        // and the structure sits at the footprint's median ground height so the terrain is blended (filled under,
        // carved above) evenly around it. See SiteFit.
        return super.findGenerationPoint(generationContext)
            .flatMap(stub -> com.human.common.gameplay.worldgen.structure.util.SiteFit.fit(generationContext, stub, MAX_RELIEF, true));
    }

    /** The most the ground may rise across the footprint before the site is refused, in blocks. */
    private static final int MAX_RELIEF = 8;

    @Override
    public @NotNull StructureType<?> type() {
        return HumanStructureTypes.MARINE_CAMP.get();
    }
}
