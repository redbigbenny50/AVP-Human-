package com.human.client.compat;

import com.human.common.gameplay.acid.AcidAvailability;
import com.human.common.gameplay.acid.AcidKind;
import com.human.common.gameplay.machine.VaporizerMachineLogic;
import com.human.common.registry.init.HumanChemicals;
import com.human.common.registry.init.HumanFluids;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

/**
 * One vaporizer conversion, for the recipe viewers: one bucket of a liquid plus one empty aluminum tank makes one
 * filled tank.
 * <p>
 * The vaporizer has no recipe files - its liquids are fixed in {@link VaporizerMachineLogic#tankFor}. This builds the
 * viewer's list from that SAME method, so JEI and REI can never show a conversion the machine does not do, or miss one
 * it does. The three acids are only listed when avp_alien is installed, the same rule that hides them elsewhere.
 * </p>
 */
public record VaporizingRecipe(
    Fluid liquid,
    Item tank
) {

    /** One vaporizer cycle: 10 seconds. */
    public static final int TIME_TICKS = 200;

    public static List<VaporizingRecipe> all() {
        var liquids = new ArrayList<Fluid>();
        liquids.add(HumanFluids.CRYOFLUID.get());
        liquids.add(Fluids.LAVA);

        if (AcidAvailability.available()) {
            liquids.add(HumanChemicals.acid(AcidKind.XENO));
            liquids.add(HumanChemicals.acid(AcidKind.NETHER));
            liquids.add(HumanChemicals.acid(AcidKind.IRRADIATED));
        }

        var recipes = new ArrayList<VaporizingRecipe>();

        for (var liquid : liquids) {
            var tank = VaporizerMachineLogic.tankFor(liquid);
            if (tank != null) {
                recipes.add(new VaporizingRecipe(liquid, tank));
            }
        }

        return recipes;
    }
}
