package com.human.client.render.item.gun.muzzled.impl;

import com.blib.api.client.render.v1.AzRendererPipelineContext;
import com.blib.api.client.render.v1.item.pipeline.AzItemRendererPipelineContext;
import com.human.client.animation.item.OldPainlessAnimator;
import com.human.client.render.item.gun.muzzled.MuzzledGunItemRenderer;
import com.human.client.render.layer.OldPainlessHeatLayer;
import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class OldPainlessItemRenderer extends MuzzledGunItemRenderer {

    private static final String BARREL_BONE_NAME = "gBarrel";

    private static final double BARREL_SPIN_RADIANS_PER_TICK = 2.8D;

    private static final List<String> MUZZLE_FLASH_BONE_NAME_LIST = List.of(
        "gFlash",
        "gFlash2",
        "gFlash3",
        "gFlash4",
        "gFlash5",
        "gFlash6"
    );

    public OldPainlessItemRenderer(String name) {
        super(
            name,
            MUZZLE_FLASH_BONE_NAME_LIST,
            createOldPainlessPrerender(),
            config -> config
                .addRenderLayer(new OldPainlessHeatLayer())
                .setAnimatorProvider(OldPainlessAnimator::new)
        );
    }

    private static Function<AzRendererPipelineContext<UUID, ItemStack>, AzRendererPipelineContext<UUID, ItemStack>> createOldPainlessPrerender() {
        return context -> {
            var itemStack = context.animatable();
            // ⭐⭐ GUI, GROUND, FRAME AND HEAD RENDERS GET A STATIC GUN. [stated] "in the gui when firing the gun and
            // spinning the model also spins and fires it doesnt stay static ... it's doing it to all of the old
            // painless icons not just the one im holding."
            // <p>
            // Two separate causes, and this closes both: the barrel angle is a STATIC field (one renderer instance
            // serves every Old Painless on screen), and the muzzle-flash bones are set on a baked model SHARED by
            // every render of this item type - so a firing gun drove every icon in the hotbar. Restricting the whole
            // prerender to in-world hand renders means the icons never touch either piece of shared state.
            // </p>
            // ⚠ FIXED_ITEM_FRAME and GROUND are excluded deliberately too: a dropped or framed minigun should sit
            // still, not mirror whatever someone nearby happens to be firing.
            // ⚠⚠ RESET, DO NOT SKIP. The baked model is SHARED by every render of this item type, so an early return
            // leaves gBarrel at whatever angle the in-hand pass just set and leaves the flash bones however it left
            // them - the GUI icon then draws that same state and appears to spin and flash along with the held gun.
            // Skipping is not neutral; the bone has to be actively put back.
            if (!isHandRender(context)) {
                var restingBarrel = context.bakedModel().getBoneOrNull(BARREL_BONE_NAME);

                if (restingBarrel != null) {
                    restingBarrel.setRotZ(0.0F);
                }

                MUZZLE_FLASH_BONE_NAME_LIST.forEach(muzzleFlashBoneName -> {
                    var maybeBone = context.bakedModel().getBoneOrNull(muzzleFlashBoneName);

                    if (maybeBone != null) {
                        maybeBone.setHidden(true);
                    }
                });

                return context;
            }

            var gameTime = getGameTime();
            var isFiring = itemStack.getOrDefault(HumanDataComponents.IS_FIRING.get(), false);
            var muzzleFlashDuration = itemStack.getOrDefault(HumanDataComponents.MUZZLE_FLASH_DURATION_IN_TICKS.get(), 0);
            var shouldShowMuzzleFlash = isFiring
                && muzzleFlashDuration > 0
                && gameTime != Long.MIN_VALUE
                && gameTime % 2L == 0L;
            var barrel = context.bakedModel().getBoneOrNull(BARREL_BONE_NAME);

            if (barrel != null) {
                barrel.setRotZ(barrelRotation(itemStack, gameTime));
            }

            MUZZLE_FLASH_BONE_NAME_LIST.forEach(muzzleFlashBoneName -> {
                var maybeBone = context.bakedModel().getBoneOrNull(muzzleFlashBoneName);

                if (maybeBone != null) {
                    maybeBone.setHidden(!shouldShowMuzzleFlash);
                }
            });

            return context;
        };
    }

    private static long getGameTime() {
        var level = Minecraft.getInstance().level;

        return level == null ? Long.MIN_VALUE : level.getGameTime();
    }

    /**
     * ⭐⭐ THE BARREL SPIN IS PROCEDURAL AND ALWAYS WAS - THIS IS THE ONLY THING THAT TURNS gBarrel.
     * <p>
     * ⚠⚠ DO NOT ALSO DRIVE IT FROM THE ANIMATION CLIPS. animation.barrelspinup/loop/down exist in
     * old_painless.animation.json and are wired through OldPainlessAnimationDispatcher, but enabling those made TWO
     * things rotate the same bone every frame - which renders as a spinning model ghosting over a still one, most
     * obvious as red/grey checkering once the heat layer tints it. One driver only, and this is it.
     * </p>
     * <p>
     * ⚠ THE OLD VERSION WAS {@code isFiring ? spin : 0.0F} - a hard snap on and an instant stop, which is exactly the
     * "no wind-up, no wind-down, spins only while firing" report. The fix is to RAMP here, not to add a second driver.
     * Speed follows the spin phase the server already tracks:
     * </p>
     * <ul>
     * <li>SPINNING_UP - accelerate from 0 to full across the gun's 20-tick shoot delay, so the barrels come up to speed
     * while WEAPON_OLD_PAINLESS_SHOOT_START plays and hit full speed exactly as the first round leaves.</li>
     * <li>LOOPING - full speed.</li>
     * <li>STOPPED - coast down over {@value #SPIN_DOWN_TICKS} ticks rather than stopping dead.</li>
     * </ul>
     * <p>
     * ⚠ THE ANGLE IS ACCUMULATED, NOT COMPUTED FROM gameTime. The old code derived the angle straight from gameTime,
     * which is fine at constant speed but jumps discontinuously the moment speed changes - the barrels would visibly
     * snap to a new angle when the ramp started. Integrating keeps it continuous.
     * </p>
     */
    private static float barrelRotation(ItemStack itemStack, long gameTime) {
        if (gameTime == Long.MIN_VALUE) {
            return 0.0F;
        }

        var phase = itemStack.getOrDefault(HumanDataComponents.OLD_PAINLESS_SPIN_PHASE.get(), 0);
        var speed = switch (phase) {
            case 1 -> {
                // Ramp across the shoot delay. lastFireTick is stamped every tick the trigger is held, so the
                // elapsed count restarts cleanly on each fresh pull.
                var elapsed = gameTime - itemStack.getOrDefault(HumanDataComponents.OLD_PAINLESS_LAST_FIRE_TICK.get(), 0);
                yield (float) Math.min(1.0D, Math.max(0.0D, elapsed / (double) SPIN_UP_TICKS));
            }
            case 2 -> 1.0F;
            default -> {
                var since = itemStack.getOrDefault(HumanDataComponents.OLD_PAINLESS_SPIN_DOWN_TICK.get(), Integer.MAX_VALUE);
                yield since >= SPIN_DOWN_TICKS ? 0.0F : 1.0F - (since / (float) SPIN_DOWN_TICKS);
            }
        };

        if (speed <= 0.0F) {
            accumulatedAngle = 0.0D;
            lastGameTime = gameTime;
            return 0.0F;
        }

        // ⚠⚠ ADVANCE PER TICK, NOT PER FRAME. This method is called from the PRERENDER, which runs every RENDER
        // FRAME - 60+ times a second, not 20. Accumulating a full tick's rotation per call span the barrels roughly
        // three times too fast AND made the speed depend on framerate. Gating on the gameTime delta fixes both: the
        // angle advances once per tick no matter how often the renderer asks for it.
        var elapsedTicks = gameTime - lastGameTime;

        if (elapsedTicks > 0L) {
            lastGameTime = gameTime;
            accumulatedAngle = (accumulatedAngle + BARREL_SPIN_RADIANS_PER_TICK * speed * elapsedTicks) % (Math.PI * 2.0D);
        }

        return (float) accumulatedAngle;
    }

    /** Matches GunData.OLD_PAINLESS's withShootDelayInTicks(20) - full speed as the first round fires. */
    private static final int SPIN_UP_TICKS = 20;

    /** Coast-down length. Roughly animation.barrelspindown's 13.3 ticks, rounded for a clean feel. */
    private static final int SPIN_DOWN_TICKS = 14;

    /** ⚠ Integrated angle - see barrelRotation. Static because the renderer is a singleton per item type. */
    private static double accumulatedAngle;

    /** The tick the angle last advanced on, so the prerender's per-FRAME calls advance it only once per tick. */
    private static long lastGameTime;

    /**
     * True only for the four in-hand transforms. Everything else - GUI icons, dropped stacks, item frames, heads -
     * renders a still gun.
     * <p>
     * ⚠ {@code AzItemRendererPipelineContext} is NOT GENERIC - it already fixes both type parameters
     * ({@code extends AzRendererPipelineContext<UUID, ItemStack>}), so writing {@code <ItemStack>} on the instanceof is
     * a compile error: "type AzItemRendererPipelineContext does not take parameters". ⚠ Written as a switch rather than
     * chained &amp;&amp;/|| because the mixed precedence there is easy to get wrong.
     * </p>
     */
    private static boolean isHandRender(AzRendererPipelineContext<UUID, ItemStack> context) {
        if (!(context instanceof AzItemRendererPipelineContext itemContext)) {
            return false;
        }

        return switch (itemContext.getTransformType()) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND ->
                true;
            default -> false;
        };
    }
}
