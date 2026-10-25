package com.human.neoforge.client.compat.create;

import com.human.neoforge.compat.create.AlternatorBlock;
import com.human.neoforge.compat.create.AlternatorBlockEntity;
import com.human.neoforge.compat.create.ConverterEdition;
import com.human.neoforge.compat.create.ElectricGearboxBlock;
import com.human.neoforge.compat.create.ElectricGearboxBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.Map;

/**
 * The turning gear on each Create face of the converters.
 * <p>
 * [stated] Oct 24 - "the out port and in ports for the create side should be animated spinning gears". It is a MODEL,
 * not an animated texture: the gear (a 16-sided body, eight box teeth and a raised hub, all flat colours) is turned
 * every frame by Create's own kinetic transform, so it spins at the network's real speed and direction, stays in step
 * with the shafts it meets, and stops dead when they stop. [stated] Oct 24 - an animated texture made the pixels jump;
 * a turning model only moves its edges.
 * </p>
 * <p>
 * ⚠ Deliberately NOT KineticBlockEntityRenderer: that renderer draws nothing when Flywheel's instancing is on,
 * expecting a Flywheel visual. These blocks have no visual, so Flywheel falls back to this renderer, which must always
 * draw.
 * </p>
 */
public class ConverterGearRenderer<T extends KineticBlockEntity> extends SafeBlockEntityRenderer<T> {

    /** One gear model per edition, in that edition's colours. Created during client start-up, before models bake. */
    public static final Map<ConverterEdition, PartialModel> GEARS = new EnumMap<>(ConverterEdition.class);

    static {
        for (var edition : ConverterEdition.values()) {
            GEARS.put(
                edition,
                PartialModel.of(ResourceLocation.fromNamespaceAndPath("avp_human", "block/" + edition.id("converter_gear")))
            );
        }
    }

    /** Touching this class creates the partial models; called from client start-up. */
    public static void init() {}

    @Override
    protected void renderSafe(T be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        var state = be.getBlockState();

        if (!state.hasProperty(HorizontalKineticBlock.HORIZONTAL_FACING)) {
            return;
        }

        var front = state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
        var vb = buffer.getBuffer(RenderType.solid());

        if (be instanceof AlternatorBlockEntity && state.getBlock() instanceof AlternatorBlock block) {
            // Back, left and right - every horizontal face but the front - as Create's gearbox does.
            var time = AnimationTickHolder.getRenderTime(be.getLevel());

            for (var face : Direction.Plane.HORIZONTAL) {
                if (face != front) {
                    renderGear(be, state, GEARS.get(block.edition()), face, gearboxAngle(be, face, time), ms, vb, light);
                }
            }
        } else if (be instanceof ElectricGearboxBlockEntity && state.getBlock() instanceof ElectricGearboxBlock block) {
            // The one shaft, out of the back.
            var back = front.getOpposite();
            renderGear(
                be,
                state,
                GEARS.get(block.edition()),
                back,
                KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), back.getAxis()),
                ms,
                vb,
                light
            );
        }
    }

    private static void renderGear(
        KineticBlockEntity be,
        BlockState state,
        PartialModel gear,
        Direction face,
        float angle,
        PoseStack ms,
        VertexConsumer vb,
        int light
    ) {
        var model = CachedBuffers.partialFacing(gear, state, face);
        KineticBlockEntityRenderer.kineticRotationTransform(model, be, face.getAxis(), angle, light).renderInto(ms, vb);
    }

    /**
     * Create's gearbox rule, exactly as its own GearboxRenderer works it out, so each gear turns the same way as the
     * shaft touching it: faces on the source's axis turn with it or against it, faces across it flip by direction.
     */
    private static float gearboxAngle(KineticBlockEntity be, Direction face, float time) {
        var pos = be.getBlockPos();
        var offset = KineticBlockEntityRenderer.getRotationOffsetForPosition(be, pos, face.getAxis());
        var angle = (time * be.getSpeed() * 3F / 10F) % 360F;

        if (be.getSpeed() != 0F && be.hasSource()) {
            var source = be.source.subtract(pos);
            var sourceFacing = Direction.getNearest(source.getX(), source.getY(), source.getZ());

            if (sourceFacing.getAxis() == face.getAxis()) {
                angle *= sourceFacing == face ? 1F : -1F;
            } else if (sourceFacing.getAxisDirection() == face.getAxisDirection()) {
                angle *= -1F;
            }
        }

        angle += offset;
        return angle / 180F * (float) Math.PI;
    }
}
