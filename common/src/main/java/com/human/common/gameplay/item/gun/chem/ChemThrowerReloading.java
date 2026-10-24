package com.human.common.gameplay.item.gun.chem;

import com.blib.api.common.entity.v1.BLibEntityPredicates;
import com.blib.api.common.server.v1.ServerScheduler;
import com.human.Human;
import com.human.common.gameplay.item.GunItem;
import com.human.common.gameplay.item.gun.GunData;
import com.human.common.gameplay.item.gun.animation.GunAnimationEvents;
import com.human.common.gameplay.vapor.VaporKind;
import com.human.common.network.packet.S2COpenChemWheelPayload;
import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

/**
 * The chem thrower's reload - [stated] Oct 18, his rules.
 * <h2>THE FLOW</h2>
 * <ol>
 * <li>Reload with a tank on the gun: <b>remove</b> - the tank comes off (its skin stays on until the clip lifts it
 * away) - then <b>purge</b>: what was left of that vapor jets out of the gun's right side ({@link #purgeJet}) and is
 * gone. The gun is now EMPTY.</li>
 * <li>Then the new tank. Carrying ONE kind of vapor tank (however many - including only more of the same), it goes
 * straight on. Carrying TWO OR MORE kinds, the selection wheel opens ({@code ChemWheelScreen}) - only the kinds
 * actually carried - and the player keeps moving and jumping while it is up.</li>
 * <li><b>insert</b> - the chosen tank goes on in its own skin.</li>
 * </ol>
 * Reloading an EMPTY gun skips straight to step 2. [stated] Switching to another item and coming back to a gun that was
 * emptied mid-reload leaves it empty: the wheel closes, nothing is lost but the purged vapor, and the next reload (or
 * pulling the trigger) asks again.
 * <h2>GUARDS</h2>
 * <ul>
 * <li>Nothing is removed or purged unless a tank can follow it - no tank on you, no reload ("No vapor tank").</li>
 * <li>When the only tank you carry is the vapor already loaded, the flamethrower's rule holds: not until below 10 %, so
 * a stray press cannot purge a full tank. With another vapor to switch to, it is allowed any time.</li>
 * <li>Every step is checked again on the server: the pick must be a tank you really carry, the gun must be empty and
 * not mid-reload.</li>
 * </ul>
 * The tank is used up whole, 1,000 vapor ([stated] one tank = one fuel tank). Creative players load for free.
 */
public final class ChemThrowerReloading {

    /** His clip lengths: reload.remove 1.458 s, reload.purge 0.417 s (played 3 times), reload.insert 1.625 s. */
    public static final int REMOVE_TICKS = 29;

    public static final int PURGE_TICKS = 25;

    public static final int INSERT_TICKS = 33;

    /** Taking the tank off and purging it - after which the gun is empty. */
    public static final int UNLOAD_TICKS = REMOVE_TICKS + PURGE_TICKS;

    /** The whole reload with no pause at the wheel: what GunData reports as the reload time. */
    public static final int RELOAD_TICKS = UNLOAD_TICKS + INSERT_TICKS;

    /** QUICK CHARGE: each level takes this much off the remove and insert (the other guns' 20 % a level). */
    private static final double QUICK_CHARGE_PER_LEVEL = 0.2;

    /**
     * [stated] Oct 18 - QUICK CHARGE makes the REMOVE and INSERT faster; the PURGE cannot be. The share of the normal
     * time they take: 1.0, 0.8, 0.6, 0.4 for none / I / II / III. Their clips play faster to match
     * ({@code ChemThrowerAnimator}), and every timing here - the cooldowns, when the old tank's skin comes off, when
     * the purge jet starts, when the wheel opens - follows the same numbers.
     */
    public static double quickChargeScale(net.minecraft.world.level.Level level, ItemStack gun) {
        var quickCharge = com.blib.api.common.enchantment.v1.EnchantmentUtil.getLevel(
            level,
            gun,
            net.minecraft.world.item.enchantment.Enchantments.QUICK_CHARGE
        );

        return Math.max(0.25, 1.0 - QUICK_CHARGE_PER_LEVEL * Math.max(0, quickCharge));
    }

    public static int removeTicks(net.minecraft.world.level.Level level, ItemStack gun) {
        return Math.max(1, (int) Math.round(REMOVE_TICKS * quickChargeScale(level, gun)));
    }

    public static int insertTicks(net.minecraft.world.level.Level level, ItemStack gun) {
        return Math.max(1, (int) Math.round(INSERT_TICKS * quickChargeScale(level, gun)));
    }

