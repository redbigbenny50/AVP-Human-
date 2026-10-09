package com.human.client.render.block;

import com.blib.api.client.render.v1.block.AzBlockEntityRenderer;
import com.blib.api.client.render.v1.block.AzBlockEntityRendererConfig;
import com.blib.api.client.render.v1.layer.AzAutoGlowingLayer;
import com.human.HumanResources;
import com.human.client.animation.block.HydroelectricGeneratorAnimator;
import com.human.common.gameplay.block.entity.power.impl.HydroelectricGeneratorBlockEntity;
import com.human.common.gameplay.block.power.HydroelectricGeneratorBlock;
import com.human.common.gameplay.block.power.PowerMode;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * A wheel section: the touching side plates disappear so neighbours read as one long wheel, and every section of every
 * wheel turns in step.
 * <h2>Why the spin is computed, not played</h2> ⭐ [stated] "when you attach a new wheel it syncs up with the others as
 * one long wheel." A looping clip starts when its section starts playing it, so sections placed at different moments
 * would run out of phase forever. The angle here comes from WORLD TIME instead, so every section is at the same angle
 * by construction, a newly placed one is instantly in step, and the 25/50/100% speeds are exact. The clips ship with
 * the model and remain the source of the geometry (one full turn per second at full speed, {@code heg.forwardspin} /
 * {@code heg.backspin} / {@code heg.hold}).
 * <h2>The side plates</h2> [stated] "the sides can be turned off and on so say i place a second wheel next to the first
 * it would turn off the left side and right side of the respective models where they touch." {@code gPanelLeft} and
 * {@code gPanelRight} are separate bones in the model, so this is a visibility flip per side, decided from the two
 * neighbours along the axle.
 */
public class HydroelectricGeneratorRenderer extends AzBlockEntityRenderer<HydroelectricGeneratorBlockEntity> {

    public static final String NAME = "hydroelectric_generator";

    private static final String ROOT_BONE = "root";

    /**
     * ⚠ ZERO - the authored model is already centred on its block, and moving it broke that.
     * <p>
     * [stated] Sep 25: "the center of the middle block isnt lined up with the center anymore i think you need to raise
     * it by half a block." Dropping it 8px put the bottom blade on the ground but pulled the HUB off the block centre -
     * and the hub is the cable port, so that alignment is the one that matters. The authored span y -0.50..+1.50
     * already places the hub at the block's middle; the block is simply placed one higher so the port sits a full block
     * up.
     * </p>
     * <p>
     * Kept as a named constant rather than deleted: this is the third position tried, and the next person reading this
     * should see that zero is a decision, not an omission.
     * </p>
     */
    private static final float MODEL_LIFT_PIXELS = 0.0F;

    /** ⚠ [stated] named for where the water COMES FROM: water at the back plays backspin. */
    /** ⚠ Must match the track registered in HydroelectricGeneratorAnimator. */
    private static final String TRACK_NAME = "base_controller";

    private static final String FORWARD_CLIP = "heg.forwardspin";

    private static final String BACK_CLIP = "heg.backspin";

    private static final String HOLD_CLIP = "heg.hold";

    private static final String TURBINE_BONE = "gTurbine";

    private static final String PANEL_LEFT_BONE = "gPanelLeft";

    private static final String PANEL_RIGHT_BONE = "gPanelRight";

    /** The clips turn the wheel once per second at full speed. */
    private static final float TICKS_PER_TURN = 20.0F;

    /** [stated] low 25%, med 50%, max 100%. */
    private static final float LOW_SPEED = 0.25F;

    private static final float MED_SPEED = 0.5F;

    private static final float MAX_SPEED = 1.0F;

    private static final ResourceLocation MODEL_LOCATION = HumanResources.blockGeoModelLocation(NAME);

    private static final ResourceLocation TEXTURE_OFF = HumanResources.blockTextureLocation(NAME);

    private static final ResourceLocation TEXTURE_LOW = HumanResources.blockTextureLocation(NAME + "_low");

    private static final ResourceLocation TEXTURE_MED = HumanResources.blockTextureLocation(NAME + "_med");

    private static final ResourceLocation TEXTURE_MAX = HumanResources.blockTextureLocation(NAME + "_max");

