package com.human.client.render.block;

import com.blib.api.client.render.v1.block.AzBlockEntityRenderer;
import com.blib.api.client.render.v1.block.AzBlockEntityRendererConfig;
import com.human.HumanResources;
import com.human.client.animation.block.VaporizerAnimator;
import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import com.human.common.gameplay.block.power.machine.ElectricMachineBlock;
import com.human.common.gameplay.machine.MachineState;
import com.human.common.registry.init.HumanVaporizer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

/**
 * The vaporizer: its geo model in its edition's skin, and the liquid in its tank drawn behind the side windows.
 * <h2>THE LIQUID</h2> [stated] Oct 17: it can hold any of the liquids it takes, so the model's own liquid is hidden and
 * the REAL fluid is drawn in its place - its own texture and colour from the loader (FluidClientInfo), filled to the
 * tank's level. Lava glows; nothing else does. The tank's level reaches the client twice a second at most, and only
 * when it changes (ElectricMachineBlockEntity's client sync).
 * <h2>THE DOCKED TANK</h2> The aluminum tank at the back only shows while it works, filling over each 10-second job.
 * <h2>COST</h2> One extra textured box per vaporizer in view; nothing when the tank is empty.
 */
public class VaporizerRenderer extends AzBlockEntityRenderer<ElectricMachineBlockEntity> implements com.human.client.render.OversizedRender<ElectricMachineBlockEntity> {

    private static final ResourceLocation MODEL = HumanResources.blockGeoModelLocation("vaporizer");

    private static final ResourceLocation STANDARD = HumanResources.blockTextureLocation("vaporizer");

    private static final ResourceLocation WY = HumanResources.blockTextureLocation("wy_vaporizer");

    private static final ResourceLocation USCM = HumanResources.blockTextureLocation("uscm_vaporizer");

    /** The model's liquid column, in model pixels: x and z -4.9..4.9, from y 2.1 to 11 when full. */
    private static final float LIQUID_HALF = 4.9F / 16.0F;

    private static final float LIQUID_BOTTOM = 2.1F / 16.0F;

    private static final float LIQUID_HEIGHT = 8.9F / 16.0F;

    public VaporizerRenderer() {
        super(
            AzBlockEntityRendererConfig.<ElectricMachineBlockEntity>builder($ -> MODEL, VaporizerRenderer::textureFor)
                .setAnimatorProvider(VaporizerAnimator::new)
                .setPrerenderEntry(context -> {
                    var model = context.bakedModel();

                    if (model != null) {
                        var liquid = model.getBoneOrNull("liquid");

                        // The real fluid is drawn instead (render below), in its own colour.
                        if (liquid != null) {
                            liquid.setHidden(true);
                        }

                        var tank = model.getBoneOrNull("tank");

                        if (tank != null) {
                            tank.setHidden(!isWorking(context.animatable()));
                        }

                        var fill = model.getBoneOrNull("tank_fill");

                        if (fill != null) {
                            fill.setScaleY(fillOf(context.animatable()));
                        }
                    }

                    return context;
                })
                .build()
        );
    }

    public static ResourceLocation textureFor(ElectricMachineBlockEntity machine) {
        var block = machine.getBlockState().getBlock();

        if (block == HumanVaporizer.WY_VAPORIZER.get()) {
            return WY;
        }

        return block == HumanVaporizer.USCM_VAPORIZER.get() ? USCM : STANDARD;
    }

    private static boolean isWorking(ElectricMachineBlockEntity machine) {
        var state = machine.getBlockState();

        return state.hasProperty(ElectricMachineBlock.STATE) && state.getValue(ElectricMachineBlock.STATE) == MachineState.WORKING;
    }

    /** How far the docked tank's strip has filled: one sweep per 10-second job, from the machine's own clock. */
    private static float fillOf(ElectricMachineBlockEntity machine) {
        var level = machine.getLevel();

        if (level == null || !isWorking(machine)) {
            return 0.0F;
        }

        var ticks = level.getGameTime() - machine.clientWorkStart;

        return Math.max(0.05F, (ticks % 200L) / 200.0F);
    }

