package com.human.neoforge.mixin.client;

import com.human.client.render.OversizedRender;
import com.human.client.render.block.CrateRenderer;
import com.human.client.render.block.CryotankRenderer;
import com.human.client.render.block.FabricatorRenderer;
import com.human.client.render.block.FluidTankRenderer;
import com.human.client.render.block.HydroelectricGeneratorRenderer;
import com.human.client.render.block.QuarryTerminalRenderer;
import com.human.client.render.block.VaporizerRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Oct 17 FIX - [stated] "theres an active quarry here... it seems to struggle to stay visible at most angles; crates
 * also". Root cause, read from the source of the mod in his instance: EMBEDDIUM does not ask NeoForge's
 * {@code ClientHooks.isBlockEntityRendererVisible} - which {@link MixinClientHooks_OversizedRenders} answers - it calls
 * {@code renderer.getRenderBoundingBox(blockEntity)} itself (EmbeddiumWorldRenderer.isBlockEntityRendererVisible) and
 * frustum-tests that, for global block entities too. Our renderers never declared that method, so Embeddium got
 * NeoForge's default - the single block the block entity sits on - and culled the whole quarry gantry, crate or
 * fabricator the moment that one block left the view.
 * <p>
 * This gives every oversized renderer the method itself, answering with its real size ({@link OversizedRender}), so
 * EVERY caller - NeoForge's hook, Embeddium, Oculus's shadow pass, any other mod - sees the true box.
 * </p>
 * <p>
 * ⚠ The parameter is the ERASED type, {@code BlockEntity}: the interface method is {@code getRenderBoundingBox(T)},
 * which compiles to {@code getRenderBoundingBox(BlockEntity)}. A merged method typed
 * {@code (QuarryTerminalBlockEntity)} would have a different descriptor, override nothing, and change nothing. It is a
 * plain merged method, not an {@code @Overwrite}: the renderers do not declare it - it is a default on NeoForge's
 * {@code IBlockEntityRendererExtension}, which only exists on NeoForge, so it cannot live in the common renderers.
 * </p>
 */
@Mixin(
    {
        QuarryTerminalRenderer.class,
        CrateRenderer.class,
        FabricatorRenderer.class,
        FluidTankRenderer.class,
        CryotankRenderer.class,
        HydroelectricGeneratorRenderer.class,
        VaporizerRenderer.class
    }
)
public abstract class MixinOversizedRenderers_RenderBox {

    @SuppressWarnings("unchecked")
    public AABB getRenderBoundingBox(BlockEntity blockEntity) {
        return ((OversizedRender<BlockEntity>) (Object) this).renderBox(blockEntity);
    }
}
