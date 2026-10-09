package com.human.client.render.block;

import com.human.common.gameplay.block.cryo.CryotankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Draws the frozen specimen inside a cryotank, standing in the middle of the tank's floor and scaled down if it had to
 * be squeezed in. [stated] Oct 1: the mob sits centred in the tank's total volume, asleep.
 * <p>
 * The drawn mob is a client-only copy made from the saved data: it is never added to the world, never ticks and has no
 * AI. One copy per tank, rebuilt only when the specimen changes. ⏭ Special per-mob cryo poses plug in here later.
 * </p>
 */
public class CryotankRenderer implements BlockEntityRenderer<CryotankBlockEntity> {

    private record Cached(
        CompoundTag source,
        Entity entity
    ) {}

    private final Map<CryotankBlockEntity, Cached> cache = new WeakHashMap<>();

    /**
     * The world these caches were built in. Joining another world (or rejoining after changing resource packs) starts
     * them over, so a pack's new cryo_poses.json or a re-sized mob texture is picked up.
     */
    private static Object cachedFor;

    public CryotankRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
        @NotNull CryotankBlockEntity tank,
        float partialTick,
        @NotNull PoseStack poseStack,
        @NotNull MultiBufferSource buffers,
        int packedLight,
        int packedOverlay
    ) {
        if (!tank.isMaster() || !tank.hasSpecimen() || tank.getLevel() == null) {
            return;
        }

        if (cachedFor != tank.getLevel()) {
            cachedFor = tank.getLevel();
            cache.clear();
            com.human.client.render.cryo.CryoPose.clear();
            com.human.client.render.cryo.FrostTextureScale.clear();
            com.human.client.render.cryo.VillagerHatMeta.clear();
            com.human.client.render.cryo.CryoIcon.clear();
        }

        var tag = tank.getSpecimen();
        var cached = cache.get(tank);

        if (cached == null || cached.source() != tag) {
            var made = EntityType.create(tag, tank.getLevel()).orElse(null);

            if (made == null) {
                return;
            }

            // Marked as a display copy: that is what gives it the frost overlay and holds it in its frozen pose.
            com.human.client.render.cryo.CryoDisplay.mark(made);

            // ⚠ The copy loads with the position it was CAPTURED at, which may be far away. Anything the renderer works
            // out from the entity's own position - its shadow, the light its name tag reads - would come from there.
            // Put it where it is drawn: the tank's centre.
            var origin = tank.getBlockPos();
            made.moveTo(
                origin.getX() + tank.sizeX() / 2.0,
                origin.getY() + (tank.sizeY() - tank.specimenHeight() * tank.getScale()) / 2.0,
                origin.getZ() + tank.sizeZ() / 2.0,
                made.getYRot(),
                0.0F
            );

            // [stated] Oct 3: specimens faced backwards. Loading a mob restores where it LOOKS (yRot) but not its body
            // rotation, which stayed 0 - and the model turns by the body, so every specimen faced one fixed way. Body
            // and
            // head now follow the saved facing (which loading sets toward the player who loaded it).
            if (made instanceof net.minecraft.world.entity.LivingEntity living) {
                living.setYBodyRot(living.getYRot());
                living.yBodyRotO = living.getYRot();
                living.setYHeadRot(living.getYRot());
                living.yHeadRotO = living.getYRot();
            }

            made.setXRot(0.0F);

            cached = new Cached(tag, made);
            cache.put(tank, cached);
        }

        var entity = cached.entity();
        var scale = tank.getScale();

        // Centred in the tank's whole volume, using the size it was CAPTURED at (the copy's own box can differ for
        // mobs whose size depends on live state).
        poseStack.pushPose();
        poseStack.translate(tank.sizeX() / 2.0, (tank.sizeY() - tank.specimenHeight() * scale) / 2.0, tank.sizeZ() / 2.0);
        poseStack.scale(scale, scale, scale);

        // No shadow: it would be cast on the tank floor from inside the glass, a dark smudge under a floating mob.
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);

        try {
            dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0F, 0.0F, poseStack, buffers, packedLight);
        } finally {
            // Back to true, exactly as vanilla's own InventoryScreen restores it: the player's shadow OPTION is checked
            // separately, so this flag is always true outside such a draw.
            dispatcher.setRenderShadow(true);
        }

        poseStack.popPose();
    }

    /** A big tank's specimen is drawn from the master, which may be off screen while the rest of the tank is not. */
    @Override
    public boolean shouldRenderOffScreen(@NotNull CryotankBlockEntity tank) {
        // Only the master draws anything, so only it needs to bypass culling - not every block of a 3,375-block tank.
        return tank.isMaster() && tank.hasSpecimen();
    }
}