    @Override
    public void render(
        @NotNull ElectricMachineBlockEntity machine,
        float partialTick,
        @NotNull PoseStack poseStack,
        @NotNull MultiBufferSource buffers,
        int packedLight,
        int packedOverlay
    ) {
        super.render(machine, partialTick, poseStack, buffers, packedLight, packedOverlay);

        var content = machine.getFluid();
        var capacity = machine.getCapacity();

        if (content.isEmpty() || capacity <= 0) {
            return;
        }

        var fill = Math.min(1.0F, content.amount() / (float) capacity);

        drawLiquid(content.fluid(), fill, poseStack, buffers, packedLight);
    }

    private static void drawLiquid(Fluid fluid, float fill, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        var info = com.human.client.fluid.FluidClientInfo.Holder.get();
        var texture = info.stillTexture(fluid);

        if (texture == null) {
            return;
        }

        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        var tint = info.tint(fluid);
        var light = fluid.isSame(net.minecraft.world.level.material.Fluids.LAVA) ? LightTexture.FULL_BRIGHT : packedLight;

        var buffer = buffers.getBuffer(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS));
        var pose = poseStack.last();

        float x0 = 0.5F - LIQUID_HALF, x1 = 0.5F + LIQUID_HALF;
        float z0 = 0.5F - LIQUID_HALF, z1 = 0.5F + LIQUID_HALF;
        float y0 = LIQUID_BOTTOM, y1 = LIQUID_BOTTOM + LIQUID_HEIGHT * Math.max(0.04F, fill);

        // Top, then the four sides (the bottom sits on the vessel floor and is never seen).
        quad(buffer, pose, sprite, tint, light, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0, 1.0F);
        quad(buffer, pose, sprite, tint, light, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 0, 0, -1, y1 - y0);
        quad(buffer, pose, sprite, tint, light, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, 0, 0, 1, y1 - y0);
        quad(buffer, pose, sprite, tint, light, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, -1, 0, 0, y1 - y0);
        quad(buffer, pose, sprite, tint, light, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, 1, 0, 0, y1 - y0);
    }

    /** One quad, corners in order; {@code vSpan} trims the sprite to the face's height so it is not squashed. */
    private static void quad(
        VertexConsumer buffer,
        PoseStack.Pose pose,
        TextureAtlasSprite sprite,
        int tint,
        int light,
        float ax,
        float ay,
        float az,
        float bx,
        float by,
        float bz,
        float cx,
        float cy,
        float cz,
        float dx,
        float dy,
        float dz,
        float nx,
        float ny,
        float nz,
        float vSpan
    ) {
        var u0 = sprite.getU0();
        var u1 = sprite.getU1();
        var v0 = sprite.getV0();
        var v1 = sprite.getV0() + (sprite.getV1() - sprite.getV0()) * Math.min(1.0F, vSpan);

        vertex(buffer, pose, ax, ay, az, u0, v1, tint, light, nx, ny, nz);
        vertex(buffer, pose, bx, by, bz, u0, v0, tint, light, nx, ny, nz);
        vertex(buffer, pose, cx, cy, cz, u1, v0, tint, light, nx, ny, nz);
        vertex(buffer, pose, dx, dy, dz, u1, v1, tint, light, nx, ny, nz);
    }

    private static void vertex(
        VertexConsumer buffer,
        PoseStack.Pose pose,
        float x,
        float y,
        float z,
        float u,
        float v,
        int tint,
        int light,
        float nx,
        float ny,
        float nz
    ) {
        buffer.addVertex(pose, x, y, z)
            .setColor(tint)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, nx, ny, nz);
    }

    /** Two blocks tall: so the whole model stays drawn when only its top block is on screen. */
    @Override
    public AABB renderBox(ElectricMachineBlockEntity machine) {
        var pos = machine.getBlockPos();

        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull ElectricMachineBlockEntity machine) {
        return true;
    }
}
