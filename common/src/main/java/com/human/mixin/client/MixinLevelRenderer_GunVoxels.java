package com.human.mixin.client;

import com.human.client.effect.VoxelGunEffects;
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

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer_GunVoxels {

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lorg/joml/Matrix4fStack;popMatrix()Lorg/joml/Matrix4fStack;",
            shift = At.Shift.BEFORE
        )
    )
    private void avp_human$renderGunVoxels(
        DeltaTracker deltaTracker,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f projectionMatrix,
        Matrix4f positionMatrix,
        CallbackInfo callbackInfo
    ) {
        var minecraft = Minecraft.getInstance();
        var buffers = minecraft.renderBuffers().bufferSource();
        VoxelGunEffects.render(
            new PoseStack(),
            buffers,
            camera.getPosition(),
            deltaTracker.getGameTimeDeltaPartialTick(false)
        );
        buffers.endBatch(RenderType.lines());
        buffers.endBatch(RenderType.debugQuads());
    }
}
