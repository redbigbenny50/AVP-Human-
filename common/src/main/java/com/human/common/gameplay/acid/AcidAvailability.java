package com.human.common.gameplay.acid;

import com.human.common.registry.init.HumanChemicals;
import com.human.compatibility.avp_alien.AVPAlien;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

/**
 * [stated] Oct 17: everything made from xeno blood is LOCKED BEHIND avp_alien. Without it these are hidden from the
 * creative tabs and from every recipe viewer, and their recipes do not load, so nobody sees an item they could never
 * make in survival - they can only be had with commands, and then they work normally.
 * <ul>
 * <li>the acid tanks: acidium, nether acidium and irradium</li>
 * <li>the three acid buckets, and the three acid fluids themselves (a recipe viewer lists fluids too)</li>
 * <li>[stated] Oct 20 - the resonator: its whole purpose is avp_alien's xenos, so without avp_alien it is hidden the
 * same way (it simply does nothing if placed).</li>
 * </ul>
 * The press recipes that make the acids carry a mod-loaded condition in their JSON, so they never load without
 * avp_alien.
 */
public final class AcidAvailability {

    private AcidAvailability() {}

    public static boolean available() {
        return AVPAlien.MOD.isLoaded();
    }

    /** Every item and fluid id that is hidden when avp_alien is missing. */
    public static Set<ResourceLocation> lockedIds() {
        var ids = new HashSet<ResourceLocation>();

        ids.add(BuiltInRegistries.ITEM.getKey(HumanChemicals.ACIDIUM_TANK.get()));
        ids.add(BuiltInRegistries.ITEM.getKey(HumanChemicals.NETHER_ACIDIUM_TANK.get()));
        ids.add(BuiltInRegistries.ITEM.getKey(HumanChemicals.IRRADIUM_TANK.get()));
        ids.add(BuiltInRegistries.ITEM.getKey(com.human.common.registry.init.HumanBlocks.RESONATOR_BLOCK.get().asItem()));

        for (var kind : AcidKind.values()) {
            ids.add(BuiltInRegistries.ITEM.getKey(HumanChemicals.acidBucket(kind)));
            ids.add(BuiltInRegistries.FLUID.getKey(HumanChemicals.acid(kind)));
            ids.add(BuiltInRegistries.FLUID.getKey(HumanChemicals.flowingAcid(kind)));
        }

        return ids;
    }
}
