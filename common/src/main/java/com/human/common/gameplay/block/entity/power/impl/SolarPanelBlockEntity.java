package com.human.common.gameplay.block.entity.power.impl;

import com.human.common.gameplay.block.entity.power.PowerNodeBlockEntity;
import com.human.common.gameplay.block.power.PowerMode;
import com.human.common.gameplay.power.PowerNode;
import com.human.common.registry.init.HumanBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tier-1 solar. Output per the Sep 22/24 rulings (1 unit = 1 FE):
 * <ul>
 * <li>OFF (no light) - night, or no sky over the panel: 0</li>
 * <li>LOW (red) - dawn/dusk, or any daytime rain: {@value #LOW_OUTPUT}</li>
 * <li>MED (yellow) - clear day: {@value #MED_OUTPUT}</li>
 * <li>MAX (green) - clear sky in the five hours around noon, 10:00 to 14:59: {@value #MAX_OUTPUT}</li>
 * </ul>
 * ⚠ SENSED EVERY {@value #SENSE_INTERVAL_TICKS} TICKS, NOT EVERY TICK. The band is worked out from sky, time and
 * weather on that cadence and cached; the grid reads the cached number every tick for free, and the light (a block
 * state) changes at most once per interval, so a field of a hundred panels costs a hundred block-state compares every
 * two seconds and nothing else.
 */
public class SolarPanelBlockEntity extends PowerNodeBlockEntity implements PowerNode.PowerProducer {

    /**
     * ⭐ [stated] Sep 25, derived from measuring the competition rather than guessing: Oritech's Big Solar Panel makes
     * 256 over a 2x3 footprint, and ours occupies 2 blocks - about a quarter of the space - so a quarter of the output
     * is the like-for-like figure. 64 is MED, with the bands either side of it.
     * <p>
     * ⚠ That puts us at roughly 32 FE/t per block at med and 48 at max, against Oritech's 43 - near parity, but reached
     * in 2 blocks with no machine core, where every Oritech machine needs at least one core block that generates
     * nothing. So matching their density means beating them in practice, which is the intent.
     * </p>
     */
    public static final long LOW_OUTPUT = 32;

    public static final long MED_OUTPUT = 64;

    public static final long MAX_OUTPUT = 96;

    private static final int SENSE_INTERVAL_TICKS = 40;

    private static final long DAY_LENGTH = 24000L;

    /** Minecraft's clock: 0 = 06:00. Daylight runs from ~23000 (05:00) round to ~13000 (19:00). */
    private static final long SUNRISE = 23000L;

    private static final long SUNSET = 13000L;

    /** The first and last daylight hour are dawn and dusk. */
    private static final long DAWN_END = 0L; // 06:00

    private static final long DUSK_START = 12000L; // 18:00

    /** [stated] "the 5 hours around noon: 2 leading up to it, noon itself and the two hours after" = 10:00-14:59. */
    private static final long PEAK_START = 4000L;

    private static final long PEAK_END = 9000L;

    private long cachedOutput;

    private PowerMode cachedMode = PowerMode.OFF;

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(HumanBlockEntityTypes.SOLAR_PANEL.get(), pos, state);
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
        var abovePos = blockPos.above();

        if (!level.canSeeSky(abovePos) || level.getBrightness(LightLayer.SKY, abovePos) == 0) {
            return PowerMode.OFF;
        }

        var time = level.getDayTime() % DAY_LENGTH;
        var isDaylight = time >= SUNRISE || time < SUNSET;

        if (!isDaylight) {
            return PowerMode.OFF;
        }

        if (level.isRaining()) {
            return PowerMode.LOW; // thunder is rain too
        }

        var isDawnOrDusk = time >= SUNRISE || time >= DUSK_START;

        if (isDawnOrDusk) {
            return PowerMode.LOW;
        }

        if (time >= PEAK_START && time < PEAK_END) {
            return PowerMode.MAX;
        }

        return PowerMode.MED;
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

    /**
     * [stated] Oct 22 - an array of panels needs ONE cable, not a cable under every panel: a panel joins the panels
     * touching its four sides directly. Cables still connect on the bottom only, so a cable run into a panel's side
     * does nothing, as before.
     */
    @Override
    public boolean joinsNeighbour(Direction direction, com.human.common.gameplay.block.entity.power.PowerNodeBlockEntity neighbour) {
        return direction.getAxis().isHorizontal() && neighbour instanceof SolarPanelBlockEntity;
    }
}
