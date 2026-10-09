package com.human.common.gameplay.block.entity.power.impl;

import com.human.common.gameplay.block.IndustrialFurnaceBlock;
import com.human.common.gameplay.block.entity.power.PowerNodeBlockEntity;
import com.human.common.gameplay.block.power.PowerMode;
import com.human.common.gameplay.power.PowerNode;
import com.human.common.registry.init.HumanBlockEntityTypes;
import com.human.common.registry.init.HumanBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.SmokerBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * Heat into power. [stated] "the thermal generator has 5 sides with plates and a wire connects to the top its power
 * depends on what side a heat source is touching, all 5 is max power."
 * <p>
 * ⚠⚠ THE TOP IS NOT READ. The old version swept all six neighbours; the top is where the cable lands, so a heat source
 * there now counts for nothing and only the four sides and the bottom - the five plated faces of the model - are
 * scanned. That is the whole point of the plates.
 * </p>
 * <p>
 * Each plate scores its neighbour from the heat table (lava {@value #LAVA_HEAT} down to a redstone torch), the scores
 * are summed, and the total is both the output and the band the light shows. Five lava plates come to
 * {@value #MAX_OUTPUT}, which is the ceiling; a ring of torches lights red and trickles, which is honest - the plate
 * COUNT alone would have shown green for five torches.
 * <p>
 * ⭐ [stated] Sep 24: "the thermal generator should be one of the highest generators at max." At {@value #MAX_OUTPUT} it
 * is the strongest SINGLE-BLOCK generator in the tier by a wide margin - more than double the aerokinetic's 125 - and
 * it runs flat out day and night in any weather, which none of the others do. What it costs is the build: five lava
 * source blocks held against its plates, carried or piped in, and kept there.
 * </p>
 * </p>
 * Sensed every {@value #SENSE_INTERVAL_TICKS} ticks and cached, and a neighbour change re-reads it at once, so lighting
 * a furnace beside one takes effect immediately.
 */
public class ThermoelectricGeneratorBlockEntity extends PowerNodeBlockEntity implements PowerNode.PowerProducer {

    /**
     * ⭐ [stated] Sep 25: "each panel heated should give 200 rf maxing at 1000" and "only certain things are counted as
     * a heat source though not their intensity".
     * <p>
     * ⚠⚠ THIS REPLACED A GRADED HEAT TABLE, AND THE FLAT RATE IS THE POINT. Output used to scale with what each
     * neighbour was - lava 64, a furnace 30, a torch 2 - which made a ring of torches a real, if feeble, generator and
     * made the number impossible to predict without reading the table. A face is now simply HOT or COLD: hot pays
     * {@value #HEAT_PER_PLATE}, cold pays nothing, and five hot faces is the tier ceiling of {@value #MAX_OUTPUT}.
     * </p>
     */
    public static final long HEAT_PER_PLATE = 200L;

    /**
     * ⚠⚠ THE FOUR SIDES AND THE BOTTOM - the plated faces. UP is the cable connection and is deliberately absent, so a
     * heat source on top counts for nothing. That is the whole point of the plates.
     */
    private static final Direction[] PLATED_FACES = {
        Direction.NORTH,
        Direction.EAST,
        Direction.SOUTH,
        Direction.WEST,
        Direction.DOWN
    };

    private static final int SENSE_INTERVAL_TICKS = 40;

    /** Five plated faces at {@value #HEAT_PER_PLATE}. The top of tier 1. */
    public static final long MAX_OUTPUT = 1_000L;

    /**
     * What counts as hot. [stated] "a camp fire would work, a fireblock would work, a furnace cooking would work, and
     * of course lava. So the torches counting doesnt."
     * <p>
     * ⚠ TORCHES, WALL TORCHES, REDSTONE TORCHES AND LANTERNS ARE DELIBERATELY ABSENT. They are light, not heat, and
     * they are cheap enough that counting them would make a free generator out of a decorated wall.
     * </p>
     * <p>
     * ⚠ A furnace only counts while it is actually LIT - an idle one is a cold box. That check happens in
     * {@link #isHot} because it needs the block state, not just the block.
     * </p>
     */
    private static final Set<Block> HOT_BLOCKS = Set.of(
        Blocks.LAVA,
        Blocks.LAVA_CAULDRON,
        Blocks.FIRE,
        Blocks.SOUL_FIRE,
        Blocks.CAMPFIRE,
        Blocks.SOUL_CAMPFIRE,
        Blocks.MAGMA_BLOCK
    );

    private long cachedOutput;

    private PowerMode cachedMode = PowerMode.OFF;

    public ThermoelectricGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(HumanBlockEntityTypes.THERMOELECTRIC_GENERATOR.get(), pos, state);
    }

    @Override
    public void serverTick(Level level, BlockPos blockPos, BlockState blockState) {
        super.serverTick(level, blockPos, blockState);

        if ((level.getGameTime() + blockPos.hashCode()) % SENSE_INTERVAL_TICKS != 0) {
            return;
        }

        var hotPlates = countHotPlates(level, blockPos);
        var heat = hotPlates * HEAT_PER_PLATE;
        // Bands follow the PLATE COUNT now, not a score: 5 is max, 3-4 med, 1-2 low.
        var mode = hotPlates >= PLATED_FACES.length
            ? PowerMode.MAX
            : hotPlates >= 3 ? PowerMode.MED : hotPlates >= 1 ? PowerMode.LOW : PowerMode.OFF;

        cachedOutput = heat;
        cachedMode = mode;

        if (blockState.getValue(PowerMode.PROPERTY) != mode) {
            level.setBlock(blockPos, blockState.setValue(PowerMode.PROPERTY, mode), 3);
        }
    }

    private static int countHotPlates(Level level, BlockPos blockPos) {
        var hot = 0;

        for (var face : PLATED_FACES) {
            if (isHot(level.getBlockState(blockPos.relative(face)))) {
                hot++;
            }
        }

        return hot;
    }

    /** Hot or cold - no grading. A lit furnace counts; an idle one does not. */
    private static boolean isHot(BlockState state) {
        if (HOT_BLOCKS.contains(state.getBlock())) {
            return true;
        }

        if (state.is(Blocks.FURNACE)) {
            return state.getValue(FurnaceBlock.LIT);
        }

        if (state.is(Blocks.BLAST_FURNACE)) {
            return state.getValue(BlastFurnaceBlock.LIT);
        }

        if (state.is(Blocks.SMOKER)) {
            return state.getValue(SmokerBlock.LIT);
        }

        return state.is(HumanBlocks.INDUSTRIAL_FURNACE.get()) && state.getValue(IndustrialFurnaceBlock.LIT);
    }

    public PowerMode getPowerMode() {
        return cachedMode;
    }

    @Override
    public long getAvailablePower() {
        return level == null || level.isClientSide ? 0 : cachedOutput;
    }

    @Override
    public long extractPower(long maxAmount) {
        return maxAmount;
    }

    /**
     * [stated] Oct 5: cables connect top only - it took a cable on any side. Cables, the grid walk and the wiring tool
     * all read this one rule.
     */
    @Override
    public boolean canConnectOn(Direction direction) {
        return direction == Direction.UP;
    }
}
