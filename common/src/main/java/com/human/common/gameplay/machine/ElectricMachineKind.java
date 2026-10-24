package com.human.common.gameplay.machine;

import com.blib.api.common.registry.v1.BLibHolder;
import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import com.human.common.gameplay.fluid.FluidContent;
import com.human.common.gameplay.menu.machine.ElectricMachineMenu;
import com.human.common.registry.init.HumanBlockEntityTypes;
import com.human.common.registry.init.HumanMenuTypes;
import com.human.common.registry.init.HumanRecipes;
import com.human.common.registry.init.HumanSoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The tier-1 machines. [stated] Sep 27/29: uniform single blocks on the machine block, power from the back, a working
 * sound and animation each.
 * <p>
 * Each kind carries what differs between machines: its power draw, its {@link MachineLogic} (what it actually does),
 * its SLOT LAYOUT - where the slots sit on his GUI art - and where the progress bolt is. Those positions were measured
 * from his GUI textures, not guessed; the furnace types share one layout.
 * </p>
 */
public enum ElectricMachineKind {

    FURNACE("electric_furnace", 30L, HumanSoundEvents.MACHINE_ELECTRIC_FURNACE, Layout.FURNACE, Animations.FURNACE_FLICKER) {

        @Override
        public MachineLogic createLogic() {
            return new CookingMachineLogic(() -> RecipeType.SMELTING);
        }
    },
    SMOKER("electric_smoker", 30L, HumanSoundEvents.MACHINE_ELECTRIC_SMOKER, Layout.FURNACE, Animations.FURNACE_FLICKER) {

        @Override
        public MachineLogic createLogic() {
            return new CookingMachineLogic(() -> RecipeType.SMOKING);
        }
    },
    BLAST_FURNACE(
        "electric_blast_furnace", 45L, HumanSoundEvents.MACHINE_ELECTRIC_BLAST_FURNACE, Layout.FURNACE, Animations.FURNACE_FLICKER
    ) {

        @Override
        public MachineLogic createLogic() {
            return new CookingMachineLogic(() -> RecipeType.BLASTING);
        }
    },
    INDUSTRIAL_FURNACE(
        "electric_industrial_furnace", 60L, HumanSoundEvents.MACHINE_ELECTRIC_INDUSTRIAL_FURNACE, Layout.FURNACE, Animations.FURNACE_FLICKER
    ) {

        @Override
        public MachineLogic createLogic() {
            return new CookingMachineLogic(HumanRecipes.INDUSTRIAL_FURNACE_RECIPE_TYPE::get);
        }
    },
    /** [stated] Sep 29: 1 input, 9 outputs - ores to dusts, sediment, coal to carbon. 60 FE/t. */
    GRINDER("electric_grinder", 45L, HumanSoundEvents.MACHINE_ELECTRIC_GRINDER, Layout.GRINDER, Animations.GRINDER) {

        @Override
        public MachineLogic createLogic() {
            return new GrinderMachineLogic();
        }
    },
    /**
     * [stated] readme + Sep 29/30: squeezes fluid out of blocks and presses blocks into blocks. Item in top or left,
     * item out right, fluid out the BOTTOM. Two switches on the left panel throw away the item or the fluid output.
     */
    PRESS(
        "electric_press", 60L, HumanSoundEvents.MACHINE_ELECTRIC_PRESS, Layout.PRESS, Animations.PRESS, Tank.PRESS, Faces.IN_TOP_LEFT_OUT_RIGHT
    ) {

        @Override
        public MachineLogic createLogic() {
            return new PressMachineLogic();
        }
    },
    /**
     * [stated] readme: makes its own water while powered and floods blocks with it. Item in top or left, item out
     * right, water out the BOTTOM.
     */
    HYDRATOR(
        "electric_hydrator", 30L, HumanSoundEvents.MACHINE_ELECTRIC_HYDRATOR, Layout.HYDRATOR, Animations.HYDRATOR, Tank.HYDRATOR, Faces.IN_TOP_LEFT_OUT_RIGHT
    ) {

        @Override
        public MachineLogic createLogic() {
            return new HydratorMachineLogic();
        }
    },
    /**
     * [stated] readme: freezes blocks, items or fluid. Item in the TOP, item out bottom or right, fluid in the LEFT.
     */
    FREEZER(
        "electric_freezer", 45L, HumanSoundEvents.MACHINE_ELECTRIC_FREEZER, Layout.FREEZER, Animations.FREEZER, Tank.FREEZER, Faces.IN_TOP_OUT_BOTTOM_RIGHT
    ) {

        @Override
        public MachineLogic createLogic() {
            return new FreezerMachineLogic();
        }
    },
    /**
     * [stated] Oct 17: the vaporizer - liquid into an aluminum tank, a filled tank out. 1 bucket per tank, 10 seconds,
     * an 8-bucket tank, 45 FE/t only while working. Two blocks tall: power from the BOTTOM ONLY, the liquid intake on
     * the front of the bottom block, empty tanks in through the TOP, filled tanks out of the LEFT and RIGHT of the top
     * block. It never gives liquid out. See {@link VaporizerMachineLogic}.
     */
    VAPORIZER(
        "vaporizer", 45L, HumanSoundEvents.MACHINE_VAPORIZER, Layout.VAPORIZER, Animations.NONE, Tank.VAPORIZER, Faces.IN_TOP_OUT_LEFT_RIGHT
    ) {

        @Override
        public MachineLogic createLogic() {
            return new VaporizerMachineLogic();
        }
    },
    /**
     * [stated] Oct 17 - the distributor, an industrial fog machine (DISTRIBUTOR_SPEC.md). 30 FE/t, only while pouring.
     * The 1,000-vapor chamber, the modes and the room scan live in
     * {@link com.human.common.gameplay.distributor.DistributorBlockEntity}; this logic only says "pour this tick". Fan
     * on the FRONT, liquid in on the TOP, tanks in on the LEFT and RIGHT, power on the BACK, a plain plate under. Its
     * working sound is its own (Oct 19), playing while it pours.
     */
    DISTRIBUTOR(
        "electric_distributor", 30L, HumanSoundEvents.MACHINE_DISTRIBUTOR, Layout.DISTRIBUTOR, Animations.DISTRIBUTOR, Tank.DISTRIBUTOR, Faces.IN_LEFT_RIGHT
    ) {

        @Override
        public MachineLogic createLogic() {
            return new DistributorMachineLogic();
        }
    };

