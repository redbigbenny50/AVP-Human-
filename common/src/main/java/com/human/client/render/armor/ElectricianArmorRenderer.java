package com.human.client.render.armor;

import com.blib.api.client.render.v1.armor.AzArmorRenderer;
import com.blib.api.client.render.v1.armor.AzArmorRendererConfig;
import com.human.HumanResources;
import net.minecraft.resources.ResourceLocation;

/**
 * The electrician's hard hat and jacket, on the colonist work-armour rig (the dropship's navigator rig, brought over
 * with the colonist). One texture for both pieces.
 */
public class ElectricianArmorRenderer extends AzArmorRenderer {

    private static final ResourceLocation MODEL = HumanResources.location("geo/entity/armor/colonist_work_armor.geo.json");

    private static final ResourceLocation TEXTURE = HumanResources.location("textures/entity/colonist/profession/electrician_armor.png");

    public ElectricianArmorRenderer() {
        super(AzArmorRendererConfig.builder(MODEL, TEXTURE).build());
    }
}
