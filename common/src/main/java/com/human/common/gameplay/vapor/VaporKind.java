package com.human.common.gameplay.vapor;

import com.human.common.gameplay.machine.VaporizerMachineLogic;
import com.human.common.registry.init.HumanChemicals;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The five vapors. [stated] Oct 17: the vaporizer turns each liquid into its tank, and the distributor and the chem
 * thrower run on those tanks - the distributor also straight from the liquid (1 bucket = 1 tank = 1,000 vapor).
 * <ul>
 * <li>CRYO - cryofluid / Cryo Tank</li>
 * <li>INFERNIUM - lava / Infernium Tank</li>
 * <li>ACIDIUM, NETHER_ACIDIUM, IRRADIUM - the three xeno acids / their tanks (locked behind avp_alien, see
 * {@link com.human.common.gameplay.acid.AcidAvailability})</li>
 * </ul>
 * ⭐ ONE PLACE says which liquid and which tank make which vapor. The liquid side reads
 * {@link VaporizerMachineLogic#tankFor}, so the vaporizer and the distributor cannot drift apart.
 * <p>
 * The colours are the tanks' own stripes and bodies (the brightest body shade of each tank, sampled from the art) -
 * they colour the placeholder fog and the chem spray until the real vapor visuals are made.
 * </p>
 */
public enum VaporKind {

    CRYO("cryo", 0x1025DE, HumanChemicals.CRYO_TANK::get),
    INFERNIUM("infernium", 0xD5570F, HumanChemicals.INFERNIUM_TANK::get),
    ACIDIUM("acidium", 0x98CD2A, HumanChemicals.ACIDIUM_TANK::get),
    NETHER_ACIDIUM("nether_acidium", 0x1670C6, HumanChemicals.NETHER_ACIDIUM_TANK::get),
    IRRADIUM("irradium", 0x66EDE4, HumanChemicals.IRRADIUM_TANK::get);

    /** [stated] One tank = one fuel tank's worth = 1,000 uses (the flamethrower's 1,000 rounds). */
    public static final int PER_TANK = 1000;

    /** [stated] 1 bucket = 1,000 vapor (the vaporizer's 1 bucket = 1 tank). So 1 mB = 1 vapor. */
    public static final int PER_BUCKET = PER_TANK;

    private static final VaporKind[] VALUES = values();

    private final String id;

    private final int colour;

    private final Supplier<Item> tank;

    VaporKind(String id, int colour, Supplier<Item> tank) {
        this.id = id;
        this.colour = colour;
        this.tank = tank;
    }

    public String id() {
        return id;
    }

    /** 0xRRGGBB. */
    public int colour() {
        return colour;
    }

    public Item tank() {
        return tank.get();
    }

    public String translationKey() {
        return "vapor.avp_human." + id;
    }

    /** The vapor a filled tank holds, or null for anything else (including the empty aluminum tank). */
    public static @Nullable VaporKind fromTank(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        for (var kind : VALUES) {
            if (stack.is(kind.tank())) {
                return kind;
            }
        }

        return null;
    }

    /**
     * The vapor a liquid makes, or null for a liquid that makes none. ⭐ Read from the vaporizer's own table
     * ({@link VaporizerMachineLogic#tankFor}) - the liquid that fills a Cryo Tank there is the liquid that makes cryo
     * here.
     */
    public static @Nullable VaporKind fromFluid(@Nullable Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            return null;
        }

        var tank = VaporizerMachineLogic.tankFor(fluid);

        if (tank == null) {
            return null;
        }

        for (var kind : VALUES) {
            if (kind.tank() == tank) {
                return kind;
            }
        }

        return null;
    }

    /** By saved id; null for an unknown or empty one, so a bad tag reads back as "no vapor" rather than failing. */
    public static @Nullable VaporKind byId(@Nullable String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }

        for (var kind : VALUES) {
            if (kind.id.equals(id)) {
                return kind;
            }
        }

        return null;
    }

    /** For menu data: -1 for none. */
    public static @Nullable VaporKind byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : null;
    }
}
