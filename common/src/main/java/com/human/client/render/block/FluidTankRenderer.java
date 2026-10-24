package com.human.client.render.block;

import com.human.HumanResources;
import com.human.common.gameplay.block.tank.FluidTankBlock;
import com.human.common.gameplay.block.tank.FluidTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;

/**
 * Draws a tank block's VALVES: one on each face that has a pipe attached. [stated] Sep 29: green where fluid goes IN to
 * the tank (the top, and sides set to input), red where it comes OUT (the bottom, and sides set to output) - the same
 * colours as the power flanges. Each edition has its own valve art.
 * <h2>WHY A RENDERER AND NOT THE BLOCK MODEL</h2> A valve state for each of six faces, on top of the tank's position
 * properties, would multiply every tank block into hundreds of thousands of block states. The valves live in the block
 * entity instead, and this draws them. A block with no pipe returns at once - the first line of {@link #render} - so
 * the other blocks of a big tank cost next to nothing. Pass 3 draws the tank's fluid from here too.
 */
public class FluidTankRenderer implements BlockEntityRenderer<FluidTankBlockEntity>, com.human.client.render.OversizedRender<FluidTankBlockEntity> {

    /** How far the valve sits off the face, so it never z-fights the wall. */
    private static final float LIFT = 0.002F;

