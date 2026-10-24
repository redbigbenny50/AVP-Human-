package com.human.client.render.block;

import com.blib.api.client.render.v1.block.AzBlockEntityRenderer;
import com.blib.api.client.render.v1.block.AzBlockEntityRendererConfig;
import com.blib.api.client.render.v1.layer.AzAutoGlowingLayer;
import com.human.HumanResources;
import com.human.common.gameplay.quarry.QuarryEdition;
import com.human.common.gameplay.quarry.QuarryTerminalBlock;
import com.human.common.gameplay.quarry.QuarryTerminalBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

/**
 * The quarry terminal on its core block, in its edition's skin and showing its screen state ([stated] Oct 13):
 * {@code _off} blank, the plain texture powered, {@code _working} animated - EIGHT SEPARATE FRAME TEXTURES
 * ({@code _working_0} to {@code _working_7}), stepped here by game time, 3 ticks a frame. Each has its own
 * {@code _glowmask}; the off one holds a single invisible marker pixel (BLib refuses an empty glowmask).
 * <p>
 * ⚠ Oct 16 fix - this WAS one 8-frame strip with a {@code .mcmeta}, left to the texture system to animate. In play the
 * WORKING screen showed the whole strip squeezed onto the model (every face mapped to an eighth of its height), so the
 * terminal looked squashed and rearranged whenever it worked: an animated-texture wrapper other than BLib's (GeckoLib's
 * is installed alongside and wraps the same lookup) is never advanced or cropped by BLib's renderer. Plain single
 * frames chosen by the renderer depend on no animated-texture support at all, from any mod.
 * </p>
 * <p>
 * BLib turns the model to the block's FACING itself (north 0, west 90, south 180, east 270 about +Y), so the model's
 * front (-Z) faces the way the block faces.
 * </p>
 */
public class QuarryTerminalRenderer extends AzBlockEntityRenderer<QuarryTerminalBlockEntity> implements com.human.client.render.OversizedRender<QuarryTerminalBlockEntity> {

    /** The drill can be a hundred blocks down and three chunks away - draw it from well beyond a chest's 64 blocks. */
    private static final int VIEW_DISTANCE = 256;

    /** Half the carriage's width with its arm sockets (66 px), in blocks: where an arm meets the collar. */
    private static final double SOCKET = 66.0D / 16.0D;

    /** Half a truck's body (7 px), in blocks. */
    private static final double TRUCK_HALF = 7.0D / 16.0D;

    // The moving parts, each its own model in the terminal's edition.
    /** The cutter (10x10 + 12x12 bit) lowers and spins; the housing and 8x8 stay still ([stated] Oct 5). */
    private final QuarryPartRenderer head = new QuarryPartRenderer("quarry_drill_head", true, (terminal, model) -> {
        var cutter = model.getBoneOrNull("cutter");

        if (cutter != null) {
            cutter.setPosY((float) (-terminal.shownDepth() * 16.0D));
            cutter.setRotY(this.spin);
        }
    });

    /** Which shaft segment is being drawn, so the joiner ring shows on every third one only ([stated] Oct 6). */
    private int segment;

    /** This frame's cutter spin, shared by the cutter and the shaft. */
    private float spin;

    private final QuarryPartRenderer shaft = new QuarryPartRenderer("quarry_drill_shaft", false, (terminal, model) -> {
        var body = model.getBoneOrNull("shaft");
        var ring = model.getBoneOrNull("joiner_ring");

        if (body != null) {
            body.setRotY(this.spin);
        }

        if (ring != null) {
            ring.setRotY(this.spin);
            ring.setHidden(this.segment % 3 != 2);
        }
    });

    /** The housing bottom (top of the shaft run) and the cutter's top, in pixels above the head's origin. */
    private static final double HOUSING_BOTTOM_PX = 32.0D;

    private static final double SEGMENT_PX = 64.0D;

    private final QuarryPartRenderer carriage = new QuarryPartRenderer("quarry_gantry_carriage", false);

