package com.human.client.render.block;

import com.blib.api.client.model.v1.AzBone;
import com.blib.api.client.render.v1.block.AzBlockEntityRenderer;
import com.blib.api.client.render.v1.block.AzBlockEntityRendererConfig;
import com.human.HumanResources;
import com.human.common.gameplay.cosmetic.CosmeticBlockEntity;
import com.human.common.gameplay.cosmetic.CosmeticModelTables;
import com.human.common.gameplay.cosmetic.CosmeticSpecimenBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Every AzureLib cosmetic design (screens, LEDs, glowing crystals, see-through glass or a swappable interior): its
 * design's geo, its edition's skin with the {@code _glowmask}, and three per-bone rules -
 * <ul>
 * <li>the other factions' parts are hidden (one geo holds every faction's parts, [stated] Oct 31);</li>
 * <li>of the swappable interiors ({@code gContents...}), only the one in the block's CONTENTS state shows;</li>
 * <li>the {@code gGlass} group draws see-through on designs flagged translucent. Translucent geometry is drawn after
 * the cutout pass, so the contents behind the glass are already there when it blends over them.</li>
 * </ul>
 * BLib applies all of them every frame without touching the shared baked model.
 */
public class CosmeticDecorRenderer extends AzBlockEntityRenderer<CosmeticBlockEntity> implements com.human.client.render.OversizedRender<CosmeticBlockEntity> {

    public static final String GLASS_BONE = "gGlass";

    public CosmeticDecorRenderer() {
        super(config());
    }

    private static AzBlockEntityRendererConfig<CosmeticBlockEntity> config() {
        var builder = AzBlockEntityRendererConfig.<CosmeticBlockEntity>builder(
            decor -> HumanResources.blockGeoModelLocation("cosmetic/" + decor.block().design().id()),
            CosmeticDecorRenderer::texture
        ).addRenderLayer(new com.human.client.render.CosmeticGlowLayer<>());

        // Floor / ceiling mounts and the doors, lids and spinners (see CosmeticDecorModelRenderer).
        builder.setModelRenderer(
            (pipeline, layer) -> new CosmeticDecorModelRenderer(
                (com.blib.api.client.render.v1.block.pipeline.AzBlockEntityRendererPipeline<CosmeticBlockEntity>) pipeline,
                layer
            )
        );

        // ⚠ Never null for a non-glass bone: BLib's per-bone overrides must hand back the BASE render type there,
        // otherwise a previous bone's buffer can leak onto the next. entityCutout is the builder's own default.
        builder.setBoneRenderTypeOverrideProvider(CosmeticDecorRenderer::renderType);

        // ⚠ setBoneVisibilityFilter is only on the base builder, whose return type is the base builder - so it is
        // called
        // for its effect, and the block builder does the building.
        builder.setBoneVisibilityFilter(CosmeticDecorRenderer::hide);

        // BLib 0.3.12c - see-through parts of one piece (tank walls, a window over glass vials, a specimen jar's glass
        // over its liquid) are sorted back to front together, so glass behind glass blends instead of one pane hiding
        // the other. Only translucent geometry is held back; solid parts draw as before.
        builder.setTranslucencySorting(true);

        return builder.build();
    }

