package com.human.client.animation.item;

import com.blib.api.client.animation.v1.track.AzAnimationTrack;
import com.blib.api.client.animation.v1.track.AzAnimationTrackContainer;
import com.human.HumanResources;
import com.human.common.gameplay.item.gun.animation.GunAnimationEvents;
import com.human.common.gameplay.item.gun.animation.dispatcher.impl.ChemThrowerAnimationDispatcher;
import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Drives the chem thrower - the sniper's pattern (the working arm rig in this mod), plus two things only a stream
 * weapon with a vapor switch needs:
 * <ul>
 * <li>THE TWO RELOAD HALVES (unload, insert), gun animation events the shared animator does not know - caught here
 * first.</li>
 * <li>BACK TO IDLE WHEN THE SHOOTING STOPS. {@code fire} loops while shots keep arriving (one a tick); a few ticks
 * after the last one the idle is put back, so the arms settle instead of looping the fire clip forever.</li>
 * </ul>
 */
public class ChemThrowerAnimator extends GunItemAnimator {

    private static final String NAME = "chem_thrower";

    private static final ResourceLocation ANIMATION = HumanResources.itemAnimationLocation(NAME);

    /** Ticks without a shot before the fire loop gives way to the idle. */
    private static final long FIRE_SETTLE_TICKS = 4L;

    private int lastSeenAnimationId = Integer.MIN_VALUE;

    private long lastShotTick = Long.MIN_VALUE / 2;

    @Override
    public void registerTracks(AzAnimationTrackContainer<ItemStack> container) {
        container.add(
            AzAnimationTrack.builder(this, ChemThrowerAnimationDispatcher.CONTROLLER_MAIN)
                .setTransitionLength(1)
                .build()
        );
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(ItemStack animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(ItemStack animatable, float partialTicks) {
        super.setCustomAnimations(animatable, partialTicks);

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;

        if (player == null || minecraft.level == null) {
            return;
        }

        var now = minecraft.level.getGameTime();
        var animationId = animatable.getOrDefault(HumanDataComponents.GUN_ANIMATION_ID.get(), 0);
        var type = animatable.getOrDefault(HumanDataComponents.GUN_ANIMATION_TYPE.get(), GunAnimationEvents.NONE);

        // ⚠ Read BEFORE the shared handler below, which consumes the id change and ignores a type it does not know.
        if (animationId != lastSeenAnimationId) {
            lastSeenAnimationId = animationId;

            if (animationId != 0 && type == GunAnimationEvents.CHEM_UNLOAD) {
                ChemThrowerAnimationDispatcher.INSTANCE.unload(player, animatable);
            } else if (animationId != 0 && type == GunAnimationEvents.CHEM_INSERT) {
                ChemThrowerAnimationDispatcher.INSTANCE.insert(player, animatable);
            } else if (type == GunAnimationEvents.SHOOT) {
                lastShotTick = now;
            } else if (animationId != 0 && type == GunAnimationEvents.NONE) {
                // Oct 21 - a reload called off by switching away: back to idle, not the rest of the reload.
                ChemThrowerAnimationDispatcher.INSTANCE.idle(player, animatable);
            }
        }

        runGunAnimationEvents(animatable, ChemThrowerAnimationDispatcher.INSTANCE);
        applyClipSpeed(animatable, minecraft.level);

        if (now - lastShotTick > FIRE_SETTLE_TICKS && isPlaying(ChemThrowerAnimationDispatcher.ANIMATION_FIRE)) {
            ChemThrowerAnimationDispatcher.INSTANCE.idle(player, animatable);
        }
    }

    /**
     * [stated] Oct 18 - QUICK CHARGE plays the REMOVE and INSERT clips faster; everything else (the purge, the idle,
     * the fire loop) at normal speed.
     * <p>
     * ⚠ VERIFIED IN BLib, not assumed: a sequence STAGE's own speed is never read ({@code tryCreateAnimationQueue}
     * takes only the play behaviour and the reverse flag from a stage), and a replay resets the track to its default
     * speed. The speed that does work is the TRACK's, read every frame by its timer as
     * {@code speed x time since the clip
     * began}. So the speed is set on the track here, each time the clip on it changes - at the very start of the clip,
     * when that product is still near zero, so the switch does not jump the pose.
     * </p>
     */
    private void applyClipSpeed(ItemStack animatable, net.minecraft.world.level.Level level) {
        var track = getAnimationTrackContainer().getOrNull(ChemThrowerAnimationDispatcher.CONTROLLER_MAIN);

        if (track == null) {
            return;
        }

        var clip = track.currentAnimation() == null ? null : track.currentAnimation().animation().name();
        var fast = ChemThrowerAnimationDispatcher.ANIMATION_REMOVE.equals(clip)
            || ChemThrowerAnimationDispatcher.ANIMATION_INSERT.equals(clip);
        var speed = fast ? 1.0 / com.human.common.gameplay.item.gun.chem.ChemThrowerReloading.quickChargeScale(level, animatable) : 1.0;
        var properties = track.animationProperties();

        if (properties.animationSpeed() != speed) {
            track.setAnimationProperties(properties.withAnimationSpeed(speed));
        }
    }

    private boolean isPlaying(String clip) {
        var track = getAnimationTrackContainer().getOrNull(ChemThrowerAnimationDispatcher.CONTROLLER_MAIN);

        return track != null && track.currentAnimation() != null && clip.equals(track.currentAnimation().animation().name());
    }
}
