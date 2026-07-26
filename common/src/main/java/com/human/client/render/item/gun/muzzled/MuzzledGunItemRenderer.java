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
            )
                // TODO: An AzureLib bug prevents glow layers from working with "item w/ player arms" animations.
                // Uncomment this once that bug is fixed.
                // .addRenderLayer(new AzAutoGlowingLayer<>())
                .useNewOffset(true)
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
            var shouldShowMuzzleFlash = shouldShowMuzzleFlash(itemStack);

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
}
