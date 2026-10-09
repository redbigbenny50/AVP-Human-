package com.human.client.render.block;

import com.blib.api.client.render.v1.block.AzBlockEntityRenderer;
import com.blib.api.client.render.v1.block.AzBlockEntityRendererConfig;
import com.blib.api.client.render.v1.layer.AzAutoGlowingLayer;
import com.human.HumanResources;
import com.human.client.animation.block.ThermoelectricGeneratorAnimator;
import com.human.common.gameplay.block.entity.power.impl.ThermoelectricGeneratorBlockEntity;
import com.human.common.gameplay.block.power.PowerMode;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * [stated] Sep 24: "when it is off there is no spinning fan it uses the hold anim, when its low it spins at 50% speed
 * when its med its 100% and when its max its 200%." The fan turns the opposite way to the wind rotor, which is how the
 * clip is authored ({@code fan_spin} runs to -360), so the sign is carried here.
 */
public class ThermoelectricGeneratorRenderer extends AzBlockEntityRenderer<ThermoelectricGeneratorBlockEntity> {

    public static final String NAME = "thermoelectric_generator";

    private static final String FAN_BONE = "gFan";

    /** The clip is one full turn per second. */
    private static final float TICKS_PER_TURN = 20.0F;

    private static final float LOW_SPEED = 0.5F;

    private static final float MED_SPEED = 1.0F;

    private static final float MAX_SPEED = 2.0F;

    /** {@code fan_spin} rotates to -360, so the fan turns anticlockwise. */
    private static final float DIRECTION = -1.0F;

    private static final ResourceLocation MODEL_LOCATION = HumanResources.blockGeoModelLocation(NAME);

    private static final ResourceLocation TEXTURE_OFF = HumanResources.blockTextureLocation(NAME);

    private static final ResourceLocation TEXTURE_LOW = HumanResources.blockTextureLocation(NAME + "_low");

    private static final ResourceLocation TEXTURE_MED = HumanResources.blockTextureLocation(NAME + "_med");

    private static final ResourceLocation TEXTURE_MAX = HumanResources.blockTextureLocation(NAME + "_max");

    public ThermoelectricGeneratorRenderer() {
        super(
            AzBlockEntityRendererConfig.<ThermoelectricGeneratorBlockEntity>builder(
                $ -> MODEL_LOCATION,
                ThermoelectricGeneratorRenderer::textureFor
            )
                .setAnimatorProvider(ThermoelectricGeneratorAnimator::new)
                .setPrerenderEntry(context -> {
                    var fan = context.bakedModel().getBoneOrNull(FAN_BONE);

                    if (fan != null) {
                        fan.setRotY(spinAngleRadians(context.animatable(), context.partialTick()));
                    }

                    return context;
                })
                .addRenderLayer(new AzAutoGlowingLayer<>())
                .build()
        );
    }

    private static ResourceLocation textureFor(ThermoelectricGeneratorBlockEntity blockEntity) {
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

    private static float spinAngleRadians(ThermoelectricGeneratorBlockEntity blockEntity, float partialTick) {
        var state = blockEntity.getBlockState();

        if (!state.hasProperty(PowerMode.PROPERTY)) {
            return 0.0F;
        }

        var speed = switch (state.getValue(PowerMode.PROPERTY)) {
            case LOW -> LOW_SPEED;
            case MED -> MED_SPEED;
            case MAX -> MAX_SPEED;
            case OFF -> 0.0F; // spinhold
        };

        if (speed <= 0.0F) {
            return 0.0F;
        }

        var level = Minecraft.getInstance().level;

        if (level == null) {
            return 0.0F;
        }

        var ticks = (float) (level.getGameTime() % 100000L) + partialTick;

        return (float) (DIRECTION * ticks * speed / TICKS_PER_TURN * 2.0 * Math.PI % (2.0 * Math.PI));
    }
}