    /**
     * The model, then - for a weapon rack - each gun drawn on its mount with the game's own item rendering (FIXED, as
     * an item frame draws it), so AVP, TACZ and vanilla weapons each look exactly as their mod draws them. The guns get
     * the same turns as the model: facing about Y, then a floor/ceiling mount's X turn about the block middle.
     */
    @Override
    public void render(
        @org.jetbrains.annotations.NotNull CosmeticBlockEntity decor,
        float partialTick,
        @org.jetbrains.annotations.NotNull com.mojang.blaze3d.vertex.PoseStack poseStack,
        @org.jetbrains.annotations.NotNull net.minecraft.client.renderer.MultiBufferSource source,
        int packedLight,
        int packedOverlay
    ) {
        // A baked piece (a storage crate, say) has a block entity only for its contents: its model is in the chunk
        // mesh already, so there is nothing to draw here.
        if (!decor.block().design().animated()) {
            return;
        }

        // A big piece is drawn even when its own block's section is off screen (see shouldRenderOffScreen) - and
        // vanilla
        // and Fabric draw such pieces with no view check at all. So skip it here when it is wholly behind the camera:
        // a room full of big pieces then costs nothing while the player faces away from them.
        if (isBig(decor) && behindCamera(decor)) {
            return;
        }

        super.render(decor, partialTick, poseStack, source, packedLight, packedOverlay);

        if (decor.block().design() == com.human.common.gameplay.cosmetic.CosmeticDesign.WALL_DISPLAY_ARRAY) {
            renderWallMap(decor, poseStack, source);
        } else if (decor.block().design() == com.human.common.gameplay.cosmetic.CosmeticDesign.BRIEFING_TABLE) {
            renderTableMap(decor, poseStack, source);
        } else if (decor.isHolotable() && !decor.holoMap().isEmpty()) {
            renderHologram(decor, poseStack, source);
        }

        var mounts = CosmeticModelTables.SLOTS.get(decor.block().design().id());

        if (mounts == null || !decor.hasMounts()) {
            return;
        }

        // Items on racks and shelves are small: past 32 blocks they are not drawn (the rack or shelf itself still is).
        var eye = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        if (eye.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(decor.getBlockPos())) > 32 * 32) {
            return;
        }

        var state = decor.getBlockState();
        var itemRenderer = net.minecraft.client.Minecraft.getInstance().getItemRenderer();
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(
            com.mojang.math.Axis.YP.rotationDegrees(switch (state.getValue(com.human.common.gameplay.cosmetic.CosmeticBlock.FACING)) {
                case SOUTH -> 180;
                case WEST -> 90;
                case EAST -> 270;
                default -> 0;
            })
        );

        if (state.hasProperty(com.human.common.gameplay.cosmetic.CosmeticBlock.MOUNT)) {
            var mount = state.getValue(com.human.common.gameplay.cosmetic.CosmeticBlock.MOUNT);

            if (mount != com.human.common.gameplay.cosmetic.CosmeticMount.WALL) {
                poseStack.translate(0, 0.5, 0);
                poseStack.mulPose(
                    com.mojang.math.Axis.XP.rotationDegrees(mount == com.human.common.gameplay.cosmetic.CosmeticMount.FLOOR ? 90 : -90)
                );
                poseStack.translate(0, -0.5, 0);
            }
        }

        for (int i = 0; i < mounts.length; i++) {
            var stack = decor.getItem(i);

            if (stack.isEmpty()) {
                continue;
            }

            var m = mounts[i];
            poseStack.pushPose();
            poseStack.translate(m[0] / 16F, m[1] / 16F, m[2] / 16F);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(m[3]));
            poseStack.scale(m[4], m[4], m[4]);
            itemRenderer.renderStatic(
                stack,
                net.minecraft.world.item.ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                source,
                decor.getLevel(),
                (int) decor.getBlockPos().asLong() + i
            );
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Big pieces - the repo's established fix for models reaching past their own block (OversizedRender, Oct 15)
    // ------------------------------------------------------------------------------------------------------------

    /** A piece of four or more blocks: big enough to be cut off when only its own block's section is culled. */
    private static boolean isBig(CosmeticBlockEntity decor) {
        return decor.block().design().cells().length >= 4;
    }

    /** How far, in blocks, the piece reaches from its core in any direction - every facing and mount covered. */
    private static int reach(CosmeticBlockEntity decor) {
        var reach = 0;

        for (var cell : decor.block().design().cells()) {
            for (var offset : cell) {
                reach = Math.max(reach, Math.abs(offset));
            }
        }

        return reach;
    }

    /**
     * Big pieces stay drawn while any of them is in view, not only while their core block's section is (the same
     * approach as the crafter, quarry and big crates). Small pieces keep vanilla's cheap section culling.
     */
    @Override
    public boolean shouldRenderOffScreen(@NotNull CosmeticBlockEntity decor) {
        return isBig(decor);
    }

