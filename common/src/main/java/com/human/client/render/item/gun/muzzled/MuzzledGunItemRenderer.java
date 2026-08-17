package com.human.client.render.item.gun.muzzled;

import com.blib.api.client.render.v1.AzRendererPipelineContext;
import com.blib.api.client.render.v1.item.AzItemRenderer;
import com.blib.api.client.render.v1.item.AzItemRendererConfig;
import com.blib.api.client.render.v1.item.pipeline.AzItemRendererPipelineContext;
import com.human.HumanResources;
import com.human.client.effect.VoxelGunEffects;
import com.human.client.input.GunZoomHandler;
import com.human.common.registry.init.HumanDataComponents;
import com.human.common.registry.init.item.HumanGunItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public abstract class MuzzledGunItemRenderer extends AzItemRenderer {

    private static final List<String> DEFAULT_MUZZLE_FLASH_BONE_LIST = List.of("gFlash");

    protected MuzzledGunItemRenderer(String name, UnaryOperator<AzItemRendererConfig.Builder> configBuilderUnaryOperator) {
        this(name, DEFAULT_MUZZLE_FLASH_BONE_LIST, configBuilderUnaryOperator);
    }

    protected MuzzledGunItemRenderer(
        String name,
        List<String> muzzleFlashBoneNames,
        Function<AzRendererPipelineContext<UUID, ItemStack>, AzRendererPipelineContext<UUID, ItemStack>> extraPrerender,
        UnaryOperator<AzItemRendererConfig.Builder> configBuilderUnaryOperator
    ) {
        super(
            configBuilderUnaryOperator.apply(
                AzItemRendererConfig.builder(
                    HumanResources.itemGeoModelLocation(name),
                    HumanResources.itemTextureLocation(name)
                )
                    // ⚠ SET *INSIDE* THE OPERATOR'S ARGUMENT, DELIBERATELY — this is a DEFAULT, not a mandate. The
                    // per-gun configBuilderUnaryOperator runs AFTER it and can call setShouldAnimateInContext again
                    // to override (M42A3SniperRifleItemRenderer already does). Chaining it after the operator, as I
                    // first wrote it, would have silently clobbered every per-gun override.
                    .setShouldAnimateInContext(MuzzledGunItemRenderer::animatesInContext)
            )
                // TODO: An AzureLib bug prevents glow layers from working with "item w/ player arms" animations.
                // Uncomment this once that bug is fixed.
                // .addRenderLayer(new AzAutoGlowingLayer<>())
                .useNewOffset(true)
                // ⭐⭐ NO GUN ANIMATES OUTSIDE THE PLAYER'S HANDS — THE STANDARD FOR EVERY GUN, EXISTING AND NEW.
                // [stated] "they still do their firing animation in the gui icon ... we want this to become the
                // standard for guns existing and new."
                // <p>
                // ⚠⚠ THIS IS A BLib FEATURE THAT WAS SIMPLY NEVER SWITCHED ON. AzItemRendererConfig already carries
                // a shouldAnimateInContext predicate, and AzItemModelRenderer.renderRecursively already FREEZES bone
                // transforms when it returns false — but the builder default is `$ -> true`, so every gun animated
                // everywhere. M42A3SniperRifleItemRenderer was the one place that set it; this moves the rule to the
                // shared base so nobody has to remember it per gun.
                // </p>
                // <p>
                // ⚠ WHY THE RENDER LEVEL AND NOT THE DISPATCH: animation state lives PER STACK, so once a clip is
                // playing every render of that stack shows the pose — including the held gun's own hotbar icon.
                // Gating when animations START cannot fix that; only freezing the transforms for the GUI pass can.
                // </p>
                // ⚠ A GUN THAT GENUINELY WANTS A MOVING ICON CAN STILL OVERRIDE IT — see the note at the call site.
                .setPrerenderEntry(createMuzzleFlashPrerender(muzzleFlashBoneNames, extraPrerender))
                .build()
        );
    }

    protected MuzzledGunItemRenderer(
        String name,
        List<String> muzzleFlashBoneNames,
        UnaryOperator<AzItemRendererConfig.Builder> configBuilderUnaryOperator
    ) {
        this(name, muzzleFlashBoneNames, context -> context, configBuilderUnaryOperator);
    }

    private static Function<AzRendererPipelineContext<UUID, ItemStack>, AzRendererPipelineContext<UUID, ItemStack>> createMuzzleFlashPrerender(
        List<String> muzzleFlashBoneNames,
        Function<AzRendererPipelineContext<UUID, ItemStack>, AzRendererPipelineContext<UUID, ItemStack>> extraPrerender
    ) {
        return context -> {
            var itemStack = context.animatable();
            var hideScopedGun = shouldHideScopedGun(context);
            // ⚠⚠ HAND RENDERS ONLY. This was `shouldShowMuzzleFlash(itemStack)` with no notion of WHICH pass is being
            // drawn, so a firing gun lit the muzzle flash on its own hotbar icon - and on every other icon of that
            // gun, because the baked model is SHARED by every render of the item type. [stated] "the reload flash it
            // gives on the icon as its firing also happens on all icons for that gun in the gui".
            // <p>
            // ⚠ FIXED HERE, IN THE BASE CLASS, ON PURPOSE — every muzzled gun inherits this prerender, so the same
            // bleed existed for the smartgun, pulse rifle and the rest. Fixing it per-gun would have left the others.
            // </p>
            var shouldShowMuzzleFlash = isHandRender(context) && shouldShowMuzzleFlash(itemStack);

            if (hideScopedGun) {
                context.bakedModel().getTopLevelBones().forEach(bone -> bone.setHidden(true));
                return context;
            }

            context.bakedModel().getTopLevelBones().forEach(bone -> bone.setHidden(false));

            muzzleFlashBoneNames.forEach(muzzleFlashBoneName -> {
                var maybeBone = context.bakedModel().getBoneOrNull(muzzleFlashBoneName);

                if (maybeBone != null) {
                    maybeBone.setHidden(!shouldShowMuzzleFlash);
                }
            });

            return extraPrerender.apply(context);
        };
    }

    /**
     * True only for the four in-hand transforms — GUI icons, dropped stacks, item frames and heads are excluded.
     * <p>
     * ⚠ {@code AzItemRendererPipelineContext} is NOT generic; it already fixes {@code <UUID, ItemStack>}, so a type
     * argument on the instanceof is a compile error.
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

    private static boolean shouldHideScopedGun(AzRendererPipelineContext<UUID, ItemStack> context) {
        return context instanceof AzItemRendererPipelineContext itemContext
            && isFirstPerson(itemContext.getTransformType())
            && isHiddenDuringScopedAim(context.animatable());
    }

    private static boolean isHiddenDuringScopedAim(ItemStack itemStack) {
        var minecraft = Minecraft.getInstance();
        var item = itemStack.getItem();

        return item == HumanGunItems.M42A3_SNIPER_RIFLE.get() && GunZoomHandler.isAimingSniper(minecraft);
    }

    private static boolean isFirstPerson(ItemDisplayContext transformType) {
        return transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
            || transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
    }

    private static boolean shouldShowMuzzleFlash(ItemStack itemStack) {
        var muzzleFlashDuration = itemStack.getOrDefault(HumanDataComponents.MUZZLE_FLASH_DURATION_IN_TICKS.get(), 0);
        return muzzleFlashDuration > 0 || VoxelGunEffects.hasLocalMuzzleFlash(itemStack);
    }

    /**
     * Guns animate only in the four in-hand transforms. GUI icons, dropped stacks, item frames and heads render the
     * model frozen at its rest pose.
     * <p>
     * ⚠ Written as an explicit allow-list rather than "not GUI", because {@code GROUND}, {@code FIXED} (item frames)
     * and {@code HEAD} have exactly the same problem — a dropped or framed gun should not mirror whatever someone
     * nearby is firing.
     * </p>
     */
    private static boolean animatesInContext(ItemDisplayContext transformType) {
        return switch (transformType) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND ->
                true;
            default -> false;
        };
    }
}
