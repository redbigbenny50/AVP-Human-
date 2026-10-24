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

    /** Water this deep or deeper at a sample counts as DEEP: a lake, river or ocean edge, not a marsh. */
    private static final int DEEP_WATER_BLOCKS = 3;

    /** How many of the nine samples may be deep water before a site is refused. */
    private static final int MAX_DEEP_SAMPLES = 3;

    /**
     * Whether a site is mostly shallow - the rule for the swamp and jungle mining camps.
     * <p>
     * [stated] Oct 23 - "swamps are normally 60% water", so {@link #isSiteDry} (any water at all refuses the site)
     * would keep the swamp camp out of almost every swamp. This allows shallow water anywhere on the site - marsh and
     * puddles - and refuses only a site that sits on DEEP water ({@value #DEEP_WATER_BLOCKS}+ blocks): the centre deep,
     * or more than {@value #MAX_DEEP_SAMPLES} of the same nine samples ({@value #FOOTPRINT_STEP} blocks apart) deep.
     * That keeps both camps off lakes, rivers and ocean edges inside their biomes.
     * </p>
     * <p>
     * ⚡ Same cost as isSiteDry: one raw-terrain column per sample, at most nine, once per candidate site, and it stops
     * as soon as the answer is known.
     * </p>
     */
    public static boolean isSiteMostlyShallow(@NotNull Structure.GenerationContext generationContext) {
        var chunkPos = generationContext.chunkPos();
        var centreX = chunkPos.getMiddleBlockX();
        var centreZ = chunkPos.getMiddleBlockZ();
        var deepSamples = 0;

        for (int dx = -FOOTPRINT_STEP; dx <= FOOTPRINT_STEP; dx += FOOTPRINT_STEP) {
            for (int dz = -FOOTPRINT_STEP; dz <= FOOTPRINT_STEP; dz += FOOTPRINT_STEP) {
                if (!isDeepWaterAt(generationContext, centreX + dx, centreZ + dz)) {
                    continue;
                }
                if ((dx == 0 && dz == 0) || ++deepSamples > MAX_DEEP_SAMPLES) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Whether the raw terrain at (x, z) has at least {@value #DEEP_WATER_BLOCKS} blocks of water from its surface down.
     */
    private static boolean isDeepWaterAt(@NotNull Structure.GenerationContext generationContext, int x, int z) {
        var generator = generationContext.chunkGenerator();
        var heightAccessor = generationContext.heightAccessor();
        var surfaceY = generator.getFirstOccupiedHeight(
            x,
            z,
            Heightmap.Types.WORLD_SURFACE_WG,
            heightAccessor,
            generationContext.randomState()
        );
        var column = generator.getBaseColumn(x, z, heightAccessor, generationContext.randomState());

        for (int depth = 0; depth < DEEP_WATER_BLOCKS; depth++) {
            var y = surfaceY - depth;
            if (y < heightAccessor.getMinBuildHeight() || !column.getBlock(y).getFluidState().is(FluidTags.WATER)) {
                return false;
            }
        }

        return true;
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
