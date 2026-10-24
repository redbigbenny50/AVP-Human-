package com.human.common.gameplay.block.entity.power.impl;

import com.human.common.gameplay.block.entity.power.PowerNodeBlockEntity;
import com.human.common.gameplay.block.power.HydroelectricGeneratorBlock;
import com.human.common.gameplay.block.power.PowerMode;
import com.human.common.gameplay.power.PowerNode;
import com.human.common.registry.init.HumanBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

/**
 * A hydroelectric wheel: one block is one SECTION, and sections placed along the axle are one long wheel that turns
 * together. All of the arithmetic below is [stated] Sep 24.
 * <h2>What one section's water is worth</h2>
 * <ul>
 * <li>{@value #STRENGTH_FALLING} - water falling onto it from above ("water flowing from on top down is max")</li>
 * <li>{@value #STRENGTH_NEAR} - flow at the front or back within 1-2 blocks of its source ("normal")</li>
 * <li>{@value #STRENGTH_FAR} - flow from further out ("anything past that is low")</li>
 * <li>0 - the weakest flow block ("the last water flow which is too low it counts as 0"), or nothing</li>
 * </ul>
 * <h2>What the whole wheel does with those</h2>
 *
 * <pre>level = best section's strength - number of DRY sections, clamped 0..3
 * and MAX only if EVERY section is at {@value #STRENGTH_FALLING}</pre>
 *
 * [stated] "the only way the wheel spins at max is if all sections do", and his four worked examples: one waterfall
 * alone is max; adding one dry section drops it to med; two dry to low; three dry stops it; four sections with two
 * waterfalls is low. Adding an unfed section therefore COSTS power, which is the intent - "the extra wheel actually
 * reduces power if its attached without water" - and a wider wheel needs more water to hold its level.
 * <h2>Jamming</h2> [stated] "water flowing from both sides stops it the wheel cant turn if theres water on both sides
 * flowing opposite directions." One jammed section stops the whole wheel, which is how a real axle behaves.
 * <h2>Cost</h2> ⚠ The scan runs every {@value #SENSE_INTERVAL_TICKS} ticks and only on the section that finds itself
 * FIRST along the axle, which then writes the answer to every section. A ten-wide wheel therefore costs ONE scan of ten
 * sections every two seconds, not ten scans; the grid reads a cached number every tick.
 */
public class HydroelectricGeneratorBlockEntity extends PowerNodeBlockEntity implements PowerNode.PowerProducer {

    /**
     * ⭐ [stated] Sep 25: "since its close to the solar panels volume we can make max power 256 med 128 and low 64 ...
     * if they are planning to upscale to a structure level of power then they should be rewarded."
     * <p>
     * ⚠ PER SECTION, AND SECTIONS MULTIPLY. A ten-wide wheel running at max is 2,560 FE/t - more than twice Oritech's
     * fuel generator, which is their best. That is deliberate: a wheel that wide is a dam, it needs a river and falling
     * water along its whole length, and every dry section drags the whole wheel down a band. The reward is meant to
     * scale with the build, not with the block.
     * </p>
     * <p>
     * A single section at max beats a solar panel at max (96) by 2.7x for roughly comparable volume - which is the
     * trade for needing water, and for stopping dead when it freezes or the flow is cut.
     * </p>
     */
    public static final long LOW_OUTPUT_PER_SECTION = 64;

    public static final long MED_OUTPUT_PER_SECTION = 128;

    public static final long MAX_OUTPUT_PER_SECTION = 256;

    public static final int STRENGTH_FALLING = 3;

    public static final int STRENGTH_NEAR = 2;

    public static final int STRENGTH_FAR = 1;

    /** Vanilla water: getAmount() 8 is a source, 7 is one block out, down to 1 for the thinnest edge. */
    private static final int AMOUNT_TWO_BLOCKS_OUT = 6;

    private static final int AMOUNT_WEAKEST = 1;

    private static final int SENSE_INTERVAL_TICKS = 40;