    @Override
    public void render(
        @NotNull FluidTankBlockEntity tank,
        float partialTick,
        @NotNull PoseStack poseStack,
        @NotNull MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        if (tank.isController() && tank.hasWindows()) {
            renderFluid(tank, poseStack, bufferSource, packedLight);
        }

        if (!tank.hasAnyPipe() || tank.getLevel() == null || !(tank.getBlockState().getBlock() instanceof FluidTankBlock block)) {
            return;
        }

        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        var edition = block.getEdition().id();
        var consumer = bufferSource.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
        var pose = poseStack.last();

        for (var face : Direction.values()) {
            if (!tank.hasPipe(face)) {
                continue;
            }

            // In = green, out = red. The top is always in and the bottom always out. Oct 18: a SIDE is both ways and
            // the attached pipe's flange decides - the flange already shows the direction in colour, so the valve only
            // marks the port with the existing art: EXTRACT = out, anything else = in (the look an untouched side
            // always had), OFF = no port, no valve. [stated] no new art: "the pipes already make colored flanges".
            String valve;

            if (face == Direction.UP) {
                valve = "in";
            } else if (face == Direction.DOWN) {
                valve = "out";
            } else {
                var setting = tank.getLevel()
                    .getBlockEntity(tank.getBlockPos().relative(face)) instanceof com.human.common.gameplay.block.pipe.PipeBlockEntity pipe
                        ? pipe.getOverride(face.getOpposite())
                        : com.human.common.gameplay.block.pipe.PipeBlockEntity.AUTO;
                var mode = com.human.common.gameplay.block.pipe.PipeBlockEntity.effective(setting, true, true);

                if (mode == 0) {
                    continue;
                }

                valve = mode == com.human.common.gameplay.block.pipe.PipeBlockEntity.OUT ? "out" : "in";
            }

            var sprite = atlas.apply(HumanResources.location("block/" + edition + "_fluid_tank_valve_" + valve));
            var light = LevelRenderer.getLightColor(tank.getLevel(), tank.getBlockPos().relative(face));

            quad(consumer, pose, face, sprite, light);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The fluid (pass 3)
    // ------------------------------------------------------------------------------------------------------------

    /**
     * ⚠ Only the CONTROLLER is drawn from anywhere on screen. It sits in the tank's bottom corner, and a 32-tall tank
     * spans several chunk sections; without this, looking at the top of a tall tank with its bottom section off screen
     * would make the fluid vanish. Vanilla keeps such block entities in a global list (read in
     * {@code SectionCompiler}). Every other tank block stays in the normal per-section list.
     */
    @Override
    public boolean shouldRenderOffScreen(@NotNull FluidTankBlockEntity tank) {
        return tank.isController();
    }

    /** Big tanks are seen from further away than a chest. */
    @Override
    public int getViewDistance() {
        return 96;
    }

    /** How far the fluid stays inside the walls, so it never touches the inner wall faces. */
    private static final float INSET = 0.02F;

    /**
     * One box for the whole tank, drawn by the controller - never one per block. Filled from the floor up to the fill
     * level, with the fluid's OWN moving texture: split into block-sized tiles so it is one fluid pixel per model
     * pixel, like the placed canister. Lava and anything else that glows as a block glows here. Skipped when the
     * windows are closed - nothing could see it.
     */
    private static void renderFluid(FluidTankBlockEntity tank, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        var content = tank.getContent();
        var capacity = tank.getCapacity();

        if (content.isEmpty() || capacity <= 0) {
            return;
        }

        var info = com.human.client.fluid.FluidClientInfo.Holder.get();
        var texture = info.stillTexture(content.fluid());

        if (texture == null) {
            return;
        }

        var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        var tint = info.tint(content.fluid());
        var light = content.fluid().defaultFluidState().createLegacyBlock().getLightEmission() > 0
            ? net.minecraft.client.renderer.LightTexture.FULL_BRIGHT
            : packedLight;

        float width = tank.getWidth();
        float height = tank.getHeight();
        float top = INSET + (height - 2.0F * INSET) * Math.min(1.0F, (float) content.amount() / capacity);
        float lo = INSET;
        float hi = width - INSET;

        var consumer = bufferSource.getBuffer(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS));
        var pose = poseStack.last();
        var colour = new int[] { (tint >> 16) & 0xFF, (tint >> 8) & 0xFF, tint & 0xFF };

        // Surface: one tile per block column.
        for (var x = 0; x < width; x++) {
            for (var z = 0; z < width; z++) {
                float x0 = Math.max(lo, x);
                float x1 = Math.min(hi, x + 1);
                float z0 = Math.max(lo, z);
                float z1 = Math.min(hi, z + 1);

                fluidQuad(
                    consumer,
                    pose,
                    sprite,
                    colour,
                    light,
                    0,
                    1,
                    0,
                    x0,
                    top,
                    z0,
                    x0,
                    top,
                    z1,
                    x1,
                    top,
                    z1,
                    x1,
                    top,
                    z0,
                    x0 - x,
                    z0 - z,
                    x1 - x,
                    z1 - z
                );
            }
        }

        // Walls: one tile per block, the top row cut to the fill level.
        for (var y = 0; y < Math.ceil(top); y++) {
            float y0 = Math.max(lo, y);
            float y1 = Math.min(top, y + 1);

            if (y1 <= y0) {
                continue;
            }

            for (var along = 0; along < width; along++) {
                float a0 = Math.max(lo, along);
                float a1 = Math.min(hi, along + 1);
                float v0 = (y + 1) - y1;
                float v1 = (y + 1) - y0;

                // north (-z), south (+z), west (-x), east (+x) - each wound to face OUT of the box
                fluidQuad(
                    consumer,
                    pose,
                    sprite,
                    colour,
                    light,
                    0,
                    0,
                    -1,
                    a1,
                    y1,
                    lo,
                    a1,
                    y0,
                    lo,
                    a0,
                    y0,
                    lo,
                    a0,
                    y1,
                    lo,
                    1 - (a1 - along),
                    v0,
                    1 - (a0 - along),
                    v1
                );
                fluidQuad(
                    consumer,
                    pose,
                    sprite,
                    colour,
                    light,
                    0,
                    0,
                    1,
                    a0,
                    y1,
                    hi,
                    a0,
                    y0,
                    hi,
                    a1,
                    y0,
                    hi,
                    a1,
                    y1,
                    hi,
                    a0 - along,
                    v0,
                    a1 - along,
                    v1
                );
                fluidQuad(
                    consumer,
                    pose,
                    sprite,
                    colour,
                    light,
                    -1,
                    0,
                    0,
                    lo,
                    y1,
                    a0,
                    lo,
                    y0,
                    a0,
                    lo,
                    y0,
                    a1,
                    lo,
                    y1,
                    a1,
                    a0 - along,
                    v0,
                    a1 - along,
                    v1
                );
                fluidQuad(
                    consumer,
                    pose,
                    sprite,
                    colour,
                    light,
                    1,
                    0,
                    0,
                    hi,
                    y1,
                    a1,
                    hi,
                    y0,
                    a1,
                    hi,
                    y0,
                    a0,
                    hi,
                    y1,
                    a0,
                    1 - (a1 - along),
                    v0,
                    1 - (a0 - along),
                    v1
                );
            }
        }
    }

    /**
     * One fluid quad. Corners in order top-left, bottom-left, bottom-right, top-right as seen from outside; the UV
     * fractions (0-1 within one block) pick the matching part of the sprite, so a cut tile shows a cut texture rather
     * than a squashed whole one.
     */
    private static void fluidQuad(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        TextureAtlasSprite sprite,
        int[] colour,
        int light,
        float nx,
        float ny,
        float nz,
        float x0,
        float y0,
        float z0,
        float x1,
        float y1,
        float z1,
        float x2,
        float y2,
        float z2,
        float x3,
        float y3,
        float z3,
        float u0,
        float v0,
        float u1,
        float v1
    ) {
        vertex(consumer, pose, sprite, colour, light, nx, ny, nz, x0, y0, z0, u0, v0);
        vertex(consumer, pose, sprite, colour, light, nx, ny, nz, x1, y1, z1, u0, v1);
        vertex(consumer, pose, sprite, colour, light, nx, ny, nz, x2, y2, z2, u1, v1);
        vertex(consumer, pose, sprite, colour, light, nx, ny, nz, x3, y3, z3, u1, v0);
    }

    private static void vertex(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        TextureAtlasSprite sprite,
        int[] colour,
        int light,
        float nx,
        float ny,
        float nz,
        float x,
        float y,
        float z,
        float u,
        float v
    ) {
        consumer.addVertex(pose, x, y, z)
            .setColor(colour[0], colour[1], colour[2], 255)
            .setUv(sprite.getU(clamp01(u)), sprite.getV(clamp01(v)))
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, nx, ny, nz);
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    /**
     * One full-face quad just outside the given face, texture upright for a viewer in front of it, wound
     * counter-clockwise as seen from outside so back-face culling keeps it.
     */
    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, Direction face, TextureAtlasSprite sprite, int light) {
        float lo = -LIFT;
        float hi = 1.0F + LIFT;

        // Corners in order: top-left, bottom-left, bottom-right, top-right - as the viewer facing this face sees them.
        float[][] c = switch (face) {
            case NORTH -> new float[][] { { 1, 1, lo }, { 1, 0, lo }, { 0, 0, lo }, { 0, 1, lo } };
            case SOUTH -> new float[][] { { 0, 1, hi }, { 0, 0, hi }, { 1, 0, hi }, { 1, 1, hi } };
            case EAST -> new float[][] { { hi, 1, 1 }, { hi, 0, 1 }, { hi, 0, 0 }, { hi, 1, 0 } };
            case WEST -> new float[][] { { lo, 1, 0 }, { lo, 0, 0 }, { lo, 0, 1 }, { lo, 1, 1 } };
            case UP -> new float[][] { { 0, hi, 0 }, { 0, hi, 1 }, { 1, hi, 1 }, { 1, hi, 0 } };
            case DOWN -> new float[][] { { 1, lo, 0 }, { 1, lo, 1 }, { 0, lo, 1 }, { 0, lo, 0 } };
        };
        float[][] uv = {
            { sprite.getU0(), sprite.getV0() },
            { sprite.getU0(), sprite.getV1() },
            { sprite.getU1(), sprite.getV1() },
            { sprite.getU1(), sprite.getV0() } };

        for (var index = 0; index < 4; index++) {
            consumer.addVertex(pose, c[index][0], c[index][1], c[index][2])
                .setColor(255, 255, 255, 255)
                .setUv(uv[index][0], uv[index][1])
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, face.getStepX(), face.getStepY(), face.getStepZ());
        }
    }

    /** The controller is in the bottom corner of a tank up to 5 x 5 x 32. */
    @Override
    public net.minecraft.world.phys.AABB renderBox(@NotNull FluidTankBlockEntity tank) {
        // Only the controller draws; the tank's other blocks keep their own one-block box.
        if (!tank.isController()) {
            return new net.minecraft.world.phys.AABB(tank.getBlockPos());
        }

        return new net.minecraft.world.phys.AABB(tank.getBlockPos()).inflate(6.0D, 1.0D, 6.0D).expandTowards(0.0D, 33.0D, 0.0D);
    }
}
