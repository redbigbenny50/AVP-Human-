package com.human.mixin.client;

import com.human.client.freeze.FrozenSolidClient;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * [stated] Oct 19 (tester): frozen mobs "make movement or sprinting animations when pulled while frozen" on a capture
 * chain, cord or lead. A statue being dragged does not walk.
 * <p>
 * BLib mobs are already held in their frozen pose ({@code MixinAzModelRenderer_FrozenSolid} - their clock stops and no
 * new clip, walk or sprint included, can start). This covers VANILLA-model mobs: their leg swing is driven by
 * {@code updateWalkAnimation}, fed by how far they moved, so dragging one swung its legs. For a mob frozen solid the
 * update sees no movement, so the stride stops and the legs stand still. Client only - it is purely visual.
 * </p>
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity_FrozenWalk {

    /**
     * ⚠⚠ Oct 19 (his report: a frozen cow's "legs began vibrating in place rapidly"). This used to CANCEL the update
     * outright - which froze the walk state with whatever stride speed the mob had at that moment. The renderer then
     * draws the legs at {@code position - speed x (1 - partialTick)}: with the speed stuck above zero that offset
     * swings back and forth every frame, so the legs shook. Now the update still runs, but as if the mob had not moved:
     * the stride winds down to a stop within a few ticks and the legs settle still, however it is dragged.
     */
    @ModifyVariable(method = "updateWalkAnimation", at = @At("HEAD"), argsOnly = true)
    private float avp_human$frozenDoesNotWalk(float distance) {
        var self = (LivingEntity) (Object) this;

        // A netted mob keeps whatever posing the net gives it - [stated] the net pose takes priority.
        if (
            self.level().isClientSide()
                && FrozenSolidClient.isFrozen(self)
                && !com.human.compatibility.avp_predator.HumanPredatorNet.isNettedOnClient(self)
        ) {
            return 0.0F;
        }

        return distance;
    }
}