    /**
     * Where the slots and the progress bolt sit on a machine's GUI texture. Slot positions are the item position (one
     * pixel inside the well), as vanilla's Slot takes them.
     */
    public record Layout(
        int inputX,
        int inputY,
        int[][] outputs,
        int indicatorX,
        int indicatorY,
        int fillX,
        int fillY
    ) {

        /** No fill slot. */
        private static final int NONE = Integer.MIN_VALUE;

        public Layout(int inputX, int inputY, int[][] outputs, int indicatorX, int indicatorY) {
            this(inputX, inputY, outputs, indicatorX, indicatorY, NONE, NONE);
        }

        /** The four furnace types: input and output wells either side of the bolt. */
        public static final Layout FURNACE = new Layout(44, 35, new int[][] { { 116, 35 } }, 83, 35);

        /**
         * [stated] Sep 30: the bucket icon on top, the FILL slot under it, the item output under that. [stated] Oct 2
         * his reworked press art moved everything down - measured from it: wells at (132, 31) and (132, 51), the input
         * in the press frame at (49, 41), the bolt's top at row 37.
         */
        public static final Layout PRESS = new Layout(49, 41, new int[][] { { 133, 52 } }, 88, 37, 133, 32);

        /**
         * [stated] Sep 30: stacked the same way as the press. Input at (79, 33). [stated] Oct 2 the bucket and both
         * wells moved down to the press's heights: wells at (128, 31) and (128, 51), bucket icon rows 16-28.
         */
        public static final Layout HYDRATOR = new Layout(80, 34, new int[][] { { 129, 52 } }, 106, 34, 129, 32);

        /**
         * His freezer: the input in the middle of the snowflake, the output right of the bolt. No fill slot. [stated]
         * Oct 2 his reworked art moved the bolt 4 px left (now x 103); the slots did not move.
         */
        public static final Layout FREEZER = new Layout(50, 35, new int[][] { { 134, 35 } }, 103, 35);

        /** His grinder GUI: the input between the wheels, a 3 x 3 output block. Measured from the texture. */
        public static final Layout GRINDER = new Layout(
            36,
            36,
            new int[][] {
                { 107, 18 },
                { 125, 18 },
                { 143, 18 },
                { 107, 36 },
                { 125, 36 },
                { 143, 36 },
                { 107, 54 },
                { 125, 54 },
                { 143, 54 }
            },
            86,
            36
        );

        /**
         * [stated] Oct 17 - his approved mockup: the empty-tank well under the tube, the bolt between, a 3 x 3 grid of
         * filled tanks on the right. Measured from the GUI art (item position = one pixel inside each well).
         */
        public static final Layout VAPORIZER = new Layout(
            53,
            40,
            new int[][] {
                { 101, 22 },
                { 119, 22 },
                { 137, 22 },
                { 101, 40 },
                { 119, 40 },
                { 137, 40 },
                { 101, 58 },
                { 119, 58 },
                { 137, 58 }
            },
            79,
            40
        );

        /**
         * [stated] Oct 17 - his approved distributor mockup: just the TANK slot on the right (item position one pixel
         * inside the well at 130, 37, measured). No outputs and no progress bolt - the bolt is parked off the screen so
         * the shared screen never draws or hovers it; the distributor screen shows its status on the chamber instead.
         */
        public static final Layout DISTRIBUTOR = new Layout(131, 38, new int[0][], -1000, -1000);

        public boolean hasFillSlot() {
            return fillX != NONE;
        }

        /** The fill slot's index: after the input and every output. Only meaningful when {@link #hasFillSlot()}. */
        public int fillSlot() {
            return 1 + outputs.length;
        }

        public int slotCount() {
            return 1 + outputs.length + (hasFillSlot() ? 1 : 0);
        }
    }

