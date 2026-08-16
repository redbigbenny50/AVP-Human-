package com.human.common.gameplay.item.old_painless;

import com.human.common.gameplay.item.GunItem;
import com.human.common.gameplay.item.gun.GunData;
import com.human.common.gameplay.item.gun.animation.GunAnimationEvents;
import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class OldPainlessItem extends GunItem {

    public OldPainlessItem() {
        super(GunData.OLD_PAINLESS);
    }

    /**
     * ⭐⭐ THE BARREL SPIN STATE MACHINE. [stated] "its supposed to spin when you hold down fire and stops when you
     * let go. it loops as its firing. when it overheats it locks up the spin."
     * <p>
     * ⚠⚠ NONE OF THIS EXISTED. The animations, {@code OldPainlessAnimationDispatcher} and the {@code GunItemAnimator}
     * switch were all built and wired, but {@code GunAnimationEvents.trigger} was NEVER called with any of the three
     * spin constants anywhere in the mod - a complete pipeline with no producer at the top. Spin-up, spin-loop AND
     * spin-down were all dead, not just the wind-up.
     * </p>
     * <p>
     * ⚠ THE PHASE LIVES ON THE STACK, NOT IN A FIELD. Item instances are SINGLETONS shared by every player and every
     * stack in the world - a field here would have one wielder's spin state stomping another's. The component also
     * rides to the client automatically, which is what the animator reads.
     * </p>
     */
    private static final int SPIN_PHASE_STOPPED = 0;

    private static final int SPIN_PHASE_SPINNING_UP = 1;

    private static final int SPIN_PHASE_LOOPING = 2;

    /**
     * ⭐ How long the wind-up runs before the loop takes over. MUST MATCH the length of
     * {@code animation.barrelspinup} in the model's animation json, or the loop cuts the wind-up short / leaves a
     * stalled gap after it. This is the dial to tune if the transition looks wrong.
     */
    private static final int SPIN_UP_TICKS = 20;

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity livingEntity, @NotNull ItemStack itemStack, int tickCountdown) {
        var tickProgress = Math.abs(START_TICK_PROGRESS - tickCountdown);

        tickSpin(itemStack, tickProgress);

        // ⚠ THE PLAYER EARLY-RETURN STAYS, and it has to sit BELOW the spin tick. Player firing is driven elsewhere,
        // so returning here first - as this method used to - meant a player holding fire never reached any spin
        // logic at all. Marines still fire from here.
        if (livingEntity instanceof Player) {
            return;
        }

        fire(level, livingEntity, itemStack, tickProgress);
    }

    /**
     * Drives spin-up → loop while use is held, and locks the barrels the moment the gun overheats.
     * <p>
     * ⚠ OVERHEAT SPINS DOWN AND STAYS DOWN. Resetting to STOPPED (rather than merely skipping the loop) is what makes
     * the barrels visibly seize and then wind up again from cold when the lockout expires - the "locks up" he asked
     * for. {@code OldPainlessHeat.isOverheated} is the same flag the heat layer renders from, so the visual and the
     * lockout can never disagree.
     * </p>
     */
    private void tickSpin(ItemStack itemStack, int tickProgress) {
        var phase = itemStack.getOrDefault(HumanDataComponents.OLD_PAINLESS_SPIN_PHASE.get(), SPIN_PHASE_STOPPED);

        if (OldPainlessHeat.isOverheated(itemStack)) {
            if (phase != SPIN_PHASE_STOPPED) {
                stopSpin(itemStack);
            }

            return;
        }

        if (phase == SPIN_PHASE_STOPPED) {
            itemStack.set(HumanDataComponents.OLD_PAINLESS_SPIN_PHASE.get(), SPIN_PHASE_SPINNING_UP);
            GunAnimationEvents.trigger(itemStack, GunAnimationEvents.OLD_PAINLESS_SPIN_UP);
            return;
        }

        // ⚠ The hand-off is driven by tickProgress, NOT a counter of our own - tickProgress restarts at 0 on every
        // fresh use, so releasing and re-holding replays the wind-up instead of resuming mid-loop.
        if (phase == SPIN_PHASE_SPINNING_UP && tickProgress >= SPIN_UP_TICKS) {
            itemStack.set(HumanDataComponents.OLD_PAINLESS_SPIN_PHASE.get(), SPIN_PHASE_LOOPING);
            GunAnimationEvents.trigger(itemStack, GunAnimationEvents.OLD_PAINLESS_SPIN_LOOP);
        }
    }

    private void stopSpin(ItemStack itemStack) {
        itemStack.set(HumanDataComponents.OLD_PAINLESS_SPIN_PHASE.get(), SPIN_PHASE_STOPPED);
        GunAnimationEvents.trigger(itemStack, GunAnimationEvents.OLD_PAINLESS_SPIN_DOWN);
    }

    @Override
    public void releaseUsing(@NotNull ItemStack itemStack, @NotNull Level level, @NotNull LivingEntity livingEntity, int tickCountdown) {
        // Let go of fire - wind down. Guarded so a release with the barrels already stopped does not re-trigger the
        // spin-down from cold.
        if (itemStack.getOrDefault(HumanDataComponents.OLD_PAINLESS_SPIN_PHASE.get(), SPIN_PHASE_STOPPED) != SPIN_PHASE_STOPPED) {
            stopSpin(itemStack);
        }

        super.releaseUsing(itemStack, level, livingEntity, tickCountdown);
    }

    @Override
    protected void playUseAnimations(Entity shooter, ItemStack itemStack) {}

    @Override
    public void inventoryTick(@NotNull ItemStack itemStack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean selected) {
        super.inventoryTick(itemStack, level, entity, slotId, selected);
        if (!level.isClientSide) {
            OldPainlessHeat.cool(itemStack);
        }
    }

}
