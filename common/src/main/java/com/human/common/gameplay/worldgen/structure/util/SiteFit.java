package com.human.common.gameplay.worldgen.structure.util;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

/**
 * Fits a planned structure to the ground under its WHOLE footprint (Oct 28). [stated] "make sure it places and gently
 * carves the land around it ... reduce structures greatly overhanging cliffs or not placing on ground".
 * <p>
 * Vanilla's jigsaw drops a structure to the ground height at ONE point - the middle of its footprint - so a 47-block
 * outpost on a slope or a badlands mesa edge could hang off a cliff on one side and sit buried on the other. This:
 * </p>
 * <ol>
 * <li>builds the planned pieces and reads their real footprint (whatever way the jigsaw rotated them);</li>
 * <li>samples the natural ground on a 5x5 grid across it;</li>
 * <li>REFUSES the site if the ground's rise across the footprint is more than the structure allows (a cliff, a mesa
 * edge, a ravine) - and, for structures that must stay dry, if any sample is water;</li>
 * <li>otherwise moves the structure to the MEDIAN ground height, so about half the footprint is filled up to it and
 * half dug down to it. The structure's terrain adaptation (beard) then blends that into the land around it - filling
 * under, carving above, with a soft falloff at the edges - instead of a sheer wall or a floating foundation.</li>
 * </ol>
 * <p>
 * ⚠ Everything here reads only the raw terrain the chunk generator predicts, so {@code /locate} and real generation
 * make the same decision. ⚡ Cost: one piece build and 25 height samples (plus 25 water columns for dry structures) per
 * candidate site, and candidate sites are tens of chunks apart.
 * </p>
 */
public final class SiteFit {

    private static final int GRID = 5;

    /** Pulled in from the footprint edge so a sample does not land on the structure's outermost fence line. */
    private static final int INSET = 2;

    private SiteFit() {}

    /**
     * Fits {@code stub}'s pieces to the ground. Empty when the ground is too uneven (or wet, if {@code mustBeDry}).
     *
     * @param maxRelief the most the ground may rise across the footprint, in blocks
     */
    public static Optional<Structure.GenerationStub> fit(
        @NotNull Structure.GenerationContext context,
        @NotNull Structure.GenerationStub stub,
        int maxRelief,
        boolean mustBeDry
    ) {
        var builder = stub.getPiecesBuilder();

        if (builder.isEmpty()) {
            return Optional.empty();
        }

        // The START piece is what the jigsaw grounded (children such as the marine spawn markers sit inside it).
        var box = builder.build().pieces().getFirst().getBoundingBox();
        var generator = context.chunkGenerator();
        var heightAccessor = context.heightAccessor();
        var randomState = context.randomState();

        var minX = Math.min(box.minX() + INSET, box.maxX());
        var maxX = Math.max(box.maxX() - INSET, box.minX());
        var minZ = Math.min(box.minZ() + INSET, box.maxZ());
        var maxZ = Math.max(box.maxZ() - INSET, box.minZ());

        var heights = new int[GRID * GRID];
        var index = 0;

        for (var gx = 0; gx < GRID; gx++) {
            var x = minX + (maxX - minX) * gx / (GRID - 1);

            for (var gz = 0; gz < GRID; gz++) {
                var z = minZ + (maxZ - minZ) * gz / (GRID - 1);
                var free = generator.getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState);

                if (mustBeDry) {
                    var column = generator.getBaseColumn(x, z, heightAccessor, randomState);
                    if (column.getBlock(free - 1).getFluidState().is(FluidTags.WATER)) {
                        return Optional.empty();
                    }
                }

                heights[index++] = free;
            }
        }

        var sorted = heights.clone();
        Arrays.sort(sorted);

        if (sorted[sorted.length - 1] - sorted[0] > maxRelief) {
            return Optional.empty();
        }

        // The jigsaw placed the pieces against the ground at the middle of the footprint (the same heightmap, the same
        // point it uses); move them so that ground level becomes the footprint's median instead.
        var centreX = (box.minX() + box.maxX()) / 2;
        var centreZ = (box.minZ() + box.maxZ()) / 2;
        var placedAt = generator.getFirstFreeHeight(centreX, centreZ, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState);
        var median = sorted[sorted.length / 2];
        var offset = median - placedAt;

        if (offset != 0) {
            builder.offsetPiecesVertically(offset);
        }

        var pieces = builder.build().pieces();
        return Optional.of(new Structure.GenerationStub(stub.position(), pieceBuilder -> pieces.forEach(pieceBuilder::addPiece)));
    }
}
