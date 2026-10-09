package com.human.client.render.entity;

import com.blib.api.client.render.v1.entity.AzEntityRenderer;
import com.blib.api.client.render.v1.entity.AzEntityRendererConfig;
import com.blib.api.client.render.v1.layer.AzAutoGlowingLayer;
import com.human.HumanResources;
import com.human.client.animation.entity.SentryTurretAnimator;
import com.human.common.gameplay.entity.machine.SentryTurret;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class SentryTurretRenderer extends AzEntityRenderer<SentryTurret> {

    public static final String NAME = "sentry_turret";

    private static final ResourceLocation GEO = HumanResources.blockGeoModelLocation(NAME);

    private static final ResourceLocation TEX = HumanResources.blockTextureLocation(NAME);

    public SentryTurretRenderer(EntityRendererProvider.Context context) {
        super(
            // [stated] Oct 2: each turret draws its own skin, falling back to the USCM texture.
            AzEntityRendererConfig.<SentryTurret>builder(
                turret -> GEO,
                turret -> com.human.client.skin.SentrySkins.texture(turret.getSkin(), TEX)
            )
                .setAnimatorProvider(SentryTurretAnimator::new)
                .addRenderLayer(new AzAutoGlowingLayer<>())
                .build(),
            context
        );
    }
}
