package com.human.client.render;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/**
 * A block-entity renderer whose model reaches past its own block. [stated] Oct 15: "make sure certain camera angles
 * dont remove the drilling head or arms ... it seems to be a pattern with your models".
 * <h2>WHY THE MODELS VANISHED</h2> {@code shouldRenderOffScreen} puts a block entity in vanilla's global list, so it no
 * longer disappears with its chunk section - and on vanilla and Fabric that is the end of it. NeoForge, though,
 * frustum-checks that global list a second time with {@code getRenderBoundingBox}, which defaults to the SINGLE block
 * the block entity is on. So whenever that one block left the screen, the whole multi-block model blinked out, however
 * much of it was still in view.
 * <p>
 * {@code getRenderBoundingBox} cannot simply be overridden from :common - vanilla has no such method, and declaring it
 * with the erased signature clashes with NeoForge's generic one when the NeoForge build compiles :common. So a renderer
 * says how big it really is through this interface, and a NeoForge-only mixin on {@code ClientHooks} uses it.
 * </p>
 */
public interface OversizedRender<T extends BlockEntity> {

    /** Everything this renderer can draw for {@code blockEntity}, in world coordinates. */
    AABB renderBox(T blockEntity);
}
