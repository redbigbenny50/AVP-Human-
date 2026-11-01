package com.human.client.render.block;

import com.blib.api.client.render.v1.block.AzBlockEntityRenderer;
import com.blib.api.client.render.v1.block.AzBlockEntityRendererConfig;
import com.blib.api.client.render.v1.layer.AzAutoGlowingLayer;
import com.human.HumanResources;
import com.human.client.animation.block.CosmeticCrafterAnimator;
import com.human.common.gameplay.cosmetic.CosmeticCrafterBlockEntity;
import com.human.common.gameplay.cosmetic.CosmeticModelTables;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * The Cosmetic Crafter on its core (the back-middle block): its edition's skin and glowing screen, the other
 * factions' add-ons hidden, and the drawer/screen clips. It reaches past its core on every side, so it reports its
 * real size ({@link com.human.client.render.OversizedRender}) and never blinks out with the core's own block.
 */
public class CosmeticCrafterRenderer extends AzBlockEntityRenderer<CosmeticCrafterBlockEntity> implements com.human.client.render.OversizedRender<CosmeticCrafterBlockEntity> {

    public CosmeticCrafterRenderer() {
        super(config());
    }

    private static AzBlockEntityRendererConfig<CosmeticCrafterBlockEntity> config() {
        var builder = AzBlockEntityRendererConfig.<CosmeticCrafterBlockEntity>builder(
            crafter -> HumanResources.blockGeoModelLocation("cosmetic/cosmetic_crafter"),
            crafter -> HumanResources.blockTextureLocation("cosmetic/" + name(crafter))
        ).setAnimatorProvider(CosmeticCrafterAnimator::new).addRenderLayer(new AzAutoGlowingLayer<>());

        builder.setBoneVisibilityFilter((bone, crafter) -> hidden(crafter).contains(bone.getName()));

        return builder.build();
    }

    private static String name(CosmeticCrafterBlockEntity crafter) {
        return BuiltInRegistries.BLOCK.getKey(crafter.getBlockState().getBlock()).getPath();
    }

    private static Set<String> hidden(CosmeticCrafterBlockEntity crafter) {
        return CosmeticModelTables.HIDDEN.getOrDefault(name(crafter), Set.of());
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull CosmeticCrafterBlockEntity crafter) {
        return true;
    }

    /** 3 wide, 2 deep (the core is the back row), 2 tall - a block of margin all round covers every facing. */
    @Override
    public AABB renderBox(@NotNull CosmeticCrafterBlockEntity crafter) {
        return new AABB(crafter.getBlockPos()).inflate(2.0D, 0.0D, 2.0D).expandTowards(0.0D, 2.0D, 0.0D);
    }
}
