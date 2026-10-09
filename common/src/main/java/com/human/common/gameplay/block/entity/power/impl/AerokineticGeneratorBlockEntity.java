package com.human.common.gameplay.block.entity.power.impl;

import com.human.common.gameplay.block.entity.power.PowerNodeBlockEntity;
import com.human.common.gameplay.block.power.PowerMode;
import com.human.common.gameplay.power.PowerNode;
import com.human.common.registry.init.HumanBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Wind. The old version multiplied the block's Y by a biome and a weather factor, which gave a continuous number with
 * no bands; the generator now reports one of the four power modes like every other tier-1 producer.
 * <ul>
 * <li>OFF - no sky above the rotor. A buried turbine makes nothing.</li>
 * <li>LOW (red) - low ground or sheltered air: {@value #LOW_OUTPUT}</li>
 * <li>MED (yellow) - high and open, or exposed terrain (mountains, hills, ocean, beach): {@value #MED_OUTPUT}</li>
 * <li>MAX (green) - very high altitude, or a thunderstorm anywhere above the low line: {@value #MAX_OUTPUT}</li>
 * </ul>
 * ⚠ The two heights are CONFIGURABLE-BY-EDIT constants rather than a magic pair buried in a formula, because the right
 * numbers are a matter of play: {@value #MED_HEIGHT} and {@value #MAX_HEIGHT}.
 * <p>
 * Sensed every {@value #SENSE_INTERVAL_TICKS} ticks and cached, so a field of turbines costs almost nothing per tick.
 * </p>
 */
public class AerokineticGeneratorBlockEntity extends PowerNodeBlockEntity implements PowerNode.PowerProducer {

    /**
     * ⭐ [stated] Sep 25: the turbine "should put itself inbetween water wheel and solar".
     * <p>
     * Solar is 32/64/96 over 2 blocks; a hydro section is 64/128/256. The turbine sits between them at each band, for
     * about the same footprint as solar.
     * </p>
     * <p>
     * ⚠ MAX IS THE MOST GENEROUS RELATIVE STEP, and on purpose: solar reaches its peak every clear noon, and a
     * waterwheel holds max as long as the river runs, but a turbine only reaches {@value #MAX_OUTPUT} at
     * {@value #MAX_HEIGHT} blocks up or in a thunderstorm. A ceiling you rarely touch can afford to be higher.
     * </p>
     */
    public static final long LOW_OUTPUT = 48;

    public static final long MED_OUTPUT = 96;

    public static final long MAX_OUTPUT = 160;

    public static final int MED_HEIGHT = 80;

    public static final int MAX_HEIGHT = 160;

    private static final int SENSE_INTERVAL_TICKS = 40;

    private long cachedOutput;

    private PowerMode cachedMode = PowerMode.OFF;

    public AerokineticGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(HumanBlockEntityTypes.AEROKINETIC_GENERATOR.get(), pos, state);
    }

    @Override
    public void serverTick(Level level, BlockPos blockPos, BlockState blockState) {
        super.serverTick(level, blockPos, blockState);

        if ((level.getGameTime() + blockPos.hashCode()) % SENSE_INTERVAL_TICKS != 0) {
            return;
        }

        var mode = sense(level, blockPos);

        if (mode != cachedMode) {
            cachedMode = mode;
            cachedOutput = switch (mode) {
                case OFF -> 0;
                case LOW -> LOW_OUTPUT;
                case MED -> MED_OUTPUT;
                case MAX -> MAX_OUTPUT;
            };
        }

        if (blockState.getValue(PowerMode.PROPERTY) != mode) {
            level.setBlock(blockPos, blockState.setValue(PowerMode.PROPERTY, mode), 3);
        }
    }

    private static PowerMode sense(Level level, BlockPos blockPos) {
        if (!level.canSeeSky(blockPos.above())) {
            return PowerMode.OFF;
        }

        var height = blockPos.getY();

        if (height >= MAX_HEIGHT) {
            return PowerMode.MAX;
        }

        var biome = level.getBiome(blockPos);
        var exposed = biome.is(BiomeTags.IS_MOUNTAIN)
            || biome.is(BiomeTags.IS_HILL)
            || biome.is(BiomeTags.IS_OCEAN)
            || biome.is(BiomeTags.IS_BEACH);

        // A storm is the one thing that can push a turbine to its ceiling without altitude - but it still has to be
        // somewhere the wind reaches, so the low line applies.
        // ⚠⚠ A THUNDERSTORM IS MAX AT ANY HEIGHT, PROVIDED THE TURBINE CAN SEE SKY.
        //
        // [stated] Sep 26: "set weather to thunder and aerokinetic generator is still low power should be max."
        // This used to require height >= MED_HEIGHT as well, so a turbine at ordinary ground level - where almost
        // every one of these actually gets built - ignored the storm completely and sat at LOW. The sky check at the
        // top of this method is the only condition that should gate it: a sheltered turbine feels nothing, an
        // exposed one is in the full force of the weather whatever its altitude.
        if (level.isThundering()) {
            return PowerMode.MAX;
        }

        if (height >= MED_HEIGHT || exposed) {
            return PowerMode.MED;
        }

        return PowerMode.LOW;
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
     * [stated] Oct 5: cables connect bottom only - it took a cable on any side. Cables, the grid walk and the wiring
     * tool all read this one rule.
     */
    @Override
    public boolean canConnectOn(Direction direction) {
        return direction == Direction.DOWN;
    }
}
