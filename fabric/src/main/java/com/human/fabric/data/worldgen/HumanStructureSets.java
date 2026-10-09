package com.human.fabric.data.worldgen;

import com.human.common.registry.key.HumanStructureKeys;
import com.human.common.registry.key.HumanStructureSetKeys;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

public class HumanStructureSets {

    /**
     * ⚠⚠ THE SALTS ARE NOT THE NUMBERS FROM THE STRUCTURE DATAPACK, AND THEY CANNOT BE. Four of the six salts in that
     * pack are larger than {@link Integer#MAX_VALUE} (up to 7,086,091,425). Vanilla parses this field with
     * {@code ExtraCodecs.NON_NEGATIVE_INT}, so an oversized literal truncates to a NEGATIVE int and the codec then
     * REJECTS the whole structure set - the structure would simply never generate, with only a line in the log to say
     * so. Each one here is the pack's value reduced modulo Integer.MAX_VALUE, which keeps it distinct and legal.
     * <p>
     * ⚠ THE SPACING AND SEPARATION ARE EXACTLY THE PACK'S. Those are the deliberate part - every structure used to sit
     * at 40/15, and the pack spreads them out by rarity (camps 50/30, outposts 80-120/45-75).
     * </p>
     * <p>
     * ⚠ CHANGING A SALT OR SPACING RE-ROLLS WHERE THESE GENERATE. Existing worlds keep whatever is already placed, but
     * ungenerated chunks will place them somewhere new.
     * </p>
     */
    public static void bootstrap(BootstrapContext<StructureSet> registry) {
        var structureLookup = registry.lookup(Registries.STRUCTURE);

        registry.register(
            HumanStructureSetKeys.COMMUNICATIONS_OUTPOST,
            new StructureSet(
                structureLookup.getOrThrow(HumanStructureKeys.COMMUNICATIONS_OUTPOST),
                new RandomSpreadStructurePlacement(90, 45, RandomSpreadType.TRIANGULAR, 48668024)
            )
        );

        registry.register(
            HumanStructureSetKeys.MARINE_CAMP_GRASS,
            new StructureSet(
                structureLookup.getOrThrow(HumanStructureKeys.MARINE_CAMP_GRASS),
                new RandomSpreadStructurePlacement(50, 30, RandomSpreadType.TRIANGULAR, 2031715575)
            )
        );

        registry.register(
            HumanStructureSetKeys.MOBILE_LAB,
            new StructureSet(
                structureLookup.getOrThrow(HumanStructureKeys.MOBILE_LAB),
                new RandomSpreadStructurePlacement(60, 25, RandomSpreadType.TRIANGULAR, 290743993)
            )
        );

        registry.register(
            HumanStructureSetKeys.MUNITIONS_OUTPOST,
            new StructureSet(
                structureLookup.getOrThrow(HumanStructureKeys.MUNITIONS_OUTPOST),
                new RandomSpreadStructurePlacement(80, 55, RandomSpreadType.TRIANGULAR, 893135114)
            )
        );

        registry.register(
            HumanStructureSetKeys.SUPPLY_OUTPOST_BADLANDS,
            new StructureSet(
                structureLookup.getOrThrow(HumanStructureKeys.SUPPLY_OUTPOST_BADLANDS),
                new RandomSpreadStructurePlacement(120, 75, RandomSpreadType.TRIANGULAR, 218145562)
            )
        );

        registry.register(
            HumanStructureSetKeys.SUPPLY_OUTPOST_DESERT,
            new StructureSet(
                structureLookup.getOrThrow(HumanStructureKeys.SUPPLY_OUTPOST_DESERT),
                new RandomSpreadStructurePlacement(120, 75, RandomSpreadType.TRIANGULAR, 643640484)
            )
        );
    }
}
