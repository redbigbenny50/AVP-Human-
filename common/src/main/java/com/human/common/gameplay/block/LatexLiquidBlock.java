package com.human.common.gameplay.block;

import com.human.common.registry.init.block.CoreBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.jetbrains.annotations.NotNull;

/**
 * The placed liquid-latex block: hardening, and what it does to anything standing in it.
 * <h2>Hardening</h2> [stated] Sep 24: "if left alone the source block hardens into latex blocks and the flowing blocks
 * vanish."
 * <p>
 * ⚠⚠ ONLY A SOURCE HARDENS. A flowing block simply disappears when its source is gone, which is vanilla's own behaviour
 * and needs no code - so a spill leaves latex blocks where the sources were and nothing where the stream ran. That is
 * also what stops a spill from paving the landscape.
 * </p>
 * The timer is a RANDOM TICK, not a counter, so nothing has to be stored per block and unloaded chunks cost nothing. At
 * the default random tick rate a block gets a tick about every 68 s, and at {@value #CURE_CHANCE_PER_RANDOM_TICK} that
 * averages a bit over two minutes - close to the 120 s cauldron cure, and deliberately not exact, so a big spill sets
 * in patches rather than all at once.
 * <h2>Standing in it</h2> [stated] "if you walk into the latex it gives you maximum slowness while in it", "if you land
 * in it you take no fall damage and kinda bounce on the surface slightly", "less bouncy than slime". The slowness is
 * here; the landing - no damage, the slight bounce - is in {@code FluidFallCushion}, because vanilla never asks a
 * liquid about a landing.
 */
public class LatexLiquidBlock extends LiquidBlock {

    /**
     * [stated] "maximum slowness". Slowness stacks at -15% movement speed per level
     * ({@code MobEffects.MOVEMENT_SLOWDOWN}, ADD_MULTIPLIED_TOTAL, scaled by amplifier + 1), so amplifier 5 is Slowness
     * VI and -90%.
     */
    public static final int SLOWNESS_AMPLIFIER = 5;

    /**
     * ⭐ [stated] Sep 24: "so players dont cheese it aliens and predators only slow down 50% not level 6 slowness." A
     * pool of latex must not be a xeno trap.
     * <p>
     * ⚠ NO INTEGER SLOWNESS LEVEL IS 50%. At -15% per level the options either side are Slowness III (-45%) and
     * Slowness IV (-60%); amplifier 2 is the closer of the two and the one that errs toward letting them out, which is
     * the point of the exemption. If you want it exact, it has to stop being a potion effect and become our own
     * attribute modifier.
     * </p>
     */
    public static final int RESISTANT_SLOWNESS_AMPLIFIER = 2;

    /**
     * ⚠⚠ MATCHED BY ID STRING, NOT BY A TAG CONSTANT FROM THE SIBLING MOD. avp_alien and avp_predator are
     * modCompileOnly here, so referencing their tag classes compiles and then silently does nothing when the sibling
     * mod is absent - the trap that has already killed sibling tag generation twice on this project. A string key
     * resolves at runtime if the mod is there and is simply empty if it is not.
     */
    private static final TagKey<EntityType<?>> XENOMORPHS = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("avp_alien", "xenomorphs")
    );

    private static final TagKey<EntityType<?>> PREDATORS = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("avp_predator", "predators")
    );

    /** Re-applied every tick the entity is inside, so it ends the moment they climb out. */
    private static final int SLOWNESS_DURATION_TICKS = 20;

    public static final float CURE_CHANCE_PER_RANDOM_TICK = 0.5F;

    /**
     * Gentler than {@link LatexBlock}: the liquid gives, so less comes back. Read by {@code FluidFallCushion}, which is
     * where landing in liquid latex is actually handled now.
     */
    public static final double SURFACE_BOUNCE = 0.08;

    public LatexLiquidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected boolean isRandomlyTicking(@NotNull BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(
        @NotNull BlockState state,
        @NotNull ServerLevel level,
        @NotNull BlockPos pos,
        @NotNull RandomSource random
    ) {
        // A flowing block is not latex that is setting - it is latex on its way somewhere. Only a full source cures.
        if (state.getValue(LEVEL) != 0) {
            return;
        }

        if (random.nextFloat() >= CURE_CHANCE_PER_RANDOM_TICK) {
            return;
        }

        level.setBlockAndUpdate(pos, CoreBlocks.LATEX_BLOCK.get().defaultBlockState());
    }

    @Override
    protected void entityInside(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Entity entity
    ) {
        super.entityInside(state, level, pos, entity);

        if (level.isClientSide || !(entity instanceof LivingEntity living)) {
            return;
        }

        var amplifier = isResistant(living) ? RESISTANT_SLOWNESS_AMPLIFIER : SLOWNESS_AMPLIFIER;

        living.addEffect(
            new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_DURATION_TICKS, amplifier, true, false)
        );
    }

    /** Xenomorphs and yautja wade out of latex; everything else is stuck in it. */
    private static boolean isResistant(LivingEntity living) {
        var type = living.getType();

        return type.is(XENOMORPHS) || type.is(PREDATORS);
    }

    // ⚠⚠ NO fallOn / updateEntityAfterFallOn HERE, AND THERE MUST NEVER BE. Both used to live here and neither ever
    // ran:
    // vanilla calls them on the SOLID block an entity lands on, and a liquid has no collision, so it is never that
    // block.
    // The landing rules for liquid latex - no fall damage, a slight bounce, both skipped while sneaking - now live in
    // FluidFallCushion, reached from MixinEntity_FluidFallCushion at the moment of landing, on both loaders.
}
