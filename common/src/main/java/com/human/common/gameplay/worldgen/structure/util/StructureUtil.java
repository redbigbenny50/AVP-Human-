package com.human.common.gameplay.worldgen.structure.util;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.NotNull;

public class StructureUtil {

    /** Spacing of the water samples across a structure's site, in blocks from its centre. */
    private static final int FOOTPRINT_STEP = 12;

    /**
     * Whether the WHOLE site is dry. [stated] Oct 4: a marine camp generated in a lake - the old check
     * ({@code hasAtLeast1NonWaterCorner}) tested ONE block at the chunk's corner, so a site with water under the rest
     * of it still passed. This samples a 3x3 grid across the site - centre, four corners and four edges,
     * {@value #FOOTPRINT_STEP} blocks apart - and refuses the site if ANY of them is water. A lake, river or pond
     * inside an otherwise allowed biome no longer gets a camp in it.
     * <p>
     * ⚡ getBaseColumn is the expensive call - nine of them, once per candidate site (structures are tens of chunks
     * apart), and it stops at the first wet sample.
     * </p>
     */
    public static boolean isSiteDry(@NotNull Structure.GenerationContext generationContext) {
        var chunkPos = generationContext.chunkPos();
        var centreX = chunkPos.getMiddleBlockX();
        var centreZ = chunkPos.getMiddleBlockZ();

        for (int dx = -FOOTPRINT_STEP; dx <= FOOTPRINT_STEP; dx += FOOTPRINT_STEP) {
            for (int dz = -FOOTPRINT_STEP; dz <= FOOTPRINT_STEP; dz += FOOTPRINT_STEP) {
                if (isWaterAt(generationContext, centreX + dx, centreZ + dz)) {
                    return false;
                }
            }
        }

        return true;
    }

    /** Whether the top of the raw terrain at (x, z) is water. The raw column holds only stone, water and air. */
    private static boolean isWaterAt(@NotNull Structure.GenerationContext generationContext, int x, int z) {
        var generator = generationContext.chunkGenerator();
        var surfaceY = generator.getFirstOccupiedHeight(
            x,
            z,
            Heightmap.Types.WORLD_SURFACE_WG,
            generationContext.heightAccessor(),
            generationContext.randomState()
        );
        var column = generator.getBaseColumn(x, z, generationContext.heightAccessor(), generationContext.randomState());
        return column.getBlock(surfaceY).getFluidState().is(FluidTags.WATER);
    }

    /** @deprecated tests a single block only - use {@link #isSiteDry}. Kept so nothing else that calls it breaks. */
    @Deprecated
    public static boolean hasAtLeast1NonWaterCorner(@NotNull Structure.GenerationContext generationContext) {
        var chunkPos = generationContext.chunkPos();
        return !isWaterAt(generationContext, chunkPos.getBlockX(0), chunkPos.getBlockZ(0));
    }

    private StructureUtil() {
        throw new UnsupportedOperationException();
    }
}
