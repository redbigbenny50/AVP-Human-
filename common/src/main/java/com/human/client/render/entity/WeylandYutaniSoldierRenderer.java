package com.human.client.render.entity;

import com.human.HumanResources;
import com.human.client.animation.entity.MarineAnimator;
import com.human.client.render.layer.human.HumanOutfitLayer;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * The Weyland-Yutani soldiers ([stated] Oct 13): the marine's rendering and animations on the weyland_yutani_employee
 * body - the colonist's model, "a better match than the marine". The employee model is the marine's skeleton without
 * the canteen and two pouch bones, which no code or animation uses (checked), so every marine pose, aim and swim
 * animation works on it. Skin, hair, beard, eyes and jumpsuit come from the soldier's
 * WeylandYutaniEmployeeFeatureManager.
 */
public class WeylandYutaniSoldierRenderer extends AbstractHumanRenderer<Marine> {

    private static final ResourceLocation MALE_MODEL = HumanResources.entityGeoModelLocation("weyland_yutani_employee_male");

    private static final ResourceLocation FEMALE_MODEL = HumanResources.entityGeoModelLocation("weyland_yutani_employee_female");

    private static final ResourceLocation BASE_TEXTURE = HumanResources.entityTextureLocation("weyland_yutani_employee/base");

    public WeylandYutaniSoldierRenderer(EntityRendererProvider.Context context) {
        super(
            AbstractHumanRenderer.<Marine>createConfig(entity -> entity.isMale.get() ? MALE_MODEL : FEMALE_MODEL, entity -> BASE_TEXTURE)
                .setAnimatorProvider(MarineAnimator::new)
                .addRenderLayer(new HumanOutfitLayer<>()),
            context
        );
    }
}