    /** The flamethrower's rule: a tank may be swapped for the same vapor once below this fraction. */
    private static final float RELOAD_THRESHOLD = 0.10F;

    /**
     * QUALITY CHECK (Oct 18): the trigger on an EMPTY gun with no tanks reaches {@link #reload} every tick (the fire
     * pipeline's auto-reload), and "No vapor tank" went out as a packet each time. Once a second per player is plenty.
     */
       /**
        * ⚠ Oct 21 (tester: "if you switch items / chemthrowers whilst the first chemthrower is reloading it bugs out").
        * The reload in progress, per player, tied to the ONE gun that started it. Before, the steps after the purge
        * acted on whatever chem thrower was in the hand by then - so switching to a second one loaded a tank into the
        * wrong gun - and the reload's cooldown is on the chem thrower ITEM, so it locked every chem thrower the player
        * carried.
        */
    private record ActiveReload(
        ItemStack gun,
        long endTick
    ) {}

    private static final java.util.Map<Player, ActiveReload> ACTIVE = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private static final java.util.Map<Player, Long> NO_TANK_SHOWN = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private static final long NO_TANK_INTERVAL_TICKS = 20L;

    private ChemThrowerReloading() {}

    private static void noTank(Player player) {
        var now = player.level().getGameTime();
        var last = NO_TANK_SHOWN.get(player);

        if (last == null || now - last >= NO_TANK_INTERVAL_TICKS || now < last) {
            NO_TANK_SHOWN.put(player, now);
            player.displayClientMessage(Component.translatable("message.avp_human.chem_thrower.no_tank"), true);
        }
    }

    public static boolean isChemThrower(Item item) {
        return item instanceof GunItem gunItem && gunItem.getGunConfig() == GunData.CHEM_THROWER;
    }

    public static boolean isChemThrower(ItemStack stack) {
        return isChemThrower(stack.getItem());
    }

    /**
     * The vapor the gun sprays. ⚠ A gun with rounds but no vapor recorded (made by a command) reads as CRYO rather than
     * spraying nothing.
     */
    public static VaporKind loaded(ItemStack gun) {
        var kind = recorded(gun);
        return kind == null ? VaporKind.CRYO : kind;
    }

    /** The vapor recorded on the gun, or null if it has never been loaded. */
    public static @Nullable VaporKind recorded(ItemStack gun) {
        return VaporKind.byId(gun.get(HumanDataComponents.CHEM_VAPOR.get()));
    }

    /** The tank on the gun right now: none when it is empty. */
    public static @Nullable VaporKind onGun(ItemStack gun) {
        return gun.getOrDefault(HumanDataComponents.AMMUNITION.get(), 0) > 0 ? loaded(gun) : null;
    }

    /**
     * Which tank the gun SHOWS at this game time - the renderer's question. [stated] no ammo = no tank (it looks
     * empty). The one exception: while the remove clip plays, the tank being taken off is still drawn.
     */
    public static @Nullable VaporKind visibleTank(ItemStack gun, net.minecraft.world.level.Level level) {
        var gameTime = level.getGameTime();

        if (gun.getOrDefault(HumanDataComponents.AMMUNITION.get(), 0) > 0) {
            return loaded(gun);
        }

        var previous = VaporKind.byId(gun.get(HumanDataComponents.CHEM_PREVIOUS_VAPOR.get()));
        var started = gun.get(HumanDataComponents.CHEM_RELOAD_TICK.get());

        if (previous != null && started != null) {
            var since = gameTime - started;

            if (since >= 0 && since < removeTicks(level, gun)) {
                return previous;
            }
        }

        return null;
    }

    /** [stated] "it should only show the tanks on me": the kinds of filled vapor tank the player carries. */
    public static Set<VaporKind> carriedKinds(Player player) {
        var kinds = EnumSet.noneOf(VaporKind.class);
        var inventory = player.getInventory();

        for (var stack : inventory.items) {
            add(kinds, stack);
        }

        for (var stack : inventory.offhand) {
            add(kinds, stack);
        }

        return kinds;
    }

    private static void add(Set<VaporKind> kinds, ItemStack stack) {
        var kind = VaporKind.fromTank(stack);

        if (kind != null) {
            kinds.add(kind);
        }
    }

    /** Whether a reload has to ask: two or more kinds carried. */
    public static boolean needsChoice(Player player) {
        return choiceKinds(player).size() >= 2;
    }

