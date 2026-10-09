package com.human.common.gameplay.item;

import com.blib.api.common.data_sync.v1.model.DataUser;
import com.human.common.registry.init.HumanDataSyncKeys;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The titanium capture cord's own tether. [stated] Oct 3 - the CLEAN ROUTE: the cord no longer rides on vanilla's
 * leash.
 * <p>
 * ⚠ Why: a vanilla leash draws vanilla's rope, and on the alien capture chain THREE separate ways of cancelling that
 * rope (inject+cancel, WrapWithCondition, WrapOperation) all left it showing, on both loaders. With no vanilla leash
 * there is no rope to hide - the cord is only ever drawn by our own renderer - and the "break" is ours, so it always
 * returns the cord, never a lead.
 * </p>
 * <h2>How it holds</h2> Within 6 blocks the mob is free; past that it is pulled toward the holder with vanilla's own
 * elastic formula and walks after them; past {@link TitaniumCaptureCordItem#SNAP_DISTANCE} the cord snaps and drops. It
 * also drops if either end dies, the holder leaves (logs out, changes dimension) or the server restarts. Right-clicking
 * the mob you hold lets it go and hands the cord back.
 * <h2>State</h2> Server: the mob keeps {@link TitaniumCaptureCordItem#CORD_TAG} (saved), and who holds it is kept here.
 * Clients: the holder's entity id is synced on the mob, which is all the renderer needs.
 */
public final class CaptureCordTether {

    /** Free play before the pull starts - vanilla's leash uses the same 6. */
    private static final double SLACK = 6.0;

    /** The synced "nobody holds it" value. */
    public static final int NO_HOLDER = -1;

    /** Held mob -> its holder. Server only; not saved - a restart drops the cord on the floor, never loses it. */
    private static final Map<UUID, UUID> HELD = new HashMap<>();

    private CaptureCordTether() {}

    // ------------------------------------------------------------------------------------------------------------
    // Attaching and letting go
    // ------------------------------------------------------------------------------------------------------------

    /** Ties {@code mob} to {@code player}. Server only. */
    public static void attach(Mob mob, Player player) {
        mob.addTag(TitaniumCaptureCordItem.CORD_TAG);
        HELD.put(mob.getUUID(), player.getUUID());
        syncHolder(mob, player.getId());
        mob.level().playSound(null, mob.blockPosition(), SoundEvents.LEASH_KNOT_PLACE, SoundSource.NEUTRAL, 0.5F, 1.0F);
    }

    /** Lets {@code mob} go and hands the cord to {@code player} (on the floor if their inventory is full). */
    public static void release(Mob mob, Player player) {
        clear(mob);

        if (!player.getAbilities().instabuild) {
            var cord = new ItemStack(HumanItems.TITANIUM_CAPTURE_CORD.get());

            if (!player.getInventory().add(cord)) {
                player.drop(cord, false);
            }
        }

        mob.level().playSound(null, mob.blockPosition(), SoundEvents.LEASH_KNOT_BREAK, SoundSource.NEUTRAL, 0.5F, 1.0F);
    }

    /** The cord snaps: it falls at the mob. */
    private static void snap(Mob mob) {
        clear(mob);
        mob.spawnAtLocation(HumanItems.TITANIUM_CAPTURE_CORD.get());
        mob.level().playSound(null, mob.blockPosition(), SoundEvents.LEASH_KNOT_BREAK, SoundSource.NEUTRAL, 0.5F, 1.0F);
    }

    private static void clear(Mob mob) {
        mob.removeTag(TitaniumCaptureCordItem.CORD_TAG);
        HELD.remove(mob.getUUID());
        syncHolder(mob, NO_HOLDER);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Asking
    // ------------------------------------------------------------------------------------------------------------

    /** Whether {@code mob} is on a cord right now - server or client. */
    public static boolean isHeld(Mob mob) {
        return holderId(mob) != NO_HOLDER;
    }

    /** The holder's entity id as synced, or {@link #NO_HOLDER}. Works on both sides. */
    public static int holderId(Mob mob) {
        var id = ((DataUser) mob).getDataContainer().get(HumanDataSyncKeys.ENTITY_CAPTURE_CORD_HOLDER.get());
        return id == null ? NO_HOLDER : id;
    }

    /** Whether {@code player} holds {@code mob} on a cord. Works on both sides. */
    public static boolean isHeldBy(Mob mob, Player player) {
        return holderId(mob) == player.getId();
    }

    /** The nearest mob {@code player} holds on a cord, within reach, or null. Works on both sides. */
    public static @Nullable Mob heldBy(Level level, Player player) {
        var reach = TitaniumCaptureCordItem.SNAP_DISTANCE + 2.0;

        return level.getEntitiesOfClass(Mob.class, new AABB(player.blockPosition()).inflate(reach), mob -> isHeldBy(mob, player))
            .stream()
            .min(Comparator.comparingDouble(mob -> mob.distanceToSqr(player)))
            .orElse(null);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The tick
    // ------------------------------------------------------------------------------------------------------------

    /** Every server tick of a mob carrying the cord tag (or still showing a holder). Cheap for everything else. */
    public static void tick(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        if (!mob.getTags().contains(TitaniumCaptureCordItem.CORD_TAG)) {
            if (holderId(mob) != NO_HOLDER) {
                syncHolder(mob, NO_HOLDER);
            }

            return;
        }

        // A world saved before the clean route: the cord was a vanilla leash. Move it onto the tether, no item dropped.
        if (mob.isLeashed() && mob.getLeashHolder() instanceof Player oldHolder) {
            mob.dropLeash(true, false);
            mob.addTag(TitaniumCaptureCordItem.CORD_TAG);
            HELD.put(mob.getUUID(), oldHolder.getUUID());
        }

        var holderUuid = HELD.get(mob.getUUID());
        var holder = holderUuid == null ? null : level.getServer().getPlayerList().getPlayer(holderUuid);

        if (holder == null || !holder.isAlive() || holder.isSpectator() || holder.level() != level || !mob.isAlive()) {
            snap(mob);
            return;
        }

        var distance = mob.distanceTo(holder);

        if (distance > TitaniumCaptureCordItem.SNAP_DISTANCE) {
            snap(mob);
            return;
        }

        if (distance > SLACK) {
            // Vanilla's elastic leash pull, verbatim in form: a nudge toward the holder that grows with the gap.
            var dx = (holder.getX() - mob.getX()) / distance;
            var dy = (holder.getY() - mob.getY()) / distance;
            var dz = (holder.getZ() - mob.getZ()) / distance;
            mob.setDeltaMovement(
                mob.getDeltaMovement()
                    .add(Math.copySign(dx * dx * 0.4, dx), Math.copySign(dy * dy * 0.4, dy), Math.copySign(dz * dz * 0.4, dz))
            );
            mob.getNavigation().moveTo(holder, 1.0);
        }

        if (holderId(mob) != holder.getId()) {
            syncHolder(mob, holder.getId());
        }
    }

    private static void syncHolder(Mob mob, int id) {
        ((DataUser) mob).getDataContainer().set(HumanDataSyncKeys.ENTITY_CAPTURE_CORD_HOLDER.get(), id);
    }
}
