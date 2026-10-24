package com.human.common.gameplay.vapor;

import com.blib.api.common.block.v1.BlockBreakProgressManager;
import com.human.common.gameplay.acid.AcidKind;
import com.human.common.gameplay.acid.AcidLiquidBlock;
import com.human.common.gameplay.distributor.DistributorRoom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a vapor does to BLOCKS. [stated] Oct 18: "the distributors vapor doesnt affect blocks but the chem throwers does
 * as its more concentrated" - so only the chem thrower's blast and its lingering cloud call this; the distributor's fog
 * never does.
 * <p>
 * Each vapor does to the blocks it touches what its liquid does to the block it rests on ([stated] "just like their
 * liquids ... its the blocks the vapor is touching"):
 * </p>
 * <ul>
 * <li><b>CRYO</b> - [stated] Oct 18: turns water to ice and lava to obsidian; and, as cryofluid does, puts fires
 * out.</li>
 * <li><b>INFERNIUM</b> - sets fire wherever fire can stand in it: on solid ground, beside anything flammable, through
 * grass and plants. Obeys the doFireTick game rule, as lava does.</li>
 * <li><b>ACIDIUM</b> - eats the block, at the acid's own bite (avp_alien's crack progress, its immunity tags, composite
 * lasting three times as long).</li>
 * <li><b>NETHER ACIDIUM</b> - eats it and starts fires as it eats.</li>
 * <li><b>IRRADIUM</b> - freezes water and lava exactly as cryo does ([stated] Oct 18), and otherwise eats the block -
 * what it eats through turns to blue ice (nether resin to netherrack), as the irradiated acid does.</li>
 * </ul>
 * <h2>HOW IT TOUCHES - and why it costs little</h2> The caller picks a few points inside the vapor each time
 * ({@value #BLAST_SAMPLES_PER_TICK} a tick for the blast); each point that is in open space touches ONE of the six
 * blocks around it, chosen at random. So the surfaces the vapor sits against get worked on, more where it is thick
 * (each touch lands with a chance equal to the vapor's strength there), and nothing behind them. ⚡ A handful of block
 * reads a tick per firing player; never a chunk load.
 */
public final class VaporBlockEffects {

    /** Points sampled per tick of the blast. Raising it melts and burns faster. */
    public static final int BLAST_SAMPLES_PER_TICK = 4;

    /** Points sampled each time the lingering cloud acts (every 5 ticks). */
    public static final int CLOUD_SAMPLES = 2;

    /** Chance a touch of infernium sets the spot alight, at full strength (raised from 0.25 on Oct 19). */
    private static final float INFERNIUM_IGNITE_CHANCE = 0.5F;

    private static final Direction[] DIRECTIONS = Direction.values();

    private static final TagKey<Block> ACID_RESISTANT_3X = TagKey.create(
        Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath("avp_human", "acid_resistant_3x")
    );

    private static final float ACID_RESISTANT_3X_FACTOR = 3.0F;

    private static final ResourceLocation BURN_SOUND = ResourceLocation.fromNamespaceAndPath("avp_alien", "block.acid.burn");

    private VaporBlockEffects() {}

    /**
     * One touch: the vapor at {@code cell} (an open block inside it) touching a random neighbour.
     *
     * @param strength 0-1 - the vapor's strength at that point; the touch lands with that chance
     */
    public static void touch(ServerLevel level, VaporKind kind, BlockPos cell, float strength, RandomSource random) {
        touch(level, kind, cell, strength, random, 1.0F);
    }

    /**
     * As above, with the chem thrower's POWER potency ([stated] Oct 18 - 25 % a level): the acids eat that much faster
     * and infernium catches that much more readily.
     */
    public static void touch(ServerLevel level, VaporKind kind, BlockPos cell, float strength, RandomSource random, float potency) {
        if (strength <= 0.0F || random.nextFloat() >= strength || !level.isLoaded(cell)) {
            return;
        }

        var cellState = level.getBlockState(cell);

        // The vapor only sits in open space.
        if (!DistributorRoom.isOpen(level, cell, cellState)) {
            return;
        }

        var touched = cell.relative(DIRECTIONS[random.nextInt(DIRECTIONS.length)]);

        if (!level.isLoaded(touched)) {
            return;
        }

        var touchedState = level.getBlockState(touched);

        switch (kind) {
            case CRYO -> {
                if (!freezeFluid(level, cell, cellState) && !freezeFluid(level, touched, touchedState)) {
                    extinguish(level, cell, cellState, touched, touchedState);
                }
            }
            case INFERNIUM -> {
                // [stated] Oct 20 - infernium is a burning source: resin it reaches burns to basalt, in its own shape.
                if (!com.human.compatibility.avp_alien.HumanResinConversion.burn(level, touched, touchedState)) {
                    ignite(level, cell, cellState, touchedState, random, potency);
                }
            }
            case ACIDIUM -> bite(level, AcidKind.XENO, cell, touched, touchedState, random, potency);
            case NETHER_ACIDIUM -> bite(level, AcidKind.NETHER, cell, touched, touchedState, random, potency);
            case IRRADIUM -> {
                // [stated] whatever freezing cryo does irradium does too - and it still eats like the acid it is.
                if (!freezeFluid(level, cell, cellState) && !freezeFluid(level, touched, touchedState)) {
                    bite(level, AcidKind.IRRADIATED, cell, touched, touchedState, random, potency);
                }
            }
        }
    }

    /**
     * [stated] Oct 18 - the freeze: water to ice, lava to obsidian. A still SOURCE of lava becomes obsidian and FLOWING
     * lava cobblestone, as when water meets lava; only still water freezes (moving water is left, as Frost Walker
     * leaves it). Shared with the irradiated acid liquid. Server side; never loads a chunk.
     *
     * @return whether it froze something
     */
    public static boolean freezeFluid(ServerLevel level, BlockPos pos, BlockState state) {
        // [stated] Oct 20 - avp_human's cold freezes nether resin to netherrack, in its own shape, like powder snow.
        if (com.human.compatibility.avp_alien.HumanResinConversion.freeze(level, pos, state)) {
            return true;
        }

        Block result = null;

        if (state.is(Blocks.WATER) && state.getFluidState().isSource()) {
            result = Blocks.ICE;
        } else if (state.is(Blocks.LAVA)) {
            result = state.getFluidState().isSource() ? Blocks.OBSIDIAN : Blocks.COBBLESTONE;
        }

        if (result == null) {
            return false;
        }

        level.setBlockAndUpdate(pos, result.defaultBlockState());
        // Vanilla's fizz: the sound and smoke of lava meeting water.
        level.levelEvent(1501, pos, 0);
        return true;
    }

    /** Cryofluid puts fires out - the fire in the vapor, or the one it touches. */
    private static void extinguish(ServerLevel level, BlockPos cell, BlockState cellState, BlockPos touched, BlockState touchedState) {
        if (cellState.getBlock() instanceof BaseFireBlock) {
            level.removeBlock(cell, false);
        } else if (touchedState.getBlock() instanceof BaseFireBlock) {
            level.removeBlock(touched, false);
        }
    }

    /**
     * [stated] Oct 19 - "infernium doesnt seem to ignite as much as its vapor should". It used to light only the open
     * space right beside something FLAMMABLE, so across ordinary ground - grass blocks, dirt, stone, a field of tall
     * grass - it lit almost nothing. Now it sets fire wherever fire could stand in the vapor, like a fire charge: on
     * any solid ground under it or beside anything flammable, and it burns through grass, flowers and other plants in
     * its way. On ground that cannot burn the fire flickers out by itself, as vanilla fire does; on wood and leaves it
     * spreads. Still obeys doFireTick, and still only the chem thrower's vapor touches blocks.
     */
    private static void ignite(
        ServerLevel level,
        BlockPos cell,
        BlockState cellState,
        BlockState touchedState,
        RandomSource random,
        float potency
    ) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)) {
            return;
        }

        // Open air, or a plant the fire would burn straight through (grass, flowers, ferns, dead bushes).
        var burnable = cellState.isAir() || cellState.canBeReplaced() && cellState.getFluidState().isEmpty();

        if (!burnable || cellState.getBlock() instanceof BaseFireBlock) {
            return;
        }

        if (random.nextFloat() >= Math.min(1.0F, INFERNIUM_IGNITE_CHANCE * potency)) {
            return;
        }

        var fire = BaseFireBlock.getState(level, cell);

        if (fire.canSurvive(level, cell)) {
            level.setBlockAndUpdate(cell, fire);
        }
    }

    /** The acid's bite (AcidLiquidBlock.bite), on the block the vapor touches. */
    private static void bite(
        ServerLevel level,
        AcidKind kind,
        BlockPos cell,
        BlockPos touched,
        BlockState target,
        RandomSource random,
        float potency
    ) {
        // Something solid, never a fluid, and something this strain can eat.
        if (target.isAir() || !target.getFluidState().isEmpty() || target.getCollisionShape(level, touched).isEmpty()) {
            return;
        }

        if (!kind.canEat(target)) {
            return;
        }

        var amount = AcidLiquidBlock.BITE_DAMAGE * potency;

        if (target.is(ACID_RESISTANT_3X)) {
            amount /= ACID_RESISTANT_3X_FACTOR;
        }

        var result = BlockBreakProgressManager.damage(level, touched, amount);

        if (result == BlockBreakProgressManager.Result.NOT_DAMAGED) {
            return;
        }

        if (result == BlockBreakProgressManager.Result.DAMAGED && kind == AcidKind.NETHER && level.getBlockState(cell).isAir()) {
            var fire = BaseFireBlock.getState(level, cell);

            if (fire.canSurvive(level, cell)) {
                level.setBlockAndUpdate(cell, fire);
            }
        }

        if (result == BlockBreakProgressManager.Result.DESTROYED && kind == AcidKind.IRRADIATED) {
            var remains = AcidKind.frozenRemains(target);

            if (remains != null && level.getBlockState(touched).isAir()) {
                level.setBlockAndUpdate(touched, remains);
            }
        }

        if (random.nextInt(10) == 0) {
            BuiltInRegistries.SOUND_EVENT.getOptional(BURN_SOUND)
                .ifPresent(sound -> level.playSound(null, cell, sound, SoundSource.BLOCKS, 1.0F, 1.0F));
        }
    }
}
