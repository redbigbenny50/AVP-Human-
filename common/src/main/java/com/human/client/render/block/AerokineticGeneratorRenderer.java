package com.human.client.render.block;

import com.blib.api.client.render.v1.block.AzBlockEntityRenderer;
import com.blib.api.client.render.v1.block.AzBlockEntityRendererConfig;
import com.blib.api.client.render.v1.layer.AzAutoGlowingLayer;
import com.human.HumanResources;
import com.human.client.animation.block.AerokineticGeneratorAnimator;
import com.human.common.gameplay.block.entity.power.impl.AerokineticGeneratorBlockEntity;
import com.human.common.gameplay.block.power.PowerMode;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * [stated] Sep 24: "when its off the hold animation plays, when its low power it spins at 50% speed when its medium its
 * 100% and when its max its 150% speed."
 * <p>
 * ⭐ Same reasoning as the hydro wheel and the solar panel: the rotor angle comes from WORLD TIME rather than from
 * playing {@code akg.spin} at a speed, so every turbine on a farm is at the same angle, one placed later is instantly
 * in step, and the three speeds are exact multiples rather than approximations.
 * </p>
 */
public class AerokineticGeneratorRenderer extends AzBlockEntityRenderer<AerokineticGeneratorBlockEntity> {

    public static final String NAME = "aerokinetic_generator";

    private static final String TURBINE_BONE = "gTurbine";

    /** The clip is one full turn per second. */
    private static final float TICKS_PER_TURN = 20.0F;

    private static final float LOW_SPEED = 0.5F;

    private static final float MED_SPEED = 1.0F;

    private static final float MAX_SPEED = 1.5F;

    private static final ResourceLocation MODEL_LOCATION = HumanResources.blockGeoModelLocation(NAME);

    private static final ResourceLocation TEXTURE_OFF = HumanResources.blockTextureLocation(NAME);

    private static final ResourceLocation TEXTURE_LOW = HumanResources.blockTextureLocation(NAME + "_low");

    private static final ResourceLocation TEXTURE_MED = HumanResources.blockTextureLocation(NAME + "_med");

    private static final ResourceLocation TEXTURE_MAX = HumanResources.blockTextureLocation(NAME + "_max");

    public AerokineticGeneratorRenderer() {
        super(
            AzBlockEntityRendererConfig.<AerokineticGeneratorBlockEntity>builder(
                $ -> MODEL_LOCATION,
                AerokineticGeneratorRenderer::textureFor
            )
                .setAnimatorProvider(AerokineticGeneratorAnimator::new)
                .setPrerenderEntry(context -> {
                    var turbine = context.bakedModel().getBoneOrNull(TURBINE_BONE);

                    if (turbine != null) {
                        turbine.setRotY(spinAngleRadians(context.animatable(), context.partialTick()));
                    }

                    return context;
                })
                .addRenderLayer(new AzAutoGlowingLayer<>())
                .build()
        );
    }

    private static ResourceLocation textureFor(AerokineticGeneratorBlockEntity blockEntity) {
        var state = blockEntity.getBlockState();

        if (!state.hasProperty(PowerMode.PROPERTY)) {
            return TEXTURE_OFF;
        }

        return switch (state.getValue(PowerMode.PROPERTY)) {
            case OFF -> TEXTURE_OFF;
            case LOW -> TEXTURE_LOW;
            case MED -> TEXTURE_MED;
            case MAX -> TEXTURE_MAX;
        };
    }

    private static float spinAngleRadians(AerokineticGeneratorBlockEntity blockEntity, float partialTick) {
        var state = blockEntity.getBlockState();

        if (!state.hasProperty(PowerMode.PROPERTY)) {
            return 0.0F;
        }

        var speed = switch (state.getValue(PowerMode.PROPERTY)) {
            case LOW -> LOW_SPEED;
            case MED -> MED_SPEED;
            case MAX -> MAX_SPEED;
            case OFF -> 0.0F; // the hold pose
        };

        if (speed <= 0.0F) {
            return 0.0F;
        }

        var level = Minecraft.getInstance().level;

        if (level == null) {
            return 0.0F;
        }

        var ticks = (float) (level.getGameTime() % 100000L) + partialTick;

        return (float) (ticks * speed / TICKS_PER_TURN * 2.0 * Math.PI % (2.0 * Math.PI));
    }
}
