package com.human.client.render.layer;

import com.blib.api.client.model.v1.AzBone;
import com.blib.api.client.render.v1.AzRendererPipelineContext;
import com.blib.api.client.render.v1.layer.AzRenderLayer;
import com.human.client.fluid.FluidClientInfo;
import com.human.common.gameplay.block.entity.CanisterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Paints the liquid inside a placed canister with the fluid's OWN texture - water ripples, lava crawls and glows, any
 * mod's fluid looks like itself. [stated] Sep 29: option B.
 * <h2>HOW IT USES HIS MODEL</h2> The model has eight groups, {@code gFill_1} to {@code gFill_8}, each the whole liquid
 * shape at one fill level. The renderer's bone filter hides all eight, so his own (empty) fill texture never draws;
 * this layer then takes the group for the current level and draws ITS cubes with the fluid's texture. Only the geometry
 * comes from his model, so the liquid is exactly his shape.
 * <h2>⚠ WHY IT DRAWS FROM {@code gLiquidLevel}</h2> BLib calls a layer's {@code renderForBone} after that bone's
 * transform is applied, and only for bones it actually renders (read from {@code AzModelRenderer.renderRecursively}).
 * The fill groups are filtered out, so they never get that call - but their parent {@code gLiquidLevel} does, and every
 * fill group has a zero pivot and no rotation, so its cubes are in exactly the parent's space. Cube vertices are stored
 * in block units (BLib divides by 16 when baking).
 * <h2>TEXTURE SCALE</h2> One fluid pixel per model pixel: a face 10 pixels wide shows 10/16 of the fluid sprite, never
 * a squashed whole one.
 */
public class CanisterFluidLayer implements AzRenderLayer<Long, CanisterBlockEntity> {

    public static final String LEVEL_BONE = "gLiquidLevel";

    public static final String FILL_BONE_PREFIX = "gFill_";

    @Override
    public void preRender(AzRendererPipelineContext<Long, CanisterBlockEntity> context) {}

    @Override
    public void render(AzRendererPipelineContext<Long, CanisterBlockEntity> context) {}

    @Override
    public void renderForBone(AzRendererPipelineContext<Long, CanisterBlockEntity> context, AzBone bone) {
        if (!LEVEL_BONE.equals(bone.getName())) {
            return;
        }

        var canister = context.animatable();
        var level = canister.fillLevel();

        if (level <= 0) {
            return;
        }

        var fillBone = context.bakedModel().getBoneOrNull(FILL_BONE_PREFIX + level);

        if (fillBone == null) {
            return;
        }

        var fluid = canister.getContent().fluid();
        var info = FluidClientInfo.Holder.get();
        var texture = info.stillTexture(fluid);

        if (texture == null) {
            return;
        }

        var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        var tint = info.tint(fluid);
        // Lava and anything else that glows as a block glows in the canister too.
        var light = fluid.defaultFluidState().createLegacyBlock().getLightEmission() > 0
            ? LightTexture.FULL_BRIGHT
            : context.packedLight();
        var consumer = context.multiBufferSource().getBuffer(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS));
        var pose = context.poseStack().last();

        for (var cube : fillBone.getCubes()) {
            for (var quad : cube.quads()) {
                if (quad == null) {
                    continue;
                }

                drawQuad(consumer, pose, quad.vertices(), quad.direction(), sprite, tint, light);
            }
        }
    }

    private static void drawQuad(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        com.blib.internal.client.model.GeoVertex[] vertices,
        Direction direction,
        TextureAtlasSprite sprite,
        int tint,
        int light
    ) {
        // The face's own 2D extent, in pixels, so UVs start at the face's corner and run one texel per pixel.
        var minA = Float.MAX_VALUE;
        var maxB = -Float.MAX_VALUE;

        for (var vertex : vertices) {
            var position = vertex.position();

            minA = Math.min(minA, axisA(direction, position.x(), position.z()) * 16.0F);
            maxB = Math.max(maxB, axisB(direction, position.y(), position.z()) * 16.0F);
        }

        var red = (tint >> 16) & 0xFF;
        var green = (tint >> 8) & 0xFF;
        var blue = tint & 0xFF;

        for (var vertex : vertices) {
            var position = vertex.position();
            var a = axisA(direction, position.x(), position.z()) * 16.0F - minA;
            var b = maxB - axisB(direction, position.y(), position.z()) * 16.0F;

            consumer.addVertex(pose, position.x(), position.y(), position.z())
                .setColor(red, green, blue, 255)
                .setUv(sprite.getU(clamp01(a / 16.0F)), sprite.getV(clamp01(b / 16.0F)))
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, direction.getStepX(), direction.getStepY(), direction.getStepZ());
        }
    }

    /** Horizontal texture axis of a face: x for north/south/up/down faces, z for east/west. */
    private static float axisA(Direction direction, float x, float z) {
        return direction.getAxis() == Direction.Axis.X ? z : x;
    }

    /** Vertical texture axis: height for side faces, z for the top and bottom. */
    private static float axisB(Direction direction, float y, float z) {
        return direction.getAxis() == Direction.Axis.Y ? z : y;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
