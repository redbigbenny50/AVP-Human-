package com.human.client.render.layer;

import com.blib.api.client.model.v1.AzBone;
import com.blib.api.client.render.v1.AzRendererPipelineContext;
import com.blib.api.client.render.v1.item.pipeline.AzItemRendererPipelineContext;
import com.blib.api.client.render.v1.layer.AzRenderLayer;
import com.human.common.gameplay.item.old_painless.OldPainlessHeat;
import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.UUID;

/** Adds a red-hot overlay to Old Painless's barrel assembly without tinting the receiver. */
public class OldPainlessHeatLayer implements AzRenderLayer<UUID, ItemStack> {

    private static final String BARREL_BONE = "gBarrel";

    private static final float VISIBLE_HEAT_THRESHOLD = 0.30F;

    @Override
    public void preRender(AzRendererPipelineContext<UUID, ItemStack> context) {}

    /**
     * ⚠⚠ HAND RENDERS ONLY. This layer runs on EVERY render of the item type, GUI icons included, so without this gate
     * a hot gun tinted its own hotbar icon (and any other Old Painless icon on screen) red.
     * <p>
     * ⚠ {@code AzItemRendererPipelineContext} is NOT generic - it already fixes {@code <UUID, ItemStack>}, so writing a
     * type argument on the instanceof is a compile error.
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

    @Override
    public void render(AzRendererPipelineContext<UUID, ItemStack> context) {
        if (!isHandRender(context)) {
            return;
        }

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

        // ⚠⚠ RESTORE THE PREVIOUS HIDDEN STATE, DO NOT BLANKET-UNHIDE. This used to finish with
        // `forEach(bone -> bone.setHidden(false))`, which un-hid EVERY top-level bone on a model that is SHARED by
        // every render of this item type - including the six gFlash muzzle bones that OldPainlessItemRenderer's
        // prerender had deliberately hidden. The next render (a GUI icon) then drew them, which is the grey flash
        // that appeared on the hotbar icons while firing.
        var wasHidden = new HashMap<AzBone, Boolean>();
        context.bakedModel().getTopLevelBones().forEach(bone -> wasHidden.put(bone, bone.isHidden()));

        context.bakedModel().getTopLevelBones().forEach(bone -> bone.setHidden(bone != barrel));
        context.setRenderColor(FastColor.ARGB32.colorFromFloat(1.0F, 1.0F, green, blue));
        context.rendererPipeline().reRender(context);
        context.setRenderColor(oldColor);
        context.bakedModel().getTopLevelBones().forEach(bone -> bone.setHidden(wasHidden.getOrDefault(bone, false)));
    }

    @Override
    public void renderForBone(AzRendererPipelineContext context, AzBone bone) {}
}
