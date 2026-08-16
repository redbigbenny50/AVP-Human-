package com.human.client.render.layer;

import com.blib.api.client.model.v1.AzBone;
import com.blib.api.client.render.v1.AzRendererPipelineContext;
import com.blib.api.client.render.v1.layer.AzRenderLayer;
import com.human.common.gameplay.item.old_painless.OldPainlessHeat;
import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Adds a red-hot overlay to Old Painless's barrel assembly without tinting the receiver. */
public class OldPainlessHeatLayer implements AzRenderLayer<UUID, ItemStack> {

    private static final String BARREL_BONE = "gBarrel";

    private static final float VISIBLE_HEAT_THRESHOLD = 0.30F;

    @Override
    public void preRender(AzRendererPipelineContext<UUID, ItemStack> context) {}

    @Override
    public void render(AzRendererPipelineContext<UUID, ItemStack> context) {
        var heat = OldPainlessHeat.getHeat(context.animatable()) / (float) OldPainlessHeat.MAX_HEAT;
        if (heat <= VISIBLE_HEAT_THRESHOLD) {
            return;
        }

        var barrel = context.bakedModel().getBoneOrNull(BARREL_BONE);
        if (barrel == null) {
            return;
        }

        var overheated = context.animatable().getOrDefault(HumanDataComponents.OLD_PAINLESS_OVERHEATED.get(), false);
        var intensity = overheated
            ? 1.0F
            : Math.min(1.0F, (heat - VISIBLE_HEAT_THRESHOLD) / (1.0F - VISIBLE_HEAT_THRESHOLD));
        // Start at the original texture color, then progressively remove green/blue.
        // The renderer's cutout path does not reliably honor translucent tint alpha.
        var green = 1.0F - 0.92F * intensity;
        var blue = 1.0F - intensity;
        var oldColor = context.renderColor();
        context.bakedModel().getTopLevelBones().forEach(bone -> bone.setHidden(bone != barrel));
        context.setRenderColor(FastColor.ARGB32.colorFromFloat(1.0F, 1.0F, green, blue));
        context.rendererPipeline().reRender(context);
        context.setRenderColor(oldColor);
        context.bakedModel().getTopLevelBones().forEach(bone -> bone.setHidden(false));
    }

    @Override
    public void renderForBone(AzRendererPipelineContext context, AzBone bone) {}
}