    /** Sanity bound on one wheel so a griefed line of thousands cannot make one scan expensive. */
    /**
     * ⭐ [stated] Sep 25: "has it support a maximum of 10 together and then the 11th one would start a new wheel."
     * <p>
     * ⚠ CAPPING ALONE IS NOT ENOUGH. Only the section with no neighbour behind it scans and drives the rest, so a
     * 15-long run would have given one 10-wheel and five blocks that never scanned and never turned. Every 10th section
     * is therefore treated as a NEW wheel start - see the index test in {@code serverTick}.
     * </p>
     */
    private static final int MAX_SECTIONS = 10;

    /** ⚠ Bounds the backward walk so a malformed or enormous run cannot spin the scan out. */
    private static final int MAX_SCAN_BACK = 256;

    private long cachedOutput;

    private PowerMode cachedMode = PowerMode.OFF;

    /** -1 back, 0 stopped, +1 forward - the renderer reads this through the block state and its own neighbour walk. */
    private static final String NBT_SPIN = "spin";

    private int cachedSpin;

    public HydroelectricGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(HumanBlockEntityTypes.HYDROELECTRIC_GENERATOR.get(), pos, state);
    }

    @Override
    public void serverTick(Level level, BlockPos blockPos, BlockState blockState) {
        super.serverTick(level, blockPos, blockState);

        if (level.getGameTime() % SENSE_INTERVAL_TICKS != 0) {
            return;
        }

        var axle = axleAxis(blockState);

        if (axle == null) {
            return;
        }

        // Only a wheel START scans; it then writes the result to its own 10 sections.
        //
        // ⚠ "Start" means index 0, 10, 20... along the run - NOT simply "nothing behind me". A long line of sections
        // is cut into independent wheels of MAX_SECTIONS, so an 11th block begins a second wheel that scans and turns
        // on its own instead of being stranded past the cap.
        if (!isWheelStart(level, blockPos, blockState, axle)) {
            return;
        }

        var sections = collectWheel(level, blockPos, blockState, axle);
        applyToWheel(level, sections, blockState);
    }

    /** The axle runs left-to-right across the face, so the wheel grows sideways from the block the player placed. */
    private static Direction axleAxis(BlockState blockState) {
        if (!blockState.hasProperty(HydroelectricGeneratorBlock.FACING)) {
            return null;
        }

        return blockState.getValue(HydroelectricGeneratorBlock.FACING).getClockWise();
    }

    /** True when the neighbour is another section of the SAME wheel: same block, same facing. */
    public static boolean isSection(BlockGetter level, BlockPos pos, BlockState reference) {
        var state = level.getBlockState(pos);

        return state.getBlock() == reference.getBlock()
            && state.hasProperty(HydroelectricGeneratorBlock.FACING)
            && state.getValue(HydroelectricGeneratorBlock.FACING) == reference.getValue(HydroelectricGeneratorBlock.FACING);
    }

    /**
     * True when this section BEGINS a wheel - index 0, 10, 20... along the run.
     * <p>
     * ⚠⚠ THE RENDERER USES THIS TOO, and it must: the side plates close a wheel's end, so they have to appear at the
     * same seams the tick treats as boundaries. Hiding a plate on bare adjacency made a 15-long run look like one
     * continuous wheel while behaving as two, with the join between the 10th and 11th invisible.
     * </p>
     */
    public static boolean isWheelStart(Level level, BlockPos pos, BlockState blockState, Direction axle) {
        return sectionsBehind(level, pos, blockState, axle.getOpposite()) % MAX_SECTIONS == 0;
    }

    /** How many sections sit behind this one along the axle, bounded by {@link #MAX_SCAN_BACK}. */
    private static int sectionsBehind(Level level, BlockPos pos, BlockState blockState, Direction back) {
        var count = 0;
        var cursor = pos.relative(back);

        while (count < MAX_SCAN_BACK && isSection(level, cursor, blockState)) {
            count++;
            cursor = cursor.relative(back);
        }

        return count;
    }

    private static List<BlockPos> collectWheel(Level level, BlockPos first, BlockState blockState, Direction axle) {
        var sections = new ArrayList<BlockPos>();
        var cursor = first;

        while (sections.size() < MAX_SECTIONS && isSection(level, cursor, blockState)) {
            sections.add(cursor);
            cursor = cursor.relative(axle);
        }

        return sections;
    }

    private void applyToWheel(Level level, List<BlockPos> sections, BlockState blockState) {
        var facing = blockState.getValue(HydroelectricGeneratorBlock.FACING);
        var best = 0;
        var dry = 0;
        var allFalling = true;
        var jammed = false;
        var forward = 0;
        var backward = 0;

        for (var section : sections) {
            // ⚠⚠ SAMPLE THE BLADE ROW AS WELL AS THE HUB ROW.
            //
            // The hub is ONE SOLID CUBE and the blades hang a block below it - so once the wheel is placed with its
            // hub above the waterline (which is what makes it look right and stops water pouring over the top),
            // every neighbour AT HUB LEVEL is air. Checking only that row meant a wheel sitting perfectly in a river
            // reported no water at all and never turned.
            //
            // Both rows are checked and the stronger wins: a wheel mounted low still reads its hub row exactly as
            // before, and a correctly-mounted one reads the water its blades are actually in.
            // ⚠⚠ SAMPLE BOTH ROWS THE WHEEL REACCHES INTO, NOT ONE.
            //
            // The wheel spans y -0.50..+1.50 about its hub, so its blades occupy the block BELOW and the block
            // ABOVE. Picking one row coupled this check to the render offset - and that offset moved three times
            // while the anchor was being settled, silently pointing detection at the ground or at open air each
            // time. Checking both rows makes the water test independent of where the model is drawn.
            var bladeBelow = section.below();
            var bladeAbove = section.above();

            // ⚠⚠ WATER POURING DOWN A FACE IS A FEED, AND flowStrength CANNOT SEE IT.
            //
            // flowStrength scores the HORIZONTAL component only (flow.x * stepX + flow.z * stepZ). Water running
            // down a vertical wall flows almost entirely in Y, so that dot product is ~0 and a curtain of water
            // falling against the blades scored ZERO - which is exactly the dam he built, and why it would not turn
            // however much water was poured over it.
            //
            // A sheet falling against a face now counts as the strongest feed there is, pushing the wheel away from
            // that face - the same direction a horizontal flow arriving there would.
            var frontFlow = Math.max(
                Math.max(
                    flowStrength(level, section.relative(facing), facing.getOpposite()),
                    Math.max(
                        flowStrength(level, bladeBelow.relative(facing), facing.getOpposite()),
                        flowStrength(level, bladeAbove.relative(facing), facing.getOpposite())
                    )
                ),
                Math.max(
                    fallingOnFace(level, section.relative(facing), bladeBelow.relative(facing)),
                    fallingOnFace(level, bladeAbove.relative(facing), bladeAbove.relative(facing))
                )
            );
            var backFlow = Math.max(
                Math.max(
                    flowStrength(level, section.relative(facing.getOpposite()), facing),
                    Math.max(
                        flowStrength(level, bladeBelow.relative(facing.getOpposite()), facing),
                        flowStrength(level, bladeAbove.relative(facing.getOpposite()), facing)
                    )
                ),
                Math.max(
                    fallingOnFace(level, section.relative(facing.getOpposite()), bladeBelow.relative(facing.getOpposite())),
                    fallingOnFace(level, bladeAbove.relative(facing.getOpposite()), bladeAbove.relative(facing.getOpposite()))
                )
            );
            // Falling water lands on the hub row from above, or runs down the blade row behind it.
            var falling = Math.max(fallingStrength(level, section), fallingStrength(level, bladeAbove));

            // BOTH SIDES PUSHING = A LOCKED AXLE. One jammed section stops every section.
            if (frontFlow > 0 && backFlow > 0) {
                jammed = true;
                break;
            }

            var strength = Math.max(falling, Math.max(frontFlow, backFlow));

            if (strength <= 0) {
                dry++;
                allFalling = false;
            } else if (strength < STRENGTH_FALLING) {
                allFalling = false;
            }

            best = Math.max(best, strength);

            if (frontFlow > 0) {
                forward++;
            } else if (backFlow > 0) {
                backward++;
            }
        }

        // ⚠⚠ DRY SECTIONS NOW COST A PROPORTION, NOT A COUNT.
        //
        // This was best - dry, a raw subtraction. It behaves on a 3-wide wheel and collapses on a long one: a
        // 10-section wheel with even three dry bays could never leave OFF however hard the rest was fed, because
        // 3 - 3 is already zero. A dam - the exact build this was meant to reward - was the hardest thing to make work.
        //
        // The wet FRACTION scales the level instead, so a partly-fed wheel turns weakly rather than not at all, and
        // a fully-fed one is unchanged: dry = 0 leaves best untouched, as before.
        var wet = sections.size() - dry;
        var level3 = 0;

        if (!jammed && wet > 0) {
            var scaled = (best * wet) / (float) sections.size();

            // Any wet section at all is worth at least LOW - a wheel visibly taking water should never read OFF.
            level3 = Math.max(STRENGTH_FAR, Math.min(STRENGTH_FALLING, Math.round(scaled)));
        }

        // [stated] "the only way the wheel spins at max is if all sections do" - anything short of that caps at med.
        if (level3 >= STRENGTH_FALLING && !allFalling) {
            level3 = STRENGTH_NEAR;
        }

        var mode = switch (level3) {
            case 3 -> PowerMode.MAX;
            case 2 -> PowerMode.MED;
            case 1 -> PowerMode.LOW;
            default -> PowerMode.OFF;
        };

        // Falling water turns the wheel the way the nearest flow does; with none, the majority side wins.
        var spin = mode == PowerMode.OFF ? 0 : (backward > forward ? -1 : 1);

        for (var section : sections) {
            if (!(level.getBlockEntity(section) instanceof HydroelectricGeneratorBlockEntity blockEntity)) {
                continue;
            }

            // ⚠⚠ THE SPIN HAS TO REACH THE CLIENT, AND IT NEVER DID.
            //
            // cachedSpin was a plain field written only here, on the SERVER. The renderer runs on the client, where
            // it was therefore ALWAYS 0 - so spinAngleRadians returned 0 every frame and the wheel never moved. The
            // power mode reached the client because it is a blockstate; the direction had no route at all.
            //
            // That is why swapping the rotation axis changed nothing, and why clip playback picked heg.hold: none of
            // those attempts were ever handed a non-zero spin. Persisting it puts it in the update tag that
            // PowerNodeBlockEntity already publishes.
            var spinChanged = blockEntity.cachedSpin != spin;

            blockEntity.cachedMode = mode;
            blockEntity.cachedSpin = spin;

            if (spinChanged) {
                blockEntity.setChanged();
                level.sendBlockUpdated(section, level.getBlockState(section), level.getBlockState(section), 3);
            }
            blockEntity.cachedOutput = switch (mode) {
                case OFF -> 0;
                case LOW -> LOW_OUTPUT_PER_SECTION;
                case MED -> MED_OUTPUT_PER_SECTION;
                case MAX -> MAX_OUTPUT_PER_SECTION;
            };

            var state = level.getBlockState(section);

            if (state.hasProperty(PowerMode.PROPERTY) && state.getValue(PowerMode.PROPERTY) != mode) {
                level.setBlock(section, state.setValue(PowerMode.PROPERTY, mode), 3);
            }
        }
    }

    /**
     * Water falling onto the wheel from above.
     * <p>
     * ⚠ Oct 21 (tester screenshot: a row of sources on top of the wheels, flowing ACROSS them, made full power). This
     * counted ANY water directly above as falling - a still source sitting on the hub, or a sheet running sideways
     * along the tops, scored the same as a waterfall. [stated] "the intent is water on top is flowing down not across."
     * Only water actually MOVING DOWN onto the wheel (the same downward-flow test the faces use, {@link #isFalling})
     * counts now; water lying on or running across the top turns nothing.
     * </p>
     */
    private static int fallingStrength(Level level, BlockPos section) {
        return isFalling(level, section.above()) ? STRENGTH_FALLING : 0;
    }

    /**
     * Flow arriving at one side of the wheel. Only water actually MOVING toward the blades counts - a still pool or a
     * flow running parallel to the face turns nothing.
     */
    /**
     * Water falling DOWN one of the wheel's faces, at either the hub row or the blade row.
     * <p>
     * ⚠ Tested on the Y component, which is the one {@link #flowStrength} throws away. A source block sitting still
     * against the face is not a feed; only water with real downward movement is.
     * </p>
     */
    private static int fallingOnFace(Level level, BlockPos hubRow, BlockPos bladeRow) {
        return isFalling(level, hubRow) || isFalling(level, bladeRow) ? STRENGTH_FALLING : 0;
    }

    private static boolean isFalling(Level level, BlockPos pos) {
        var fluid = level.getFluidState(pos);

        if (!fluid.is(Fluids.WATER) && !fluid.is(Fluids.FLOWING_WATER)) {
            return false;
        }

        // ⚠ Oct 21 - vanilla's FALLING flag first. Checked against vanilla's FlowingFluid.getFlow: it only adds the
        // downward component when a SOLID face sits beside the falling water, so a waterfall dropping through open air
        // onto the wheel reported no downward flow at all. FALLING is set on every block of a descending column,
        // touching a wall or not; a source lying still, or a sheet running sideways, never has it.
        if (
            fluid.hasProperty(net.minecraft.world.level.material.FlowingFluid.FALLING)
                && fluid.getValue(net.minecraft.world.level.material.FlowingFluid.FALLING)
        ) {
            return true;
        }

        // ⚠ Negative Y is downward. A still source returns ~0 here and correctly scores nothing.
        return fluid.getFlow(level, pos).y < -1.0e-4;
    }

    private static int flowStrength(Level level, BlockPos pos, Direction towardWheel) {
        FluidState fluid = level.getFluidState(pos);

        if (!fluid.is(Fluids.WATER) && !fluid.is(Fluids.FLOWING_WATER)) {
            return 0;
        }

        var flow = fluid.getFlow(level, pos);

        if (flow.lengthSqr() < 1.0e-4) {
            return 0; // a still source block pressed against the wheel does nothing
        }

        var towards = flow.x * towardWheel.getStepX() + flow.z * towardWheel.getStepZ();

        if (towards <= 0) {
            return 0; // running past or away from the blades
        }

        var amount = fluid.getAmount();

        if (amount <= AMOUNT_WEAKEST) {
            return 0; // [stated] the last, thinnest flow block counts as nothing
        }

        return amount >= AMOUNT_TWO_BLOCKS_OUT ? STRENGTH_NEAR : STRENGTH_FAR;
    }

    /** -1 back, 0 stopped, +1 forward. */
    public int getSpin() {
        return cachedSpin;
    }

    /** ⚠ Rides the update tag PowerNodeBlockEntity publishes, which is how the client learns the direction. */
    @Override
    protected void saveAdditional(
        net.minecraft.nbt.@org.jetbrains.annotations.NotNull CompoundTag tag,
        net.minecraft.core.HolderLookup.@org.jetbrains.annotations.NotNull Provider registries
    ) {
        super.saveAdditional(tag, registries);
        tag.putInt(NBT_SPIN, cachedSpin);
    }

    @Override
    protected void loadAdditional(
        net.minecraft.nbt.@org.jetbrains.annotations.NotNull CompoundTag tag,
        net.minecraft.core.HolderLookup.@org.jetbrains.annotations.NotNull Provider registries
    ) {
        super.loadAdditional(tag, registries);
        // Clamped: only -1, 0 and +1 are meaningful, and a pasted tag can hold anything.
        cachedSpin = Math.max(-1, Math.min(1, tag.getInt(NBT_SPIN)));
    }

    @Override
    public long getAvailablePower() {
        return level == null || level.isClientSide ? 0 : cachedOutput;
    }

    @Override
    public long extractPower(long maxAmount) {
        return maxAmount;
    }

    /**
     * [stated] Oct 5: cables connect left or right of its front - it took a cable on any side. Cables, the grid walk
     * and the wiring tool all read this one rule.
     */
    @Override
    public boolean canConnectOn(Direction direction) {
        var state = getBlockState();

        var facingProperty = net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;

        if (!state.hasProperty(facingProperty)) {
            return direction.getAxis().isHorizontal();
        }

        var facing = state.getValue(facingProperty);
        return direction == facing.getClockWise() || direction == facing.getCounterClockWise();
    }
}