    private final QuarryPartRenderer arm = new QuarryPartRenderer("quarry_gantry_arm", false);

    private final QuarryPartRenderer truck = new QuarryPartRenderer("quarry_gantry_truck", true);

    private static final ResourceLocation MODEL = HumanResources.blockGeoModelLocation("quarry_terminal");

    private static final Map<QuarryEdition, Map<QuarryTerminalBlockEntity.Screen, ResourceLocation>> TEXTURES = new EnumMap<>(
        QuarryEdition.class
    );

    static {
        for (var edition : QuarryEdition.values()) {
            var byScreen = new EnumMap<QuarryTerminalBlockEntity.Screen, ResourceLocation>(QuarryTerminalBlockEntity.Screen.class);

            for (var screen : QuarryTerminalBlockEntity.Screen.values()) {
                var suffix = screen == QuarryTerminalBlockEntity.Screen.ON ? "" : "_" + screen.key();

                byScreen.put(screen, HumanResources.blockTextureLocation("quarry/quarry_terminal_" + edition.textureKey() + suffix));
            }

            TEXTURES.put(edition, byScreen);
        }
    }

    public QuarryTerminalRenderer() {
        super(
            AzBlockEntityRendererConfig.<QuarryTerminalBlockEntity>builder($ -> MODEL, QuarryTerminalRenderer::textureFor)
                .addRenderLayer(new AzAutoGlowingLayer<>())
                .addRenderLayer(new EmitterPulseLayer())
                .build()
        );
    }

    /** The emitter lens on its own, per edition: the glowmask's lens pixels lifted toward white, nothing else. */
    private static final Map<QuarryEdition, ResourceLocation> EMITTER = new EnumMap<>(QuarryEdition.class);

    static {
        for (var edition : QuarryEdition.values()) {
            EMITTER.put(edition, HumanResources.blockTextureLocation("quarry/quarry_terminal_" + edition.textureKey() + "_emitter_pulse"));
        }
    }

    /**
     * The atomizer PULSE: while the atomizer fires, the blue emitter on the terminal's back flares with each shot and
     * fades between them. A second, additive glow pass of the emitter lens alone (the screen glow is untouched), its
     * brightness from {@link QuarryTerminalBlockEntity#emitterPulse}. When there is no pulse it returns no render type,
     * so the pass is skipped entirely - it costs nothing while the atomizer is quiet.
     */
    private static final class EmitterPulseLayer extends AzAutoGlowingLayer<Long, QuarryTerminalBlockEntity> {

        @Override
        protected net.minecraft.client.renderer.RenderType determineRenderType(
            com.blib.api.client.render.v1.AzRendererPipelineContext<Long, QuarryTerminalBlockEntity> context
        ) {
            var terminal = context.animatable();

            if (terminal.emitterPulse(context.partialTick()) <= 0.01F) {
                return null;
            }

            return net.minecraft.client.renderer.RenderType.eyes(EMITTER.get(terminal.edition()));
        }

        @Override
        protected int getGlowColor(com.blib.api.client.render.v1.AzRendererPipelineContext<Long, QuarryTerminalBlockEntity> context) {
            var pulse = context.animatable().emitterPulse(context.partialTick());
            var level = Math.round(255 * Math.clamp(pulse, 0.0F, 1.0F));

            // Additive: scaling the colour IS the brightness.
            return 0xFF000000 | level << 16 | level << 8 | level;
        }
    }

