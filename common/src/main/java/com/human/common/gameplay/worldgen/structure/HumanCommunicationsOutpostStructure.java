package com.human.common.gameplay.worldgen.structure;

import com.blib.api.common.worldgen.v1.JigsawBackedStructure;
import com.human.common.registry.init.HumanStructureTypes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class HumanCommunicationsOutpostStructure extends JigsawBackedStructure {

    public static final MapCodec<HumanCommunicationsOutpostStructure> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter((structure) -> structure.startPool)
        )
            .apply(instance, HumanCommunicationsOutpostStructure::new)
    );

    public static final String NAME = "outpost_comm_bottom";

    public static final String NAME_TOP = "outpost_comm_top";

    public HumanCommunicationsOutpostStructure(
        StructureSettings structureSettings,
        Holder<StructureTemplatePool> startPool
    ) {
        super(
            structureSettings,
            startPool,
            Optional.empty(),
            4,
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
    public @NotNull StructureType<?> type() {
        return HumanStructureTypes.COMMUNICATIONS_OUTPOST.get();
    }

    /**
     * [stated] Oct 5: no structure of ours that is not meant to be flooded may generate in water - the whole site must
     * be dry (StructureUtil.isSiteDry samples a 3x3 grid across it). The marine camp and mobile lab already had this.
     */
    @Override
    protected @NotNull Optional<GenerationStub> findGenerationPoint(@NotNull GenerationContext generationContext) {
        if (!com.human.common.gameplay.worldgen.structure.util.StructureUtil.isSiteDry(generationContext)) {
            return Optional.empty();
        }

        return super.findGenerationPoint(generationContext);
    }
}