    /** Everything the piece can draw: its cells in any facing or mount, plus a block above for a map projection. */
    @Override
    public net.minecraft.world.phys.AABB renderBox(@NotNull CosmeticBlockEntity decor) {
        var reach = reach(decor);
        var box = new net.minecraft.world.phys.AABB(decor.getBlockPos()).inflate(reach);

        return decor.isHolotable() ? box.expandTowards(0.0D, 1.0D, 0.0D) : box;
    }

    /**
     * Wholly behind the camera: everything in view lies in front of the camera's plane, so this never hides a piece.
     */
    private static boolean behindCamera(CosmeticBlockEntity decor) {
        var camera = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
        var look = camera.getLookVector();
        var centre = net.minecraft.world.phys.Vec3.atCenterOf(decor.getBlockPos());
        var to = centre.subtract(camera.getPosition());
        var radius = reach(decor) + 1.75D;

        return to.x * look.x() + to.y * look.y() + to.z * look.z() < -radius;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Map displays - [stated] Oct 31: the holotable's 3D projection, and the flat map on the wall display and the
    // briefing table. Built for rooms with MANY of them: the per-column arrays are cached on the block entity and
    // rebuilt only when the map data changes (never per frame), half the columns are drawn past 24 blocks, and nothing
    // at all past 48.
    // ------------------------------------------------------------------------------------------------------------

    /**
     * The projector surface spans model x / z -6..22 (28 px), centred on 8; the projection starts at its top (12 px).
     */
    private static final float HOLO_SPAN = 28F, HOLO_CENTRE = 8F, HOLO_BASE = 12F, HOLO_MAX = 14F;

    /** Past this distance the projection is not drawn at all; past the half distance only every other column is. */
    private static final double HOLO_DRAW_DISTANCE_SQR = 48 * 48, HOLO_HALF_DETAIL_SQR = 24 * 24;

    /** The flat maps' square (model px): 24 px across. */
    private static final float WALL_MAP_HALF = 12F, WALL_MAP_BOTTOM = 4F, WALL_MAP_SIZE = 24F, WALL_SCREEN_Z = 3.9F;

    /** Squared distance from the camera, or -1 when too far to draw. */
    private static double viewDistanceSqr(CosmeticBlockEntity decor) {
        var camera = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        var d = camera.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(decor.getBlockPos()));
        return d > HOLO_DRAW_DISTANCE_SQR ? -1 : d;
    }

    /** Rebuilds the cached arrays when the map data has changed since they were built. */
    private static void refreshCache(CosmeticBlockEntity decor) {
        if (decor.clientCacheVersion == decor.holoVersion && decor.clientShade != null) {
            return;
        }

        var n = CosmeticBlockEntity.HOLO;
        var shade = new float[n * n]; // flat maps: -1 = not drawn, 0..1 = land brightness, 2 = water
        var columns = new float[n * n]; // holotable: -1 = not drawn, else column height in px
        var map = !decor.holoMap().isEmpty();

        if (map) {
            var heights = decor.holoHeights();
            var kinds = decor.holoKinds();
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;

            for (int i = 0; i < heights.length; i++) {
                if (kinds[i] != 0 && heights[i] != CosmeticBlockEntity.UNKNOWN) {
                    min = Math.min(min, heights[i]);
                    max = Math.max(max, heights[i]);
                }
            }

            if (min == Integer.MAX_VALUE) {
                min = 0;
                max = 0;
            }

            var range = Math.max(1, max - min);

            for (int i = 0; i < shade.length; i++) {
                var h = heights[i] == CosmeticBlockEntity.UNKNOWN ? min : heights[i];
                var v = (h - min) / (float) range;
                shade[i] = kinds[i] == 0 ? -1F : kinds[i] == 2 ? 2F : v;
                columns[i] = kinds[i] == 0 ? -1F : 1F + v * (HOLO_MAX - 1F);
            }
        } else {
            if (decor.randomMap == null) {
                decor.randomMap = randomTerrain(decor.getBlockPos().asLong());
            }

            for (int i = 0; i < shade.length; i++) {
                var h = decor.randomMap[i] & 0xFF;
                shade[i] = h == 0 ? 2F : h / 255F;
                columns[i] = 1F + (h / 255F) * (HOLO_MAX - 1F);
            }
        }

        decor.clientShade = shade;
        decor.clientColumns = columns;
        decor.clientCacheVersion = decor.holoVersion;
    }

