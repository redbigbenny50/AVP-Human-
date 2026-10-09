package com.human.client.targeting;

import com.human.client.render.CameraMatrices;
import com.human.client.render.hud.SmartHudLayout;
import com.human.common.gameplay.item.gun.targeting.GunTargetingProfile;
import com.human.common.gameplay.item.gun.targeting.SmartTargeting;
import com.human.common.registry.tag.HumanEntityTypeTags;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Client-side acquisition: who is in the frame, who is being tracked, and how far through the lock we are.
 * <p>
 * ⚠⚠ THIS RUNS ON THE CLIENT AND DECIDES NOTHING THAT MATTERS. It picks what to DRAW. When the lock starts granting
 * accuracy and armour penetration in phase three, the chosen entity is sent to the server and the server RE-VALIDATES
 * it on every shot - still in frame, still visible, still in range. An unvalidated "I am locked onto X" packet is a
 * damage buff a modified client can claim against anything, anywhere.
 * </p>
 * <p>
 * ⚠ SCANNED ONCE PER TICK, DRAWN EVERY FRAME. The entity search and line-of-sight tests are the expensive part and
 * twenty times a second is plenty; the renderer re-projects the chosen target each frame so the box still moves
 * smoothly at any frame rate.
 * </p>
 */
public final class SmartTargetingClient {

    /**
     * How long the button must be held before a press counts as HOLDING rather than a TAP. A quarter of a second is
     * comfortably longer than any click and comfortably shorter than a deliberate hold.
     */
    private static final int HOLD_THRESHOLD_TICKS = 5;

    private static @Nullable LivingEntity target;

    /**
     * ⭐⭐ THE PLAYER'S CHOICE, AND IT OUTRANKS THE AUTOMATIC PICK ENTIRELY. Once set it is held until the target dies,
     * leaves range, or the player chooses again - it does NOT revert when the button is released, and the automatic
     * "most central contact" rule is not consulted at all while it is set.
     */
    private static @Nullable LivingEntity manualTarget;

    private static int dwellTicks;

    private static boolean locked;

    private static boolean useWasDown;

    private static int useHeldTicks;

    private static final List<LivingEntity> contacts = new ArrayList<>();

    public static void clientTick(Minecraft minecraft) {
        var player = minecraft.player;
        var level = minecraft.level;

        if (player == null || level == null || !minecraft.options.getCameraType().isFirstPerson()) {
            clear(minecraft, null);
            return;
        }

        var heldStack = player.getMainHandItem();
        var profile = SmartTargeting.resolve(heldStack);

        if (profile == null || !CameraMatrices.isValid()) {
            clear(minecraft, profile);
            return;
        }

        var layout = SmartHudLayout.of(
            profile,
            minecraft.getWindow().getGuiScaledWidth(),
            minecraft.getWindow().getGuiScaledHeight()
        );

        refreshContacts(minecraft, player, profile, layout);

        var previous = target;

        handleRetargetInput(minecraft);
        forgetManualTargetIfGone(player, profile);

        // ⚠ MANUAL FIRST, ALWAYS. The automatic rule is only consulted when the player has not made a choice.
        target = manualTarget != null && contacts.contains(manualTarget) ? manualTarget : pickAutomatically();

        applyTargetChange(minecraft, profile, previous);
        advanceDwell(minecraft, profile);
    }

    /**
     * ⚠ LINE OF SIGHT IS PART OF ACQUISITION, NOT A NICETY. Without it the tracker sees every hostile in range through
     * terrain, which is a wallhack wearing a HUD. Combined with the frame test it also gives exactly the stated rule:
     * only what the player can actually see.
     */
    private static void refreshContacts(Minecraft minecraft, Player player, GunTargetingProfile profile, SmartHudLayout layout) {
        contacts.clear();

        var range = profile.range();
        var searchBox = new AABB(player.position(), player.position()).inflate(range);
        var guiWidth = minecraft.getWindow().getGuiScaledWidth();
        var guiHeight = minecraft.getWindow().getGuiScaledHeight();

        for (var entity : minecraft.level.getEntities(player, searchBox, SmartTargetingClient::isTargetable)) {
            if (!(entity instanceof LivingEntity livingEntity)) {
                continue;
            }

            if (player.distanceTo(livingEntity) > range || !player.hasLineOfSight(livingEntity)) {
                continue;
            }

            var screen = CameraMatrices.project(centerOfMass(livingEntity), guiWidth, guiHeight);

            if (screen != null && layout.contains(screen[0], screen[1])) {
                contacts.add(livingEntity);
            }
        }

        contacts.sort(Comparator.comparingDouble(candidate -> screenScore(minecraft, layout, candidate)));
    }

