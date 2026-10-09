package com.human.common.gameplay.item.gun;

import com.human.common.gameplay.item.ItemCooldownUser;
import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Server-owned heat and lockout state for any gun carrying a {@link GunHeatProfile}.
 * <p>
 * ⭐ GENERALISED OUT OF THE OLD {@code OldPainlessHeat}, WHICH IS NOW DELETED. Everything here was Old Painless's
 * behaviour and it is unchanged for that gun - only the numbers moved into {@link GunData}, and the state moved onto
 * the shared GUN_HEAT / GUN_OVERHEATED components. The second machine gun was the point at which copying the class
 * would have meant two overheat systems to keep in step.
 * </p>
 * <p>
 * ⚠ THE HEAT LIVES ON THE STACK, NOT IN A FIELD. Item instances are singletons shared by every player and every stack,
 * so a field would have one wielder's heat stomping another's. Network-synchronised because the CLIENT is what renders
 * the barrel tint.
 * </p>
 * <p>
 * ⚠ APPLIES TO EVERY SHOOTER, NOT JUST PLAYERS. {@code GunShootContext} runs the same pipeline for marines, so a
 * marine's gun heats, quenches and locks out too. Whether a marine ever reaches the ceiling is a function of their
 * burst pause, not of anything here - see the duty-cycle note on {@link GunHeatProfile}.
 * </p>
 */
public final class GunHeat {

    /**
     * ⚠ THE WARNING FLASHES BY BEING RE-SENT AND THEN CLEARED. The vanilla action bar has no blink of its own: it
     * simply fades over about 60 ticks. Sending the text on one phase of this period and an empty component on the
     * other is what makes it pulse rather than sit there as static text the player stops seeing.
     */
    private static final int WARNING_FLASH_PERIOD_IN_TICKS = 20;

    public static boolean isOverheated(ItemStack stack) {
        return stack.getOrDefault(HumanDataComponents.GUN_OVERHEATED.get(), false);
    }

    public static int getHeat(ItemStack stack) {
        return stack.getOrDefault(HumanDataComponents.GUN_HEAT.get(), 0);
    }

    public static float getHeatFraction(ItemStack stack, GunHeatProfile profile) {
        return getHeat(stack) / (float) profile.maxHeat();
    }

    public static void addShotHeat(LivingEntity shooter, ItemStack stack, Item gunItem, GunHeatProfile profile) {
        var heat = Math.min(profile.maxHeat(), getHeat(stack) + profile.heatPerShot());
        stack.set(HumanDataComponents.GUN_HEAT.get(), heat);

        if (heat < profile.maxHeat() || isOverheated(stack)) {
            return;
        }

        stack.set(HumanDataComponents.GUN_OVERHEATED.get(), true);

        // ⭐ THE QUENCH. [stated] "when the gun overheats and stops i want it to make that noise when lave and water
        // mix to make stone and obsidian" - that is vanilla LAVA_EXTINGUISH (block.lava.extinguish), the hiss played
        // by LiquidBlock when lava meets water.
        // ⚠ SERVER-SIDE AND POSITIONAL, deliberately: passing null as the excluded player means EVERYONE nearby hears
        // it, including the shooter, and a marine's gun hisses too. ⚠ It sits INSIDE the branch that has already
        // returned unless heat just crossed the ceiling and the gun was not already overheated, so it fires exactly
        // ONCE per overheat, not every tick of the lockout.
        shooter.level()
            .playSound(
                null,
                shooter.getX(),
                shooter.getY(),
                shooter.getZ(),
                SoundEvents.LAVA_EXTINGUISH,
                SoundSource.PLAYERS,
                1.0F,
                // Slightly low and slightly random so a squad of overheating gunners does not sound like one gun.
                0.8F + shooter.getRandom().nextFloat() * 0.2F
            );

        var cooldowns = ItemCooldownUser.getItemCooldownsOrNull(shooter);

        if (cooldowns != null) {
            cooldowns.addCooldown(gunItem, profile.lockoutTicks());
        }

        var overheatedMessage = profile.overheatedMessage();

        if (shooter instanceof Player player && overheatedMessage != null) {
            player.displayClientMessage(Component.literal(overheatedMessage), true);
        }
    }

    /**
     * ⚠ THE LOCKOUT IS THE COOL-DOWN TO ZERO, NOT {@code lockoutTicks}. The overheated flag clears only when heat
     * reaches 0, so the real lockout is {@code maxHeat / overheatedCoolingPerTick} ticks and the item cooldown is just
     * the matching HUD sweep. At 420 heat and 4 per tick that is 105 ticks, a little over five seconds.
     */
    public static void cool(ItemStack stack, GunHeatProfile profile) {
        var heat = getHeat(stack);

        if (heat <= 0) {
            stack.remove(HumanDataComponents.GUN_OVERHEATED.get());
            return;
        }

        var coolingRate = isOverheated(stack) ? profile.overheatedCoolingPerTick() : profile.passiveCoolingPerTick();
        heat = Math.max(0, heat - coolingRate);
        stack.set(HumanDataComponents.GUN_HEAT.get(), heat);

        if (heat == 0) {
            stack.remove(HumanDataComponents.GUN_OVERHEATED.get());
        }
    }