    public HydroelectricGeneratorRenderer() {
        super(
            AzBlockEntityRendererConfig.<HydroelectricGeneratorBlockEntity>builder(
                $ -> MODEL_LOCATION,
                HydroelectricGeneratorRenderer::textureFor
            )
                .setAnimatorProvider(HydroelectricGeneratorAnimator::new)
                .setPrerenderEntry(context -> {
                    var blockEntity = context.animatable();

                    // ⚠⚠ THE MODEL HANGS HALF A BLOCK BELOW ITS OWN BLOCK, AND THAT IS WHY IT LOOKED BURIED.
                    //
                    // Measured with the gTurbine bone's [180, 0, 90] roll applied, the wheel spans y -0.50 .. +1.50
                    // blocks - the roll turns cubes authored +-16px in X into +-1 block in Y. So placed normally its
                    // lower blades sit half a block INTO the ground.
                    //
                    // NO PLACEMENT OFFSET CAN FIX THAT: blocks land on integer positions, and the error is half a
                    // block. So the BLOCK is placed one higher (the hub is the cable port and belongs a full block
                    // up) and the model is dropped half a block to sit around it - together those put the bottom
                    // blade exactly at ground level with the hub centred a block above it.
                    var root = context.bakedModel().getBoneOrNull(ROOT_BONE);

                    if (root != null) {
                        root.setPosY(MODEL_LIFT_PIXELS);
                    }

                    // ⚠ PROCEDURAL, AND CLIPS MUST NOT COME BACK WITHOUT MOVING THE PLATE VISIBILITY.
                    //
                    // An animation resets bone state each frame, which wipes the plates' setHidden flags. Both
                    // setPrerenderEntry and a layer's preRender run BEFORE the animation, so with clips playing the
                    // end plates reappeared every time. Until the visibility lives somewhere after the animation,
                    // the spin stays procedural - and it now has a real direction to work with, which it never did
                    // before, so this is the first time the rotation has actually been exercised.
                    var turbine = context.bakedModel().getBoneOrNull(TURBINE_BONE);

                    if (turbine != null) {
                        turbine.setRotY(spinAngleRadians(blockEntity, context.partialTick()));
                    }

                    // ⚠⚠ THE BONES ARE SWAPPED RELATIVE TO THE AXLE, AND THAT IS DELIBERATE.
                    //
                    // [stated] "looking at it from the front the left sides wall has the right side turned on and the
                    // rights side has its left wall turned on." gPanelLeft sits on the +axle side of the model, not
                    // the -axle side its name suggests, so pairing it with the "left" neighbour closed the wrong face
                    // at each end of the wheel.
                    //
                    // ⚠ This is invisible on interior sections - they have neighbours both ways, so both plates hide
                    // whichever mapping is used. Only the two ENDS ever show the error, which is why it survived.
                    // ⚠⚠ [stated] OBSERVED AT BOTH ENDS OF A 10-WHEEL: "wheel 0 is turning on its right wall
                    // should be left, wheel 9 is turning on its left wall should be right." So gPanelLeft belongs
                    // with the -axle neighbour and gPanelRight with the +axle one. Do not flip this again without
                    // that same end-by-end reading - it cannot be judged from a single screenshot.
                    applyPlateVisibility(context.bakedModel().getBoneOrNull(PANEL_LEFT_BONE), blockEntity, true);
                    applyPlateVisibility(context.bakedModel().getBoneOrNull(PANEL_RIGHT_BONE), blockEntity, false);

                    return context;
                })
                // ⚠⚠ THE PLATES ARE HIDDEN FROM A LAYER, NOT FROM THE PRERENDER ENTRY.
                //
                // An animation RESETS bone state every frame. applyPlateVisibility used to run in setPrerenderEntry,
                // which happens BEFORE the animation is applied - so the moment a clip started playing, every
                // setHidden flag was wiped and the wheel's end plates reappeared. While nothing played there was
                // nothing to wipe them, which is exactly why the plates were fine until clips were introduced.
                //
                // A layer's preRender runs AFTER the animation, so the flags survive to the draw.
                .addRenderLayer(new AzAutoGlowingLayer<>())
                .build()
        );
    }

