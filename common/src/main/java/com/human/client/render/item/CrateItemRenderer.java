package com.human.client.render.item;

import com.blib.api.client.render.v1.item.AzItemRenderer;
import com.blib.api.client.render.v1.item.AzItemRendererConfig;
import com.human.common.gameplay.storage.CrateType;

/**
 * A crate in the hand, the inventory or on the ground. The geo model is full crate size; it is shrunk to one block by
 * the item model's display transforms (models/item/&lt;id&gt;.json), not here - the geo item renderer applies its own
 * scale BEFORE its centring offset, which would push a shrunk crate off centre.
 */
public class CrateItemRenderer extends AzItemRenderer {

    public CrateItemRenderer(CrateType type) {
        super(AzItemRendererConfig.builder(type.modelLocation(), type.textureLocation()).useNewOffset(true).build());
    }
}
