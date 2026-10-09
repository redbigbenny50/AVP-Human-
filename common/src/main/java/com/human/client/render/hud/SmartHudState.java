package com.human.client.render.hud;

import com.human.common.gameplay.item.gun.targeting.SmartTargeting;
import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Client-side liveness for the smart HUD: when the tracker last powered on, and how far through its boot sweep it is.
 * <p>
 * ⚠⚠ TIMED IN WALL-CLOCK MILLISECONDS, NOT TICKS. Anything keyed to a tick counter advances twenty times a second no
 * matter the player's frame rate, so a sweep driven by ticks stutters visibly at 144fps and is the single difference
 * between reading as a device and reading as a slideshow. The game clock is for game state; animation is for the screen
 * clock.
 * </p>
 * <p>
 * ⚠ STATE LIVES HERE, NOT IN THE RENDERER. The renderer runs per frame and must stay a pure function of this state - a
 * field mutated inside a draw call is the trap that had one firing minigun spinning every hotbar icon.
 * </p>
 */
public final class SmartHudState {

    /** Matched to the length of the boot sound so the sweep resolves as the tone settles. */
    public static final long BOOT_DURATION_MILLIS = 520L;

    private static @Nullable Item lastItem;

    private static @Nullable ResourceLocation lastModuleId;

    private static long bootStartedAtMillis;

    private static boolean pendingBootSound;

    /**
     * ⚠⚠ COMPARES THE ITEM AND THE SMART MODULE ONLY - NEVER THE WHOLE STACK. This used to call
     * {@code ItemStack.isSameItemSameComponents}, which was catastrophically wrong: a firing gun rewrites AMMUNITION,
     * MUZZLE_FLASH_DURATION_IN_TICKS, IS_FIRING, GUN_HEAT and GUN_ANIMATION_ID EVERY SINGLE TICK, so the stack never
     * compared equal while the trigger was held. The optic re-booted twenty times a second - which blanked the HUD
     * (boot progress restarted from zero every tick, so alpha never climbed) and queued twenty overlapping copies of
     * the half-second boot sound, which then kept playing long after the trigger was released.
     * <p>
     * ⚠ THE ORIGINAL INTENT WAS STILL RIGHT: fitting or removing a smart module genuinely changes the tracker and
     * SHOULD re-boot it. That is why the module component is compared explicitly instead of dropping to a bare item
     * check - the fix keeps the intent and drops the volatile components that had nothing to do with it.
     * </p>
     * <p>
     * ⚠ IF ANOTHER STACK VALUE EVER NEEDS TO RE-BOOT THE OPTIC, ADD IT HERE BY NAME. Never widen this back to a
     * whole-stack comparison; every component a gun writes while firing is in that comparison.
     * </p>
     */
    public static void tick(ItemStack heldStack) {
        var item = heldStack.isEmpty() ? null : heldStack.getItem();
        var moduleId = heldStack.isEmpty() ? null : heldStack.get(HumanDataComponents.SMART_MODULE.get());

        if (item == lastItem && Objects.equals(moduleId, lastModuleId)) {
            return;
        }

        lastItem = item;
        lastModuleId = moduleId;

        if (SmartTargeting.hasTracker(heldStack)) {
            bootStartedAtMillis = now();
            pendingBootSound = true;
        } else {
            bootStartedAtMillis = 0L;
            pendingBootSound = false;
        }
    }

    /**
     * 0.0 through 1.0 across the power-on sweep, or 1.0 once settled.
     * <p>
     * ⚠ RETURNS 1.0 IMMEDIATELY WHEN THE ANIMATION IS TURNED OFF - the HUD is then drawn fully formed rather than
     * skipped, so the toggle removes the sweep without removing the display.
     * </p>
     */
    public static float bootProgress(boolean animationEnabled) {
        if (!animationEnabled || bootStartedAtMillis == 0L) {
            return 1.0F;
        }

        var elapsed = now() - bootStartedAtMillis;

        return elapsed >= BOOT_DURATION_MILLIS ? 1.0F : Mth.clamp((float) elapsed / BOOT_DURATION_MILLIS, 0.0F, 1.0F);
    }

    public static boolean isBooting(boolean animationEnabled) {
        return bootProgress(animationEnabled) < 1.0F;
    }

    /**
     * True exactly ONCE per power-on, then false until the next one.
     * <p>
     * ⚠ A FLAG, NOT A TIME WINDOW. Testing "less than N milliseconds since boot" looks equivalent and is not - the
     * renderer runs every frame, so a 40ms window fires the sound two or three times at 60fps and a dozen at 300.
     * </p>
     */
    public static boolean consumeBootSound() {
        if (!pendingBootSound) {
            return false;
        }

        pendingBootSound = false;
        return true;
    }

    /**
     * ⭐ SMOOTHING FOR THINGS THAT MOVE FASTER THAN THE EYE FOLLOWS. Both chases below run per FRAME toward a value that
     * changes per TICK, which is what turns a stepped 20Hz jump into continuous motion.
     * <p>
     * ⚠ FRAME-RATE COMPENSATED, NOT A FIXED FRACTION PER FRAME. A plain {@code current += (target - current) * 0.3F}
     * chases more than three times as fast at 180fps as at 60 - so the box would feel glued on a strong machine and
     * sluggish on a weak one. Raising the retention to the power of elapsed time makes the RATE identical everywhere.
     * </p>
     */
    private static float @Nullable [] boxChase;

    private static float healthGhost = -1.0F;

    private static long lastChaseMillis;

    /** Half-life in milliseconds: how long the chase takes to close half the remaining gap. Lower is snappier. */
    private static final float BOX_HALF_LIFE_MILLIS = 55.0F;

    private static final float HEALTH_HALF_LIFE_MILLIS = 220.0F;

    public static float[] chaseBox(float[] destination) {
        var elapsed = elapsedMillis();

        if (boxChase == null || boxChase.length != destination.length) {
            boxChase = destination.clone();
            return boxChase;
        }

        var retention = (float) Math.pow(0.5D, elapsed / BOX_HALF_LIFE_MILLIS);

        for (var index = 0; index < destination.length; index++) {
            boxChase[index] = destination[index] + (boxChase[index] - destination[index]) * retention;
        }

        return boxChase;
    }

    public static void clearBoxChase() {
        boxChase = null;
    }

    /**
     * ⚠ THE GHOST ONLY TRAILS DOWNWARD. Healing snaps the ghost straight to the new value - a pale bar creeping
     * BACKWARDS up the gauge reads as the target regenerating far more than it actually is.
     */
    public static float chaseHealth(float fraction) {
        var elapsed = elapsedMillis();

        if (healthGhost < 0.0F || fraction > healthGhost) {
            healthGhost = fraction;
            return healthGhost;
        }

        var retention = (float) Math.pow(0.5D, elapsed / HEALTH_HALF_LIFE_MILLIS);
        healthGhost = fraction + (healthGhost - fraction) * retention;

        return healthGhost;
    }

    public static void clearHealthGhost() {
        healthGhost = -1.0F;
    }

    private static float elapsedMillis() {
        var now = System.currentTimeMillis();
        var elapsed = lastChaseMillis == 0L ? 16L : Math.min(200L, now - lastChaseMillis);

        lastChaseMillis = now;

        return elapsed;
    }

    public static void reset() {
        lastItem = null;
        lastModuleId = null;
        bootStartedAtMillis = 0L;
        pendingBootSound = false;
        boxChase = null;
        healthGhost = -1.0F;
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private SmartHudState() {
        throw new UnsupportedOperationException();
    }
}
