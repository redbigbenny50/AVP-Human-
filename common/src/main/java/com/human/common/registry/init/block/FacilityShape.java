package com.human.common.registry.init.block;

import com.blib.api.common.registry.v1.BLibHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * A slab, stairs or wall cut from one of the W-Y or USCM facility blocks ([stated] Oct 30: the picked W-Y and USCM
 * blocks get slabs, stairs and - for the wall faces - walls, like the plastic, concrete and composite families).
 * <p>
 * Kept in their own list ({@code SHAPES}) beside each set's {@code ALL}, because they need what plain cubes don't: a
 * slab drops two when doubled, and the shapes go in the vanilla slab / stairs / wall tags (a wall only connects to
 * blocks in {@code #minecraft:walls}). Textures are the base block's own; obtained only on the stonecutter from the
 * licensed base block, so the faction rule ([stated] Oct 20: printed in the fabricator only, with the license) still
 * holds.
 * </p>
 */
public record FacilityShape(
    BLibHolder<Block> block,
    BLibHolder<Item> item,
    BLibHolder<Block> base,
    Kind kind,
    String id,
    String name,
    String baseId
) {

    public enum Kind {
        SLAB,
        STAIRS,
        WALL
    }

    /** "W-Y Wall" makes "W-Y Low Wall", not "W-Y Wall Wall"; anything else just gains " Wall". */
    public static String wallName(String baseName) {
        return baseName.endsWith(" Wall") ? baseName.substring(0, baseName.length() - 5) + " Low Wall" : baseName + " Wall";
    }
}
