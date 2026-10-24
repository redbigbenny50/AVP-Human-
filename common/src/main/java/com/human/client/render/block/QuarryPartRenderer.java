package com.human.client.render.block;

import com.blib.api.client.render.v1.block.AzBlockEntityRenderer;
import com.blib.api.client.render.v1.block.AzBlockEntityRendererConfig;
import com.blib.api.client.render.v1.layer.AzAutoGlowingLayer;
import com.human.HumanResources;
import com.human.common.gameplay.quarry.QuarryEdition;
import com.human.common.gameplay.quarry.QuarryTerminalBlockEntity;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

/**
 * One moving part of the quarry - drill head, shaft segment, gantry arm, truck or carriage - drawn by the terminal's
 * renderer wherever that part currently is. Each part is its own BLib model in the terminal's edition.
 * <p>
 * BLib turns every block-entity model to the block's facing; {@link QuarryTerminalRenderer} cancels that turn before
 * calling a part, so the gantry runs along the world's X and Z whichever way the terminal faces.
 * </p>
 */
final class QuarryPartRenderer extends AzBlockEntityRenderer<QuarryTerminalBlockEntity> {

    QuarryPartRenderer(String model, boolean glows) {
        this(model, glows, (terminal, bones) -> {});
    }

    /**
     * @param bones sets this part's moving bones for the terminal about to be drawn. ⚠ Every part model is ONE baked
     *              model shared by every quarry, so the hook must set every bone it touches on every draw - a value
     *              left over from another quarry would otherwise show here.
     */
    QuarryPartRenderer(
        String model,
        boolean glows,
        java.util.function.BiConsumer<QuarryTerminalBlockEntity, com.blib.api.client.model.v1.AzBakedModel> bones
    ) {
        super(build(model, glows, bones));
    }

    private static AzBlockEntityRendererConfig<QuarryTerminalBlockEntity> build(
        String model,
        boolean glows,
        java.util.function.BiConsumer<QuarryTerminalBlockEntity, com.blib.api.client.model.v1.AzBakedModel> bones
    ) {
        var geo = HumanResources.blockGeoModelLocation(model);
        Map<QuarryEdition, ResourceLocation> textures = new EnumMap<>(QuarryEdition.class);

        for (var edition : QuarryEdition.values()) {
            textures.put(edition, HumanResources.blockTextureLocation("quarry/" + model + "_" + edition.textureKey()));
        }

        var builder = AzBlockEntityRendererConfig.<QuarryTerminalBlockEntity>builder($ -> geo, terminal -> textures.get(terminal.edition()))
            .setPrerenderEntry(context -> {
                bones.accept(context.animatable(), context.bakedModel());
                return context;
            });

        if (glows) {
            builder.addRenderLayer(new AzAutoGlowingLayer<>());
        }

        return builder.build();
    }
}