    /**
     * ⭐ A FLASHING ACTION-BAR WARNING, FOR GUNS WITH NO RED-HOT BARREL TO SPEAK FOR THEM. [stated] "have it flash a
     * warning on screen for the player".
     * <p>
     * ⚠ OPT-IN VIA {@code warningThreshold}. A gun that already glows - Old Painless - leaves it at zero and returns
     * here immediately, so its behaviour is exactly what it always was: one message at the moment of overheat and
     * nothing else.
     * </p>
     */
    public static void tickWarning(Entity holder, ItemStack stack, GunHeatProfile profile) {
        if (profile.warningThreshold() <= 0.0F || !(holder instanceof Player player)) {
            return;
        }

        var heat = getHeat(stack);

        if (heat <= 0) {
            return;
        }

        var overheated = isOverheated(stack);

        if (!overheated && heat < profile.maxHeat() * profile.warningThreshold()) {
            return;
        }

        var phase = holder.tickCount % WARNING_FLASH_PERIOD_IN_TICKS;

        if (phase == 0) {
            var message = overheated ? profile.overheatedMessage() : profile.overheatingMessage();

            if (message != null) {
                player.displayClientMessage(Component.literal(message), true);
            }
        } else if (phase == WARNING_FLASH_PERIOD_IN_TICKS / 2) {
            // The dark half of the blink. Clearing beats waiting for the fade, which takes about 60 ticks.
            player.displayClientMessage(Component.empty(), true);
        }
    }

    /**
     * ⭐ SMOKE THAT THINS OUT AS THE BARRELS COOL. [stated] "add smokeparticles to it also that fade and go away as it
     * cools."
     * <p>
     * Density is driven straight off the heat fraction, so it is heaviest the moment the gun quenches and tapers to
     * nothing on its own — no separate timer to keep in sync with the heat, and it cannot outlive the cooldown.
     * </p>
     * <p>
     * ⚠⚠ SERVER-SIDE VIA {@code ServerLevel.sendParticles}, NOT {@code level.addParticle}. addParticle is client-only
     * and would spawn smoke for the holder alone; sendParticles broadcasts to everyone tracking the position, which is
     * what makes a squadmate's overheating gun visibly smoke.
     * </p>
     * <p>
     * ⚠ ONLY WHILE HELD. Called from inventoryTick with {@code selected}, so a hot gun sitting in a backpack still
     * COOLS (that is unchanged) but does not smoke through the player's chest.
     * </p>
     */
    public static void tickSmoke(Entity holder, ItemStack stack, GunHeatProfile profile) {
        if (!(holder.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        var heatFraction = getHeatFraction(stack, profile);

        if (heatFraction <= profile.smokeThreshold()) {
            return;
        }

        // Rescaled so the visible band spans 0..1 rather than starting a third of the way up.
        var intensity = (heatFraction - profile.smokeThreshold()) / (1.0F - profile.smokeThreshold());

        // ⚠ INTERVAL, NOT COUNT. Emitting fewer particles less often reads as thinning smoke; emitting the same
        // burst at a lower count just looks like flicker. At full heat that is every other tick, tapering to roughly
        // once a second as it cools out.
        var interval = Math.max(2, Math.round(2.0F + (1.0F - intensity) * 18.0F));

        if (holder.tickCount % interval != 0) {
            return;
        }

        // Roughly where the barrels sit: eye height, a little forward.
        var forward = holder.getLookAngle().scale(0.6D);
        var x = holder.getX() + forward.x;
        var y = holder.getEyeY() - 0.15D;
        var z = holder.getZ() + forward.z;

        // ⚠⚠ ALWAYS PLAIN SMOKE — NEVER LARGE_SMOKE. [stated] "can the particles be made maybe 20%smaller they are
        // kinda big". Vanilla smoke has NO size field: SMOKE and LARGE_SMOKE are both SimpleParticleType and
        // sendParticles takes no scale argument, so the only size lever available is WHICH of the two is used.
        // ⚠ THE QUENCH KEEPS ITS WEIGHT VIA COUNT, NOT SIZE: an overheated gun emits TWO small puffs per interval
        // instead of one.
        var count = isOverheated(stack) ? 2 : 1;

        serverLevel.sendParticles(ParticleTypes.SMOKE, x, y, z, count, 0.02D, 0.01D, 0.02D, 0.005D);
    }

    private GunHeat() {}
}
