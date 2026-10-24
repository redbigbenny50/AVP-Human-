package com.human.client.render.item;

import com.blib.api.client.render.v1.item.AzItemRenderer;
import com.blib.api.client.render.v1.item.AzItemRendererConfig;
import com.human.HumanResources;
import com.human.client.render.block.QuarryTerminalRenderer;
import com.human.common.gameplay.quarry.QuarryEdition;

/**
 * A quarry terminal in the hand, the inventory or on the ground, in its powered skin. Shrunk and centred by the item
 * model's display transforms ({@code models/item/*quarry_terminal.json}), as the fabricator is - the model is three
 * blocks wide and two deep.
 */
public class QuarryTerminalItemRenderer extends AzItemRenderer {

    public QuarryTerminalItemRenderer(QuarryEdition edition) {
        super(
            AzItemRendererConfig.builder(
                HumanResources.blockGeoModelLocation("quarry_terminal"),
                QuarryTerminalRenderer.itemTexture(edition)
            )
                .useNewOffset(true)
                .build()
        );
    }
}
