package com.human.common.gameplay.block.entity.power.impl;

import com.human.common.gameplay.block.entity.power.PowerConsumerBlockEntity;
import com.human.common.registry.init.HumanBlockEntityTypes;
import com.human.compatibility.avp_alien.AVPAlien;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * [stated] Oct 20 - the resonator, redesigned: no longer a resin cleaner. "In lore certain frequencies could slow and
 * knock out xenos." While powered it holds a 32-block sphere in which xenos are slowed and, after 15 seconds, put to
 * sleep, and eggs cannot open - everything the xenos do is avp_alien's (com.alien.common.gameplay.resonance.Resonance);
 * this block only supplies the pulse.
 * <ul>
 * <li>500 FE a tick, from a cable UNDERNEATH it (its only power face), or wirelessly from a fuse panel it is bound to
 * with the wiring tool.</li>
 * <li>One pulse a second while powered. Unpowered (or without avp_alien) it does nothing at all; xenos it had put to
 * sleep wake about two seconds after the pulses stop.</li>
 * <li>Shift-right-click it with the wiring tool to see the sphere, through blocks, for a few seconds.</li>
 * </ul>
 */
public class ResonatorBlockEntity extends PowerConsumerBlockEntity {

    public static final long POWER_PER_TICK = 500L;

    /** The field's reach - kept equal to avp_alien's Resonance.RADIUS (the human side draws it without avp_alien). */
    public static final int RADIUS = 32;

    private static final int PULSE_INTERVAL_TICKS = 20;

    /**
     * Oct 21 - how long it keeps showing as running after power stops: a grid that is short for a tick or two must not
     * flicker the spin (or flood clients with block updates).
     */
    private static final int ACTIVE_GRACE_TICKS = 20;

    /** Server: the game time it was last powered (not saved - it only smooths the running state). */
    private long lastPoweredTick = Long.MIN_VALUE / 2;

    private final ResonatorAnimationDispatcher animationDispatcher;

    public ResonatorBlockEntity(BlockPos pos, BlockState blockState) {
        super(HumanBlockEntityTypes.RESONATOR.get(), pos, blockState);
        this.animationDispatcher = new ResonatorAnimationDispatcher();
    }

    @Override
    public long getRequestedPower() {
        return POWER_PER_TICK;
    }

    /** [stated] Oct 20 - "uses power from a cable underneath it" (or a fuse panel, which is wireless). */
    @Override
    public boolean canConnectOn(Direction direction) {
        return direction == Direction.DOWN;
    }

    /**
     * ⚠ Oct 21 - SERVER. These used to ask for the animation here, but the animation only plays on the CLIENT and the
     * request returns at once on the server - so it never spun. The server now only keeps the block's ACTIVE state
     * right (changed only when it actually flips, so a steady resonator sends nothing); every client sees that state
     * and {@link #clientTick} plays the matching animation.
     */
    @Override
    public void unpoweredTick(Level level, BlockPos blockPos, BlockState blockState) {
        if (level.getGameTime() - lastPoweredTick > ACTIVE_GRACE_TICKS) {
            setActive(level, blockPos, blockState, false);
        }
    }

    /** CLIENT, every tick: spin while running, wind down when not. Both requests are no-ops once already playing. */
    public void clientTick(BlockState state) {
        if (
            state.hasProperty(com.human.common.gameplay.block.power.ResonatorBlock.ACTIVE)
                && state.getValue(com.human.common.gameplay.block.power.ResonatorBlock.ACTIVE)
        ) {
            animationDispatcher.powered(this);
        } else {
            animationDispatcher.unpowered(this);
        }
    }

    private static void setActive(Level level, BlockPos pos, BlockState state, boolean active) {
        var property = com.human.common.gameplay.block.power.ResonatorBlock.ACTIVE;

        if (!state.hasProperty(property) || state.getValue(property) == active) {
            return;
        }

        // UPDATE_CLIENTS only: the same block in a new state keeps its block entity, and neighbours need nothing.
        level.setBlock(pos, state.setValue(property, active), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }

    @Override
    public void poweredTick(Level level, BlockPos blockPos, BlockState blockState) {
        lastPoweredTick = level.getGameTime();
        setActive(level, blockPos, blockState, true);

        // Offset by position so many resonators do not all pulse on the same tick.
        if ((level.getGameTime() + blockPos.asLong()) % PULSE_INTERVAL_TICKS != 0) {
            return;
        }

        if (level instanceof ServerLevel serverLevel && AVPAlien.MOD.isLoaded()) {
            com.human.compatibility.avp_alien.AlienResonanceBridge.pulse(serverLevel, blockPos);
        }
    }
}
