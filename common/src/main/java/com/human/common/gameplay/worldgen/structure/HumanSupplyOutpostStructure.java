package com.human.common.gameplay.worldgen.structure;

import com.blib.api.common.worldgen.v1.JigsawBackedStructure;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public abstract class HumanSupplyOutpostStructure extends JigsawBackedStructure {

    public HumanSupplyOutpostStructure(StructureSettings structureSettings, Holder<StructureTemplatePool> startPool) {
        super(
            structureSettings,
            startPool,
            Optional.empty(),
            1,
            ConstantHeight.of(VerticalAnchor.absolute(0)),
            false,
            Optional.of(Heightmap.Types.WORLD_SURFACE_WG),
            116,
            List.of(),
            DimensionPadding.ZERO,
            LiquidSettings.APPLY_WATERLOGGING
        );
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
