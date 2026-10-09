package com.human.client.render.item;

import com.blib.api.client.render.v1.item.AzItemRenderer;
import com.blib.api.client.render.v1.item.AzItemRendererConfig;
import com.human.HumanResources;

/**
 * The Energy Storage Unit in hand, in the GUI and on the ground.
 * <p>
 * ⚠⚠ WITHOUT THIS THE SLOT IS EMPTY. The ESU's block model is {@code builtin/entity} - it has no vanilla geometry at
 * all, only a geo model drawn by its block-entity renderer. A BER draws PLACED blocks; nothing draws the ITEM, so the
 * stack rendered as nothing at all. Every other geo block here already had one; the battery was the omission.
 * </p>
 * <p>
 * No scale override: the geo is y 0..16px, exactly one block, so it fits the slot as authored.
 * </p>
 */
public class ChargingHubItemRenderer extends AzItemRenderer {

    public static final String NAME = "charging_hub";

    public ChargingHubItemRenderer() {
        super(
            AzItemRendererConfig.builder(HumanResources.blockGeoModelLocation(NAME), HumanResources.blockTextureLocation(NAME))
                .useNewOffset(true)
                .build()
        );
    }
}
