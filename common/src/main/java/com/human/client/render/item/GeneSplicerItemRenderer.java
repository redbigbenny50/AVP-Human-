package com.human.client.render.item;

import com.blib.api.client.render.v1.item.AzItemRenderer;
import com.blib.api.client.render.v1.item.AzItemRendererConfig;
import com.human.HumanResources;

/** The Gene Splicer item: the centred (unshifted) geo, with the edition's lit texture. */
public class GeneSplicerItemRenderer extends AzItemRenderer {

    public GeneSplicerItemRenderer(String edition) {
        super(
            AzItemRendererConfig.builder(HumanResources.itemGeoModelLocation("gene_splicer"), HumanResources.blockTextureLocation(edition))
                .useNewOffset(true)
                .build()
        );
    }
}