    /**
     * ⚠⚠ NOT {@code getRenderBoundingBox}. That method does NOT exist on vanilla's {@code BlockEntityRenderer} - it is
     * a NeoForge addition, so overriding it from :common fails to compile on the Fabric side of a multiloader build
     * ("method does not override or implement a method from a supertype"). {@code shouldRenderOffScreen} is vanilla's
     * own mechanism and exists on both loaders.
     * <p>
     * The wheel's blades reach roughly two blocks past the centre cube, so without this a section stops drawing the
     * moment its own block leaves the view frustum and the far end of a long wheel blinks out at the screen edge.
     * Returning true makes the section render whenever the chunk does, which is what vanilla uses for beacons and
     * chests with oversized visuals.
     * </p>
     */
    @Override
    public boolean shouldRenderOffScreen(HydroelectricGeneratorBlockEntity blockEntity) {
        return true;
    }

    private static ResourceLocation textureFor(HydroelectricGeneratorBlockEntity blockEntity) {
        var state = blockEntity.getBlockState();

        if (!state.hasProperty(PowerMode.PROPERTY)) {
            return TEXTURE_OFF;
        }

        return switch (state.getValue(PowerMode.PROPERTY)) {
            case OFF -> TEXTURE_OFF;
            case LOW -> TEXTURE_LOW;
            case MED -> TEXTURE_MED;
            case MAX -> TEXTURE_MAX;
        };
    }

    /** Hide the plate on a side that touches another section of the same wheel. */
    private static void applyPlateVisibility(
        com.blib.api.client.model.v1.AzBone plate,
        HydroelectricGeneratorBlockEntity blockEntity,
        boolean leftSide
    ) {
        if (plate == null) {
            return;
        }

        var level = blockEntity.getLevel();
        var state = blockEntity.getBlockState();

        if (level == null || !state.hasProperty(HydroelectricGeneratorBlock.FACING)) {
            plate.setHidden(false);
            return;
        }

        var axle = state.getValue(HydroelectricGeneratorBlock.FACING).getClockWise();
        var neighbour = blockEntity.getBlockPos().relative(leftSide ? axle.getOpposite() : axle);

        // ⚠⚠ THE WHEEL BOUNDARY DECIDES, AND THE PAIRING BELOW IS THE PART THAT WAS WRONG.
        //
        // A wheel is capped at MAX_SECTIONS, so a longer run is cut into independent wheels and each needs a plate
        // closing its end. That rule was right all along; what was broken was WHICH wall each bone controls -
        // gPanelLeft was being tested against the neighbour on the wrong side of the axle, so the test ran against
        // the wrong pair and dropped a plate into the middle of a plain 10-section wheel.
        //
        // With the corrected mapping (see the call site), "the later of the two along +axle" is:
        // leftSide == false -> neighbour sits at +axle, so the later one is the NEIGHBOUR
        // leftSide == true -> neighbour sits at -axle, so the later one is THIS section
        // A plate hides only when the neighbour is a section AND that later one is not itself a wheel start.
        var joined = HydroelectricGeneratorBlockEntity.isSection(level, neighbour, state)
            && !HydroelectricGeneratorBlockEntity.isWheelStart(
                level,
                leftSide ? blockEntity.getBlockPos() : neighbour,
                state,
                axle
            );

        plate.setHidden(joined);
    }

    /**
     * The angle every section of every wheel is at right now: world time times the wheel's own speed and direction.
     * Sections agree because they all read the same clock, never because they were started together.
     */
    private static float spinAngleRadians(HydroelectricGeneratorBlockEntity blockEntity, float partialTick) {
        var spin = blockEntity.getSpin();
        var state = blockEntity.getBlockState();

        if (spin == 0 || !state.hasProperty(PowerMode.PROPERTY)) {
            return 0.0F;
        }

        var speed = switch (state.getValue(PowerMode.PROPERTY)) {
            case LOW -> LOW_SPEED;
            case MED -> MED_SPEED;
            case MAX -> MAX_SPEED;
            case OFF -> 0.0F;
        };

        if (speed <= 0.0F) {
            return 0.0F;
        }

        var level = Minecraft.getInstance().level;

        if (level == null) {
            return 0.0F;
        }

        var ticks = (float) (level.getGameTime() % 100000L) + partialTick;
        var turns = ticks * speed / TICKS_PER_TURN;

        return (float) (spin * turns * 2.0 * Math.PI % (2.0 * Math.PI));
    }
}
