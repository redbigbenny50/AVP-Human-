package com.human.common.gameplay.item.gun.pipeline.step.impl;

import com.human.common.gameplay.item.gun.GunReloading;
import com.human.common.gameplay.item.gun.pipeline.GunShootContext;
import com.human.common.gameplay.item.gun.pipeline.GunShootResult;
import com.human.common.gameplay.item.gun.pipeline.step.GunShootStep;
import net.minecraft.world.entity.player.Player;

/**
 * Decides whether the gun has a round to fire, and starts a reload when it does not.
 * <p>
 * ⚠⚠ DO NOT RE-ADD AN OLD PAINLESS SPECIAL CASE HERE. This step used to open with a hardcoded Old Painless branch that
 * ignored the AMMUNITION component entirely and instead tried to consume {@code consumedAmmunitionPerShot} AMMUNITION
 * ITEMS straight out of the player's inventory on every single shot. That was correct for the ORIGINAL Old Painless,
 * which had {@code maximumAmmunition(Integer.MAX_VALUE)}, no reload at all, and ate loose bullets as it fired. The
 * drum-cannister pass gave it a real 1500-round magazine, a real reload and a whole-unit ammunition item - and that
 * branch was not removed with the rest of the old design.
 * </p>
 * <p>
 * ⭐⭐ THE RESULT WAS A GUN THAT COULD ONLY FIRE IN CREATIVE. With a drum as the ammunition item and
 * {@code consumedAmmunitionPerShot} at 6, the branch demanded SIX DRUM CANNISTERS in the inventory PER SHOT - and a
 * drum is {@code stacksTo(1)}, so six free slots of drums bought one round and the player's spares evaporated. In
 * practice it never once returned Full, so every shot returned FAILURE. The barrels still span, because the spin phase
 * is stamped by {@code OldPainlessItem.onFired} from the packet handler BEFORE the pipeline runs, and the reload still
 * worked and still ate the drum, because that path never came through here. Creative was unaffected only because
 * {@code isShooterImmortal} returns early above.
 * </p>
 * <p>
 * Old Painless is now an ordinary magazine-fed gun as far as this step is concerned: the drum is consumed by
 * {@code GunReloading.reload}, the magazine is spent 6 rounds at a time by {@code GunShootContext.consumeAmmunition},
 * and an empty magazine falls through to the auto-reload below like every other weapon.
 * </p>
 */
public class CheckReloadingStep implements GunShootStep {

    public static final CheckReloadingStep INSTANCE = new CheckReloadingStep();

    private CheckReloadingStep() {}

    @Override
    public GunShootResult apply(GunShootContext context) {
        var currentAmmunition = context.currentAmmunition();
        var isShooterImmortal = context.isShooterImmortal();
        var shooter = context.shooter();
        var level = shooter.level();

        if (
            // If shooter is not a player...
            !(shooter instanceof Player player)
                // OR player is immortal...
                || isShooterImmortal
        ) {
            // Then don't bother checking for reload requirements, just continue.
            // ⚠ Marines come through here: their ammunition is issued by MarineGearDecoratorUtil and spent from the
            // AMMUNITION component, and GunStrategy owns their reload. Nothing to check on their behalf.
            return GunShootResult.CONTINUE;
        }

        if (currentAmmunition <= 0) {
            // NO-OP client-side, this step should run on both sides if set up correctly.

            if (!level.isClientSide) {
                // Already server-side, so perform the reload immediately.
                GunReloading.reload(player);
            }

            return GunShootResult.RELOADING;
        }

        return GunShootResult.CONTINUE;
    }
}
