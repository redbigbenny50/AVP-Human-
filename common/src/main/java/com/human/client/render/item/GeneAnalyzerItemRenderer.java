package com.human.client.render.item;

import com.blib.api.client.render.v1.item.AzItemRenderer;
import com.blib.api.client.render.v1.item.AzItemRendererConfig;
import com.human.HumanResources;

/** The Gene Analyzer item: the same centred geo as the block, with the edition's lit texture. */
public class GeneAnalyzerItemRenderer extends AzItemRenderer {

    public GeneAnalyzerItemRenderer(String edition) {
        super(
            AzItemRendererConfig.builder(HumanResources.itemGeoModelLocation("gene_analyzer"), HumanResources.blockTextureLocation(edition))
                .useNewOffset(true)
                .build()
        );
    }
}
