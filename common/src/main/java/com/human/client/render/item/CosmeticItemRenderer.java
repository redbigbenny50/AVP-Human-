package com.human.client.render.item;

import com.blib.api.client.render.v1.item.AzItemRenderer;
import com.blib.api.client.render.v1.item.AzItemRendererConfig;
import com.human.HumanResources;
import com.human.client.render.block.CosmeticDecorRenderer;
import com.human.common.gameplay.cosmetic.CosmeticModelTables;
import net.minecraft.client.renderer.RenderType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An AzureLib cosmetic block (an animated design, or the crafter) in the hand, the inventory or the crafter's preview:
 * the design's geo in this item's skin, the other factions' parts hidden exactly as on the block, the DEFAULT interior
 * shown for pieces with swappable contents, and see-through glass where the design has it. Sized by the item model's
 * display transforms.
 */
public class CosmeticItemRenderer extends AzItemRenderer {

    public CosmeticItemRenderer(String geo, String name) {
        this(geo, name, false);
    }

    public CosmeticItemRenderer(String geo, String name, boolean translucent) {
        super(config(geo, name, translucent));
    }

    private static AzItemRendererConfig config(String geo, String name, boolean translucent) {
        Set<String> hidden = new HashSet<>(CosmeticModelTables.HIDDEN.getOrDefault(name, Set.of()));
        List<String> contents = CosmeticModelTables.CONTENTS.getOrDefault(name, List.of());

        // An item has no block state: every interior but the default one stays hidden.
        for (int i = 1; i < contents.size(); i++) {
            hidden.add(contents.get(i));
        }

        var texture = HumanResources.blockTextureLocation("cosmetic/" + name);
        var cutout = RenderType.entityCutout(texture);
        var glass = translucent ? RenderType.entityTranslucent(texture) : cutout;

        return AzItemRendererConfig.builder(HumanResources.blockGeoModelLocation("cosmetic/" + geo), texture)
            .useNewOffset(true)
            .addRenderLayer(new com.human.client.render.CosmeticGlowLayer<>())
            .setBoneVisibilityFilter((bone, stack) -> hidden.contains(bone.getName()))
            // Never null: non-glass bones get the base render type back (BLib per-bone override rule).
            .setBoneRenderTypeOverrideProvider(bone -> bone.getName().startsWith(CosmeticDecorRenderer.GLASS_BONE) ? glass : cutout)
            .build();
    }
}
