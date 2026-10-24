package com.human.client.animation.block;

import com.blib.api.client.animation.v1.animator.AzAnimatorConfig;
import com.blib.api.client.animation.v1.animator.AzBlockAnimator;
import com.blib.api.client.animation.v1.track.AzAnimationTrack;
import com.blib.api.client.animation.v1.track.AzAnimationTrackContainer;
import com.human.HumanResources;
import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** Carries vaporizer.animation.json: {@code running} while it works, {@code idle} otherwise. */
public class VaporizerAnimator extends AzBlockAnimator<ElectricMachineBlockEntity> {

    public static final String TRACK = "base_controller";

    private static final ResourceLocation ANIMATIONS = HumanResources.blockAnimationLocation("vaporizer");

    public VaporizerAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerTracks(AzAnimationTrackContainer<ElectricMachineBlockEntity> tracks) {
        tracks.add(AzAnimationTrack.builder(this, TRACK).build());
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(ElectricMachineBlockEntity animatable) {
        return ANIMATIONS;
    }
}
