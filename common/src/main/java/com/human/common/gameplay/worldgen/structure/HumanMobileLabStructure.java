package com.human.common.gameplay.worldgen.structure;

import com.blib.api.common.worldgen.v1.JigsawBackedStructure;
import com.human.common.gameplay.worldgen.structure.util.StructureUtil;
import com.human.common.registry.init.HumanStructureTypes;
import com.human.compatibility.avp_alien.AVPAlien;
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

public class HumanMobileLabStructure extends JigsawBackedStructure {

    public static final MapCodec<HumanMobileLabStructure> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter((structure) -> structure.startPool)
        )
            .apply(instance, HumanMobileLabStructure::new)
    );

    public static final String NAME = "mobile_lab";

    public HumanMobileLabStructure(
        StructureSettings structureSettings,
        Holder<StructureTemplatePool> startPool
    ) {
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

    @Override
    protected @NotNull Optional<GenerationStub> findGenerationPoint(@NotNull GenerationContext generationContext) {
        if (!AVPAlien.MOD.isLoaded()) {
            return Optional.empty();
        }

        // [stated] Oct 4: the whole site must be dry, not just one corner block.
        if (!StructureUtil.isSiteDry(generationContext)) {
            return Optional.empty();
        }

        return super.findGenerationPoint(generationContext);
    }

    @Override
    public @NotNull StructureType<?> type() {
        return HumanStructureTypes.MOBILE_LAB.get();
    }
}