    /**
     * The kinds the reload wheel offers: the tanks carried - or, [stated] Oct 19, for a creative player carrying none,
     * every kind. (R in creative used to load a free CRYO tank without asking.) Both sides.
     */
    public static Set<VaporKind> choiceKinds(Player player) {
        var carried = carriedKinds(player);

        if (carried.isEmpty() && BLibEntityPredicates.isInvulnerable(player)) {
            return EnumSet.allOf(VaporKind.class);
        }

        return carried;
    }

    /**
     * The reload key, or pulling the trigger on an empty gun - SERVER side. A tank on the gun: remove and purge it,
     * then {@link #afterPurge}. An empty gun: straight to the new tank. ⚠ With a choice to make on an empty gun this
     * does nothing - the client has already opened the wheel and the pick arrives separately.
     */
    public static void reload(Player player, ItemStack gun) {
        var level = player.level();

        if (level.isClientSide || !isChemThrower(gun) || player.getCooldowns().isOnCooldown(gun.getItem())) {
            return;
        }

        var kinds = carriedKinds(player);
        var immortal = BLibEntityPredicates.isInvulnerable(player);
        var old = onGun(gun);

        if (kinds.isEmpty() && !immortal) {
            noTank(player);
            return;
        }

        if (old == null) {
            // ⚠ Several kinds on an empty gun: NOTHING here. The wheel only opens from the reload key, and the client
            // opens it itself - this is also the path for pulling the trigger, which must not pop the wheel up.
            // Creative carrying no tank: nothing here either - R opens the wheel of every kind on the client
            // (choiceKinds), and the trigger must not.
            if (kinds.size() == 1) {
                loadOrAsk(player, gun, kinds, immortal);
            }

            return;
        }

        int current = gun.getOrDefault(HumanDataComponents.AMMUNITION.get(), 0);
        var maximum = GunData.CHEM_THROWER.maximumAmmunition();
        var onlyTheSame = kinds.size() == 1 && kinds.contains(old) || kinds.isEmpty();

        if (onlyTheSame && current >= maximum * RELOAD_THRESHOLD) {
            player.displayClientMessage(Component.translatable("message.avp.reload_too_early"), true);
            return;
        }

        // Remove + purge: the gun empties now; its old skin stays on through the remove clip.
        gun.set(HumanDataComponents.AMMUNITION.get(), 0);
        gun.set(HumanDataComponents.CHEM_PREVIOUS_VAPOR.get(), old.id());
        gun.set(HumanDataComponents.CHEM_RELOAD_TICK.get(), level.getGameTime());
        GunAnimationEvents.trigger(gun, GunAnimationEvents.CHEM_UNLOAD);
        var removeTicks = removeTicks(level, gun);
        var unloadTicks = removeTicks + PURGE_TICKS;

        player.getCooldowns().addCooldown(gun.getItem(), unloadTicks);

        var start = GunData.CHEM_THROWER.getDefaultFireMode().reloadStartSoundEvent();

        if (start != null) {
            level.playSound(null, player.blockPosition(), start.get(), SoundSource.PLAYERS);
        }

        if (level instanceof ServerLevel serverLevel) {
            purgeJet(serverLevel, player, old, removeTicks);
        }

        ownAnimator(player, gun);
        watch(player, gun, level.getGameTime() + unloadTicks);
        ServerScheduler.schedule(() -> afterPurge(player, gun), Duration.ofMillis(unloadTicks * 50L));
    }

    /**
     * Follows a reload tick by tick while it runs. If the gun that started it leaves the hand (another item, another
     * chem thrower, the gun moved in the inventory, death), the reload is called off: see {@link #cancel}.
     */
    private static void watch(Player player, ItemStack gun, long endTick) {
        var active = new ActiveReload(gun, endTick);
        ACTIVE.put(player, active);
        watchTick(player, active);
    }

    private static void watchTick(Player player, ActiveReload active) {
        ServerScheduler.schedule(() -> {
            if (ACTIVE.get(player) != active) {
                return; // finished, cancelled, or replaced by the next step
            }

            if (player.isRemoved() || !player.isAlive() || player.getMainHandItem() != active.gun()) {
                cancel(player, active.gun());
                return;
            }

            if (player.level().getGameTime() >= active.endTick()) {
                ACTIVE.remove(player, active);
                return;
            }

            watchTick(player, active);
        }, Duration.ofMillis(50L));
    }

