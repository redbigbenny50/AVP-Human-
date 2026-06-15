package com.human.common.data.worldgen;

import com.blib.api.common.worldgen.v1.key.BLibStructureProcessorListKeys;
import com.human.mixin.MixinStructureTemplatePool_Accessor;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import java.util.ArrayList;

public class HumanVillageInjection {

    public static void addBuildingToPool(
        Registry<StructureTemplatePool> templatePoolRegistry,
        Registry<StructureProcessorList> processorListRegistry,
        ResourceLocation targetResourceLocation,
        ResourceLocation resourceLocation,
        int weight
    ) {
        var holderOptional = processorListRegistry.getHolder(BLibStructureProcessorListKeys.EMPTY);

        if (holderOptional.isEmpty()) {
            return;
        }

        var emptyProcessorList = holderOptional.get();
        var structureTemplatePool = templatePoolRegistry.get(targetResourceLocation);

        if (structureTemplatePool == null) {
            return;
        }

        var legacySinglePoolElement = SinglePoolElement.legacy(resourceLocation.toString(), emptyProcessorList)
            .apply(StructureTemplatePool.Projection.RIGID);
        var accessor = (MixinStructureTemplatePool_Accessor) structureTemplatePool;

        for (var i = 0; i < weight; i++) {
            accessor.avp_human$getTemplates().add(legacySinglePoolElement);
        }

        var listOfPieceEntries = new ArrayList<>(accessor.avp_human$getRawTemplates());

        listOfPieceEntries.add(new Pair<>(legacySinglePoolElement, weight));

        accessor.avp_human$setRawTemplates(listOfPieceEntries);
    }
}
