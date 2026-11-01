package com.human.client.render.item;

import com.blib.api.client.render.v1.item.AzItemRenderer;
import com.blib.api.client.render.v1.item.AzItemRendererConfig;
import com.human.HumanResources;

/** The Gene Separator item: the centred (unshifted) geo, with the edition's lit texture. */
public class GeneSeparatorItemRenderer extends AzItemRenderer {

    public GeneSeparatorItemRenderer(String edition) {
        super(
            AzItemRendererConfig.builder(
                HumanResources.itemGeoModelLocation("gene_separator"),
                HumanResources.blockTextureLocation(edition)
            )
                .useNewOffset(true)
                .build()
        );
    }
}
