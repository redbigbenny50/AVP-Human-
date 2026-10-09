package com.human.client.render.cryo;

import com.blib.api.client.render.v1.AzRendererPipelineContext;
import com.human.HumanResources;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

import java.util.function.Function;

/**
 * The frost overlay on BLib-rendered specimens (every xenomorph). [stated] Oct 2: a frozen layer like the predator's
 * net - and, like the net, it SKIPS ZERO-THICKNESS CUBES, so flat fins and plates do not grow a frosted sheet.
 * <p>
 * Built exactly as avp_predator's {@code AzNetOverlay}: the pipeline re-draws the posed geometry with this texture
 * through its own {@code reRender}, with {@code skipFlatCubes} on, using the armour render type so it sits on the
 * surface without z-fighting, and the texture scaled to the model's own texture density.
 * </p>
 */
public final class AzFrostOverlay {

    public static final ResourceLocation TEXTURE = HumanResources.location("textures/entity/cryo_frost.png");

    /**
     * [stated] Oct 3: the frost must be see-through - frosty, but the specimen's details still showing. Drawn with a
     * TRANSLUCENT render type (the old cutout type could only draw frost fully solid or not at all). His new frost art
     * (smaller crystals, real gaps, soft 25/50/75% edges) carries its own transparency, so this draw colour only takes
     * it down to 70% overall [stated Oct 3 - "make it 70"] - at the old 55% the two would stack and a soft crystal
     * would all but vanish.
     */
    public static final int FROST_COLOUR = 0xB3FFFFFF;

    private AzFrostOverlay() {}

    public static <K, T> void render(AzRendererPipelineContext<K, T> context) {
        var target = context.animatable() instanceof Entity entity ? entity : context.currentEntity();

        // A cryotank specimen, or a live mob frozen solid (FrozenSolid).
        if (target == null || !CryoDisplay.isDisplay(target) && !com.human.client.freeze.FrozenSolidClient.isFrozen(target)) {
            return;
        }

        var pipeline = context.rendererPipeline();
        var baseTexture = pipeline.config().textureLocation(context.currentEntity(), context.animatable());
        var scale = FrostTextureScale.forEntity(baseTexture, Function.identity());
        var type = RenderType.entityTranslucent(TEXTURE);
        var scaling = scale != 1.0F;

        if (scaling) {
            RenderSystem.setTextureMatrix(new Matrix4f().scale(scale, scale, 1.0F));
        }

        var previousType = context.renderType();
        var previousConsumer = context.vertexConsumer();
        var previousSkipFlatCubes = context.skipFlatCubes();
        var previousColour = context.renderColor();

        context.setRenderType(type);
        context.setRenderColor(FROST_COLOUR);
        context.setVertexConsumer(context.multiBufferSource().getBuffer(type));
        context.setSkipFlatCubes(true);
        pipeline.reRender(context);
        context.setRenderColor(previousColour);
        context.setSkipFlatCubes(previousSkipFlatCubes);
        context.setRenderType(previousType);
        context.setVertexConsumer(previousConsumer);

        if (scaling) {
            if (context.multiBufferSource() instanceof MultiBufferSource.BufferSource source) {
                source.endBatch(type);
            }

            RenderSystem.resetTextureMatrix();
        }
    }
}
