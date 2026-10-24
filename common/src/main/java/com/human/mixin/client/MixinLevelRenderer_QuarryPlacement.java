package com.human.mixin.client;

import com.human.client.render.QuarryPlacementPreview;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the quarry terminal's placement hologram, at the same point in the frame as the gun voxels
 * ({@link MixinLevelRenderer_GunVoxels}) - world space, after the level, with its own line batch flushed.
 */
@Mixin(LevelRenderer.class)
public class MixinLevelRenderer_QuarryPlacement {

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lorg/joml/Matrix4fStack;popMatrix()Lorg/joml/Matrix4fStack;",
            shift = At.Shift.BEFORE
        )
    )
    private void avp_human$renderQuarryPlacement(
        DeltaTracker deltaTracker,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f projectionMatrix,
        Matrix4f positionMatrix,
        CallbackInfo callbackInfo
    ) {
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();

        QuarryPlacementPreview.render(new PoseStack(), buffers, camera.getPosition());
        buffers.endBatch(RenderType.lines());
    }
}
