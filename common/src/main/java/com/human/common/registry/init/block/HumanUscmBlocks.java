package com.human.common.registry.init.block;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.block.property.HumanBlockProperties;
import com.human.common.gameplay.block.uscm.ConnectedWindowBlock;
import com.human.common.gameplay.block.uscm.FlippableWallBlock;
import com.human.common.gameplay.block.uscm.FloorFacingBlock;
import com.human.common.gameplay.block.uscm.LockerBlock;
import com.human.common.gameplay.block.uscm.LockerBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The USCM marine-base building set: 50 blocks.
 * <p>
 * [stated] Sep 28: every USCM block is printed in the FABRICATOR only (no crafting-table recipe) and needs the USCM
 * LICENSE; none can be made into an individual blueprint, just like the USCM machine editions. Both rules come from the
 * {@code avp_human:faction/uscm} item tag every one of these items is in - the fabricator reads it for the license and
 * the blueprint table refuses anything in it.
 * <ul>
 * <li>Most blocks are plain cubes on composite/steel strength.</li>
 * <li>{@code composite_wall} rotates at random when placed - done in its blockstate file, no code.</li>
 * <li>{@code bordered_composite_wall} can go upside down ({@link FlippableWallBlock}).</li>
 * <li>Wall patterns with a direction use a pillar axis so they run vertical or horizontal; floor patterns with a
 * direction face the way the player looks ({@link FloorFacingBlock}).</li>
 * <li>Every light block gives light level 15, like vanilla lights.</li>
 * <li>The two lockers are storage that opens like a barrel and spills its contents when broken
 * ({@link LockerBlock}).</li>
 * <li>The window connects to its own kind, one steel frame round the whole window ({@link ConnectedWindowBlock}).</li>
 * </ul>
 * Models, blockstates and item models are hand-written under {@code assets/avp_human}; loot, names and the pickaxe tag
 * come from datagen, which reads {@link #ALL} and {@link #ENGLISH_NAMES}.
 */
public final class HumanUscmBlocks {

    private static final BLibRegistry<Block> BLOCKS = Human.MOD.registries().create(BuiltInRegistries.BLOCK);

    private static final BLibRegistry<Item> ITEMS = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    private static final BLibRegistry<BlockEntityType<?>> BLOCK_ENTITY_TYPES = Human.MOD.registries()
        .create(BuiltInRegistries.BLOCK_ENTITY_TYPE);

    private static final List<BLibHolder<Block>> ALL_MUTABLE = new ArrayList<>();

    private static final Map<BLibHolder<Block>, String> NAMES_MUTABLE = new LinkedHashMap<>();

    private static final Map<BLibHolder<Block>, BLibHolder<Item>> ITEMS_MUTABLE = new LinkedHashMap<>();

    /** Every USCM block, in creative-tab order. */
    public static final List<BLibHolder<Block>> ALL = Collections.unmodifiableList(ALL_MUTABLE);

    /** The English name of every USCM block (datagen reads this for en_us). */
    public static final Map<BLibHolder<Block>, String> ENGLISH_NAMES = Collections.unmodifiableMap(NAMES_MUTABLE);

    /** The item of every USCM block. */
    public static final Map<BLibHolder<Block>, BLibHolder<Item>> ITEM_BY_BLOCK = Collections.unmodifiableMap(ITEMS_MUTABLE);

    // ---- Base walls
    public static final BLibHolder<Block> COMPOSITE_WALL = cube("composite_wall", "Composite Wall");

    public static final BLibHolder<Block> BORDERED_COMPOSITE_WALL = block(
        "bordered_composite_wall",
        "Bordered Composite Wall",
        () -> new FlippableWallBlock(steel())
    );

    public static final BLibHolder<Block> COMPOSITE_CEMENT = cube("composite_cement", "Composite Cement");

    // ---- Accent walls
    public static final BLibHolder<Block> WALL_FRAMED = cube("uscm_wall_framed", "USCM Framed Wall");

    public static final BLibHolder<Block> WALL_RIBBED = cube("uscm_wall_ribbed", "USCM Ribbed Wall");

    public static final BLibHolder<Block> WALL_PIPES = cube("uscm_wall_pipes", "USCM Pipe Wall");

    public static final BLibHolder<Block> WALL_PIPES_VERTICAL = cube("uscm_wall_pipes_vertical", "USCM Vertical Pipe Wall");

    public static final BLibHolder<Block> WALL_CONDUIT = cube("uscm_wall_conduit", "USCM Conduit Wall");

    public static final BLibHolder<Block> WALL_CABLE_TRAY = cube("uscm_wall_cable_tray", "USCM Cable Tray Wall");

    public static final BLibHolder<Block> HAZARD_PANEL = axis("uscm_hazard_panel", "USCM Hazard Panel", 0);

    public static final BLibHolder<Block> WALL_LOUVER = cube("uscm_wall_louver", "USCM Louvered Wall");

    public static final BLibHolder<Block> WALL_DUCT = cube("uscm_wall_duct", "USCM Duct Wall");

    public static final BLibHolder<Block> WALL_ACCESS_HATCH = cube("uscm_wall_access_hatch", "USCM Access Hatch");

    public static final BLibHolder<Block> STEEL_PANEL_WALL = cube("uscm_steel_panel_wall", "USCM Steel Panel Wall");

    public static final BLibHolder<Block> STRIPED_WALL = axis("uscm_striped_wall", "USCM Striped Wall", 0);

    public static final BLibHolder<Block> WALL_PANEL_INSET = cube("uscm_wall_panel_inset", "USCM Inset Panel Wall");

    public static final BLibHolder<Block> WALL_ARMORED_BASE = cube("uscm_wall_armored_base", "USCM Armored Base Wall");

    public static final BLibHolder<Block> CAGE_MESH = block(
        "uscm_cage_mesh",
        "USCM Cage Mesh",
        () -> new Block(steel().noOcclusion())
    );

    // ---- Lights and screens
    public static final BLibHolder<Block> LIGHT_PANEL = light("uscm_light_panel", "USCM Light Panel");

    public static final BLibHolder<Block> LIGHT_STRIP = axis("uscm_light_strip", "USCM Light Strip", 15);

    public static final BLibHolder<Block> CEILING_TUBE_LIGHT = light("uscm_ceiling_tube_light", "USCM Ceiling Tube Light");

    public static final BLibHolder<Block> COMPUTER_TERMINAL = cube("uscm_computer_terminal", "USCM Computer Terminal");

    public static final BLibHolder<Block> WALL_MONITORS = cube("uscm_wall_monitors", "USCM Wall Monitors");

    public static final BLibHolder<Block> MACHINERY = cube("uscm_machinery", "USCM Machinery");

    // ---- Floors
    public static final BLibHolder<Block> FLOOR_PLATE = cube("uscm_floor_plate", "USCM Floor Plate");

    public static final BLibHolder<Block> FLOOR_GRATE = cube("uscm_floor_grate", "USCM Floor Grate");

    public static final BLibHolder<Block> FLOOR_MARKING = floor("uscm_floor_marking", "USCM Marked Floor");

    public static final BLibHolder<Block> FLOOR_HAZARD = cube("uscm_floor_hazard", "USCM Hazard Floor");

    public static final BLibHolder<Block> FLOOR_DRAIN = cube("uscm_floor_drain", "USCM Floor Drain");

    public static final BLibHolder<Block> FLOOR_TILE = cube("uscm_floor_tile", "USCM Floor Tile");

    public static final BLibHolder<Block> FLOOR_TRENCH = floor("uscm_floor_trench", "USCM Floor Trench");

    public static final BLibHolder<Block> FLOOR_ARROW = floor("uscm_floor_arrow", "USCM Arrow Floor");

    public static final BLibHolder<Block> FLOOR_LINE_WHITE = floor("uscm_floor_line_white", "USCM White Line Floor");

    public static final BLibHolder<Block> FLOOR_EDGE_LINE = floor("uscm_floor_edge_line", "USCM Edge Line Floor");

    public static final BLibHolder<Block> FLOOR_PANEL_NOTCHED = cube("uscm_floor_panel_notched", "USCM Notched Floor Panel");

    public static final BLibHolder<Block> STAIR_TREAD = floor("uscm_stair_tread", "USCM Stair Tread");

    // ---- Ceiling
    public static final BLibHolder<Block> CEILING_PANEL = light("uscm_ceiling_panel", "USCM Ceiling Light Panel");

    public static final BLibHolder<Block> CEILING_GRID = cube("uscm_ceiling_grid", "USCM Ceiling Grid");

    public static final BLibHolder<Block> CEILING_FAN = cube("uscm_ceiling_fan", "USCM Ceiling Fan");

    public static final BLibHolder<Block> CEILING_PIPES = cube("uscm_ceiling_pipes", "USCM Ceiling Pipes");

    public static final BLibHolder<Block> CEILING_MESH = cube("uscm_ceiling_mesh", "USCM Ceiling Mesh");

    // ---- Structure
    public static final BLibHolder<Block> SUPPORT_BEAM = axis("uscm_support_beam", "USCM Support Beam", 0);

    public static final BLibHolder<Block> BULKHEAD = cube("uscm_bulkhead", "USCM Bulkhead");

    public static final BLibHolder<Block> REINFORCED_WALL = cube("uscm_reinforced_wall", "USCM Reinforced Wall");

    public static final BLibHolder<Block> FUSE_BOX = cube("uscm_fuse_box", "USCM Fuse Box");

    public static final BLibHolder<Block> SUPPLY_CRATE = cube("uscm_supply_crate", "USCM Supply Crate");

    public static final BLibHolder<Block> CONSOLE_TOP = cube("uscm_console_top", "USCM Console Top");

    // ---- Lockers
    public static final BLibHolder<Block> LOCKER = block("uscm_locker", "USCM Locker", () -> new LockerBlock(steel()));

    public static final BLibHolder<Block> WEAPON_LOCKER = block(
        "uscm_weapon_locker",
        "USCM Weapon Locker",
        () -> new LockerBlock(steel())
    );

    // ---- Window
    public static final BLibHolder<Block> WINDOW = block(
        "uscm_window",
        "USCM Window",
        () -> new ConnectedWindowBlock(HumanBlockProperties.INDUSTRIAL_GLASS.build())
    );

    /** One block entity type for both lockers. */
    public static final BLibHolder<BlockEntityType<LockerBlockEntity>> LOCKER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.createHolder(
        "uscm_locker",
        () -> BlockEntityType.Builder.of(LockerBlockEntity::new, LOCKER.get(), WEAPON_LOCKER.get()).build(null)
    );

    private HumanUscmBlocks() {}

    /**
     * Steel-strength properties, from a FRESH builder every call. Some blocks here chain {@code lightLevel} or
     * {@code noOcclusion} onto the result; a fresh builder guarantees that can never leak into another block's
     * properties, whatever the shared {@code STEEL} builder does internally.
     */
    private static BlockBehaviour.Properties steel() {
        return HumanBlockProperties.STEEL_SUPPLIER.get().build();
    }

    private static BLibHolder<Block> cube(String id, String name) {
        return block(id, name, () -> new Block(steel()));
    }

    private static BLibHolder<Block> light(String id, String name) {
        return block(id, name, () -> new Block(steel().lightLevel(state -> 15)));
    }

    /** A pattern with a direction on a wall: a pillar axis, so it runs vertical or horizontal. */
    private static BLibHolder<Block> axis(String id, String name, int light) {
        return block(id, name, () -> new RotatedPillarBlock(light > 0 ? steel().lightLevel(state -> light) : steel()));
    }

    /** A floor pattern with a direction: it faces the way the player looks when placed. */
    private static BLibHolder<Block> floor(String id, String name) {
        return block(id, name, () -> new FloorFacingBlock(steel()));
    }

    private static BLibHolder<Block> block(String id, String name, Supplier<Block> supplier) {
        var holder = BLOCKS.<Block>createHolder(id, supplier);

        ALL_MUTABLE.add(holder);
        NAMES_MUTABLE.put(holder, name);
        ITEMS_MUTABLE.put(holder, ITEMS.<Item>createHolder(id, () -> new BlockItem(holder.get(), new Item.Properties())));

        return holder;
    }

    public static void initialize() {
        BLOCKS.registerAll();
        ITEMS.registerAll();
        BLOCK_ENTITY_TYPES.registerAll();
    }
}