    /** Reused for every map cell (render thread only) - a fresh array per cell per frame was 4096 a display a frame. */
    private static final int[] COLOUR = new int[4];

    /**
     * The cell's colour into {@link #COLOUR}: water teal, land green brighter with height; grid lines a touch stronger.
     */
    private static int[] mapColour(float v, boolean gridLine) {
        if (v >= 2F) {
            COLOUR[0] = 15;
            COLOUR[1] = 90;
            COLOUR[2] = 85;
            COLOUR[3] = gridLine ? 140 : 110;
        } else {
            COLOUR[0] = 30;
            COLOUR[1] = 90 + (int) (v * 165);
            COLOUR[2] = 50 + (int) (v * 40);
            COLOUR[3] = gridLine ? 200 : 160;
        }

        return COLOUR;
    }

    /**
     * The holotable's projection: glowing translucent columns (vanilla's lightning render type: full-bright, additive,
     * no texture), one per sample, height relative to the map's lowest and highest points; a top face, and sides only
     * where the terrain steps down. Land bright green, water dim teal; unexplored columns are not drawn. It turns with
     * the emitter ring, about the table's centre.
     */
    private static void renderHologram(
        CosmeticBlockEntity decor,
        com.mojang.blaze3d.vertex.PoseStack poseStack,
        net.minecraft.client.renderer.MultiBufferSource source
    ) {
        var distance = viewDistanceSqr(decor);

        if (distance < 0) {
            return;
        }

        refreshCache(decor);
        var column = decor.clientColumns;
        var kinds = decor.holoKinds();
        var n = CosmeticBlockEntity.HOLO;
        var step = distance > HOLO_HALF_DETAIL_SQR ? 2 : 1;
        var state = decor.getBlockState();
        var buffer = source.getBuffer(net.minecraft.client.renderer.RenderType.lightning());
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(
            com.mojang.math.Axis.YP.rotationDegrees(switch (state.getValue(com.human.common.gameplay.cosmetic.CosmeticBlock.FACING)) {
                case SOUTH -> 180;
                case WEST -> 90;
                case EAST -> 270;
                default -> 0;
            })
        );
        poseStack.translate(HOLO_CENTRE / 16F, HOLO_BASE / 16F, HOLO_CENTRE / 16F);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(decor.spinAngle));
        var pose = poseStack.last();
        var cell = HOLO_SPAN / n / 16F * step;
        var half = HOLO_SPAN / 32F;

        for (int j = 0; j < n; j += step) {
            for (int i = 0; i < n; i += step) {
                var h = column[j * n + i];

                if (h < 0) {
                    continue;
                }

                var water = kinds[j * n + i] == 2;
                int r = water ? 20 : 40, g = water ? 150 : 255, b = water ? 140 : 90, a = water ? 70 : 110;
                float x0 = -half + i / (float) step * cell, x1 = x0 + cell, z0 = -half + j / (float) step * cell, z1 = z0 + cell, y = h
                    / 16F;

                quad(buffer, pose, x0, y, z0, x0, y, z1, x1, y, z1, x1, y, z0, r, g, b, a);

                // Sides only where the neighbour is lower (or off the map): a dimmer wall down to it.
                float west = i >= step ? Math.max(0, column[j * n + i - step]) / 16F : 0F;
                float east = i + step < n ? Math.max(0, column[j * n + i + step]) / 16F : 0F;
                float north = j >= step ? Math.max(0, column[(j - step) * n + i]) / 16F : 0F;
                float south = j + step < n ? Math.max(0, column[(j + step) * n + i]) / 16F : 0F;
                int side = a / 2;

                if (west < y)
                    quad(buffer, pose, x0, west, z0, x0, west, z1, x0, y, z1, x0, y, z0, r, g, b, side);
                if (east < y)
                    quad(buffer, pose, x1, east, z0, x1, y, z0, x1, y, z1, x1, east, z1, r, g, b, side);
                if (north < y)
                    quad(buffer, pose, x0, north, z0, x0, y, z0, x1, y, z0, x1, north, z0, r, g, b, side);
                if (south < y)
                    quad(buffer, pose, x0, south, z1, x1, south, z1, x1, y, z1, x0, y, z1, r, g, b, side);
            }
        }

