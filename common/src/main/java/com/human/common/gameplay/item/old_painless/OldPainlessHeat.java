package com.human.common.gameplay.item.old_painless;

import com.human.common.gameplay.item.ItemCooldownUser;
import com.human.common.registry.init.HumanDataComponents;
import com.human.common.registry.init.item.HumanGunItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Server-owned heat and lockout state for Old Painless. */
public final class OldPainlessHeat {

    // The minigun fires once per tick. With one point of passive cooling, this is
    // a net three heat/tick: 420 heat reaches overheat in seven seconds.
    public static final int MAX_HEAT = 420;

    private static final int HEAT_PER_SHOT = 4;

    private static final int PASSIVE_COOLING_PER_TICK = 1;

    private static final int OVERHEATED_COOLING_PER_TICK = 4;

    private static final int OVERHEAT_LOCKOUT_TICKS = 100;

    public static boolean isOverheated(ItemStack stack) {
        return stack.getOrDefault(HumanDataComponents.OLD_PAINLESS_OVERHEATED.get(), false);
    }

    public static int getHeat(ItemStack stack) {
        return stack.getOrDefault(HumanDataComponents.OLD_PAINLESS_HEAT.get(), 0);
    }

    public static void addShotHeat(LivingEntity shooter, ItemStack stack) {
        var heat = Math.min(MAX_HEAT, getHeat(stack) + HEAT_PER_SHOT);
        stack.set(HumanDataComponents.OLD_PAINLESS_HEAT.get(), heat);

        if (heat < MAX_HEAT || isOverheated(stack)) {
            return;
        }

        stack.set(HumanDataComponents.OLD_PAINLESS_OVERHEATED.get(), true);

        // ⭐ THE QUENCH. [stated] "when the gun overheats and stops i want it to make that noise when lave and water
        // mix to make stone and obsidian" - that is vanilla LAVA_EXTINGUISH (block.lava.extinguish), the hiss played
        // by LiquidBlock when lava meets water.
        // ⚠ SERVER-SIDE AND POSITIONAL, deliberately: passing null as the excluded player means EVERYONE nearby hears
        // it, including the shooter, and a marine's gun hisses too - the overheat applies to every shooter, not just
        // players. ⚠ It sits INSIDE the branch that has already returned unless heat just crossed MAX and the gun was
        // not already overheated, so it fires exactly ONCE per overheat, not every tick of the lockout.
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
            cooldowns.addCooldown(HumanGunItems.OLD_PAINLESS.get(), OVERHEAT_LOCKOUT_TICKS);
        }
        if (shooter instanceof Player player) {
            player.displayClientMessage(Component.literal("OLD PAINLESS OVERHEATED - COOLING"), true);
        }
    }

    public static void cool(ItemStack stack) {
        var heat = getHeat(stack);
        if (heat <= 0) {
            stack.remove(HumanDataComponents.OLD_PAINLESS_OVERHEATED.get());
            return;
        }

        var coolingRate = isOverheated(stack) ? OVERHEATED_COOLING_PER_TICK : PASSIVE_COOLING_PER_TICK;
        heat = Math.max(0, heat - coolingRate);
        stack.set(HumanDataComponents.OLD_PAINLESS_HEAT.get(), heat);
        if (heat == 0) {
            stack.remove(HumanDataComponents.OLD_PAINLESS_OVERHEATED.get());
        }
    }

    private OldPainlessHeat() {}

    /**
     * ⭐ SMOKE THAT THINS OUT AS THE BARRELS COOL. [stated] "add smokeparticles to it also that fade and go away as it
     * cools."
     * <p>
     * Density is driven straight off the heat fraction, so it is heaviest the moment the gun quenches and tapers to
     * nothing on its own — no separate timer to keep in sync with the heat, and it cannot outlive the cooldown. Below
     * {@value #SMOKE_THRESHOLD} of MAX_HEAT there is no smoke at all, matching the point where OldPainlessHeatLayer
     * starts tinting the barrel red so the two effects begin together.
     * </p>
     * <p>
     * ⚠⚠ SERVER-SIDE VIA {@code ServerLevel.sendParticles}, NOT {@code level.addParticle}. addParticle is client-only
     * and would spawn smoke for the holder alone; sendParticles broadcasts to everyone tracking the position, which is
     * what makes a squadmate's overheating gun visibly smoke. Same reasoning as the spin phase being a synced component
     * rather than client state.
     * </p>
     * <p>
     * ⚠ ONLY WHILE HELD. Called from inventoryTick with {@code selected}, so a hot gun sitting in a backpack still
     * COOLS (that is unchanged) but does not smoke through the player's chest.
     * </p>
     */
    public static void tickSmoke(Entity holder, ItemStack stack) {
        if (!(holder.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        var heatFraction = getHeat(stack) / (float) MAX_HEAT;

        if (heatFraction <= SMOKE_THRESHOLD) {
            return;
        }

        // Rescaled so the visible band spans 0..1 rather than starting a third of the way up.
        var intensity = (heatFraction - SMOKE_THRESHOLD) / (1.0F - SMOKE_THRESHOLD);

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
        // LARGE_SMOKE was the big one and it is gone.
        // <p>
        // ⚠ THE QUENCH KEEPS ITS WEIGHT VIA COUNT, NOT SIZE. Dropping LARGE_SMOKE would otherwise flatten the
        // overheat plume into the same wisp as ordinary cooling, so an overheated gun now emits TWO small puffs per
        // interval instead of one. Density reads as heavier smoke just as well as scale does, and it keeps thinning
        // out on the same curve.
        // </p>
        // ⏭ IF A LITERAL SIZE NUMBER IS EVER WANTED: DustParticleOptions(Vector3f colour, float scale) extends
        // ScalableParticleOptionsBase and takes an explicit scale — but dust hangs and fades in place instead of
        // rising and drifting, so it reads as soot rather than smoke. Would need a custom particle to get both.
        var count = isOverheated(stack) ? 2 : 1;

        serverLevel.sendParticles(ParticleTypes.SMOKE, x, y, z, count, 0.02D, 0.01D, 0.02D, 0.005D);
    }

    /** Matches OldPainlessHeatLayer's VISIBLE_HEAT_THRESHOLD so the smoke and the red tint start together. */
    private static final float SMOKE_THRESHOLD = 0.30F;
}