    /**
     * The block entity is on the BACK_TOP block, but the model's origin is the CORE (front, bottom) - one block toward
     * the front and one down. Moved here, before BLib turns the model to the facing.
     */
    @Override
    public void render(
        @NotNull QuarryTerminalBlockEntity terminal,
        float partialTick,
        @NotNull PoseStack poseStack,
        @NotNull MultiBufferSource buffers,
        int packedLight,
        int packedOverlay
    ) {
        var state = terminal.getBlockState();

        if (!state.hasProperty(QuarryTerminalBlock.FACING)) {
            return;
        }

        var facing = state.getValue(QuarryTerminalBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(facing.getStepX(), -1.0D, facing.getStepZ());
        super.render(terminal, partialTick, poseStack, buffers, packedLight, packedOverlay);
        poseStack.popPose();

        if (terminal.frameState() == QuarryTerminalBlockEntity.FrameState.BUILT) {
            renderGantry(terminal, partialTick, poseStack, buffers, packedOverlay, facing);
        }
    }

    /**
     * The drill head on its carriage, and the X and Z arms running from the carriage to trucks on the top frame ring
     * ([stated] Oct 5: "X and Z arms extend from the top frame like a 3D printer with the head centred on them").
     */
    private void renderGantry(
        QuarryTerminalBlockEntity terminal,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffers,
        int packedOverlay,
        net.minecraft.core.Direction facing
    ) {
        var chunks = terminal.areaChunksForRender();

        if (chunks.isEmpty()) {
            return;
        }

        var bounds = com.human.common.gameplay.quarry.QuarryArea.bounds(chunks);
        double x0 = bounds[0] - 0.5D, z0 = bounds[1] - 0.5D, x1 = bounds[2] + 1.5D, z1 = bounds[3] + 1.5D;
        terminal.animate(partialTick);
        spin = terminal.cutterSpin(partialTick);

        var headAt = terminal.shownHead();
        var gantryY = terminal.startY() + com.human.common.gameplay.quarry.QuarryArea.LEG_HEIGHT + 2.0D;
        var hx = headAt.x;
        var hz = headAt.z;

        part(head, terminal, partialTick, poseStack, buffers, packedOverlay, facing, headAt, 0.0F);

        // The shaft: 4-block segments stacked up from the cutter's top into the housing, as many as the depth needs.
        var depthPx = terminal.shownDepth() * 16.0D;
        var cutterTop = HOUSING_BOTTOM_PX - depthPx;

        segment = 0;

        for (double bottom = cutterTop; bottom < HOUSING_BOTTOM_PX; bottom += SEGMENT_PX) {
            part(shaft, terminal, partialTick, poseStack, buffers, packedOverlay, facing, headAt.add(0.0D, bottom / 16.0D, 0.0D), 0.0F);
            segment++;
        }

        effects(terminal, headAt, terminal.shownDepth());

        // [stated] Oct 19 - the drill's two sounds, one at the bit and one at the head (each moves itself along with
        // its part from here on).
        com.human.client.sound.QuarryDrillSound.ensure(terminal);
        part(
            carriage,
            terminal,
            partialTick,
            poseStack,
            buffers,
            packedOverlay,
            facing,
            new net.minecraft.world.phys.Vec3(hx, gantryY, hz),
            0.0F
        );

        // X arm: trucks on the two ring sides that run along Z, segments from each truck to the collar.
        part(
            truck,
            terminal,
            partialTick,
            poseStack,
            buffers,
            packedOverlay,
            facing,
            new net.minecraft.world.phys.Vec3(x0, gantryY, hz),
            0.0F
        );
        part(
            truck,
            terminal,
            partialTick,
            poseStack,
            buffers,
            packedOverlay,
            facing,
            new net.minecraft.world.phys.Vec3(x1, gantryY, hz),
            0.0F
        );
        armRun(terminal, partialTick, poseStack, buffers, packedOverlay, facing, x0 + TRUCK_HALF, hx - SOCKET, gantryY, hz, true);
        armRun(terminal, partialTick, poseStack, buffers, packedOverlay, facing, x1 - TRUCK_HALF, hx + SOCKET, gantryY, hz, true);

        // Z arm: the same pieces turned a quarter, trucks on the sides that run along X.
        part(
            truck,
            terminal,
            partialTick,
            poseStack,
            buffers,
            packedOverlay,
            facing,
            new net.minecraft.world.phys.Vec3(hx, gantryY, z0),
            90.0F
        );
        part(
            truck,
            terminal,
            partialTick,
            poseStack,
            buffers,
            packedOverlay,
            facing,
            new net.minecraft.world.phys.Vec3(hx, gantryY, z1),
            90.0F
        );
        armRun(terminal, partialTick, poseStack, buffers, packedOverlay, facing, z0 + TRUCK_HALF, hz - SOCKET, gantryY, hx, false);
        armRun(terminal, partialTick, poseStack, buffers, packedOverlay, facing, z1 - TRUCK_HALF, hz + SOCKET, gantryY, hx, false);
    }

    /** How far away the digging effects are drawn, in blocks. */
    private static final double EFFECT_RANGE = 64.0D;

    /** Half the bit's width (12 x 12), in blocks. */
    private static final double BIT_HALF = 6.0D;

    /** Ticks between the big "a block just broke" bursts (with its break sound). */
    private static final int BURST_TICKS = 8;

    /**
     * Smoke from the housing's top vents, and the digging itself at the bit ([stated] Oct 15: "we want the digging
     * action to look impressive"). Client only - no packets - once per game tick, only within {@link #EFFECT_RANGE} and
     * only while the screen says WORKING.
     * <p>
     * ⚠ VANILLA DROPS ANY ORDINARY PARTICLE MORE THAN 32 BLOCKS FROM THE CAMERA (LevelRenderer.addParticleInternal). A
     * player at the top of a 50-block pit would have seen nothing at the bit. These go through the always-visible path,
     * which skips that cut - and also skips the player's PARTICLES setting, so that setting is applied here instead:
     * ALL = everything, DECREASED = half, MINIMAL = only the occasional burst.
     * </p>
     * Per tick at ALL: 24 rim debris + 4 under the face + 2 dust puffs + 1 spark, and a 10-particle burst with the
     * block's break sound every {@link #BURST_TICKS} ticks - about 32 a tick, well under vanilla's particle cap.
     */
    private static void effects(QuarryTerminalBlockEntity terminal, net.minecraft.world.phys.Vec3 headAt, double depth) {
        var level = terminal.getLevel();
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        var camera = minecraft.gameRenderer.getMainCamera().getPosition();

        if (level == null || terminal.screen() != QuarryTerminalBlockEntity.Screen.WORKING) {
            return;
        }

        var bitY = headAt.y - depth;

        if (
            !terminal.claimEffectTick(level.getGameTime()) || camera.distanceToSqr(headAt.x, bitY, headAt.z) > EFFECT_RANGE * EFFECT_RANGE
        ) {
            return;
        }

        var setting = minecraft.options.particles().get();
        var all = setting == net.minecraft.client.ParticleStatus.ALL;
        var minimal = setting == net.minecraft.client.ParticleStatus.MINIMAL;
        var random = level.getRandom();

        if (!minimal) {
            // Two of the eight top vents (pivots at +-29 / +-13 px, 6 blocks up) puff each tick ([stated] Oct 5).
            for (int puff = 0; puff < (all ? 2 : 1); puff++) {
                var vent = random.nextInt(8);
                var offset = vent < 4 ? 29.0D / 16.0D : 13.0D / 16.0D;
                double vx = vent < 4 ? (vent == 0 ? -offset : vent == 1 ? offset : 0.0D) : (vent % 2 == 0 ? -offset : offset);
                double vz = vent < 4 ? (vent == 2 ? offset : vent == 3 ? -offset : 0.0D) : (vent < 6 ? offset : -offset);

                level.addAlwaysVisibleParticle(
                    net.minecraft.core.particles.ParticleTypes.SMOKE,
                    true,
                    headAt.x + vx,
                    headAt.y + 6.05D,
                    headAt.z + vz,
                    0.0D,
                    0.06D,
                    0.0D
                );
            }

            // RIM: the whole 12 x 12 edge every tick - three points on each side, each in its own third of the side,
            // so the ring is always covered rather than a few random spots. Debris thrown outward and up.
            var perSide = all ? 3 : 2;

            for (int side = 0; side < 4; side++) {
                for (int third = 0; third < perSide; third++) {
                    var along = -BIT_HALF + (third + random.nextDouble()) * (2.0D * BIT_HALF / perSide);
                    var ex = side < 2 ? (side == 0 ? -BIT_HALF : BIT_HALF) : along;
                    var ez = side < 2 ? along : (side == 2 ? -BIT_HALF : BIT_HALF);

                    debris(level, random, headAt.x + ex, bitY, headAt.z + ez, side, 2, 0.15D);
                }
            }

            // FACE: chips kicked up from under the bit, where the teeth are cutting.
            for (int chip = 0; chip < (all ? 4 : 2); chip++) {
                var fx = headAt.x + (random.nextDouble() - 0.5D) * 2.0D * BIT_HALF;
                var fz = headAt.z + (random.nextDouble() - 0.5D) * 2.0D * BIT_HALF;

                debris(level, random, fx, bitY, fz, -1, 1, 0.05D);
            }

            // DUST: big slow puffs in the colour of the rock being cut, rising off the rim.
            for (int puff = 0; puff < (all ? 2 : 1); puff++) {
                var side = random.nextInt(4);
                var along = (random.nextDouble() - 0.5D) * 2.0D * BIT_HALF;
                var dx = headAt.x + (side < 2 ? (side == 0 ? -BIT_HALF : BIT_HALF) : along);
                var dz = headAt.z + (side < 2 ? along : (side == 2 ? -BIT_HALF : BIT_HALF));
                var state = cutState(level, dx, bitY, dz);

                if (state != null) {
                    var colour = state.getMapColor(level, net.minecraft.core.BlockPos.containing(dx, bitY - 0.5D, dz)).col;
                    var dust = new net.minecraft.core.particles.DustParticleOptions(
                        new org.joml.Vector3f(((colour >> 16) & 0xFF) / 255.0F, ((colour >> 8) & 0xFF) / 255.0F, (colour & 0xFF) / 255.0F),
                        2.5F
                    );

                    level.addAlwaysVisibleParticle(dust, true, dx, bitY + 0.3D, dz, 0.0D, 0.04D, 0.0D);
                }
            }

            // SPARK: titanium teeth on rock.
            if (all) {
                var side = random.nextInt(4);
                var along = (random.nextDouble() - 0.5D) * 2.0D * BIT_HALF;

                level.addAlwaysVisibleParticle(
                    net.minecraft.core.particles.ParticleTypes.CRIT,
                    true,
                    headAt.x + (side < 2 ? (side == 0 ? -BIT_HALF : BIT_HALF) : along),
                    bitY + 0.05D,
                    headAt.z + (side < 2 ? along : (side == 2 ? -BIT_HALF : BIT_HALF)),
                    (random.nextDouble() - 0.5D) * 0.4D,
                    0.2D,
                    (random.nextDouble() - 0.5D) * 0.4D
                );
            }
        }

        // BURST: every few ticks a block "breaks" somewhere on the rim - a spray of its pieces and its own break
        // sound. MINIMAL keeps only these, at a quarter of the rate.
        if (level.getGameTime() % (minimal ? BURST_TICKS * 4 : BURST_TICKS) == 0) {
            var side = random.nextInt(4);
            var along = (random.nextDouble() - 0.5D) * 2.0D * BIT_HALF;
            var bx = headAt.x + (side < 2 ? (side == 0 ? -BIT_HALF : BIT_HALF) : along);
            var bz = headAt.z + (side < 2 ? along : (side == 2 ? -BIT_HALF : BIT_HALF));
            var state = cutState(level, bx, bitY, bz);

            if (state != null) {
                debris(level, random, bx, bitY, bz, side, minimal ? 4 : 10, 0.3D);

                var sound = state.getSoundType();

                level.playLocalSound(
                    bx,
                    bitY,
                    bz,
                    sound.getBreakSound(),
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    0.5F,
                    sound.getPitch() * 0.8F,
                    false
                );
            }
        }
    }

    /**
     * The block the bit is cutting at a spot: the layer it is on, or - where that is already dug - the one under it.
     * Null for air and liquids, which throw nothing.
     */
    private static net.minecraft.world.level.block.state.@org.jetbrains.annotations.Nullable BlockState cutState(
        net.minecraft.world.level.Level level,
        double x,
        double bitY,
        double z
    ) {
        var cut = net.minecraft.core.BlockPos.containing(x, bitY - 0.5D, z);
        var state = level.getBlockState(cut);

        if (state.isAir()) {
            state = level.getBlockState(cut.below());
        }

        return state.isAir() || !state.getFluidState().isEmpty() ? null : state;
    }

    /**
     * Pieces of the block at a spot, flying up and - on a rim side (0 = -X, 1 = +X, 2 = -Z, 3 = +Z) - out from the bit.
     */
    private static void debris(
        net.minecraft.world.level.Level level,
        net.minecraft.util.RandomSource random,
        double x,
        double bitY,
        double z,
        int side,
        int count,
        double throwOut
    ) {
        var state = cutState(level, x, bitY, z);

        if (state == null) {
            return;
        }

        var pieces = new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, state);
        var outX = side == 0 ? -throwOut : side == 1 ? throwOut : 0.0D;
        var outZ = side == 2 ? -throwOut : side == 3 ? throwOut : 0.0D;

        for (int piece = 0; piece < count; piece++) {
            level.addAlwaysVisibleParticle(
                pieces,
                true,
                x + (random.nextDouble() - 0.5D) * 0.6D,
                bitY + 0.1D,
                z + (random.nextDouble() - 0.5D) * 0.6D,
                outX + (random.nextDouble() - 0.5D) * 0.12D,
                0.12D + random.nextDouble() * 0.15D,
                outZ + (random.nextDouble() - 0.5D) * 0.12D
            );
        }
    }

