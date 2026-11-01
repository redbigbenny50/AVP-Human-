package com.human.client.render.block;

import com.blib.api.client.render.v1.block.AzBlockEntityRenderer;
import com.blib.api.client.render.v1.block.AzBlockEntityRendererConfig;
import com.human.HumanResources;
import com.human.client.animation.block.GeneSeparatorAnimator;
import com.human.common.gameplay.gene.machine.separator.GeneSeparatorBlock;
import com.human.common.gameplay.gene.machine.separator.GeneSeparatorBlockEntity;
import net.minecraft.resources.ResourceLocation;

/**
 * The Gene Separator: the edition's texture. Its block geo is shifted half a block onto the 2x2 like the Gene
 * Storage's; the screen bars go dark through the screen_off clip.
 */
public class GeneSeparatorRenderer extends AzBlockEntityRenderer<GeneSeparatorBlockEntity> {

    private static final ResourceLocation MODEL = HumanResources.blockGeoModelLocation("gene_separator");

    public GeneSeparatorRenderer() {
        super(
            AzBlockEntityRendererConfig.<GeneSeparatorBlockEntity>builder($ -> MODEL, GeneSeparatorRenderer::textureFor)
                .setAnimatorProvider(GeneSeparatorAnimator::new)
                .build()
        );
    }

    public static ResourceLocation textureFor(GeneSeparatorBlockEntity separator) {
        var id = separator.getBlockState().getBlock() instanceof GeneSeparatorBlock block ? block.edition().id : "gene_separator";
        return HumanResources.blockTextureLocation(id);
    }
}
