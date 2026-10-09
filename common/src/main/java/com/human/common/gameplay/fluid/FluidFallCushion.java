package com.human.common.gameplay.fluid;

import com.human.common.gameplay.block.LatexLiquidBlock;
import com.human.common.registry.init.HumanFluids;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * What landing in one of our liquids does to a fall. Called from {@code MixinEntity_FluidFallCushion} at the moment an
 * entity touches the ground, before vanilla deals any damage.
 * <ul>
 * <li><b>Latex</b> absorbs the fall completely and bounces the entity slightly off the surface - what the liquid latex
 * block's code always intended ([stated] Sep 24: "if you land in it you take no fall damage and kinda bounce on the
 * surface slightly"). Sneaking skips both, as it does on a slime block.</li>
 * <li><b>Honey</b> halves the fall DAMAGE. [stated] Sep 29: "it isnt as absorbent as latex for fall damage it will half
 * your fall damage if you land on it over solid ground".</li>
 * </ul>
 * <h2>⚠⚠ WHY THIS CANNOT LIVE ON THE LIQUID BLOCK</h2> Two things in vanilla, both read from the 1.21.1 source rather
 * than assumed:
 * <ol>
 * <li>{@code Block.fallOn} is called on the block the entity is STANDING on - {@code getOnPosLegacy()}, its supporting
 * block. A liquid has no collision, so it is never that block. Latex's own {@code fallOn} override never ran.</li>
 * <li>Inside {@code Entity.move} the fall is applied (line 671) BEFORE {@code tryCheckInsideBlocks} tells blocks an
 * entity is inside them (line 728). Anyone falling fast into a shallow pool takes the full hit in the same tick they
 * enter it, before the liquid's {@code entityInside} could do anything.</li>
 * </ol>
 * Hooking the landing itself is the only place both loaders agree. What liquid latex actually did before this: on
 * NeoForge its fluid type's DEFAULT fall modifier (0.5, applied every tick inside) happened to soak up most of a fall;
 * on Fabric, which has no fluid types, it did nothing and players took full damage.
 * <h2>"OVER SOLID GROUND"</h2> This only runs on landing, and landing means the entity is standing on a solid block -
 * so it is exactly "the liquid is sitting on something". The liquid checked is the one at the entity's feet: on landing
 * its feet are in the block directly above the ground, which is where a pool sits.
 */
public final class FluidFallCushion {

    /** [stated] Sep 29: honey halves fall damage. */
    public static final float HONEY_DAMAGE_FACTOR = 0.5F;

    private FluidFallCushion() {}

    /**
     * @return the upward speed to bounce the entity back with once vanilla has stopped it on the ground, or 0 for no
     *         bounce. It cannot be applied here: straight after this, the ground block's
     *         {@code updateEntityAfterFallOn} zeroes the entity's vertical speed, so the mixin applies it after that
     *         call.
     */
    public static double onLanding(Entity entity) {
        if (entity.fallDistance <= 0.0F) {
            return 0.0D;
        }

        var fluid = entity.level().getFluidState(entity.blockPosition());

        if (fluid.isEmpty()) {
            return 0.0D;
        }

        var type = fluid.getType();

        if (type.isSame(HumanFluids.LATEX.get())) {
            // Sneaking lands normally, as on a slime block: full fall, no bounce.
            if (entity.isSuppressingBounce()) {
                return 0.0D;
            }

            var impact = entity.getDeltaMovement().y;

            entity.resetFallDistance();

            return impact < 0.0D ? -impact * LatexLiquidBlock.SURFACE_BOUNCE : 0.0D;
        }

        if (type.isSame(HumanFluids.HONEY.get())) {
            entity.fallDistance = scaledFall(entity, entity.fallDistance, HONEY_DAMAGE_FACTOR);
        }

        return 0.0D;
    }

    /**
     * The fall distance that deals {@code factor} of the damage this fall would have dealt.
     * <p>
     * ⚠ Scaling the distance itself would NOT halve the damage: vanilla subtracts the safe fall distance first
     * ({@code LivingEntity.calculateFallDamage}: damage = (distance - safe) x multiplier), so halving a 10-block fall
     * to 5 would cut 7 damage to 2. Only the part past the safe distance is scaled, which halves the damage exactly.
     * </p>
     */
    static float scaledFall(Entity entity, float fallDistance, float factor) {
        var safe = entity instanceof LivingEntity living
            ? (float) living.getAttributeValue(Attributes.SAFE_FALL_DISTANCE)
            : 3.0F;

        if (fallDistance <= safe) {
            return fallDistance;
        }

        return safe + (fallDistance - safe) * factor;
    }
}
