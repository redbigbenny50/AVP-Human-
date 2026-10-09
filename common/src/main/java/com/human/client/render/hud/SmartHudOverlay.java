package com.human.client.render.hud;

import com.human.client.config.HumanClientConfig;
import com.human.client.render.CameraMatrices;
import com.human.client.targeting.SmartTargetingClient;
import com.human.common.gameplay.item.GunItem;
import com.human.common.gameplay.item.gun.GunHeat;
import com.human.common.gameplay.item.gun.targeting.GunTargetingProfile;
import com.human.common.gameplay.item.gun.targeting.SmartTargeting;
import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The smart targeting overlay - frame, gauges and power-on sweep. PHASE ONE: chrome only, no acquisition or lock yet.
 * <p>
 * ⚠⚠ EVERY COORDINATE IS DERIVED FROM SCREEN CENTRE OUTWARD, NEVER FROM A PANEL EDGE INWARD. The reticle must sit on
 * the exact pixel the player's shots go through, so the frame is centred first and the panels are placed relative to it
 * afterwards. Positioning the frame by a margin from the side panels instead would drift it off centre at other
 * resolutions and GUI scales - felt as a constant pull the player cannot name.
 * </p>
 * <p>
 * ⚠ NOTHING HERE IS DRAWN FROM A TEXTURE. Procedural geometry recomputes from the real screen size every frame, so
 * there is no aspect-ratio stretch to design around and the frame size is a config number rather than re-cut art.
 * </p>
 * <p>
 * ⚠ BLOCKS STACK FROM AN ANCHOR; THEY ARE NOT PLACED AT FIXED POINTS. A gun with no heat omits the temperature gauge
 * entirely and the reserve block moves UP to take its place. A bolt-on module on a pistol is the case that proves it.
 * </p>
 */
public final class SmartHudOverlay {

    /**
     * ⭐ MU/TH/UR GREEN, sampled from the reference terminal: 0x11B852 is the most common pixel in it by a factor of
     * four. The HUD was a sky-blue cyan, which is a Colonial Marine tracker colour rather than a Weyland-Yutani
     * terminal one.
     */
    private static final int COLOR_PRIMARY = 0x11B852;

    private static final int COLOR_HEAT_WARN = 0xFFB347;

    private static final int COLOR_HEAT_CRITICAL = 0xFF4A4A;

    /** The locked target's own colour, so a lock is legible at a glance without reading anything. */
    private static final int COLOR_LOCK = 0xFFB347;

    private static final int BOX_MIN_HALF_WIDTH = 6;

    private static final int BOX_MIN_HALF_HEIGHT = 8;

    private static final int CORNER_ARM = 20;

    private static final int CORNER_STEP = 4;

    private static final int SCALE_GAP = 16;

    private static final int ACCENT_GAP = 10;

    private static final int PANEL_WIDTH = 84;

    /**
     * ⚠ THE GAUGE IS DELIBERATELY NARROWER THAN THE PANEL. Run to the panel's full width and it reaches most of the way
     * to the frame, which drags the eye off centre and makes the left block look like the main event. It only has to be
     * long enough to read a fraction off.
     */
    private static final int GAUGE_WIDTH = 44;

    /**
     * Where the target panel sits down the frame's right side, as a fraction of frame height. Slightly above the
     * midpoint - low enough to clear vanilla's status effect icons in the top right, high enough that the health and
     * armour lines arriving in phase two still have room beneath it.
     */
    private static final float TARGET_PANEL_VERTICAL_ANCHOR = 0.40F;

    /**
     * How far below the target panel's anchor the control legend starts. Clears the panel at its tallest - name, health
     * bar, HP line and armour line - so the legend does not collide when a target IS locked.
     */
    private static final int HINT_BLOCK_OFFSET = 46;

    private static final float TEXT_SCALE = 0.5F;

    public static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;

        if (player == null || !minecraft.options.getCameraType().isFirstPerson() || minecraft.options.hideGui) {
            return;
        }

        var heldStack = player.getMainHandItem();

        SmartHudState.tick(heldStack);

        var profile = SmartTargeting.resolve(heldStack);

        if (profile == null) {
            return;
        }

        playBootSoundIfPowering(minecraft, player, profile);

