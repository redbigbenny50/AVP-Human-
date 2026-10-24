package com.human.common.gameplay.acid;

import com.blib.api.common.block.v1.BlockBreakProgressManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.jetbrains.annotations.NotNull;

/**
 * A poured xeno acid. [stated] Oct 17: it eats blocks exactly like the blood it comes from, does to anything standing
 * in it what that blood does, and a poured source fades after 20 seconds (see {@link AcidSources}).
 * <h2>EATING BLOCKS - avp_alien's own rule</h2> Every acid block, source or flowing, bites the block it rests on every
 * {@value #BITE_INTERVAL_TICKS} ticks for 0.2 damage a tick - the blood's AcidBlockDamageUtil, with the same 4-tick
 * batching and the same BLib crack progress, so a block melts under the fluid exactly as fast as under a bleed:
 * <ul>
 * <li>avp_alien's immunity tag for the strain is respected, and avp_human's {@code acid_resistant_3x} blocks
 * (composite) take a third of the damage, lasting three times as long.</li>
 * <li>NETHER acid starts fires as it eats - the blood puts fire on top of what it bites; a fluid fills that space, so
 * the fire takes the air beside it instead.</li>
 * <li>IRRADIATED acid turns what it eats through to blue ice (nether resin to netherrack; resin veins and webs just
 * go). [stated] Oct 18: it also freezes what cryo freezes - water beside it to ice, lava to obsidian (flowing lava to
 * cobblestone).</li>
 * </ul>
 * <h2>COST</h2> One scheduled block tick per acid block every 4 ticks, one block read under it, and a crack update only
 * when it actually bites. Nothing at all while nothing is poured.
 */
public class AcidLiquidBlock extends LiquidBlock {

    /** avp_alien's SERVER_BITE_INTERVAL_TICKS: bites in batches of four ticks. */
    public static final int BITE_INTERVAL_TICKS = 4;

    /** avp_alien's damage per tick (multiplier 1), times the batch. */
    public static final float BITE_DAMAGE = 0.2F * BITE_INTERVAL_TICKS;

    /** [stated] Oct 3 (avp_alien): composite lasts three times as long under acid. */
    private static final TagKey<Block> ACID_RESISTANT_3X = TagKey.create(
        Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath("avp_human", "acid_resistant_3x")
    );

    private static final float ACID_RESISTANT_3X_FACTOR = 3.0F;

    private static final ResourceLocation BURN_SOUND = ResourceLocation.fromNamespaceAndPath("avp_alien", "block.acid.burn");

    private final AcidKind kind;

    public AcidLiquidBlock(FlowingFluid fluid, AcidKind kind, Properties properties) {
        super(fluid, properties);
        this.kind = kind;
    }

    public AcidKind kind() {
        return kind;
    }

    private static boolean isSource(BlockState state) {
        return state.getValue(LEVEL) == 0;
    }

    @Override
    protected void onPlace(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState oldState,
        boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!(level instanceof ServerLevel server)) {
            return;
        }

        // A new source - poured, or a bucket emptied into its own flow - gets the full 20 seconds.
        if (isSource(state) && (!oldState.is(this) || !isSource(oldState))) {
            AcidSources.get(server).start(pos);
        }