    /**
     * A machine's own fluid tank, and where its gauge is drawn on his GUI art.
     *
     * @param capacity millibuckets
     * @param output   true when the machine MAKES fluid (pipes and containers take it out); false when it USES fluid
     *                 (they put it in)
     * @param gaugeX   the gauge's inside, measured from his art
     * @param overlay  his hand-drawn fluid sprites are
     *                 {@code textures/gui/container/fluid/<overlay>_<fluid>_overlay.png}; a fluid without one is drawn
     *                 from its own texture
     */
    public record Tank(
        int capacity,
        boolean output,
        int gaugeX,
        int gaugeY,
        int gaugeW,
        int gaugeH,
        String overlay
    ) {

        /** Eight buckets - the same as a universal canister. */
        public static final int DEFAULT_CAPACITY = 8 * FluidContent.BUCKET;

        /**
         * The tall gauge right of the press: 16 x 51, his press overlays' size. [stated] Oct 2 his reworked press moved
         * it down 5 px - inside now rows 17-67.
         */
        static final Tank PRESS = new Tank(DEFAULT_CAPACITY, true, 106, 17, 16, 51, "press");

        /** The hydrator's tank on the left: 16 x 36, his hydrator overlay's size. */
        static final Tank HYDRATOR = new Tank(DEFAULT_CAPACITY, true, 31, 23, 16, 36, "hydrator");

        /** [his art] The freezer shows its fluid in the input well itself, 16 x 16 - his freezer overlay's size. */
        static final Tank FREEZER = new Tank(DEFAULT_CAPACITY, false, 50, 35, 16, 16, "freezer");

        /** [stated] Oct 17: 8 buckets, shown in the vessel outline on the left of his art (inside: 17 x 33). */
        static final Tank VAPORIZER = new Tank(DEFAULT_CAPACITY, false, 17, 27, 17, 33, "vaporizer");

        /**
         * [stated] Oct 17: the distributor's liquid in - 8 buckets, the tall tank on the left of his mockup (inside
         * measured at x 19-33, y 24-68). An INPUT tank, filled from the TOP.
         */
        static final Tank DISTRIBUTOR = new Tank(DEFAULT_CAPACITY, false, 19, 24, 15, 45, "distributor");
    }

    /** Which faces take items in and give items out, as seen by a player looking at the machine's front. */
    public enum Faces {

        /** The furnaces and the grinder: in through the top or left, out through the bottom or right. */
        IN_TOP_LEFT_OUT_BOTTOM_RIGHT(true, true, true, true),
        /** [stated] readme - press and hydrator: the bottom is their fluid outlet, so items leave only on the right. */
        IN_TOP_LEFT_OUT_RIGHT(true, true, false, true),
        /** [stated] readme - freezer: the left is its fluid inlet, so items enter only at the top. */
        IN_TOP_OUT_BOTTOM_RIGHT(true, false, true, true),
        /** [stated] Oct 17 - vaporizer: empty tanks in at the top, filled tanks out of the left and the right. */
        IN_TOP_OUT_LEFT_RIGHT(true, false, false, true, true),
        /** [stated] Oct 17 - distributor: vapor tanks in on the left OR the right; it gives nothing out. */
        IN_LEFT_RIGHT(false, true, false, false, false, true);

