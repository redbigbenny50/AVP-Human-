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
public class CryotankRenderer implements BlockEntityRenderer<CryotankBlockEntity>, com.human.client.render.OversizedRender<CryotankBlockEntity> {

    /** Room kept clear between the frozen pose and the glass on every side, in blocks. */
    private static final float GLASS_MARGIN = 0.12F;

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

        // ⚠ Oct 16 fix - every update the tank sends (its frost and thaw steps) loads the specimen into a NEW tag
        // object, so comparing by identity rebuilt the display copy every few seconds - and a new copy starts its
        // pose clip from the top, so frozen xenos uncurled and curled again over and over. The same specimen keeps
        // its copy: a changed object is checked by content, and only a real change (a different specimen) rebuilds.
        if (cached != null && cached.source() != tag && tag != null && tag.equals(cached.source())) {
            cached = new Cached(tag, cached.entity());
            cache.put(tank, cached);
        }

        // [stated] Oct 19 - "i saw the pose glitching out of its molt loop pose like it was uncurling ... when it was
        // in
        // the cryotank". A copy built fresh starts its frozen pose from nothing: it is drawn one frame in its bind pose
        // and then blends into the curl, which reads as the specimen uncurling and curling again. The tank's specimen
        // data still changes now and then (the stored mob's own data shifting between saves), and every change built a
        // new copy. A change to the SAME kind of mob now loads the new data into the copy already on display - its
        // animator, and so its held pose, carry straight on. Only a different mob builds a new copy.
        if (cached != null && cached.source() != tag && tag != null) {
            var shownType = cached.entity().getType();
            var incomingType = EntityType.by(tag).orElse(null);

            if (incomingType == shownType) {
                var shown = cached.entity();
                var keepX = shown.getX();
                var keepY = shown.getY();
                var keepZ = shown.getZ();
                var keepYaw = shown.getYRot();

                shown.load(tag);
                shown.moveTo(keepX, keepY, keepZ, keepYaw, 0.0F);

                if (shown instanceof net.minecraft.world.entity.LivingEntity living) {
                    living.setYBodyRot(keepYaw);
                    living.yBodyRotO = keepYaw;
                    living.setYHeadRot(keepYaw);
                    living.yHeadRotO = keepYaw;
                }

                cached = new Cached(tag, shown);
                cache.put(tank, cached);
            }
        }

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

        // The copy's size as the BLib renderer will draw it: its own scaleWidth/scaleHeight (applied before the turn,
        // as
        // BLib does) and the mob's scale attribute.
        var bodyYaw = entity instanceof net.minecraft.world.entity.LivingEntity living ? living.yBodyRot : entity.getYRot();
        var mobScale = entity instanceof net.minecraft.world.entity.LivingEntity living ? living.getScale() : 1.0F;
        var widthScale = 1.0F;
        var heightScale = 1.0F;

        if (
            Minecraft.getInstance()
                .getEntityRenderDispatcher()
                .getRenderer(entity) instanceof com.blib.api.client.render.v1.entity.AzEntityRenderer<?> azRenderer
        ) {
            @SuppressWarnings("unchecked")
            var typedRenderer = (com.blib.api.client.render.v1.entity.AzEntityRenderer<net.minecraft.world.entity.Entity>) azRenderer;
            widthScale = typedRenderer.config().scaleWidth(entity);
            heightScale = typedRenderer.config().scaleHeight(entity);
        }

        var turn = (float) Math.toRadians(180.0F - bodyYaw);

        // [stated] Oct 19 - "queens in cryotanks that say they are big enough stick out the top ... it might be because
        // its not accounting for her floating off the bottom". The fit and the seating used to come from the HITBOX,
        // but a frozen pose is not the hitbox: a curled queen is about 6.6 blocks tall against a 5.5 box, and her pose
        // does not begin at her feet. Now the posed model's own measured box (CryoDisplay.bounds) decides both: the
        // specimen is shrunk further, only if it has to be, until the whole pose sits inside the glass with a small
        // margin, and that pose - not the hitbox - is what gets centred. (The server's check of whether she fits at
        // all is CryoSpecimens.fitScale, which now also knows the royals' frozen sizes.)
        var box = com.human.client.render.cryo.CryoDisplay.bounds(entity);
        var fitScale = scale;

        if (box != null) {
            var poseWidth = Math.max(box[3] - box[0], box[5] - box[2]) * widthScale * mobScale * scale;
            var poseHeight = (box[4] - box[1]) * heightScale * mobScale * scale;
            var roomAcross = Math.min(tank.sizeX(), tank.sizeZ()) - 2.0F * GLASS_MARGIN;
            var roomUp = tank.sizeY() - 2.0F * GLASS_MARGIN;
            var shrink = Math.min(1.0F, Math.min(roomAcross / Math.max(poseWidth, 1.0E-3F), roomUp / Math.max(poseHeight, 1.0E-3F)));

            fitScale = scale * shrink;
        }

        // [stated] Oct 19 - a pose that names a centre bone (cryo_poses.json, e.g. the chestburster's curl) puts THAT
        // point in the middle of the tank; any other pose puts the middle of its measured box there; a mob with no
        // measurement yet (its first frame) falls back to the captured hitbox, as before.
        var centre = com.human.client.render.cryo.CryoDisplay.centre(entity);

        if (centre == null && box != null) {
            centre = new org.joml.Vector3f((box[0] + box[3]) / 2.0F, (box[1] + box[4]) / 2.0F, (box[2] + box[5]) / 2.0F);
        }

        if (centre != null) {
            var offset = new org.joml.Vector3f(centre)
                .rotateY(turn)
                .mul(widthScale, heightScale, widthScale)
                .mul(mobScale);

            poseStack.translate(tank.sizeX() / 2.0, tank.sizeY() / 2.0, tank.sizeZ() / 2.0);
            poseStack.scale(fitScale, fitScale, fitScale);
            poseStack.translate(-offset.x, -offset.y, -offset.z);
        } else {
            poseStack.translate(tank.sizeX() / 2.0, (tank.sizeY() - tank.specimenHeight() * scale) / 2.0, tank.sizeZ() / 2.0);
            poseStack.scale(scale, scale, scale);
        }

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

    /** The specimen sits in the middle of a tank up to 15 x 15 and as tall as it is built. */
    @Override
    public net.minecraft.world.phys.AABB renderBox(@NotNull CryotankBlockEntity tank) {
        // Only the master draws; every other block of the tank keeps its own one-block box, so the thousands of blocks
        // of a big tank are culled exactly as tightly as before and never pass the visibility check off screen.
        if (!tank.isMaster()) {
            return new net.minecraft.world.phys.AABB(tank.getBlockPos());
        }

        return new net.minecraft.world.phys.AABB(tank.getBlockPos()).inflate(16.0D, 4.0D, 16.0D).expandTowards(0.0D, 48.0D, 0.0D);
    }
}