    /**
     * ⚠ TAG FIRST, HOSTILITY SECOND. The tag lets a xenomorph or a predator be targetable without avp_human taking a
     * hard dependency on either mod - the same reason the music-disc fragments went through a tag. {@code Enemy} is the
     * fallback so vanilla hostiles work with no data at all, and a player is never a contact for a weapon whose whole
     * fiction is friend-or-foe identification.
     */
    private static boolean isTargetable(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity) || !livingEntity.isAlive() || livingEntity.isSpectator()) {
            return false;
        }

        if (livingEntity instanceof Player || CloakedTargets.isCloaked(livingEntity)) {
            return false;
        }

        // ⚠ THE EXCLUSION IS CHECKED BEFORE ANYTHING THAT WOULD INCLUDE IT, so it overrides both the targetable tag
        // and the hostile-mob fallback below. A blacklist that only beat one of the two would let endermen back in
        // through the other.
        if (livingEntity.getType().is(HumanEntityTypeTags.SMART_TARGETING_IGNORED)) {
            return false;
        }

        // Oct 5 - the same generic hostile rule the marines use (any monster or boss, from any mod, minus neutrals that
        // are not angry, tamed mobs and captives), so a smart gun locks onto whatever a marine would fight.
        return livingEntity.getType().is(HumanEntityTypeTags.SMART_TARGETABLE)
            || com.human.common.gameplay.entity.living.human.marine.MarineHuntRules.isHostileForTargeting(livingEntity);
    }

    private static double screenScore(Minecraft minecraft, SmartHudLayout layout, LivingEntity candidate) {
        var screen = CameraMatrices.project(
            centerOfMass(candidate),
            minecraft.getWindow().getGuiScaledWidth(),
            minecraft.getWindow().getGuiScaledHeight()
        );

        return screen == null ? Double.MAX_VALUE : layout.distanceFromCenter(screen[0], screen[1]);
    }

    /**
     * ⭐⭐ THE AUTOMATIC PICK STICKS TOO. [stated] "once you select a target or it auto selects one for you usually
     * closest have it keep that lock unless you lose line of sight" - so this only chooses when there is nothing being
     * tracked. A target that is still a valid contact is simply kept, whether the player picked it or the weapon did.
     * <p>
     * ⚠ THIS REPLACES THE OLD STICKINESS RATIO. That version re-ran the "most central" comparison every tick and kept
     * the current target only while no rival was 25% closer - which meant walking past a nearer xenomorph silently
     * stole the lock. Keeping the target outright is both simpler and what was actually asked for.
     * </p>
     * <p>
     * ⚠ CONTACTS ALREADY ENCODES LINE OF SIGHT, RANGE AND THE FRAME TEST. So "still in contacts" is exactly the
     * condition for keeping it, and dropping out of contacts - by ducking behind a wall, cloaking, or leaving the frame
     * - is what releases it.
     * </p>
     */
    private static @Nullable LivingEntity pickAutomatically() {
        if (target != null && contacts.contains(target)) {
            return target;
        }

        return contacts.isEmpty() ? null : contacts.getFirst();
    }

    /**
     * ⭐ TWO GESTURES ON ONE BUTTON.
     * <p>
     * TAP steps to the next contact, ordered by how central it is. HOLD hands the choice to the reticle: while the
     * button is down the target follows whatever is nearest the crosshair, so the player sweeps onto the one they want
     * and releases. Either way the result STICKS - releasing does not hand control back to the automatic rule.
     * </p>
     * <p>
     * ⚠ THE FIRST VERSION CYCLED WHILE HELD AND REVERTED ON RELEASE, which was wrong twice over: holding a button to
     * watch targets flick past is not aiming, and losing the choice the instant the button came up made the whole
     * gesture pointless.
     * </p>
     * <p>
     * ⚠ TAP IS DETECTED ON RELEASE, NOT ON PRESS. A press alone cannot be told apart from the start of a hold, so
     * acting immediately would fire the tap behaviour at the beginning of every hold as well.
     * </p>
     */
    private static void handleRetargetInput(Minecraft minecraft) {
        var down = isRetargetDown(minecraft);

        if (down) {
            useHeldTicks++;
            useWasDown = true;

            if (useHeldTicks >= HOLD_THRESHOLD_TICKS && !contacts.isEmpty()) {
                // Contacts are sorted by distance from the reticle, so the nearest is simply the first.
                manualTarget = contacts.getFirst();
            }

            return;
        }

        if (useWasDown) {
            if (useHeldTicks < HOLD_THRESHOLD_TICKS) {
                stepToNextContact();
            }

            useWasDown = false;
        }

        useHeldTicks = 0;
    }

    /**
     * ⚠ STEPS FROM WHATEVER IS CURRENTLY TRACKED, not from a stored index. An index goes stale the moment a contact
     * dies or walks out of frame, and stepping from a stale index jumps somewhere the player did not expect.
     */
    private static void stepToNextContact() {
        if (contacts.isEmpty()) {
            return;
        }

        var currentIndex = target == null ? -1 : contacts.indexOf(target);
        manualTarget = contacts.get((currentIndex + 1) % contacts.size());
    }

    /**
     * ⚠⚠ THE MANUAL CHOICE SURVIVES LOSS OF SIGHT, BUT THE LOCK DOES NOT. [stated] it "holds that target until its dead
     * or you change to another" - so a xenomorph ducking behind a wall stays chosen. What it does NOT do is stay BOXED:
     * it drops out of contacts, so nothing is drawn on it and the lock resets. Keeping the box would be a wallhack, and
     * keeping the lock would hand out the phase-three accuracy and penetration bonuses against a target the player
     * cannot see. When it steps back into view it is still the chosen one and the dwell simply restarts.
     */
    private static void forgetManualTargetIfGone(Player player, GunTargetingProfile profile) {
        if (manualTarget == null) {
            return;
        }

        // ⚠ NOT IN CONTACTS MEANS NOT VISIBLE: out of line of sight, cloaked, out of frame or out of range. [stated]
        // the lock holds "unless you lose line of sight", so this is the release condition rather than death alone.
        if (!manualTarget.isAlive() || manualTarget.isRemoved() || !contacts.contains(manualTarget)) {
            manualTarget = null;
        }
    }

    private static boolean isRetargetDown(Minecraft minecraft) {
        return minecraft.screen == null && minecraft.mouseHandler.isMouseGrabbed() && minecraft.options.keyUse.isDown();
    }

    private static void applyTargetChange(Minecraft minecraft, GunTargetingProfile profile, @Nullable LivingEntity previous) {
        if (previous == target) {
            return;
        }

        // ⚠ LOSING A LOCK IS AN EVENT, NOT AN ABSENCE. The falling two-tone only plays if a lock actually completed -
        // a contact drifting out of frame before the circle closed never had one to lose.
        if (locked && previous != null) {
            play(minecraft, profile.lockLostSound());
        }

        dwellTicks = 0;
        locked = false;

        if (target != null) {
            play(minecraft, profile.acquireSound());
        }
    }

    private static void advanceDwell(Minecraft minecraft, GunTargetingProfile profile) {
        if (target == null || !target.isAlive()) {
            if (locked) {
                play(minecraft, profile.lockLostSound());
            }

            target = null;
            manualTarget = null;
            dwellTicks = 0;
            locked = false;
            return;
        }

        if (locked) {
            return;
        }

        dwellTicks++;

        if (dwellTicks >= profile.lockDwellTicks()) {
            locked = true;
            play(minecraft, profile.lockSound());
        }
    }

    private static void clear(Minecraft minecraft, @Nullable GunTargetingProfile profile) {
        if (locked && profile != null) {
            play(minecraft, profile.lockLostSound());
        }

        target = null;
        manualTarget = null;
        dwellTicks = 0;
        locked = false;
        useWasDown = false;
        useHeldTicks = 0;
        contacts.clear();
    }

    /**
     * ⚠ PLAYED AT THE PLAYER, NOT IN THE WORLD. This is the gunner's own optic feeding their eye. A positional sound
     * would let every marine nearby hear the lock tone, and in multiplayer it would leak what someone is aiming at.
     */
    private static void play(Minecraft minecraft, net.minecraft.resources.ResourceLocation soundId) {
        var player = minecraft.player;
        var soundEvent = BuiltInRegistries.SOUND_EVENT.get(soundId);

        if (player == null || soundEvent == null || minecraft.level == null) {
            return;
        }

        minecraft.level.playLocalSound(player.getX(), player.getY(), player.getZ(), soundEvent, SoundSource.PLAYERS, 0.8F, 1.0F, false);
    }

    /** Chest height rather than the feet, so the box and the lock circle sit on the mass rather than the ground. */
    public static Vec3 centerOfMass(LivingEntity entity) {
        return entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
    }

    public static @Nullable LivingEntity target() {
        return target;
    }

    public static List<LivingEntity> contacts() {
        return contacts;
    }

    public static boolean isLocked() {
        return locked;
    }

    /** 0..1 through the lock dwell, which the right-hand tick scale and the lock circle both draw from. */
    public static float lockProgress(GunTargetingProfile profile) {
        if (locked) {
            return 1.0F;
        }

        var required = Math.max(1, profile.lockDwellTicks());

        return Math.min(1.0F, dwellTicks / (float) required);
    }

    private SmartTargetingClient() {
        throw new UnsupportedOperationException();
    }
}