        public final boolean inTop;

        public final boolean inLeft;

        public final boolean outBottom;

        public final boolean outRight;

        public final boolean outLeft;

        public final boolean inRight;

        Faces(boolean inTop, boolean inLeft, boolean outBottom, boolean outRight) {
            this(inTop, inLeft, outBottom, outRight, false);
        }

        Faces(boolean inTop, boolean inLeft, boolean outBottom, boolean outRight, boolean outLeft) {
            this(inTop, inLeft, outBottom, outRight, outLeft, false);
        }

        Faces(boolean inTop, boolean inLeft, boolean outBottom, boolean outRight, boolean outLeft, boolean inRight) {
            this.inTop = inTop;
            this.inLeft = inLeft;
            this.outBottom = outBottom;
            this.outRight = outRight;
            this.outLeft = outLeft;
            this.inRight = inRight;
        }
    }

    /** The working-front animations, driven by each machine's own clock (see MachineAnimation). */
    private static final class Animations {

        /** Furnace flicker: two frames, blended. */
        static final MachineAnimation FURNACE_FLICKER = new MachineAnimation(new int[] { 2, 2 }, true);

        /** His grinder strip: four frames, one tick each (its .mcmeta frametime 1). */
        static final MachineAnimation GRINDER = new MachineAnimation(new int[] { 1, 1, 1, 1 }, false);

        /** His press strip: seven frames, two ticks each (frametime 2) - one stroke every 14 ticks. */
        static final MachineAnimation PRESS = new MachineAnimation(new int[] { 2, 2, 2, 2, 2, 2, 2 }, false);

        /** His hydrator strip: nine frames, one tick each (frametime 1). */
        static final MachineAnimation HYDRATOR = new MachineAnimation(new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1 }, false);

        /** His freezer strip: five frames, two ticks each, blended (frametime 2, interpolate). */
        static final MachineAnimation FREEZER = new MachineAnimation(new int[] { 2, 2, 2, 2, 2 }, true);

        /** The vaporizer animates through its geo model (VaporizerRenderer), not a texture strip: one still frame. */
        static final MachineAnimation NONE = new MachineAnimation(new int[] { 1 }, false);