    /** One-block arm segments from {@code from} toward {@code to} along X or Z; the last may tuck into the collar. */
    private void armRun(
        QuarryTerminalBlockEntity terminal,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffers,
        int packedOverlay,
        net.minecraft.core.Direction facing,
        double from,
        double to,
        double y,
        double across,
        boolean alongX
    ) {
        var step = to > from ? 1.0D : -1.0D;

        for (double at = from; step > 0 ? at < to : at > to; at += step) {
            var centre = at + step * 0.5D;
            var point = alongX
                ? new net.minecraft.world.phys.Vec3(centre, y, across)
                : new net.minecraft.world.phys.Vec3(across, y, centre);

            part(arm, terminal, partialTick, poseStack, buffers, packedOverlay, facing, point, alongX ? 0.0F : 90.0F);
        }
    }

    /**
     * Draws a part with its model origin at {@code point} (world coordinates), turned {@code yaw} degrees, lit by the
     * light where it actually is. BLib moves every block-entity model to the block centre and turns it to the facing;
     * that turn is undone first so the part keeps its own world orientation.
     */
    private void part(
        QuarryPartRenderer renderer,
        QuarryTerminalBlockEntity terminal,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffers,
        int packedOverlay,
        net.minecraft.core.Direction facing,
        net.minecraft.world.phys.Vec3 point,
        float yaw
    ) {
        var level = terminal.getLevel();

        if (level == null) {
            return;
        }

        var origin = terminal.getBlockPos();

        poseStack.pushPose();
        poseStack.translate(point.x - origin.getX(), point.y - origin.getY(), point.z - origin.getZ());
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw - blibTurn(facing)));
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        renderer.render(
            terminal,
            partialTick,
            poseStack,
            buffers,
            lightAround(level, point),
            packedOverlay
        );
        poseStack.popPose();
    }

    /**
     * ⚠ Oct 17 FIX - [stated] the drill head and the cutter were drawn BLACK. Each part took its light from the one
     * block its origin sits in - and that block is usually solid: the cutter's origin is the layer it is cutting, the
     * head's often sits in the frame or in leaves. Inside a solid block the light is 0, so the whole part went black,
     * while the shaft between them (in the open pit) was lit fine.
     * <p>
     * Now each part takes the BRIGHTEST light around its origin - the block itself, the one above, the one below and
     * the four beside it - sky and block light each at their highest, which is what an object standing there would
     * show. Seven light reads per part; a quarry draws about a dozen parts.
     * </p>
     */
    private static int lightAround(net.minecraft.world.level.Level level, net.minecraft.world.phys.Vec3 point) {
        var centre = net.minecraft.core.BlockPos.containing(point);
        var sky = 0;
        var block = 0;

        for (
            var at : new net.minecraft.core.BlockPos[] {
                centre,
                centre.above(),
                centre.below(),
                centre.north(),
                centre.south(),
                centre.east(),
                centre.west()
            }
        ) {
            var light = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, at);

            sky = Math.max(sky, net.minecraft.client.renderer.LightTexture.sky(light));
            block = Math.max(block, net.minecraft.client.renderer.LightTexture.block(light));
        }

        return net.minecraft.client.renderer.LightTexture.pack(block, sky);
    }

    /** BLib's own turn for a facing (AzBlockEntityModelRenderer.rotateBlock): north 0, west 90, south 180, east 270. */
    private static float blibTurn(net.minecraft.core.Direction facing) {
        return switch (facing) {
            case WEST -> 90.0F;
            case SOUTH -> 180.0F;
            case EAST -> 270.0F;
            default -> 0.0F;
        };
    }

    /**
     * Everything the quarry can draw: the terminal, and the frame from the top of the gantry to the bottom of the dig.
     */
    @Override
    public net.minecraft.world.phys.AABB renderBox(QuarryTerminalBlockEntity terminal) {
        var box = new net.minecraft.world.phys.AABB(terminal.getBlockPos()).inflate(3.0D);
        var chunks = terminal.areaChunksForRender();

        if (terminal.frameState() == QuarryTerminalBlockEntity.FrameState.NONE || chunks.isEmpty()) {
            return box;
        }

        var bounds = com.human.common.gameplay.quarry.QuarryArea.bounds(chunks);

        return box.minmax(
            new net.minecraft.world.phys.AABB(
                bounds[0] - 1,
                terminal.bottomY() - 1,
                bounds[1] - 1,
                bounds[2] + 2,
                terminal.startY() + com.human.common.gameplay.quarry.QuarryArea.LEG_HEIGHT + 4,
                bounds[3] + 2
            )
        );
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }

    /** The model is three blocks wide and two deep - never cull it just because its brain block is off screen. */
    @Override
    public boolean shouldRenderOffScreen(@NotNull QuarryTerminalBlockEntity terminal) {
        return true;
    }

    public static ResourceLocation textureFor(QuarryTerminalBlockEntity terminal) {
        if (terminal.screen() == QuarryTerminalBlockEntity.Screen.WORKING && terminal.getLevel() != null) {
            return WORKING_FRAMES.get(terminal.edition())[(int) (terminal.getLevel().getGameTime() / WORKING_FRAME_TICKS
                % WORKING_FRAMES_COUNT)];
        }

        return TEXTURES.get(terminal.edition()).get(terminal.screen());
    }

    private static final int WORKING_FRAMES_COUNT = 8;

    private static final int WORKING_FRAME_TICKS = 3;

    private static final Map<QuarryEdition, ResourceLocation[]> WORKING_FRAMES = new EnumMap<>(QuarryEdition.class);

    static {
        for (var edition : QuarryEdition.values()) {
            var frames = new ResourceLocation[WORKING_FRAMES_COUNT];

            for (int frame = 0; frame < WORKING_FRAMES_COUNT; frame++) {
                frames[frame] = HumanResources.blockTextureLocation("quarry/quarry_terminal_" + edition.textureKey() + "_working_" + frame);
            }

            WORKING_FRAMES.put(edition, frames);
        }
    }

    /** The powered (plain) skin of an edition - what the item shows. */
    public static ResourceLocation itemTexture(QuarryEdition edition) {
        return TEXTURES.get(edition).get(QuarryTerminalBlockEntity.Screen.ON);
    }
}