        var bootAnimation = HumanClientConfig.smartHudBootAnimation();
        var bootProgress = SmartHudState.bootProgress(bootAnimation);
        var alpha = overlayAlpha(heldStack) * bootProgress;

        if (alpha <= 0.02F) {
            return;
        }

        // ⚠ THE FRAME COMES FROM SmartHudLayout, THE SAME PLACE THE ACQUISITION TEST GETS IT. Recomputing it here -
        // as this used to - lets the drawn frame and the frame that actually acquires drift apart.
        var layout = SmartHudLayout.of(profile, guiGraphics.guiWidth(), guiGraphics.guiHeight());
        var left = layout.left();
        var right = layout.right();
        var top = layout.top();
        var bottom = layout.bottom();

        renderFrame(guiGraphics, left, top, right, bottom, bootProgress, alpha);
        renderAccentLines(guiGraphics, left, top, right, bottom, alpha);
        renderTickScales(guiGraphics, left, top, right, bottom, bootProgress, alpha, profile);
        renderContacts(guiGraphics, minecraft, profile, layout, alpha);
        renderPanels(guiGraphics, minecraft, player, heldStack, profile, left, top, alpha);
        renderTargetPanel(guiGraphics, minecraft, right, top, bottom, alpha);
        renderControlHints(guiGraphics, minecraft, right, top, bottom, alpha);
    }

    /**
     * Boxes every contact in the frame, and draws the lock circle on the tracked one.
     * <p>
     * ⚠⚠ RE-PROJECTED EVERY FRAME, NOT ONCE PER TICK. Acquisition runs on the game tick - twenty times a second - but
     * if the BOX were placed from that same tick it would visibly step at any frame rate above 20. Only the choice of
     * target is a tick decision; where it is drawn is a frame decision.
     * </p>
     * <p>
     * ⭐ THE BOX CHASES RATHER THAN TELEPORTS. A few frames of lag as it settles onto a moving target reads as a
     * gyro-stabilised arm physically tracking; snapping instantly reads as a UI element pinned to a model.
     * </p>
     */
    private static void renderContacts(
        GuiGraphics guiGraphics,
        Minecraft minecraft,
        GunTargetingProfile profile,
        SmartHudLayout layout,
        float alpha
    ) {
        var guiWidth = guiGraphics.guiWidth();
        var guiHeight = guiGraphics.guiHeight();
        var target = SmartTargetingClient.target();

        for (var contact : SmartTargetingClient.contacts()) {
            if (contact == target) {
                continue;
            }

            var box = projectBox(contact, guiWidth, guiHeight);

            if (box != null && layout.contains(box[0], box[1])) {
                drawBracketBox(guiGraphics, box, color(COLOR_PRIMARY, alpha * 0.55F), false);
            }
        }

        if (target == null) {
            SmartHudState.clearBoxChase();
            return;
        }

        var box = projectBox(target, guiWidth, guiHeight);

        if (box == null) {
            return;
        }

        var chased = SmartHudState.chaseBox(box);
        var locked = SmartTargetingClient.isLocked();
        var lockProgress = SmartTargetingClient.lockProgress(profile);

        drawBracketBox(guiGraphics, chased, color(locked ? COLOR_LOCK : COLOR_PRIMARY, alpha), true);
        drawLockCircle(guiGraphics, chased[0], chased[1], lockProgress, locked, alpha);
    }

    /**
     * Screen-space bounds of an entity: centre plus half-extents, from its own bounding box projected at chest height.
     * <p>
     * ⚠ THE HALF-EXTENTS ARE MEASURED, NOT ASSUMED. Projecting the entity's actual top and side gives a box that
     * shrinks with distance on its own; a fixed-size box would swallow a distant xenomorph and rattle around inside a
     * close one.
     * </p>
     */
    private static float @Nullable [] projectBox(LivingEntity entity, int guiWidth, int guiHeight) {
        var center = CameraMatrices.project(SmartTargetingClient.centerOfMass(entity), guiWidth, guiHeight);

        if (center == null) {
            return null;
        }

        var top = CameraMatrices.project(entity.position().add(0.0D, entity.getBbHeight(), 0.0D), guiWidth, guiHeight);
        var side = CameraMatrices.project(
            SmartTargetingClient.centerOfMass(entity).add(entity.getBbWidth() * 0.5D, 0.0D, 0.0D),
            guiWidth,
            guiHeight
        );

        var halfHeight = top == null ? BOX_MIN_HALF_HEIGHT : Math.max(BOX_MIN_HALF_HEIGHT, Math.abs(center[1] - top[1]));
        var halfWidth = side == null ? BOX_MIN_HALF_WIDTH : Math.max(BOX_MIN_HALF_WIDTH, Math.abs(center[0] - side[0]));

        return new float[] { center[0], center[1], halfWidth, halfHeight };
    }

    /** Corner brackets rather than a closed rectangle - it leaves the target visible through the middle. */
    private static void drawBracketBox(GuiGraphics guiGraphics, float[] box, int color, boolean emphasised) {
        var left = Math.round(box[0] - box[2]);
        var right = Math.round(box[0] + box[2]);
        var top = Math.round(box[1] - box[3]);
        var bottom = Math.round(box[1] + box[3]);
        var arm = Math.max(3, Math.round(Math.min(right - left, bottom - top) * (emphasised ? 0.34F : 0.26F)));

        guiGraphics.fill(left, top, left + arm, top + 1, color);
        guiGraphics.fill(left, top, left + 1, top + arm, color);
        guiGraphics.fill(right - arm, top, right, top + 1, color);
        guiGraphics.fill(right - 1, top, right, top + arm, color);
        guiGraphics.fill(left, bottom - 1, left + arm, bottom, color);
        guiGraphics.fill(left, bottom - arm, left + 1, bottom, color);
        guiGraphics.fill(right - arm, bottom - 1, right, bottom, color);
        guiGraphics.fill(right - 1, bottom - arm, right, bottom, color);
    }

    /**
     * ⭐ THE LOCK CIRCLE DRAWS ITSELF AS THE DWELL FILLS, rather than appearing finished. The player watches the lock
     * happen instead of noticing it happened, and the same progress drives the right-hand scale so the two agree.
     * <p>
     * ⚠ PLOTTED AS SEGMENTS, NOT AS A CIRCLE PRIMITIVE - GuiGraphics has no arc, so this walks the angle and fills a
     * pixel per step. The step count is tied to the radius so a big circle does not come out dotted.
     * </p>
     */
    private static void drawLockCircle(GuiGraphics guiGraphics, float centerX, float centerY, float progress, boolean locked, float alpha) {
        if (progress <= 0.0F) {
            return;
        }

        var radius = 9.0F;
        var steps = Math.max(24, Math.round(radius * 6.0F));
        var lit = Math.round(steps * Mth.clamp(progress, 0.0F, 1.0F));
        var color = color(locked ? COLOR_LOCK : COLOR_PRIMARY, alpha);

        for (var step = 0; step < lit; step++) {
            // Starts at twelve o'clock and sweeps clockwise, the direction a dial is read.
            var angle = (float) (-Math.PI / 2.0D + (Math.PI * 2.0D) * step / steps);
            var x = Math.round(centerX + Mth.cos(angle) * radius);
            var y = Math.round(centerY + Mth.sin(angle) * radius);

            guiGraphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    /**
     * The target readout, on the right of the frame.
     * <p>
     * ⭐ ALWAYS DRAWN, EVEN WITH NOTHING TRACKED - it reads NONE instead of vanishing. A panel that appears and
     * disappears makes the whole HUD twitch every time a target is gained or lost, and the player cannot tell "no
     * target" apart from "the tracker is broken". A steady heading with a changing value does both jobs.
     * </p>
     * <p>
     * ⚠ PHASE ONE HAS NO ACQUISITION, so this is always NONE for now. The health and armour lines land here in phase
     * two, under the same heading rather than floating above the target's head - three boxed contacts would otherwise
     * mean three floating bars, and it would leak information about targets the player has not committed to.
     * </p>
     * <p>
     * ⚠⚠ VERTICALLY PLACED FROM THE FRAME'S OWN HEIGHT, NOT FROM ITS TOP EDGE. Anchored to the top it collided with
     * vanilla's status effect icons, which render in the upper right - and that corner was reserved for them from the
     * first mockup. Sitting a little above the frame's midpoint keeps it clear of the icons at every screen size,
     * because it scales with the frame rather than sitting a fixed distance below a corner.
     * </p>
     */
    private static void renderTargetPanel(
        GuiGraphics guiGraphics,
        Minecraft minecraft,
        int frameRight,
        int frameTop,
        int frameBottom,
        float alpha
    ) {
        var bootProgress = SmartHudState.bootProgress(HumanClientConfig.smartHudBootAnimation());
        var fade = Mth.clamp((bootProgress - 0.5F) / 0.5F, 0.0F, 1.0F);

        if (fade <= 0.0F) {
            return;
        }

        var panelAlpha = alpha * fade;
        var panelLeft = frameRight + SCALE_GAP + 12;
        var panelTop = frameTop + Math.round((frameBottom - frameTop) * TARGET_PANEL_VERTICAL_ANCHOR);
        var target = SmartTargetingClient.target();
        var locked = SmartTargetingClient.isLocked();

        drawSmallText(guiGraphics, minecraft, "TARGETING", panelLeft, panelTop, color(COLOR_PRIMARY, panelAlpha));

        if (target == null) {
            drawSmallText(guiGraphics, minecraft, "NONE", panelLeft, panelTop + 7, color(COLOR_PRIMARY, panelAlpha * 0.65F));
            SmartHudState.clearHealthGhost();
            return;
        }

        var nameColor = color(locked ? COLOR_LOCK : COLOR_PRIMARY, panelAlpha);
        drawSmallText(guiGraphics, minecraft, targetLabel(target), panelLeft, panelTop + 7, nameColor);

        var health = target.getHealth();
        var maxHealth = Math.max(1.0F, target.getMaxHealth());

        // ⭐ THE GHOST BAR IS WHAT MAKES DAMAGE LEGIBLE AGAINST A WEAPON FIRING TWENTY ROUNDS A SECOND. The solid bar
        // snaps to the true value; a paler one trails behind and catches up, so the player SEES the size of each bite
        // instead of watching a number teleport.
        var ghost = SmartHudState.chaseHealth(health / maxHealth);

        drawGhostGauge(guiGraphics, panelLeft, panelTop + 16, GAUGE_WIDTH, health / maxHealth, ghost, panelAlpha);

        drawSmallText(
            guiGraphics,
            minecraft,
            "HP " + Math.round(health) + " / " + Math.round(maxHealth),
            panelLeft,
            panelTop + 25,
            color(COLOR_PRIMARY, panelAlpha)
        );
        drawSmallText(
            guiGraphics,
            minecraft,
            "ARM " + target.getArmorValue(),
            panelLeft,
            panelTop + 32,
            color(COLOR_PRIMARY, panelAlpha * 0.85F)
        );
    }

    /**
     * The control legend, in its own box under the target readout.
     * <p>
     * ⚠ ALWAYS SHOWN, NOT A FIRST-RUN TUTORIAL. Two gestures on one button is not something a player will discover by
     * accident, and a hint that appears once and never returns is a hint nobody reads twice. It costs two lines of
     * half-scale text on a HUD that already has space reserved beside the frame.
     * </p>
     * <p>
     * ⚠ SITS BELOW THE TARGET PANEL BY A FIXED OFFSET FROM THE SAME ANCHOR, so it tracks the panel wherever
     * TARGET_PANEL_VERTICAL_ANCHOR puts it rather than needing its own tuning when that moves.
     * </p>
     */
    private static void renderControlHints(
        GuiGraphics guiGraphics,
        Minecraft minecraft,
        int frameRight,
        int frameTop,
        int frameBottom,
        float alpha
    ) {
        var bootProgress = SmartHudState.bootProgress(HumanClientConfig.smartHudBootAnimation());
        var fade = Mth.clamp((bootProgress - 0.5F) / 0.5F, 0.0F, 1.0F);

        if (fade <= 0.0F) {
            return;
        }

        var panelLeft = frameRight + SCALE_GAP + 12;
        var panelTop = frameTop + Math.round((frameBottom - frameTop) * TARGET_PANEL_VERTICAL_ANCHOR) + HINT_BLOCK_OFFSET;
        var hintColor = color(COLOR_PRIMARY, alpha * fade * 0.7F);

        // A rule above the block, so it reads as its own box rather than more of the target readout.
        guiGraphics.fill(panelLeft, panelTop - 4, panelLeft + GAUGE_WIDTH, panelTop - 3, color(COLOR_PRIMARY, alpha * fade * 0.4F));

        drawSmallText(guiGraphics, minecraft, "RIGHT CLICK TO CYCLE TARGETS", panelLeft, panelTop, hintColor);
        drawSmallText(guiGraphics, minecraft, "HOLD RIGHT CLICK TO MANUALLY", panelLeft, panelTop + 7, hintColor);
        drawSmallText(guiGraphics, minecraft, "LOCK ON", panelLeft, panelTop + 14, hintColor);
    }

    /**
     * ⚠ TRUNCATED, NOT WRAPPED. Half-scale text still overruns the panel on a long custom name, and a name bleeding
     * across the frame is worse than a clipped one.
     */
    private static String targetLabel(LivingEntity target) {
        var name = target.getDisplayName().getString().toUpperCase(java.util.Locale.ROOT);

        return name.length() <= 18 ? name : name.substring(0, 17) + "\u2026";
    }

    /** A health bar with its trailing ghost drawn behind the solid value. */
    private static void drawGhostGauge(GuiGraphics guiGraphics, int x, int y, int width, float value, float ghost, float alpha) {
        var outline = color(COLOR_PRIMARY, alpha * 0.55F);

        guiGraphics.fill(x, y, x + width, y + 1, outline);
        guiGraphics.fill(x, y + 5, x + width, y + 6, outline);
        guiGraphics.fill(x, y, x + 1, y + 6, outline);
        guiGraphics.fill(x + width - 1, y, x + width, y + 6, outline);

        var inner = width - 4;
        var ghostWidth = Math.round(inner * Mth.clamp(ghost, 0.0F, 1.0F));
        var solidWidth = Math.round(inner * Mth.clamp(value, 0.0F, 1.0F));

        if (ghostWidth > solidWidth) {
            guiGraphics.fill(x + 2 + solidWidth, y + 2, x + 2 + ghostWidth, y + 4, color(COLOR_HEAT_CRITICAL, alpha * 0.45F));
        }

        if (solidWidth > 0) {
            guiGraphics.fill(x + 2, y + 2, x + 2 + solidWidth, y + 4, color(COLOR_PRIMARY, alpha));
        }
    }

    /**
     * ⚠ PLAYED AT THE PLAYER, NOT IN THE WORLD. This is the gunner's own optic feeding their eye - a positional sound
     * would let every marine nearby hear it, and in multiplayer would leak that someone just drew a smart weapon.
     */
    private static void playBootSoundIfPowering(Minecraft minecraft, Player player, GunTargetingProfile profile) {
        if (!SmartHudState.consumeBootSound()) {
            return;
        }

        var soundEvent = BuiltInRegistries.SOUND_EVENT.get(profile.bootSound());

        if (soundEvent != null) {
            minecraft.level.playLocalSound(player.getX(), player.getY(), player.getZ(), soundEvent, SoundSource.PLAYERS, 0.8F, 1.0F, false);
        }
    }

    /**
     * ⭐ THE OVERHEAT FLICKER. A weapon whose optic stutters while the barrel cools says "you did this to yourself"
     * without another line of text competing for the same corner of the screen.
     * <p>
     * ⚠ DRIVEN BY THE WALL CLOCK so it flickers at the same rate for every player regardless of frame rate, and bottoms
     * out at 0.55 rather than 0 - a HUD that vanishes entirely reads as a crash, not as a penalty.
     * </p>
     */
    private static float overlayAlpha(ItemStack heldStack) {
        if (!GunHeat.isOverheated(heldStack) || !HumanClientConfig.smartHudOverheatFlicker()) {
            return 1.0F;
        }

        var phase = (System.currentTimeMillis() % 220L) / 220.0F;

        return 0.55F + 0.45F * Math.abs(Mth.sin(phase * Mth.PI));
    }

    /**
     * ⚠ THE CORNERS SWEEP IN FROM OUTSIDE, NEVER THE CENTRE. All boot motion is at the periphery because movement where
     * the player is aiming fights them - the reticle's pixel is the one thing on this HUD that never moves.
     */
    private static void renderFrame(GuiGraphics guiGraphics, int left, int top, int right, int bottom, float bootProgress, float alpha) {
        var sweep = Mth.clamp(bootProgress / 0.5F, 0.0F, 1.0F);
        var offset = Math.round((1.0F - ease(sweep)) * 26.0F);
        var color = color(COLOR_PRIMARY, alpha * sweep);

        if (sweep <= 0.0F) {
            return;
        }

        drawCorner(guiGraphics, left - offset, top - offset, 1, 1, color);
        drawCorner(guiGraphics, right + offset, top - offset, -1, 1, color);
        drawCorner(guiGraphics, left - offset, bottom + offset, 1, -1, color);
        drawCorner(guiGraphics, right + offset, bottom + offset, -1, -1, color);
    }

    /**
     * One chamfered corner, approximated as a staircase.
     * <p>
     * ⚠ {@code GuiGraphics.fill} DRAWS AXIS-ALIGNED RECTANGLES AND NOTHING ELSE, so the angled cut of the reference art
     * is a short stair of one-pixel steps. At GUI scale the steps are invisible and it keeps the whole overlay in one
     * simple drawing model instead of dropping to raw quad buffers for a decorative bevel.
     * </p>
     */
    /**
     * One chamfered corner: a horizontal arm, a vertical arm, and a stepped diagonal JOINING THEM.
     * <p>
     * ⚠⚠ THE TWO ARMS AND THE STAIRCASE MUST SHARE ENDPOINTS. The first version drew the staircase from the corner
     * point but started the vertical arm at that same corner point and the horizontal arm at the stepped offset - so
     * the diagonal joined nothing at one end and overlapped at the other, leaving a visible break in every corner. The
     * geometry that works is: the horizontal arm ends at (corner + step, corner), the vertical arm ends at (corner,
     * corner + step), and the staircase runs between those two points. Change one of the three and check all four
     * corners, not just the top left - the sign flips are where this goes wrong.
     * </p>
     * <p>
     * ⚠ {@code GuiGraphics.fill} DRAWS AXIS-ALIGNED RECTANGLES AND NOTHING ELSE, so the 45 degree cut is a stair of
     * one-pixel squares. At GUI scale the steps are invisible and it keeps the overlay in one simple drawing model
     * rather than dropping to raw quad buffers for a decorative bevel.
     * </p>
     */
    private static void drawCorner(GuiGraphics guiGraphics, int x, int y, int directionX, int directionY, int color) {
        // Where each arm stops short of the corner, leaving room for the diagonal.
        var insetX = x + directionX * CORNER_STEP;
        var insetY = y + directionY * CORNER_STEP;

        // Horizontal arm: runs along the frame edge at the corner's own y, starting from the inset.
        var horizontalEndX = insetX + directionX * CORNER_ARM;
        guiGraphics.fill(Math.min(insetX, horizontalEndX), y, Math.max(insetX, horizontalEndX), y + 1, color);

        // Vertical arm: runs down the frame edge at the corner's own x, starting from the inset.
        var verticalEndY = insetY + directionY * CORNER_ARM;
        guiGraphics.fill(x, Math.min(insetY, verticalEndY), x + 1, Math.max(insetY, verticalEndY), color);

        // The staircase, inclusive at BOTH ends so it lands exactly on the two arm starts.
        for (var step = 0; step <= CORNER_STEP; step++) {
            var px = x + directionX * (CORNER_STEP - step);
            var py = y + directionY * step;

            guiGraphics.fill(px, py, px + 1, py + 1, color);
        }
    }

    /** Thin dimension-style rules above and below the frame, capped at both ends turning toward it. */
    private static void renderAccentLines(GuiGraphics guiGraphics, int left, int top, int right, int bottom, float alpha) {
        var color = color(COLOR_PRIMARY, alpha * 0.7F);
        var lineLeft = left - 18;
        var lineRight = right + 18;
        var above = top - ACCENT_GAP;
        var below = bottom + ACCENT_GAP;

        guiGraphics.fill(lineLeft, above, lineRight, above + 1, color);
        guiGraphics.fill(lineLeft, above, lineLeft + 1, above + 5, color);
        guiGraphics.fill(lineRight - 1, above, lineRight, above + 5, color);

        guiGraphics.fill(lineLeft, below, lineRight, below + 1, color);
        guiGraphics.fill(lineLeft, below - 4, lineLeft + 1, below, color);
        guiGraphics.fill(lineRight - 1, below - 4, lineRight, below, color);
    }

    /**
     * ⭐ LONG / SHORT / SHORT, NOT UNIFORM. Evenly spaced ticks read as a dotted line at GUI scale; a repeating rhythm
     * still reads as a measuring scale even when each mark is only three pixels.
     * <p>
     * ⚠ THE RIGHT-HAND SCALE IS RESERVED FOR LOCK DWELL IN PHASE TWO - it will fill from the bottom as a target holds
     * in the frame. Left stays decorative on purpose: two live gauges flanking the reticle compete with it, and heat
     * already has a gauge in the left panel.
     * </p>
     */
    private static void renderTickScales(
        GuiGraphics guiGraphics,
        int left,
        int top,
        int right,
        int bottom,
        float bootProgress,
        float alpha,
        GunTargetingProfile profile
    ) {
        var wipe = Mth.clamp((bootProgress - 0.25F) / 0.5F, 0.0F, 1.0F);

        if (wipe <= 0.0F) {
            return;
        }

        var color = color(COLOR_PRIMARY, alpha * 0.85F);
        var scaleTop = top + 12;
        var scaleBottom = bottom - 12;
        var visibleBottom = scaleTop + Math.round((scaleBottom - scaleTop) * ease(wipe));
        var leftX = left - SCALE_GAP;
        var rightX = right + SCALE_GAP;

        guiGraphics.fill(leftX, scaleTop, leftX + 1, visibleBottom, color);
        guiGraphics.fill(rightX, scaleTop, rightX + 1, visibleBottom, color);

        // ⭐ THE RIGHT-HAND SCALE IS THE LOCK METER. Ticks light from the BOTTOM as dwell accumulates, so the player
        // can see how close the lock is instead of only that it arrived - which is what makes holding on a target
        // rather than swinging to a closer one a real decision.
        var lockProgress = SmartTargetingClient.target() == null ? 0.0F : SmartTargetingClient.lockProgress(profile);
        var dimColor = color(COLOR_PRIMARY, alpha * 0.30F);
        var litColor = color(SmartTargetingClient.isLocked() ? COLOR_LOCK : COLOR_PRIMARY, alpha);
        var index = 0;
        var rows = new java.util.ArrayList<Integer>();

        for (var y = scaleTop + 2; y < visibleBottom - 1; y += 5) {
            rows.add(y);
        }

        // Counted from the bottom up, so the meter fills toward the top edge of the frame.
        var litCount = Math.round(rows.size() * lockProgress);

        for (var row = 0; row < rows.size(); row++) {
            var y = rows.get(row);
            var length = index % 3 == 0 ? 6 : 3;
            var lit = rows.size() - 1 - row < litCount;

            guiGraphics.fill(leftX - length, y, leftX, y + 1, color);
            guiGraphics.fill(rightX + 1, y, rightX + 1 + length, y + 1, lit ? litColor : dimColor);
            index++;
        }
    }

    private static void renderPanels(
        GuiGraphics guiGraphics,
        Minecraft minecraft,
        Player player,
        ItemStack heldStack,
        GunTargetingProfile profile,
        int frameLeft,
        int frameTop,
        float alpha
    ) {
        var bootProgress = SmartHudState.bootProgress(HumanClientConfig.smartHudBootAnimation());
        var fade = Mth.clamp((bootProgress - 0.5F) / 0.5F, 0.0F, 1.0F);

        if (fade <= 0.0F) {
            return;
        }

        var panelAlpha = alpha * fade;
        var panelRight = frameLeft - SCALE_GAP - 12;
        var panelLeft = Math.max(4, panelRight - PANEL_WIDTH);
        var cursorY = frameTop;

        var gunItem = heldStack.getItem() instanceof GunItem gun ? gun : null;

        if (gunItem == null) {
            return;
        }

        var gunConfig = gunItem.getGunConfig();
        var heatProfile = gunConfig.heatProfile();

        // ⚠ ABSENT, NOT EMPTY. A gun with no heat leaves NO gap here - the reserve block below simply starts higher.
        if (profile.showWeaponHeat() && heatProfile != null) {
            drawSmallText(guiGraphics, minecraft, "WEP TEMP", panelLeft, cursorY, color(COLOR_PRIMARY, panelAlpha));
            cursorY += 7;

            var heat = GunHeat.getHeatFraction(heldStack, heatProfile);
            drawGauge(guiGraphics, panelLeft, cursorY, GAUGE_WIDTH, heat, heatColor(heat, heldStack), panelAlpha);
            cursorY += 9;

            var rounds = heldStack.getOrDefault(HumanDataComponents.AMMUNITION.get(), 0);
            drawSmallText(guiGraphics, minecraft, "ROUNDS", panelLeft, cursorY, color(COLOR_PRIMARY, panelAlpha * 0.75F));
            cursorY += 7;
            drawSmallText(
                guiGraphics,
                minecraft,
                rounds + " / " + gunConfig.maximumAmmunition(),
                panelLeft,
                cursorY,
                color(COLOR_PRIMARY, panelAlpha)
            );
            cursorY += 16;
        }

        var ammunitionSupplier = gunConfig.ammunitionItemSupplier();

        if (ammunitionSupplier != null) {
            drawSmallText(guiGraphics, minecraft, "RESERVE", panelLeft, cursorY, color(COLOR_PRIMARY, panelAlpha));
            cursorY += 7;

            var spare = countSpareAmmunition(player, ammunitionSupplier.get().asItem().getDefaultInstance());
            drawSmallText(guiGraphics, minecraft, String.valueOf(spare), panelLeft, cursorY, color(COLOR_PRIMARY, panelAlpha));
        }
    }

    /**
     * ⚠ COUNTS THE GUN'S OWN AMMUNITION ITEM, WHATEVER IT IS - never a hardcoded drum. The whole point of resolving
     * from config is that a pistol wearing a smart module reports pistol magazines here without a line of new code.
     */
    private static int countSpareAmmunition(Player player, ItemStack ammunition) {
        var total = 0;

        for (var slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);

            if (ItemStack.isSameItem(stack, ammunition)) {
                total += stack.getCount();
            }
        }

        return total;
    }

    private static int heatColor(float heat, ItemStack heldStack) {
        if (GunHeat.isOverheated(heldStack) || heat >= 0.80F) {
            return heat >= 0.80F && !GunHeat.isOverheated(heldStack) ? COLOR_HEAT_WARN : COLOR_HEAT_CRITICAL;
        }

        return COLOR_PRIMARY;
    }

    private static void drawGauge(GuiGraphics guiGraphics, int x, int y, int width, float fraction, int rgb, float alpha) {
        var outline = color(COLOR_PRIMARY, alpha * 0.55F);

        guiGraphics.fill(x, y, x + width, y + 1, outline);
        guiGraphics.fill(x, y + 5, x + width, y + 6, outline);
        guiGraphics.fill(x, y, x + 1, y + 6, outline);
        guiGraphics.fill(x + width - 1, y, x + width, y + 6, outline);

        var filled = Math.round((width - 4) * Mth.clamp(fraction, 0.0F, 1.0F));

        if (filled > 0) {
            guiGraphics.fill(x + 2, y + 2, x + 2 + filled, y + 4, color(rgb, alpha));
        }
    }

    /**
     * ⚠ MINECRAFT'S FONT HAS EXACTLY ONE SIZE, so "small text" means scaling the pose matrix. Half scale keeps a label
     * like OVERHEATING near 33 pixels wide, which fits the panel; it costs a little sharpness and there is no other
     * lever available short of shipping a bitmap font.
     */
    private static void drawSmallText(GuiGraphics guiGraphics, Minecraft minecraft, String text, int x, int y, int color) {
        var pose = guiGraphics.pose();

        pose.pushPose();
        pose.translate(x, y, 0.0F);
        pose.scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
        guiGraphics.drawString(minecraft.font, text, 0, 0, color, false);
        pose.popPose();
    }

    /** Smoothstep, so the sweep decelerates into place instead of arriving at full speed. */
    private static float ease(float progress) {
        var clamped = Mth.clamp(progress, 0.0F, 1.0F);

        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static int color(int rgb, float alpha) {
        var a = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);

        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private SmartHudOverlay() {
        throw new UnsupportedOperationException();
    }
}