        /** [stated] Oct 17: his distributor fan strip - eight frames, ONE tick each (frametime 1). */
        static final MachineAnimation DISTRIBUTOR = new MachineAnimation(new int[] { 1, 1, 1, 1, 1, 1, 1, 1 }, false);
    }

    private final String id;

    private final long powerPerTick;

    private final Supplier<SoundEvent> workingSound;

    private final Layout layout;

    private final MachineAnimation animation;

    private final @Nullable Tank tank;

    private final Faces faces;

    ElectricMachineKind(String id, long powerPerTick, Supplier<SoundEvent> workingSound, Layout layout, MachineAnimation animation) {
        this(id, powerPerTick, workingSound, layout, animation, null, Faces.IN_TOP_LEFT_OUT_BOTTOM_RIGHT);
    }

    ElectricMachineKind(
        String id,
        long powerPerTick,
        Supplier<SoundEvent> workingSound,
        Layout layout,
        MachineAnimation animation,
        @Nullable Tank tank,
        Faces faces
    ) {
        this.id = id;
        this.powerPerTick = powerPerTick;
        this.workingSound = workingSound;
        this.layout = layout;
        this.animation = animation;
        this.tank = tank;
        this.faces = faces;
    }

    /** A fresh logic for one machine - one per block entity, since it may cache recipe lookups. */
    public abstract MachineLogic createLogic();

    public String id() {
        return id;
    }

    /**
     * The GUI art's file name under {@code textures/gui/container/} - the machine's id, except [stated] Oct 17 the
     * distributor, whose approved mockup is {@code distributor.png}.
     */
    public String guiTexture() {
        return this == DISTRIBUTOR ? "distributor" : id;
    }

    public long powerPerTick() {
        return powerPerTick;
    }

    public SoundEvent workingSound() {
        return workingSound.get();
    }

    public Layout layout() {
        return layout;
    }

    public MachineAnimation animation() {
        return animation;
    }

    /** The machine's fluid tank, or null for the machines that have none. */
    public @Nullable Tank tank() {
        return tank;
    }

    public Faces faces() {
        return faces;
    }

    /**
     * Which side power enters, for a machine facing {@code front}. [stated] Sep 29 every machine is powered from the
     * back - except [stated] Oct 17 the vaporizer, whose power port is on its underside only.
     */
    public net.minecraft.core.Direction powerSide(net.minecraft.core.Direction front) {
        return this == VAPORIZER ? net.minecraft.core.Direction.DOWN : front.getOpposite();
    }

    /**
     * Where an INPUT tank takes fluid in by default: the freezer on its left ([stated] readme), the vaporizer on its
     * front ([stated] Oct 17 - the intake on the front of the bottom block).
     */
    public MachinePorts.Face fluidInFace() {
        return switch (this) {
            case VAPORIZER -> MachinePorts.Face.FRONT;
            // [stated] Oct 17 - distributor: liquid in on the TOP.
            case DISTRIBUTOR -> MachinePorts.Face.TOP;
            default -> MachinePorts.Face.LEFT;
        };
    }

    /**
     * Menu values a machine adds after the shared ones ({@code ElectricMachineBlockEntity.DATA_COUNT}) - the
     * distributor's chamber, vapor, mode, MUTHUR command and sensor. ⚠ Both sides build the menu from this, so the
     * client always has as many data slots as the server sends.
     */
    public int extraDataCount() {
        return this == DISTRIBUTOR ? com.human.common.gameplay.distributor.DistributorBlockEntity.EXTRA_COUNT : 0;
    }

    /**
     * Whether nearby clients are sent the tank's contents - only the vaporizer, whose model shows its liquid through
     * the windows. Every other machine's tank is only ever seen on its screen, which syncs on its own.
     */
    public boolean syncsFluidToClients() {
        return this == VAPORIZER;
    }

    /** [stated] readme: only the press has the keep/discard switches. */
    public boolean hasDiscardSwitches() {
        return this == PRESS;
    }

    public BLibHolder<BlockEntityType<ElectricMachineBlockEntity>> blockEntityType() {
        return switch (this) {
            case FURNACE -> HumanBlockEntityTypes.ELECTRIC_FURNACE;
            case SMOKER -> HumanBlockEntityTypes.ELECTRIC_SMOKER;
            case BLAST_FURNACE -> HumanBlockEntityTypes.ELECTRIC_BLAST_FURNACE;
            case INDUSTRIAL_FURNACE -> HumanBlockEntityTypes.ELECTRIC_INDUSTRIAL_FURNACE;
            case GRINDER -> HumanBlockEntityTypes.ELECTRIC_GRINDER;
            case PRESS -> HumanBlockEntityTypes.ELECTRIC_PRESS;
            case HYDRATOR -> HumanBlockEntityTypes.ELECTRIC_HYDRATOR;
            case FREEZER -> HumanBlockEntityTypes.ELECTRIC_FREEZER;
            case VAPORIZER -> com.human.common.registry.init.HumanVaporizer.VAPORIZER_BLOCK_ENTITY;
            case DISTRIBUTOR -> com.human.common.registry.init.HumanDistributor.DISTRIBUTOR_BLOCK_ENTITY;
        };
    }

    public BLibHolder<MenuType<ElectricMachineMenu>> menuType() {
        return switch (this) {
            case FURNACE -> HumanMenuTypes.ELECTRIC_FURNACE;
            case SMOKER -> HumanMenuTypes.ELECTRIC_SMOKER;
            case BLAST_FURNACE -> HumanMenuTypes.ELECTRIC_BLAST_FURNACE;
            case INDUSTRIAL_FURNACE -> HumanMenuTypes.ELECTRIC_INDUSTRIAL_FURNACE;
            case GRINDER -> HumanMenuTypes.ELECTRIC_GRINDER;
            case PRESS -> HumanMenuTypes.ELECTRIC_PRESS;
            case HYDRATOR -> HumanMenuTypes.ELECTRIC_HYDRATOR;
            case FREEZER -> HumanMenuTypes.ELECTRIC_FREEZER;
            case VAPORIZER -> com.human.common.registry.init.HumanVaporizer.VAPORIZER_MENU;
            case DISTRIBUTOR -> com.human.common.registry.init.HumanDistributor.DISTRIBUTOR_MENU;
        };
    }
}
