package com.human.client.render.armor;

import com.blib.api.client.render.v1.armor.AzArmorRenderer;
import com.blib.api.client.render.v1.armor.AzArmorRendererConfig;
import com.human.HumanResources;
import net.minecraft.resources.ResourceLocation;

/**
 * The Geneticist's goggles, lab coat and coat tails, on the colonist work-armour rig (same bones as the electrician).
 * [stated] Oct 28: the coat drapes over the legs to the knee - the tails are on the leg bones so they swing with the
 * legs instead of clipping through them.
 */
public class GeneticistArmorRenderer extends AzArmorRenderer {

    private static final ResourceLocation MODEL = HumanResources.location("geo/entity/armor/geneticist_armor.geo.json");

    private static final ResourceLocation TEXTURE = HumanResources.location("textures/entity/colonist/profession/geneticist_armor.png");

    public GeneticistArmorRenderer() {
        super(AzArmorRendererConfig.builder(MODEL, TEXTURE).build());
    }
}
