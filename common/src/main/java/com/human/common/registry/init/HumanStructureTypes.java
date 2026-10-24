package com.human.common.registry.init;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.worldgen.structure.HumanCommunicationsOutpostStructure;
import com.human.common.gameplay.worldgen.structure.HumanMarineCampStructure;
import com.human.common.gameplay.worldgen.structure.HumanMobileLabStructure;
import com.human.common.gameplay.worldgen.structure.HumanMunitionsOutpostStructure;
import com.human.common.gameplay.worldgen.structure.HumanSupplyOutpostBadlandsStructure;
import com.human.common.gameplay.worldgen.structure.HumanSupplyOutpostDesertStructure;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public class HumanStructureTypes {

    private static final BLibRegistry<StructureType<?>> REGISTRY = Human.MOD.registries().create(BuiltInRegistries.STRUCTURE_TYPE);

    /** Pool element types live in their own registry; registered from the same initialize() so nothing else changes. */
    private static final BLibRegistry<net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType<?>> POOL_ELEMENT_REGISTRY =
        Human.MOD.registries().create(BuiltInRegistries.STRUCTURE_POOL_ELEMENT);

    /** Structure processor types, in their own registry, registered from the same initialize(). */
    private static final BLibRegistry<net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType<?>> PROCESSOR_REGISTRY =
        Human.MOD.registries().create(BuiltInRegistries.STRUCTURE_PROCESSOR);

    /** [stated] Oct 23 - edge trees keep their branches and canopy over a mining camp's open ground. */
    public static final BLibHolder<net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType<com.human.common.gameplay.worldgen.structure.MiningCampEdgeTreesProcessor>> MINING_CAMP_EDGE_TREES =
        PROCESSOR_REGISTRY.createHolder(
            com.human.common.gameplay.worldgen.structure.MiningCampEdgeTreesProcessor.NAME,
            () -> () -> com.human.common.gameplay.worldgen.structure.MiningCampEdgeTreesProcessor.CODEC
        );

    /** [stated] Oct 23 - a single pool element whose ground is not its bottom block (the mining camps). */
    public static final BLibHolder<net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType<com.human.common.gameplay.worldgen.structure.GroundedSinglePoolElement>> GROUNDED_SINGLE_POOL_ELEMENT =
        POOL_ELEMENT_REGISTRY.createHolder(
            com.human.common.gameplay.worldgen.structure.GroundedSinglePoolElement.NAME,
            () -> () -> com.human.common.gameplay.worldgen.structure.GroundedSinglePoolElement.CODEC
        );

    public static final BLibHolder<StructureType<HumanCommunicationsOutpostStructure>> COMMUNICATIONS_OUTPOST = create(
        HumanCommunicationsOutpostStructure.NAME,
        HumanCommunicationsOutpostStructure.CODEC
    );

    public static final BLibHolder<StructureType<HumanMarineCampStructure>> MARINE_CAMP = create(
        HumanMarineCampStructure.NAME,
        HumanMarineCampStructure.CODEC
    );

    /** [stated] Oct 23 - the swamp and jungle mining camps: shallow water allowed, deep water refused. */
    public static final BLibHolder<StructureType<com.human.common.gameplay.worldgen.structure.HumanMiningCampStructure>> MINING_CAMP =
        create(
            com.human.common.gameplay.worldgen.structure.HumanMiningCampStructure.NAME,
            com.human.common.gameplay.worldgen.structure.HumanMiningCampStructure.CODEC
        );

    public static final BLibHolder<StructureType<HumanMobileLabStructure>> MOBILE_LAB = create(
        HumanMobileLabStructure.NAME,
        HumanMobileLabStructure.CODEC
    );

    public static final BLibHolder<StructureType<HumanMunitionsOutpostStructure>> MUNITIONS_OUTPOST = create(
        HumanMunitionsOutpostStructure.NAME,
        HumanMunitionsOutpostStructure.CODEC
    );

    public static final BLibHolder<StructureType<HumanSupplyOutpostBadlandsStructure>> SUPPLY_OUTPOST_BADLANDS = create(
        HumanSupplyOutpostBadlandsStructure.NAME,
        HumanSupplyOutpostBadlandsStructure.CODEC
    );

    public static final BLibHolder<StructureType<HumanSupplyOutpostDesertStructure>> SUPPLY_OUTPOST_DESERT = create(
        HumanSupplyOutpostDesertStructure.NAME,
        HumanSupplyOutpostDesertStructure.CODEC
    );

    private static <T extends Structure> BLibHolder<StructureType<T>> create(String path, MapCodec<T> codec) {
        return REGISTRY.createHolder(path, () -> () -> codec);
    }

    public static void initialize() {
        REGISTRY.registerAll();
        POOL_ELEMENT_REGISTRY.registerAll();
        PROCESSOR_REGISTRY.registerAll();
    }
}