        poseStack.popPose();
    }

    /**
     * The wall display's screen ([stated] Oct 31): the given map, or a random one of its own, flat on the screen face
     * (model z = 3.9 px, x -21..21 after the 3-wide centring). Read like a wall map: north on top, west on the left.
     * Dim grid side panels either side; a few blinking contacts on a random map.
     */
    private static void renderWallMap(
        CosmeticBlockEntity decor,
        com.mojang.blaze3d.vertex.PoseStack poseStack,
        net.minecraft.client.renderer.MultiBufferSource source
    ) {
        var distance = viewDistanceSqr(decor);

        if (distance < 0) {
            return;
        }

        refreshCache(decor);
        var shade = decor.clientShade;
        var n = CosmeticBlockEntity.HOLO;
        var step = distance > HOLO_HALF_DETAIL_SQR ? 2 : 1;
        var map = !decor.holoMap().isEmpty();
        var buffer = source.getBuffer(net.minecraft.client.renderer.RenderType.lightning());
        poseStack.pushPose();
        turnLikeTheModel(decor.getBlockState(), poseStack);
        var pose = poseStack.last();
        var cell = WALL_MAP_SIZE / n / 16F * step;
        var z = WALL_SCREEN_Z / 16F;

        // Viewed from the front (-Z), the viewer's left is +X: west (column 0) goes on the left, north (row 0) on top.
        for (int j = 0; j < n; j += step) {
            for (int i = 0; i < n; i += step) {
                var v = shade[j * n + i];

                if (v < 0) {
                    continue;
                }

                float x1 = (WALL_MAP_HALF - i * WALL_MAP_SIZE / n) / 16F, x0 = x1 - cell;
                float y1 = (WALL_MAP_BOTTOM + WALL_MAP_SIZE - j * WALL_MAP_SIZE / n) / 16F, y0 = y1 - cell;
                var c = mapColour(v, i % 8 == 0 || j % 8 == 0);
                quad(buffer, pose, x0, y0, z, x1, y0, z, x1, y1, z, x0, y1, z, c[0], c[1], c[2], c[3]);
            }
        }

        for (int sideSign = -1; sideSign <= 1; sideSign += 2) {
            float inner = sideSign * WALL_MAP_HALF / 16F, outer = sideSign * 20F / 16F;
            float lo = Math.min(inner, outer) + 0.03F, hi = Math.max(inner, outer) - 0.03F;

            for (int row = 0; row < 6; row++) {
                float y = (WALL_MAP_BOTTOM + 2 + row * 4) / 16F;
                quad(buffer, pose, lo, y, z, hi, y, z, hi, y + 0.02F, z, lo, y + 0.02F, z, 40, 160, 70, 90);
            }
        }

        if (!map) {
            var time = decor.getLevel() == null ? 0 : decor.getLevel().getGameTime();
            // Contact positions from a hash of the display's position - stable, and nothing allocated per frame.
            var seed = decor.getBlockPos().asLong() * 31L;

            for (int k = 0; k < 5; k++) {
                seed = seed * 6364136223846793005L + 1442695040888963407L;
                var fx = ((seed >>> 40) & 0xFFFF) / 65535F;
                seed = seed * 6364136223846793005L + 1442695040888963407L;
                var fy = ((seed >>> 40) & 0xFFFF) / 65535F;
                float cx = (WALL_MAP_HALF - 2 - fx * (WALL_MAP_SIZE - 4)) / 16F;
                float cy = (WALL_MAP_BOTTOM + 2 + fy * (WALL_MAP_SIZE - 4)) / 16F;

                if (((time / 10) + k) % 3 == 0) {
                    continue;
                }

                float d = 0.04F;
                int red = k % 2 == 0 ? 255 : 230, green = k % 2 == 0 ? 60 : 190;
                quad(
                    buffer,
                    pose,
                    cx - d,
                    cy - d,
                    z - 0.002F,
                    cx + d,
                    cy - d,
                    z - 0.002F,
                    cx + d,
                    cy + d,
                    z - 0.002F,
                    cx - d,
                    cy + d,
                    z - 0.002F,
                    red,
                    green,
                    40,
                    230
                );
            }
        }

        poseStack.popPose();
    }

    /**
     * The briefing table's screen top ([stated] Oct 31): the same map as the wall display, lying flat at y = 13.1 px
     * over the 24 px square centred on z = 8. Read from the front like a map on a table: north at the far side (+Z),
     * west on the left (+X).
     */
    private static void renderTableMap(
        CosmeticBlockEntity decor,
        com.mojang.blaze3d.vertex.PoseStack poseStack,
        net.minecraft.client.renderer.MultiBufferSource source
    ) {
        var distance = viewDistanceSqr(decor);

        if (distance < 0) {
            return;
        }

        refreshCache(decor);
        var shade = decor.clientShade;
        var n = CosmeticBlockEntity.HOLO;
        var step = distance > HOLO_HALF_DETAIL_SQR ? 2 : 1;
        var buffer = source.getBuffer(net.minecraft.client.renderer.RenderType.lightning());
        poseStack.pushPose();
        turnLikeTheModel(decor.getBlockState(), poseStack);
        var pose = poseStack.last();
        var cell = WALL_MAP_SIZE / n / 16F * step;
        var y = 13.1F / 16F;

        for (int j = 0; j < n; j += step) {
            for (int i = 0; i < n; i += step) {
                var v = shade[j * n + i];

                if (v < 0) {
                    continue;
                }

                float x1 = (12F - i * WALL_MAP_SIZE / n) / 16F, x0 = x1 - cell;
                float z1 = (20F - j * WALL_MAP_SIZE / n) / 16F, z0 = z1 - cell;
                var c = mapColour(v, i % 8 == 0 || j % 8 == 0);
                quad(buffer, pose, x0, y, z0, x0, y, z1, x1, y, z1, x1, y, z0, c[0], c[1], c[2], c[3]);
            }
        }

        poseStack.popPose();
    }

    /** The same turns the model gets: facing about Y, then a floor/ceiling mount's X turn about the block middle. */
    private static void turnLikeTheModel(
        net.minecraft.world.level.block.state.BlockState state,
        com.mojang.blaze3d.vertex.PoseStack poseStack
    ) {
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(
            com.mojang.math.Axis.YP.rotationDegrees(switch (state.getValue(com.human.common.gameplay.cosmetic.CosmeticBlock.FACING)) {
                case SOUTH -> 180;
                case WEST -> 90;
                case EAST -> 270;
                default -> 0;
            })
        );

        if (state.hasProperty(com.human.common.gameplay.cosmetic.CosmeticBlock.MOUNT)) {
            var mount = state.getValue(com.human.common.gameplay.cosmetic.CosmeticBlock.MOUNT);

            if (mount != com.human.common.gameplay.cosmetic.CosmeticMount.WALL) {
                poseStack.translate(0, 0.5, 0);
                poseStack.mulPose(
                    com.mojang.math.Axis.XP.rotationDegrees(mount == com.human.common.gameplay.cosmetic.CosmeticMount.FLOOR ? 90 : -90)
                );
                poseStack.translate(0, -0.5, 0);
            }
        }
    }

    /** A random but stable tactical map: smoothed value noise, the lowest ground flooded as water (height 0). */
    private static byte[] randomTerrain(long seed) {
        var n = CosmeticBlockEntity.HOLO;
        var random = new java.util.Random(seed);
        var grid = 9;
        var corners = new float[grid * grid];

        for (int i = 0; i < corners.length; i++) {
            corners[i] = random.nextFloat();
        }

        var out = new byte[n * n];

        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                float gx = i * (grid - 1) / (float) n, gy = j * (grid - 1) / (float) n;
                int x0 = (int) gx, y0 = (int) gy;
                float fx = gx - x0, fy = gy - y0;
                fx = fx * fx * (3 - 2 * fx);
                fy = fy * fy * (3 - 2 * fy);
                float top = corners[y0 * grid + x0] * (1 - fx) + corners[y0 * grid + x0 + 1] * fx;
                float bottom = corners[(y0 + 1) * grid + x0] * (1 - fx) + corners[(y0 + 1) * grid + x0 + 1] * fx;
                var h = top * (1 - fy) + bottom * fy;
                out[j * n + i] = h < 0.35F ? 0 : (byte) Math.max(1, Math.min(255, (int) ((h - 0.35F) / 0.65F * 255)));
            }
        }

        return out;
    }

    /** One quad, emitted in both windings so it shows from either side whatever the render type culls. */
    private static void quad(
        com.mojang.blaze3d.vertex.VertexConsumer buffer,
        com.mojang.blaze3d.vertex.PoseStack.Pose pose,
        float ax,
        float ay,
        float az,
        float bx,
        float by,
        float bz,
        float cx,
        float cy,
        float cz,
        float dx,
        float dy,
        float dz,
        int r,
        int g,
        int b,
        int a
    ) {
        buffer.addVertex(pose, ax, ay, az).setColor(r, g, b, a);
        buffer.addVertex(pose, bx, by, bz).setColor(r, g, b, a);
        buffer.addVertex(pose, cx, cy, cz).setColor(r, g, b, a);
        buffer.addVertex(pose, dx, dy, dz).setColor(r, g, b, a);
        buffer.addVertex(pose, dx, dy, dz).setColor(r, g, b, a);
        buffer.addVertex(pose, cx, cy, cz).setColor(r, g, b, a);
        buffer.addVertex(pose, bx, by, bz).setColor(r, g, b, a);
        buffer.addVertex(pose, ax, ay, az).setColor(r, g, b, a);
    }

    private static ResourceLocation texture(CosmeticBlockEntity decor) {
        return HumanResources.blockTextureLocation("cosmetic/" + name(decor));
    }

    private static RenderType renderType(@Nullable CosmeticBlockEntity decor, AzBone bone) {
        if (decor == null) {
            return RenderType.entityCutout(HumanResources.blockTextureLocation("cosmetic/cosmetic_crafter"));
        }

        var texture = texture(decor);
        var glass = bone.getName().startsWith(GLASS_BONE) && decor.block().design().translucent();

        return glass ? RenderType.entityTranslucent(texture) : RenderType.entityCutout(texture);
    }

    private static boolean hide(AzBone bone, CosmeticBlockEntity decor) {
        var name = name(decor);
        var boneName = bone.getName();

        if (CosmeticModelTables.HIDDEN.getOrDefault(name, Set.of()).contains(boneName)) {
            return true;
        }

        // A shelf spot holding an item hides that spot's decoration ([stated] Oct 31: the item replaces it).
        var spots = CosmeticModelTables.SLOT_BONES.get(decor.block().design().id());

        if (spots != null) {
            for (int i = 0; i < spots.length; i++) {
                if (!decor.getItem(i).isEmpty()) {
                    for (var spotBone : spots[i]) {
                        if (spotBone.equals(boneName)) {
                            return true;
                        }
                    }
                }
            }
        }

        List<String> contents = CosmeticModelTables.CONTENTS.get(name);

        if (contents == null || !contents.contains(boneName)) {
            return false;
        }

        var state = decor.getBlockState();

        // Only the chosen interior shows; before the state is known, the default (first) one.
        var shown = state.hasProperty(CosmeticSpecimenBlock.CONTENTS)
            ? state.getValue(CosmeticSpecimenBlock.CONTENTS).bone()
            : contents.getFirst();

        return !boneName.equals(shown);
    }

    private static String name(CosmeticBlockEntity decor) {
        return BuiltInRegistries.BLOCK.getKey(decor.getBlockState().getBlock()).getPath();
    }
}
