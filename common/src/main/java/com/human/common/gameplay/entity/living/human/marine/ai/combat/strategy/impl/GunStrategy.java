package com.human.common.gameplay.entity.living.human.marine.ai.combat.strategy.impl;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.blib.api.common.inventory.v1.BLibInventory;
import com.blib.api.common.inventory.v1.BLibInventoryHolder;
import com.human.common.gameplay.entity.living.human.ai.AttributeUtil;
import com.human.common.gameplay.entity.living.human.marine.ai.combat.CombatSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.combat.strategy.WeaponStrategy;
import com.human.common.gameplay.item.GunItem;
import com.human.common.gameplay.item.gun.FireModeConfig;
import com.human.common.gameplay.item.gun.animation.GunAnimationEvents;
import com.human.common.registry.init.HumanDataComponents;
import com.human.common.registry.tag.HumanItemTags;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.state.Blackboard;
import com.just.ai.goap.state.ReadableWorldState;
import com.just.core.functional.option.Option;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class GunStrategy implements WeaponStrategy {

    private static final StateKey<Integer> TICK_COUNTDOWN = StateKey.sensed("tick_countdown");

    /** Tick at which this marine may next pull the trigger. */
    private static final StateKey<Integer> NEXT_SHOT_TICK = StateKey.sensed("next_shot_tick");

    /** Rounds left in the current burst, for weapons fast enough to need bursting. */
    private static final StateKey<Integer> BURST_REMAINING = StateKey.sensed("burst_remaining");

    /** Gap between bursts. Burst LENGTH is per-weapon; see {@code FireModeConfig.burstRounds}. */
    private static final int BURST_PAUSE_IN_TICKS = 30;

    /**
     * What the crowd-control axis scores when there is only one enemy - the middle, so a single target neither rewards
     * nor punishes any weapon for its spread.
     */
    private static final double NEUTRAL_CROWD_CONTROL_SCORE = 0.5;

    /**
     * Projectiles per second at which the crowd-control term reaches half strength. Set near a rifle's rate so the
     * curve separates rifles from miniguns rather than bunching them at the top.
     */
    private static final double CROWD_CONTROL_HALF_POINT = 10.0;

    /**
     * Inside this distance a spin-up weapon starts being the wrong tool. Roughly the point at which something closing
     * on a marine will reach it before the barrels come up to speed.
     */
    private static final double CLOSE_QUARTERS_DISTANCE_IN_BLOCKS = 8.0;

    /**
     * Spin-up at which the close-quarters handicap is at full strength. Old Painless is the only weapon that declares
     * one at all, at exactly this figure - but the penalty is derived rather than named, so a future spin-up weapon
     * inherits it and a weapon whose spin-up is tuned down is handicapped less, both without touching this code.
     */
    private static final double FULL_SPIN_UP_IN_TICKS = 20.0;

    /**
     * How much of the range score a full spin-up loses at point-blank.
     * <p>
     * ⚠ TUNED AGAINST THE ISSUED-WEAPON BONUS, not in isolation. At 0.6 a minigun ranked below the PISTOL at one block,
     * which is a caricature rather than a handicap. At 0.45 the shotguns take the top spot inside about four blocks
     * even when the marine was ISSUED the minigun and merely handed the shotgun, the minigun still outranks the pulse
     * rifle and pistol at every distance, and it reclaims first place by eight blocks.
     */
    private static final double MAXIMUM_CLOSE_QUARTERS_PENALTY = 0.45;

    /** Tick at which an in-progress reload completes, or {@link #NOT_RELOADING} when idle. */
    private static final StateKey<Integer> RELOAD_FINISH_TICK = StateKey.sensed("reload_finish_tick");

    /** Blackboard has set/get but no remove, so idleness needs a value rather than an absence. */
    private static final int NOT_RELOADING = Integer.MIN_VALUE;

    /**
     * A marine swaps magazines once the current one drops below this fraction, the same threshold players are held to.
     * <p>
     * Deliberately NOT zero. At zero the gun stops being a valid weapon, GOAP re-scores and hands the marine something
     * else entirely, and the reload never happens. Reloading while there are still rounds left keeps the weapon
     * equipped and keeps them dangerous through the swap.
     */
    private static final float RELOAD_THRESHOLD = 0.10F;

    @Override
    public boolean isValidItemStack(ItemStack itemStack) {
        if (!itemStack.is(HumanItemTags.GUNS)) {
            return false;
        }

        return itemStack.getOrDefault(HumanDataComponents.AMMUNITION.get(), 0) > 0;
    }

    @Override
    public boolean isValidWorldState(LivingEntity livingEntity, ReadableWorldState worldState) {
        return true;
    }

    @Override
    public ScoreResult computeScore(LivingEntity livingEntity, ReadableWorldState worldState, ItemStack itemStack) {
        if (!(livingEntity instanceof Mob mob)) {
            return ScoreResult.zero();
        }

        var target = mob.getTarget();

        if (target == null) {
            return ScoreResult.zero();
        }

        var effectivenessScore = computeEffectivenessScore(mob, target, worldState, itemStack);
        var rangeScore = computeRangeScore(mob, target, worldState, itemStack);
        var riskScore = computeRiskScore(mob, target, worldState, itemStack);
        var crowdControlScore = computeCrowdControlScore(worldState, itemStack);
        var familiarityScore = computeFamiliarityScore(itemStack);

        return ScoreResult.of(
            Weights.DEFAULT,
            effectivenessScore,
            rangeScore,
            riskScore,
            crowdControlScore,
            familiarityScore
        );
    }

    @Override
    public double getRangeForWeapon(LivingEntity livingEntity, ItemStack itemStack) {
        if (itemStack.getItem() instanceof GunItem gunItem) {
            return gunItem.getGunConfig().getDefaultFireMode().range();
        }

        var attributes = livingEntity.getAttributes();
        var attribute = Attributes.ENTITY_INTERACTION_RANGE;

        return attributes.hasAttribute(attribute)
            ? attributes.getBaseValue(attribute)
            : attribute.value().getDefaultValue();
    }

    @Override
    public Action.Signal execute(Action.Context<? extends LivingEntity> context) {
        var livingEntity = context.getActor();
        var worldState = context.getWorldState();
        var blackboard = context.getBlackboard(Blackboard.Scope.ACTION);

        if (!(livingEntity instanceof Mob mob)) {
            return Action.Signal.ABORT;
        }

        var target = mob.getTarget();

        if (target == null) {
            return Action.Signal.ABORT;
        }

        var currentTickCountdown = blackboard.getOrNull(TICK_COUNTDOWN);

        if (currentTickCountdown == null) {
            currentTickCountdown = Integer.MAX_VALUE;
            blackboard.set(TICK_COUNTDOWN, currentTickCountdown);
        } else {
            currentTickCountdown -= 1;
            blackboard.set(TICK_COUNTDOWN, currentTickCountdown);
        }

        var tickCountdown = currentTickCountdown;
        var equippedWeaponOption = worldState.getOrDefault(CombatSensors.BEST_WEAPON_IN_HANDS.key(), Option.none());

        equippedWeaponOption.ifSome(equippedWeapon -> {
            var equipmentSlot = equippedWeapon.itemTarget().equipmentSlot();
            var itemStack = mob.getItemBySlot(equipmentSlot);

            if (itemStack.getItem() instanceof GunItem gunItem) {
                // Always look at the target while shooting.
                mob.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
                mob.getLookControl().setLookAt(target);
                squareBodyToAim(mob);

                if (tickReload(mob, blackboard, gunItem, itemStack)) {
                    // Committed to the magazine change. Not interruptible, and deliberately not protected either -
                    // a marine would rather take a hit mid-swap than stand there empty.
                    return;
                }

                if (canPullTrigger(mob, blackboard)) {
                    gunItem.onUseTick(mob.level(), mob, itemStack, tickCountdown);
                    scheduleNextShot(mob, blackboard, gunItem);
                }
            }
        });

        return Action.Signal.CONTINUE;
    }

    private double computeEffectivenessScore(Mob mob, LivingEntity target, ReadableWorldState worldState, ItemStack itemStack) {
        if (itemStack.getItem() instanceof GunItem gunItem) {
            var fireMode = gunItem.getGunConfig().getDefaultFireMode();
            // ⚠ PELLETS COUNT. A shotgun shell is eight projectiles and the score used to read only one of them, so a
            // weapon that hits for 128 up close was rated at 16 and ranked below a pistol.
            var attackDamage = fireMode.damage() * Math.max(1, fireMode.pelletCount());
            var attackSpeed = 20.0 / Math.max(fireMode.cooldownInTicks(), 0.001);
            var damagePerSecond = attackDamage * attackSpeed;
            var targetHealth = target.getHealth();
            var timeToKillInSeconds = targetHealth / Math.max(damagePerSecond, 0.001);

            // TODO: This is a very rough approximation and doesn't account for the target's held weapons.
            var incomingDamagePerSecond = AttributeUtil.getAttributeBaseOrDefaultValue(target, Attributes.ATTACK_DAMAGE)
                * AttributeUtil.getAttributeBaseOrDefaultValue(target, Attributes.ATTACK_SPEED);
            // How long until the target can kill us?
            var timeToFailureInSeconds = mob.getHealth() / Math.max(incomingDamagePerSecond, 0.001);
            // If less than 1, the target killing us is faster than us killing the target.
            // If greater than 1, the target killing us is slower than us killing the target.
            var ratio = timeToFailureInSeconds / Math.max(timeToKillInSeconds, 0.001);
            // Ex. 99 / 100 = 0.99... Larger ratios approach 1.
            // 0.01 / 1.01 = 0.0099... Smaller ratios approach 0.
            var effectiveness = ratio / (ratio + 1.0);

            return Math.clamp(effectiveness, 0.0, 1.0);
        }

        return 0;
    }

    /**
     * How well this weapon suits the distance the fight is actually being fought at.
     * <p>
     * ⚠ THIS USED TO RETURN A FLAT 1.0 FOR ANYTHING IN RANGE, and since the shortest range in the game is sixteen
     * blocks, every gun scored full marks in every normal engagement. Range contributed nothing, and a marine would
     * happily pick a shotgun for a target thirty blocks away.
     * <p>
     * The curve is the weapon's OWN damage falloff, the same numbers {@code HitScanGunAttackAction} applies when a
     * round lands - so a shotgun (falloff from 6 blocks, down to 15% at 20) scores 1.0 in a corridor and 0.15 across a
     * clearing, while a rifle holds most of its score to 64. Nothing is invented: a weapon that keeps its damage keeps
     * its score.
     */
    private double computeRangeScore(Mob mob, LivingEntity target, ReadableWorldState worldState, ItemStack itemStack) {
        var reach = getRangeForWeapon(mob, itemStack);
        var distance = mob.distanceTo(target);

        if (distance > reach) {
            // Out of range entirely. Falls away sharply rather than to zero, so an out-of-reach gun is still preferred
            // over no gun and the marine closes distance with it rather than standing empty-handed.
            return Math.exp(-(distance - reach) * 2.5) * 0.1;
        }

        if (!(itemStack.getItem() instanceof GunItem gunItem)) {
            return 1.0;
        }

        var fireMode = gunItem.getGunConfig().getDefaultFireMode();
        var falloffStart = reach * fireMode.damageFalloffStartFraction();
        var falloffSpan = Math.max(0.001, reach - falloffStart);
        var progress = Math.clamp((distance - falloffStart) / falloffSpan, 0.0, 1.0);
        var falloffScore = 1.0 - ((1.0 - fireMode.minimumDamageMultiplier()) * progress);

        return falloffScore * (1.0 - closeQuartersPenalty(fireMode, distance));
    }

    /**
     * The handicap a weapon that has to spin up carries in close quarters.
     * <p>
     * A minigun is not the thing you want in your hands when something is already inside arm's reach: the barrels are
     * still coming up to speed while a shotgun has already fired twice. Read off {@code shootDelayInTicks} rather than
     * naming a weapon, so it is the SPIN-UP being penalised and not Old Painless specifically.
     * <p>
     * Scales from nothing at eight blocks to full strength at point-blank, so the weapon is only ever discouraged where
     * the problem actually exists - past that distance it is the best gun in the game again.
     */
    private double closeQuartersPenalty(FireModeConfig fireMode, double distance) {
        var spinUpFraction = Math.clamp(fireMode.shootDelayInTicks() / FULL_SPIN_UP_IN_TICKS, 0.0, 1.0);

        if (spinUpFraction <= 0.0 || distance >= CLOSE_QUARTERS_DISTANCE_IN_BLOCKS) {
            return 0.0;
        }

        var closeness = Math.clamp(
            (CLOSE_QUARTERS_DISTANCE_IN_BLOCKS - distance) / CLOSE_QUARTERS_DISTANCE_IN_BLOCKS,
            0.0,
            1.0
        );

        return closeness * spinUpFraction * MAXIMUM_CLOSE_QUARTERS_PENALTY;
    }

    /**
     * How well this weapon handles more than one enemy at a time.
     * <p>
     * Rated on what actually makes a weapon good in a crowd: how many projectiles leave the barrel per second. A
     * flamethrower and a minigun win on rate, a shotgun on spread, a bolt-action sniper loses on both. The term is
     * NEUTRAL against a lone target and only starts mattering as a second and third enemy arrive, so it never drags a
     * marine off a precise weapon in a duel.
     */
    private double computeCrowdControlScore(ReadableWorldState worldState, ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof GunItem gunItem)) {
            return NEUTRAL_CROWD_CONTROL_SCORE;
        }

        var targetCount = worldState.getOrDefault(GOAPSensors.NEAREST_ATTACKABLE_TARGETS.key(), List.<LivingEntity>of())
            .size();

        if (targetCount <= 1) {
            return NEUTRAL_CROWD_CONTROL_SCORE;
        }

        var fireMode = gunItem.getGunConfig().getDefaultFireMode();
        var projectilesPerSecond = Math.max(1, fireMode.pelletCount())
            * (20.0 / Math.max(fireMode.cooldownInTicks(), 0.001));
        // Saturating, so the difference between a rifle and a minigun matters far more than between two miniguns.
        var suitability = projectilesPerSecond / (projectilesPerSecond + CROWD_CONTROL_HALF_POINT);
        // Blended in by crowd size, reaching full strength at four enemies.
        var crowdWeight = Math.clamp((targetCount - 1) / 3.0, 0.0, 1.0);

        return NEUTRAL_CROWD_CONTROL_SCORE + (suitability - NEUTRAL_CROWD_CONTROL_SCORE) * crowdWeight;
    }

    /**
     * A thumb on the scale for the weapon the marine was issued with.
     * <p>
     * ⚠ Deliberately small, and this is the balance he asked for: a marine carries spare magazines for its OWN weapon
     * and none for anything it picks up, so its issued rifle is worth more to it than the same rifle would be to
     * someone else. But a genuinely better gun handed over by a player still wins on the other four axes - the bonus
     * settles close calls, it does not veto.
     */
    private double computeFamiliarityScore(ItemStack itemStack) {
        return itemStack.getOrDefault(HumanDataComponents.MARINE_OWNED.get(), false) ? 1.0 : 0.0;
    }

    private double computeRiskScore(Mob mob, LivingEntity target, ReadableWorldState worldState, ItemStack itemStack) {
        var targetAttackDamage = AttributeUtil.getAttributeBaseOrDefaultValue(target, Attributes.ATTACK_DAMAGE);
        return Math.clamp(targetAttackDamage / mob.getHealth(), 0, 1);
    }

    /**
     * Whether this marine is allowed to fire this tick.
     * <p>
     * Marines used to call {@code onUseTick} every single tick and lean entirely on the weapon's own
     * {@code ItemCooldowns} gate to space the shots out. Whatever the reason that gate was not holding for them, the
     * result was every gun emptying at tick rate — a pump shotgun sounding like a minigun. Pacing here does not depend
     * on it: the marine simply does not pull the trigger until its own clock says so, and if the item cooldown IS
     * working the two agree rather than fight, because both are minimum intervals.
     */
    private static boolean canPullTrigger(Mob mob, Blackboard blackboard) {
        var nextShotTick = blackboard.getOrNull(NEXT_SHOT_TICK);

        return nextShotTick == null || mob.tickCount >= nextShotTick;
    }

    /**
     * Books the next trigger pull, one weapon cooldown later — or a longer pause if that round finished a burst.
     * <p>
     * Burst LENGTH comes from the weapon itself rather than a rule of thumb about its rate of fire: a pulse rifle fires
     * three, a smartgun sixteen, Old Painless sixty-four. Weapons that declare no burst simply keep firing.
     */
    private static void scheduleNextShot(Mob mob, Blackboard blackboard, GunItem gunItem) {
        var fireMode = gunItem.getGunConfig().getDefaultFireMode();
        var cooldownInTicks = Math.max(1, fireMode.cooldownInTicks());
        var burstRounds = fireMode.burstRounds();

        // burstRounds 1 means the weapon does not burst at all - a flamethrower should keep going, a pump shotgun is
        // already paced by its own twenty-tick cooldown. Only weapons that declare a burst get the pause.
        if (burstRounds <= 1) {
            blackboard.set(NEXT_SHOT_TICK, mob.tickCount + cooldownInTicks);

            return;
        }

        var burstRemaining = blackboard.getOrNull(BURST_REMAINING);

        if (burstRemaining == null) {
            burstRemaining = burstRounds;
        }

        burstRemaining -= 1;

        if (burstRemaining <= 0) {
            blackboard.set(BURST_REMAINING, burstRounds);
            blackboard.set(NEXT_SHOT_TICK, mob.tickCount + BURST_PAUSE_IN_TICKS);

            return;
        }

        blackboard.set(BURST_REMAINING, burstRemaining);
        blackboard.set(NEXT_SHOT_TICK, mob.tickCount + cooldownInTicks);
    }

    /**
     * Runs the reload state machine. Returns true while the marine is busy swapping magazines and must not fire.
     * <p>
     * Started at {@link #RELOAD_THRESHOLD} rather than on empty, so the weapon never stops being valid mid-fight. Once
     * begun it runs to completion: nothing here cancels it, and nothing here grants immunity.
     */
    private static boolean tickReload(Mob mob, Blackboard blackboard, GunItem gunItem, ItemStack itemStack) {
        var reloadFinishTick = blackboard.getOrDefault(RELOAD_FINISH_TICK, NOT_RELOADING);

        if (reloadFinishTick != NOT_RELOADING) {
            if (mob.tickCount < reloadFinishTick) {
                return true;
            }

            finishReload(mob, gunItem, itemStack);
            blackboard.set(RELOAD_FINISH_TICK, NOT_RELOADING);

            return false;
        }

        if (!(mob instanceof BLibInventoryHolder inventoryHolder)) {
            return false;
        }

        var gunConfig = gunItem.getGunConfig();
        var ammunitionItemSupplier = gunConfig.ammunitionItemSupplier();

        if (ammunitionItemSupplier == null) {
            return false;
        }

        var currentAmmunition = itemStack.getOrDefault(HumanDataComponents.AMMUNITION.get(), 0);

        if (currentAmmunition >= gunConfig.maximumAmmunition() * RELOAD_THRESHOLD) {
            return false;
        }

        if (!inventoryHolder.getInventory().hasItem(ammunitionItemSupplier.get().asItem())) {
            return false;
        }

        blackboard.set(RELOAD_FINISH_TICK, mob.tickCount + Math.max(1, gunConfig.reloadTimeInTicks()));
        // Open the next magazine with a fresh burst rather than resuming mid-string.
        blackboard.set(BURST_REMAINING, gunConfig.getDefaultFireMode().burstRounds());

        GunAnimationEvents.trigger(itemStack, GunAnimationEvents.RELOAD);
        playReloadSound(mob, gunConfig.getDefaultFireMode().reloadStartSoundEvent());

        return true;
    }

    private static void finishReload(Mob mob, GunItem gunItem, ItemStack itemStack) {
        if (!(mob instanceof BLibInventoryHolder inventoryHolder)) {
            return;
        }

        var gunConfig = gunItem.getGunConfig();
        var ammunitionItemSupplier = gunConfig.ammunitionItemSupplier();

        if (ammunitionItemSupplier == null || !inventoryHolder.getInventory().hasItem(ammunitionItemSupplier.get().asItem())) {
            return;
        }

        var currentAmmunition = itemStack.getOrDefault(HumanDataComponents.AMMUNITION.get(), 0);
        var missingAmmunition = gunConfig.maximumAmmunition() - currentAmmunition;

        if (missingAmmunition <= 0) {
            return;
        }

        // ⚠ How many ITEMS to take, not one. reloadAmount is how many rounds a single item is worth, and it DEFAULTS
        // TO 1 - only the flamethrower, smartgun and Old Painless declare otherwise. So taking a single item and adding
        // reloadAmount gave every round-fed gun exactly one bullet per reload: a shotgun came back from empty with a
        // single shell in it. This mirrors what GunReloading does for the player.
        var reloadAmount = Math.max(1, gunConfig.reloadAmount());
        var neededItems = (int) Math.ceil(missingAmmunition / (float) reloadAmount);
        var removeResult = inventoryHolder.getInventory().removeItem(ammunitionItemSupplier.get().asItem(), neededItems);

        // A partial take is a real outcome, not a failure - a marine down to its last two shells loads two and fights
        // on. Its count is what was actually removed.
        var consumedItems = neededItems;

        if (removeResult instanceof BLibInventory.RemoveResult.Partial partial) {
            consumedItems = partial.count();
        } else if (removeResult instanceof BLibInventory.RemoveResult.InventoryEmpty) {
            consumedItems = 0;
        }

        if (consumedItems <= 0) {
            return;
        }

        // Carries the leftover rounds over, exactly as a player's reload does.
        itemStack.set(HumanDataComponents.AMMUNITION.get(), currentAmmunition + consumedItems * reloadAmount);

        playReloadSound(mob, gunConfig.getDefaultFireMode().reloadFinishSoundEvent());
    }

    /**
     * Turns the marine's BODY to face what it is shooting at.
     * <p>
     * ⚠⚠ Without this the gun visibly points somewhere the bullets do not go. {@code lookAt} sets {@code yRot}, which
     * is what the shot is traced along, and {@code yHeadRot}, which turns the head - but NEITHER of those is what the
     * arms follow. The arms hang off {@code yBodyRot}, and for a STATIONARY mob that is driven by
     * {@code BodyRotationControl}, whose {@code rotateBodyIfNecessary} only ever pulls the body to within
     * {@code getMaxHeadYRot()} of the head. That is 75 degrees, and {@code Mth.rotateIfNecessary} stops the moment it
     * is inside the limit rather than continuing to square up - so a marine standing still and firing settles with its
     * body up to 75 degrees off its own aim, holding the rifle out sideways.
     * <p>
     * A marine that walks while shooting never showed it, because a MOVING mob has its body snapped to {@code yRot}
     * every tick by that same control. This only reproduces when they plant their feet.
     * <p>
     * Assigning the body directly is what a soldier shouldering a weapon actually does, and it agrees with the moving
     * case rather than fighting it. {@code yBodyRotO} is deliberately left alone so the renderer still interpolates
     * from the previous angle instead of snapping.
     */
    private static void squareBodyToAim(Mob mob) {
        mob.yBodyRot = mob.getYRot();
    }

    private static void playReloadSound(Mob mob, @Nullable Supplier<SoundEvent> soundEventSupplier) {
        if (soundEventSupplier == null) {
            return;
        }

        mob.level().playSound(null, mob.blockPosition(), soundEventSupplier.get(), SoundSource.HOSTILE);
    }
}
