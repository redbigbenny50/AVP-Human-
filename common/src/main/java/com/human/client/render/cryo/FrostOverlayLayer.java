package com.human.client.render.cryo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

/**
 * The frost overlay on vanilla-model specimens frozen in a cryotank. [stated] Oct 2: a frozen layer like the predator's
 * net. This is avp_predator's {@code NetOverlayLayer} with the frost texture: it re-draws the mob's own posed model, so
 * the frost follows the silhouette exactly, scaled to a consistent density on every mob, drawn with the armour render
 * type so it sits on the surface, and with the villager-hat fix so an unemployed villager does not grow a frosted hat.
 * <p>
 * ⚠ Only cryo display copies ever have it; the first line returns for everything else, because this layer is added to
 * every living-entity renderer in the game.
 * </p>
 */
public class FrostOverlayLayer<T extends Entity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    private static final ResourceLocation TEXTURE = AzFrostOverlay.TEXTURE;

    public FrostOverlayLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(
        @NotNull PoseStack poseStack,
        @NotNull MultiBufferSource buffers,
        int packedLight,
        @NotNull T entity,
        float limbSwing,
        float limbSwingAmount,
        float partialTick,
        float ageInTicks,
        float netHeadYaw,
        float headPitch
    ) {
        // ⚠ First line is a set lookup that returns immediately. This layer is on EVERY living renderer in the game,
        // so the not-netted case has to cost nothing.
        // A cryotank specimen, or a live mob frozen solid (FrozenSolid).
        if (!CryoDisplay.isDisplay(entity) && !com.human.client.freeze.FrozenSolidClient.isFrozen(entity)) {
            return;
        }

        var scale = FrostTextureScale.forEntity(entity, this::textureFor);
        // ⚠⚠ armorCutoutNoCull, NOT entityCutoutNoCull. This is the render type vanilla uses to draw ARMOUR over
        // skin without z-fighting, and it is the same problem: VIEW_OFFSET_Z_LAYERING nudges the geometry toward
        // the camera so it always wins the depth test, and EQUAL_DEPTH_TEST draws it exactly on the surface.
        // Translucent and see-through, as on the Az path - see AzFrostOverlay.FROST_COLOUR.
        var type = RenderType.entityTranslucent(TEXTURE);
        var scaling = scale != 1.0F;

        if (scaling) {
            RenderSystem.setTextureMatrix(new Matrix4f().scale(scale, scale, 1.0F));
        }

        poseStack.pushPose();

        // ⚠⚠ NO INFLATE. It used to scale the whole model by 1.04, and a SCALE moves a face proportionally to its
        // distance from the model ORIGIN — so the head top (y~8) gained 0.32 units while an arm's top face (y~2
        // from its pivot) gained only 0.08. That is not enough separation to win the depth test, which is why the
        // TOPS of arms and small mobs like the chicken showed no net while outer sides and heads did.
        //
        // EQUAL_DEPTH_TEST requires the geometry to be COPLANAR with the surface it covers, and the layering state
        // handles the separation instead. Constant coverage on every face regardless of where it sits.

        // ⚠ The parent model is already posed for this frame by the renderer that owns it, so the net inherits the
        // mob's current animation with no work.
        // ⚠⚠ A VILLAGER'S HAT IS GEOMETRY EVEN WHEN NOTHING IS DRAWN ON IT. Villagers and zombie villagers render
        // "hat" and "hat_rim" every frame; the TEXTURES decide whether anything shows (an unemployed plains villager's
        // hat region is transparent). The net texture has no transparent region, so those parts came out as a giant
        // mesh headdress with a brim. Ask vanilla's own hat metadata whether the hat is real, and hide the parts for
        // this pass only when it is not. ⚠ Villagers ONLY: for illagers, witches and everything else the part's
        // visible flag already says whether vanilla draws it, and the net follows that as it always did.
        var hidden = entity instanceof net.minecraft.world.entity.npc.VillagerDataHolder && !VillagerHatMeta.hasHat(entity)
            ? hideHatParts(getParentModel())
            : java.util.List.<ModelPart>of();

        getParentModel().renderToBuffer(
            poseStack,
            buffers.getBuffer(type),
            packedLight,
            OverlayTexture.NO_OVERLAY,
            AzFrostOverlay.FROST_COLOUR
        );
        restoreHatParts(hidden);

        poseStack.popPose();

        if (scaling) {
            if (buffers instanceof MultiBufferSource.BufferSource source) {
                source.endBatch(type);
            }

            RenderSystem.resetTextureMatrix();
        }
    }

    /** ⚠ {@code getTextureLocation} is protected on the layer, so it is reached through a method reference. */
    private ResourceLocation textureFor(T entity) {
        return getTextureLocation(entity);
    }

    /** Sets {@code hat} / {@code hat_rim} (under {@code head}) invisible and returns the parts that were visible. */
    private static java.util.List<ModelPart> hideHatParts(EntityModel<?> model) {
        if (!(model instanceof HierarchicalModel<?> hierarchical) || !hierarchical.root().hasChild("head")) {
            return java.util.List.of();
        }

        var head = hierarchical.root().getChild("head");
        var hidden = new java.util.ArrayList<ModelPart>(2);

        if (head.hasChild("hat")) {
            var hat = head.getChild("hat");

            hideIfVisible(hat, hidden);

            if (hat.hasChild("hat_rim")) {
                hideIfVisible(hat.getChild("hat_rim"), hidden);
            }
        }

        return hidden;
    }

    private static void hideIfVisible(ModelPart part, java.util.List<ModelPart> hidden) {
        if (part.visible) {
            part.visible = false;
            hidden.add(part);
        }
    }

    private static void restoreHatParts(java.util.List<ModelPart> hidden) {
        for (var part : hidden) {
            part.visible = true;
        }
    }
}
