package com.human.common.gameplay.item.old_painless;

import com.human.common.gameplay.item.GunItem;
import com.human.common.gameplay.item.gun.GunData;
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
     * ⭐⭐ THE BARREL SPIN PHASE. [stated] "its supposed to spin when you hold down fire and stops when you let go. it
     * loops as its firing. when it overheats it locks up the spin."
     * <p>
     * ⚠⚠ THIS TRACKS STATE ONLY - IT PLAYS NO ANIMATION. The barrels are turned procedurally by
     * {@code OldPainlessItemRenderer}, which reads this phase (and OLD_PAINLESS_SPIN_DOWN_TICK) to ramp gBarrel's rotZ
     * up and coast it down. Driving the animation clips from here as well put TWO things on the same bone and rendered
     * as a spinning model ghosting over a still one.
     * </p>
     * <p>
     * ⚠ THE PHASE LIVES ON THE STACK, NOT IN A FIELD. Item instances are SINGLETONS shared by every player and every
     * stack, so a field would have one wielder's spin state stomping another's. It is network-synchronised because the
     * CLIENT renderer is what reads it - which is also what makes other players see your barrels turn.
     * </p>
     */
    private static final int SPIN_PHASE_STOPPED = 0;

    private static final int SPIN_PHASE_SPINNING_UP = 1;

    private static final int SPIN_PHASE_LOOPING = 2;

    /**
     * ⭐⭐ 20 BECAUSE THE GUN ALREADY HAS A 20-TICK SPIN-UP: {@code GunData.OLD_PAINLESS} sets
     * {@code .withShootDelayInTicks(20)}, and {@code CheckShootDelayStep} plays WEAPON_OLD_PAINLESS_SHOOT_START on the
     * first tick then returns DELAYED until those 20 ticks elapse. So the hand-off to the loop must land exactly when
     * the first round comes out.
     * <p>
     * ⚠ DO NOT "CORRECT" THIS TO THE ANIMATION LENGTH. animation.barrelspinup is 0.6667s = 13.3 ticks, and I briefly
     * changed this to 13 on that basis - which would start the loop SEVEN TICKS BEFORE the gun fires. The animation
     * being shorter than the delay is fine; it holds its last frame. The delay is the authority, not the clip.
     * </p>
     */
    private static final int SPIN_UP_TICKS = 20;

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity livingEntity, @NotNull ItemStack itemStack, int tickCountdown) {
        // ⚠⚠ THE PLAYER EARLY-RETURN IS BACK ABOVE THE SPIN TICK, AND MUST STAY THERE. onUseTick runs while the item
        // is IN USE, which for a gun is RIGHT-CLICK / AIMING, not firing. Ticking the spin below it made the barrels
        // wind up and down on aim - [stated] "when i hold right click it also spins it does the startup and stop".
        // A player's trigger reaches the gun through onFired() from the server fire path instead.
        // ⚠ Marines DO fire from here, so they still need the spin tick below.
        if (livingEntity instanceof Player) {
            return;
        }

        var tickProgress = Math.abs(START_TICK_PROGRESS - tickCountdown);

        tickSpin(itemStack, tickProgress);

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
    /**
     * ⭐⭐ CALLED FROM THE SERVER FIRE PATH, WHICH IS THE ONLY THING A PLAYER'S TRIGGER ACTUALLY REACHES.
     * <p>
     * ⚠⚠ {@code onUseTick} IS NOT THE PLAYER FIRE PATH. Players shoot with the ATTACK button: {@code GunInputHandler}
     * streams {@code C2SGunFirePayload} every tick it is held, and {@code HumanServerListener.handleGunFirePayload}
     * calls {@code fire()} directly. {@code onUseTick} only runs while {@code isUsingItem()} is true, which for a gun
     * is AIMING, not firing - so the first version of this spin state machine ticked for marines and never once for a
     * player.
     * </p>
     * <p>
     * ⭐ AND IT MUST STAY SERVER-SIDE SO OTHER PLAYERS SEE IT. The spin phase is a synchronised component on the STACK,
     * and a held stack syncs to every tracking client - so their renderer ramps your barrels too. Driving this from
     * GunInputHandler on the client would spin only your own.
     * </p>
     */
    public void onFired(ItemStack itemStack, Entity shooter, int tickProgress) {
        if (shooter.level().isClientSide) {
            return;
        }

        itemStack.set(HumanDataComponents.OLD_PAINLESS_LAST_FIRE_TICK.get(), shooter.tickCount);
        tickSpin(itemStack, tickProgress);
    }

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
            return;
        }

        // ⚠ The hand-off is driven by tickProgress, NOT a counter of our own - tickProgress restarts at 0 on every
        // fresh use, so releasing and re-holding replays the wind-up instead of resuming mid-loop.
        if (phase == SPIN_PHASE_SPINNING_UP && tickProgress >= SPIN_UP_TICKS) {
            itemStack.set(HumanDataComponents.OLD_PAINLESS_SPIN_PHASE.get(), SPIN_PHASE_LOOPING);
        }
    }

    private void stopSpin(ItemStack itemStack) {
        itemStack.set(HumanDataComponents.OLD_PAINLESS_SPIN_PHASE.get(), SPIN_PHASE_STOPPED);
        // ⭐ Stamped so OldPainlessItemRenderer can COAST the barrels down instead of stopping them dead.
        itemStack.set(HumanDataComponents.OLD_PAINLESS_SPIN_DOWN_TICK.get(), 0);
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

            // ⭐ Smoke only for the gun actually in hand; a hot Old Painless in the pack still cools, it just does
            // not smoke through the holder's chest.
            if (selected) {
                OldPainlessHeat.tickSmoke(entity, itemStack);
            }
            tickSpinDownTimeout(itemStack, entity);
            tickSpinDownCoast(itemStack);
        }
    }

    /**
     * Winds the barrels down once the trigger has clearly been released.
     * <p>
     * ⚠ A TIMEOUT IS THE ONLY OPTION HERE - see OLD_PAINLESS_LAST_FIRE_TICK. {@code releaseUsing} never fires for a
     * player holding a gun, because the player was never "using" it in the vanilla sense.
     * </p>
     * <p>
     * ⚠ THE GRACE IS DELIBERATE. One dropped or late payload must not chatter the animation between loop and spin-down;
     * {@value #SPIN_DOWN_GRACE_TICKS} ticks is long enough to ride out a hitch and short enough that letting go still
     * reads as immediate.
     * </p>
     */
    private void tickSpinDownTimeout(ItemStack itemStack, Entity holder) {
        var phase = itemStack.getOrDefault(HumanDataComponents.OLD_PAINLESS_SPIN_PHASE.get(), SPIN_PHASE_STOPPED);

        if (phase == SPIN_PHASE_STOPPED) {
            return;
        }

        var lastFireTick = itemStack.getOrDefault(HumanDataComponents.OLD_PAINLESS_LAST_FIRE_TICK.get(), 0);

        if (holder.tickCount - lastFireTick > SPIN_DOWN_GRACE_TICKS) {
            stopSpin(itemStack);
        }
    }

    /** How many tickless ticks count as "let go of the trigger". */
    private static final int SPIN_DOWN_GRACE_TICKS = 4;

    /**
     * ⚠⚠ ADVANCES THE COAST-DOWN COUNTER. WITHOUT THIS THE BARRELS NEVER STOP.
     * <p>
     * {@code stopSpin} stamps OLD_PAINLESS_SPIN_DOWN_TICK to 0 and {@code OldPainlessItemRenderer} ramps speed down as
     * {@code 1 - tick/SPIN_DOWN_TICKS} - but something has to actually COUNT. When the earlier clear-the-animation-type
     * helper was deleted, the increment went with it, so the value sat at 0 and the renderer computed full speed
     * forever.
     * </p>
     * <p>
     * ⚠ Stops counting once past the ramp so the number cannot run away on a stack that sits in an inventory for hours;
     * the renderer treats anything at or beyond the threshold as stopped.
     * </p>
     */
    private void tickSpinDownCoast(ItemStack itemStack) {
        if (itemStack.getOrDefault(HumanDataComponents.OLD_PAINLESS_SPIN_PHASE.get(), SPIN_PHASE_STOPPED) != SPIN_PHASE_STOPPED) {
            return;
        }

        var elapsed = itemStack.getOrDefault(HumanDataComponents.OLD_PAINLESS_SPIN_DOWN_TICK.get(), SPIN_DOWN_COAST_TICKS);

        if (elapsed < SPIN_DOWN_COAST_TICKS) {
            itemStack.set(HumanDataComponents.OLD_PAINLESS_SPIN_DOWN_TICK.get(), elapsed + 1);
        }
    }

    /** ⚠ MUST MATCH SPIN_DOWN_TICKS in OldPainlessItemRenderer - they are two halves of one ramp. */
    private static final int SPIN_DOWN_COAST_TICKS = 14;
}