        level.scheduleTick(pos, this, BITE_INTERVAL_TICKS);
    }

    @Override
    protected void onRemove(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState newState,
        boolean movedByPiston
    ) {
        // The source was taken away by anything other than fading (a bucket, a block placed into it): forget it, so a
        // later pour here starts afresh. A flowing block changing level keeps nothing here.
        if (level instanceof ServerLevel server && isSource(state) && !(newState.is(this) && isSource(newState))) {
            AcidSources.get(server).forget(pos);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (isSource(state)) {
            // Twice as fast touching water, as the blood ages twice as fast in it.
            var aged = touchesWater(level, pos) ? BITE_INTERVAL_TICKS * 2 : BITE_INTERVAL_TICKS;

            if (AcidSources.get(level).age(pos, aged)) {
                // Faded. Turning the source to air lets everything that flowed from it drain away on its own.
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                return;
            }
        }

        bite(level, pos, random);

        // [stated] Oct 18 - the irradiated blood freezes what cryo freezes: water beside it to ice, lava to obsidian.
        if (kind == AcidKind.IRRADIATED) {
            freezeNeighbours(level, pos);
        }

        level.scheduleTick(pos, this, BITE_INTERVAL_TICKS);
    }

    /** Six neighbour reads every bite interval, only for the irradiated acid, never loading a chunk. */
    private static void freezeNeighbours(ServerLevel level, BlockPos pos) {
        for (var direction : Direction.values()) {
            var side = pos.relative(direction);

            if (level.isLoaded(side)) {
                com.human.common.gameplay.vapor.VaporBlockEffects.freezeFluid(level, side, level.getBlockState(side));
            }
        }
    }

    private static boolean touchesWater(ServerLevel level, BlockPos pos) {
        for (var direction : Direction.values()) {
            if (direction != Direction.DOWN && level.getFluidState(pos.relative(direction)).is(FluidTags.WATER)) {
                return true;
            }
        }

        return false;
    }

    /** One bite at the block this acid rests on - avp_alien's AcidBlockDamageUtil.damageBlock. */
    private void bite(ServerLevel level, BlockPos pos, RandomSource random) {
        var below = pos.below();
        var target = level.getBlockState(below);

        // Resting on something solid, never on another fluid, and something this strain can eat.
        if (target.isAir() || !target.getFluidState().isEmpty() || target.getCollisionShape(level, below).isEmpty()) {
            return;
        }

        if (!kind.canEat(target)) {
            return;
        }

        var amount = BITE_DAMAGE;

        if (target.is(ACID_RESISTANT_3X)) {
            amount /= ACID_RESISTANT_3X_FACTOR;
        }

        var result = BlockBreakProgressManager.damage(level, below, amount);

        if (result == BlockBreakProgressManager.Result.NOT_DAMAGED) {
            return;
        }

        if (result == BlockBreakProgressManager.Result.DAMAGED && kind == AcidKind.NETHER) {
            igniteBeside(level, pos, random);
        }

        if (result == BlockBreakProgressManager.Result.DESTROYED && kind == AcidKind.IRRADIATED) {
            var remains = AcidKind.frozenRemains(target);

            if (remains != null && level.getBlockState(below).isAir()) {
                level.setBlockAndUpdate(below, remains);
            }
        }

        // avp_alien plays its burn sound on about one bite in ten.
        if (random.nextInt(10) == 0) {
            BuiltInRegistries.SOUND_EVENT.getOptional(BURN_SOUND)
                .ifPresent(sound -> level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F));
        }
    }

    /** Nether acid: a fire in the air beside it, where a fire can stand. */
    private static void igniteBeside(ServerLevel level, BlockPos pos, RandomSource random) {
        var side = pos.relative(Direction.Plane.HORIZONTAL.getRandomDirection(random));

        if (level.getBlockState(side).isAir()) {
            var fire = BaseFireBlock.getState(level, side);

            if (fire.canSurvive(level, side)) {
                level.setBlockAndUpdate(side, fire);
            }
        }
    }

    @Override
    protected void entityInside(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Entity entity) {
        super.entityInside(state, level, pos, entity);

        if (level instanceof ServerLevel server && isCountedBlock(level, pos, entity)) {
            AcidEffects.tick(kind, server, entity);
        }
    }

    /**
     * ⚠ {@code entityInside} runs for EVERY block an entity overlaps, so standing in acid two blocks deep would count
     * twice a tick. Only one block counts: the one at the feet, or, when the feet are out of it, the one at the eyes.
     */
    private boolean isCountedBlock(Level level, BlockPos pos, Entity entity) {
        var feet = entity.blockPosition();

        if (level.getBlockState(feet).is(this)) {
            return pos.equals(feet);
        }

        return pos.equals(BlockPos.containing(entity.getEyePosition()));
    }

    /** The blood's look: smoke rising off normal and nether acid, frost off irradiated. Client only. */
    @Override
    public void animateTick(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        super.animateTick(state, level, pos, random);

        if (random.nextInt(3) != 0) {
            return;
        }

        var height = state.getFluidState().getHeight(level, pos);
        var particle = kind == AcidKind.IRRADIATED ? ParticleTypes.SNOWFLAKE : ParticleTypes.SMOKE;

        level.addParticle(
            particle,
            pos.getX() + random.nextDouble(),
            pos.getY() + height + 0.05D,
            pos.getZ() + random.nextDouble(),
            0.0D,
            0.02D,
            0.0D
        );
    }
}