    /**
     * Switched away mid-reload. [stated] switching away leaves the gun empty, to be filled on the next reload - and now
     * also: the cooldown comes off (it is on the chem thrower ITEM, so it locked every other chem thrower too), the old
     * tank's skin goes, and the gun's own animation is sent back to idle so it does not finish a reload that is no
     * longer happening when it is next held. A gun switched away DURING the insert keeps its new tank.
     */
    private static void cancel(Player player, ItemStack gun) {
        ACTIVE.remove(player);
        player.getCooldowns().removeCooldown(gun.getItem());
        gun.set(HumanDataComponents.CHEM_PREVIOUS_VAPOR.get(), "");
        GunAnimationEvents.trigger(gun, GunAnimationEvents.NONE);
    }

    /**
     * Each gun animates on its own animator, found by the stack's animation id - and a COPIED stack (creative
     * pick-block, a duplicated stack) carries the same id, so two chem throwers would play each other's reloads. The
     * reloading gun gets a fresh id if any other stack the player carries shares its id.
     */
    private static void ownAnimator(Player player, ItemStack gun) {
        var idType = com.blib.mod.common.registry.init.BLibDataComponents.AZ_ID.get();
        var id = gun.get(idType);

        if (id == null) {
            return;
        }

        var inventory = player.getInventory();

        for (var slot = 0; slot < inventory.getContainerSize(); slot++) {
            var other = inventory.getItem(slot);

            if (other != gun && !other.isEmpty() && id.equals(other.get(idType))) {
                gun.set(idType, java.util.UUID.randomUUID());
                return;
            }
        }
    }

    /**
     * The purge has finished. ⚠ Only if the player is still holding an empty chem thrower: [stated] switching away
     * leaves the gun empty, to be filled on the next reload.
     */
    private static void afterPurge(Player player, ItemStack gun) {
        var active = ACTIVE.get(player);

        // ⚠ Oct 21 - only the gun that started this reload, still in the hand (the watch has already called it off
        // otherwise). Never a different chem thrower picked up meanwhile.
        if (player.isRemoved() || !player.isAlive() || player.getMainHandItem() != gun || active == null || active.gun() != gun) {
            return;
        }

        ACTIVE.remove(player);

        if (!isChemThrower(gun) || onGun(gun) != null) {
            return;
        }

        loadOrAsk(player, gun, carriedKinds(player), BLibEntityPredicates.isInvulnerable(player));
    }

    /**
     * An empty gun: one kind carried - load it; several - open the wheel; none - stays empty (creative: the wheel of
     * every kind, after a purge).
     */
    private static void loadOrAsk(Player player, ItemStack gun, Set<VaporKind> kinds, boolean immortal) {
        if (kinds.size() == 1) {
            insert(player, gun, kinds.iterator().next());
        } else if (kinds.size() >= 2) {
            if (player instanceof ServerPlayer serverPlayer) {
                Human.MOD.networking().sendToClient(serverPlayer, S2COpenChemWheelPayload.INSTANCE);
            }
        } else if (immortal && player instanceof ServerPlayer serverPlayer) {
            // Creative with no tank: pick any kind from the wheel, free.
            Human.MOD.networking().sendToClient(serverPlayer, S2COpenChemWheelPayload.INSTANCE);
        }
    }

    /**
     * The insert: the chosen tank goes on an EMPTY gun - from the wheel, or because it was the only kind carried.
     * SERVER side; refuses anything that does not hold up (no such tank on the player, a gun that is not empty, a gun
     * mid-reload).
     */
    public static void insert(Player player, ItemStack gun, VaporKind chosen) {
        var level = player.level();

        if (level.isClientSide || !isChemThrower(gun) || onGun(gun) != null || player.getCooldowns().isOnCooldown(gun.getItem())) {
            return;
        }

        var immortal = BLibEntityPredicates.isInvulnerable(player);
        var source = findTank(player, chosen);

        if (source.isEmpty() && !immortal) {
            noTank(player);
            return;
        }

        if (!immortal) {
            // [stated] the tank is used up - nothing returned.
            source.shrink(1);
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }

        gun.set(HumanDataComponents.AMMUNITION.get(), VaporKind.PER_TANK);
        gun.set(HumanDataComponents.CHEM_VAPOR.get(), chosen.id());
        gun.set(HumanDataComponents.CHEM_PREVIOUS_VAPOR.get(), "");
        gun.set(HumanDataComponents.CHEM_RELOAD_TICK.get(), level.getGameTime());
        GunAnimationEvents.trigger(gun, GunAnimationEvents.CHEM_INSERT);
        var insertTicks = insertTicks(level, gun);

        player.getCooldowns().addCooldown(gun.getItem(), insertTicks);
        ownAnimator(player, gun);
        watch(player, gun, level.getGameTime() + insertTicks);

        var finish = GunData.CHEM_THROWER.getDefaultFireMode().reloadFinishSoundEvent();

        if (finish != null) {
            ServerScheduler.schedule(() -> {
                if (player.isAlive() && player.getMainHandItem() == gun) {
                    level.playSound(null, player.blockPosition(), finish.get(), SoundSource.PLAYERS);
                }
            }, Duration.ofMillis(insertTicks * 50L));
        }
    }

