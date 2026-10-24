package com.human.client.render;

import com.human.common.gameplay.quarry.QuarryPlacement;
import com.human.common.gameplay.quarry.QuarryTerminalBlock;
import com.human.common.gameplay.quarry.QuarryTerminalPart;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The placement hologram: while a player holds a quarry terminal and looks at a block, show where it would go.
 * <ul>
 * <li>The terminal's six blocks where it would land - GREEN when it can go there, AMBER when it fits but is not at a
 * chunk edge, RED when something is in the way.</li>
 * <li>When it is not at an edge, the same outline in green where it WOULD fit, so the player sees how far to move.</li>
 * <li>The chunk it would start mining (cyan) and the frame ring around it (yellow), at the terminal's level.</li>
 * </ul>
 * It reads the exact rule the block enforces ({@link QuarryPlacement}), so the hologram and the block never disagree.
 * <h2>COST</h2> Nothing unless a terminal is in hand. Then six block lookups and a few line boxes per frame.
 */
public final class QuarryPlacementPreview {

    private static final float[] GREEN = { 0.35F, 1.0F, 0.5F };

    private static final float[] AMBER = { 1.0F, 0.75F, 0.25F };

    private static final float[] RED = { 1.0F, 0.25F, 0.25F };

    private static final float[] CYAN = { 0.3F, 0.85F, 1.0F };

    private static final float[] YELLOW = { 1.0F, 0.9F, 0.2F };

    private QuarryPlacementPreview() {}

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Vec3 camera) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;

        if (
            player == null || level == null || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
        ) {
            return;
        }

        var terminal = heldTerminal(minecraft);

        if (terminal == null) {
            return;
        }

        // Where vanilla would put the block: into the aimed block if it can be replaced, else against the face.
        var core = level.getBlockState(hit.getBlockPos()).canBeReplaced()
            ? hit.getBlockPos()
            : hit.getBlockPos().relative(hit.getDirection());
        var facing = player.getDirection().getOpposite();
        var state = terminal.defaultBlockState().setValue(QuarryTerminalBlock.FACING, facing);
        var result = QuarryPlacement.check(level, core, facing, state, null);
        var lines = buffers.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        var colour = !result.fits() ? RED : result.shift() != 0 ? AMBER : GREEN;

        footprint(poseStack, lines, core, facing, colour);

        var goodCore = core;

        if (result.shift() != 0) {
            goodCore = core.relative(facing.getOpposite(), result.shift());
            footprint(poseStack, lines, goodCore, facing, GREEN);
        }

        // The chunk it would mine and the frame ring around it, one block tall at the terminal's level.
        var chunk = new ChunkPos(goodCore.relative(facing.getOpposite(), QuarryPlacement.AREA_OFFSET));
        var y = goodCore.getY();
        var area = new AABB(chunk.getMinBlockX(), y, chunk.getMinBlockZ(), chunk.getMaxBlockX() + 1, y + 1, chunk.getMaxBlockZ() + 1);

        box(poseStack, lines, area, CYAN, 0.9F);
        box(poseStack, lines, area.inflate(1.0D, 0.0D, 1.0D), YELLOW, 0.6F);

        poseStack.popPose();
    }

    private static QuarryTerminalBlock heldTerminal(Minecraft minecraft) {
        for (var hand : InteractionHand.values()) {
            if (
                minecraft.player.getItemInHand(hand).getItem() instanceof BlockItem item && item
                    .getBlock() instanceof QuarryTerminalBlock terminal
            ) {
                return terminal;
            }
        }

        return null;
    }

    private static void footprint(
        PoseStack poseStack,
        com.mojang.blaze3d.vertex.VertexConsumer lines,
        BlockPos core,
        Direction facing,
        float[] colour
    ) {
        for (var part : QuarryTerminalPart.values()) {
            box(poseStack, lines, new AABB(part.fromCore(core, facing)).deflate(0.02D), colour, 1.0F);
        }
    }

    private static void box(PoseStack poseStack, com.mojang.blaze3d.vertex.VertexConsumer lines, AABB box, float[] colour, float alpha) {
        LevelRenderer.renderLineBox(poseStack, lines, box, colour[0], colour[1], colour[2], alpha);
    }
}