    /**
     * [stated] "purge would do a small fog jet out the right side of the gun of what ammo was inside": while the purge
     * clip plays, a short line of the OLD vapor's colour pushes out of the gun's right side. Sent from the server, so
     * every player nearby sees it, in the vapor puff.
     * <p>
     * ⚠ The jet follows the player each tick (it is re-aimed from where they now stand and look), and stops if they die
     * or put the gun away.
     * </p>
     */
    /** Where the gun is during the purge clip, from the eye: ahead, to the side (negative = left), up. */
    private static final double PURGE_FORWARD = 0.7D;

    private static final double PURGE_SIDEWAYS = -0.15D;

    private static final double PURGE_UP = -0.12D;

    private static void purgeJet(ServerLevel level, Player player, VaporKind vapor, int removeTicks) {
        // The old vapor's own near-the-nozzle style, small and quick - a short jet, not a cloud.
        var options = com.human.common.gameplay.vapor.VaporParticleOptions.of(
            vapor,
            com.human.common.gameplay.vapor.VaporLook.of(vapor).near(),
            0.25F,
            0.8F,
            14
        );

        for (var tick = 0; tick < PURGE_TICKS; tick += 2) {
            ServerScheduler.schedule(() -> {
                var holding = isChemThrower(player.getMainHandItem()) || isChemThrower(player.getOffhandItem());

                // Logged out, died, changed dimension or put the gun away: the jet stops.
                if (player.isRemoved() || !player.isAlive() || player.level() != level || !holding) {
                    return;
                }

                // [stated] Oct 19 - the jet came out at the lower right of the screen, where the gun is held normally -
                // but the purge clip lifts the gun up and across to the LEFT of centre, so the vapor appeared beside
                // empty air. It now starts where the gun sits during the purge (left of the crosshair, just below the
                // eye line) and blows out of its far side, away from the centre of the view. Camera-relative, so it
                // follows where the player looks, up or down. ⚠ Placed from his screenshot - tune in game.
                var look = player.getLookAngle();
                var yaw = Math.toRadians(player.getYRot());
                var right = new Vec3(-Math.cos(yaw), 0.0D, -Math.sin(yaw));
                var up = right.cross(look).normalize();
                var origin = player.getEyePosition()
                    .add(look.scale(PURGE_FORWARD))
                    .add(right.scale(PURGE_SIDEWAYS))
                    .add(up.scale(PURGE_UP));
                var out = right.scale(-1.0D).add(look.scale(0.45D)).add(up.scale(0.2D)).normalize();

                // A short jet: dust along a line out of the gun's side, thickest at the gun.
                for (var step = 0; step < 6; step++) {
                    var distance = 0.15D + step * 0.22D;
                    var point = origin.add(out.scale(distance));
                    var spread = 0.03D + step * 0.025D;

                    level.sendParticles(options, point.x, point.y, point.z, 2, spread, spread, spread, 0.0D);
                }
            }, Duration.ofMillis((removeTicks + tick) * 50L));
        }
    }

    /** A tank of this vapor on the player - the inventory (hotbar first), then the offhand. EMPTY if none. */
    private static ItemStack findTank(Player player, VaporKind kind) {
        var inventory = player.getInventory();

        for (var stack : inventory.items) {
            if (VaporKind.fromTank(stack) == kind) {
                return stack;
            }
        }

        for (var stack : inventory.offhand) {
            if (VaporKind.fromTank(stack) == kind) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * The gun is empty and the trigger was pulled - CLIENT side, from GunInputHandler (the fire pipeline is
     * server-only). With a choice to make, nothing opens - [stated] the wheel only ever comes up on the reload key - it
     * just says to press it; the server, seeing the same choice, waits.
     */
    public static void onEmptyClient(Player player, ItemStack gun) {
        if (needsChoice(player)) {
            player.displayClientMessage(Component.translatable("message.avp_human.chem_thrower.choose"), true);
        }
    }
}
