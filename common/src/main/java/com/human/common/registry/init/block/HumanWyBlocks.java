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
import com.human.common.gameplay.block.wy.FrontFacingBlock;
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
 * The Weyland-Yutani building set: 51 blocks - clean white facility walls with light-grey and W-Y yellow accents,
 * MUTHUR-green screens, rubber anti-slip floors, and its own framed window. Built like {@link HumanUscmBlocks}.
 * <p>
 * [stated] Oct 20: every W-Y block is printed in the FABRICATOR only (no crafting-table recipe) and needs the W-Y
 * LICENSE; none can be made into an individual blueprint. Both rules come from the {@code avp_human:faction/wy} item
 * tag every one of these items is in - the fabricator reads it for the license and the blueprint scanner refuses
 * anything a faction owns. 8 per craft, except the locker and the lights (1 at a time); recipes are cheap, white
 * plastic and aluminium.
 * <ul>
 * <li>Most blocks are plain cubes on steel strength.</li>
 * <li>Blocks with a FRONT (console, monitor bank, wall screen, rack, keypad and alarm pillars) face the player when
 * placed ({@link FrontFacingBlock}); their sides and back are the plain W-Y wall, their top and bottom the ceiling
 * panel.</li>
 * <li>The bordered wall, grey base, grey cap and ledge light have a strip at one edge and can go upside down
 * ({@link FlippableWallBlock}); banded walls show their band on all four sides with the plain wall on top and
 * bottom.</li>
 * <li>Wall patterns with a direction use a pillar axis so they run vertical or horizontal; floor patterns with a
 * direction face the way the player looks ({@link FloorFacingBlock}).</li>
 * <li>Every light block gives light level 15, like vanilla lights.</li>
 * <li>The locker is storage that opens like a barrel and spills its contents when broken ({@link LockerBlock}, the USCM
 * class; it has its own block entity type, {@link #LOCKER_BLOCK_ENTITY}).</li>
 * <li>The window connects to its own kind, one white W-Y frame round the whole window ({@link ConnectedWindowBlock}).
 * It never joins the USCM window.</li>
 * </ul>
 * Models, blockstates and item models are hand-written under {@code assets/avp_human}; loot, names and the pickaxe tag
 * come from datagen, which reads {@link #ALL} and {@link #ENGLISH_NAMES}.
 */
public final class HumanWyBlocks {

    private static final BLibRegistry<Block> BLOCKS = Human.MOD.registries().create(BuiltInRegistries.BLOCK);

    private static final BLibRegistry<Item> ITEMS = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    private static final BLibRegistry<BlockEntityType<?>> BLOCK_ENTITY_TYPES = Human.MOD.registries()
        .create(BuiltInRegistries.BLOCK_ENTITY_TYPE);

    private static final List<BLibHolder<Block>> ALL_MUTABLE = new ArrayList<>();

    private static final Map<BLibHolder<Block>, String> NAMES_MUTABLE = new LinkedHashMap<>();

    private static final Map<BLibHolder<Block>, BLibHolder<Item>> ITEMS_MUTABLE = new LinkedHashMap<>();

    /** Every W-Y block, in creative-tab order. */
    public static final List<BLibHolder<Block>> ALL = Collections.unmodifiableList(ALL_MUTABLE);

    /** The English name of every W-Y block (datagen reads this for en_us). */
    public static final Map<BLibHolder<Block>, String> ENGLISH_NAMES = Collections.unmodifiableMap(NAMES_MUTABLE);

    /** The item of every W-Y block. */
    public static final Map<BLibHolder<Block>, BLibHolder<Item>> ITEM_BY_BLOCK = Collections.unmodifiableMap(ITEMS_MUTABLE);

    // ---- Base walls
    public static final BLibHolder<Block> WALL = cube("wy_wall", "W-Y Wall", 0);

    public static final BLibHolder<Block> BORDERED_WALL = flippable("wy_bordered_wall", "W-Y Bordered Wall", 0);

    public static final BLibHolder<Block> NOTCHED_BAY = cube("wy_wall_notched_bay", "W-Y Notched Bay Wall", 0);

    public static final BLibHolder<Block> GREY_BASE_WALL = flippable("wy_wall_grey_base", "W-Y Grey Base Wall", 0);

    // ---- Wall patterns
    public static final BLibHolder<Block> FLUTED_PANEL = cube("wy_fluted_panel", "W-Y Fluted Panel", 0);

    public static final BLibHolder<Block> RADIATOR_PANEL = cube("wy_radiator_panel", "W-Y Radiator Panel", 0);

    public static final BLibHolder<Block> ACCENT_BAND = cube("wy_accent_band", "W-Y Accent Band", 0);

    public static final BLibHolder<Block> GREY_CAP_WALL = flippable("wy_wall_grey_cap", "W-Y Grey Cap Wall", 0);

    public static final BLibHolder<Block> CHANNEL_RUN = axis("wy_channel_run", "W-Y Channel Run", 0);

    public static final BLibHolder<Block> PORT_WALL = cube("wy_wall_port_row", "W-Y Port Wall", 0);

    // ---- Feature panels
    public static final BLibHolder<Block> OCTAGON_PANEL = cube("wy_plated_octagon_panel", "W-Y Octagon Panel", 0);

    public static final BLibHolder<Block> ACCESS_HATCH = cube("wy_access_hatch_panel", "W-Y Access Hatch", 0);

    public static final BLibHolder<Block> TABBED_PLATE = cube("wy_wall_tabbed_plate", "W-Y Tabbed Plate", 0);

    public static final BLibHolder<Block> CHANNEL_LOOP = cube("wy_channel_loop", "W-Y Channel Loop", 0);

    public static final BLibHolder<Block> CHANNEL_BRACKET = cube("wy_channel_bracket", "W-Y Channel Bracket", 0);

    public static final BLibHolder<Block> HEX_WALL = cube("wy_wall_hex", "W-Y Hex Wall", 0);

    // ---- Screens and equipment
    public static final BLibHolder<Block> CONSOLE = front("wy_console", "W-Y Console", 0);

    public static final BLibHolder<Block> MONITOR_BANK = front("wy_monitor_bank", "W-Y Monitor Bank", 0);

    public static final BLibHolder<Block> WALL_SCREEN = front("wy_wall_screen", "W-Y Wall Screen", 0);

    public static final BLibHolder<Block> EQUIPMENT_RACK = front("wy_equipment_rack", "W-Y Equipment Rack", 0);

    // ---- Lights - every one gives light level 15
    public static final BLibHolder<Block> LIGHT_STRIP = axis("wy_light_strip", "W-Y Light Strip", 15);

    public static final BLibHolder<Block> TUBE_LIGHT = axis("wy_tube_light", "W-Y Tube Light", 15);

    public static final BLibHolder<Block> LEDGE_LIGHT = flippable("wy_ledge_light", "W-Y Ledge Light", 15);

    public static final BLibHolder<Block> LIGHT_SLOT = axis("wy_wall_light_slot", "W-Y Light Slot", 15);

    public static final BLibHolder<Block> STATUS_STRIP = axis("wy_wall_status_strip", "W-Y Status Strip", 15);

    // ---- Vents
    public static final BLibHolder<Block> VENT_GRILLE = cube("wy_vent_grille", "W-Y Vent Grille", 0);

    public static final BLibHolder<Block> BRACKET_VENT = cube("wy_wall_bracket_vent", "W-Y Bracket Vent", 0);

    public static final BLibHolder<Block> VENT_SLOT = cube("wy_wall_vent_slot", "W-Y Vent Slot", 0);

    public static final BLibHolder<Block> PERFORATED_PANEL = cube("wy_wall_perforated", "W-Y Perforated Panel", 0);

    // ---- Structure
    public static final BLibHolder<Block> PILLAR = axis("wy_pillar_grey_band", "W-Y Pillar", 0);

    public static final BLibHolder<Block> RAIL_COLUMN = axis("wy_wall_rail_column", "W-Y Rail Column", 0);

    public static final BLibHolder<Block> KEYPAD_PILLAR = front("wy_keypad_pillar", "W-Y Keypad Pillar", 0);

    public static final BLibHolder<Block> ALARM_PILLAR = front("wy_alarm_pillar", "W-Y Alarm Pillar", 0);

    public static final BLibHolder<Block> DOOR_FRAME = axis("wy_frame_yellow_line", "W-Y Door Frame", 0);

    public static final BLibHolder<Block> LIT_DOOR_FRAME = axis("wy_frame_light", "W-Y Lit Door Frame", 15);

    public static final BLibHolder<Block> BULKHEAD_PANEL = cube("wy_bulkhead_panel", "W-Y Bulkhead Panel", 0);

    // ---- Floors
    public static final BLibHolder<Block> FLOOR_TILE = cube("wy_floor_slab", "W-Y Floor Tile", 0);

    public static final BLibHolder<Block> SLAT_FLOOR = cube("wy_floor_slat_plate", "W-Y Slat Floor", 0);

    public static final BLibHolder<Block> MARKED_FLOOR = floor("wy_floor_marking", "W-Y Marked Floor", 0);

    public static final BLibHolder<Block> GRATE_STRIP_FLOOR = floor("wy_floor_grate_strip", "W-Y Grate Strip Floor", 0);

    public static final BLibHolder<Block> FLOOR_VENT = cube("wy_floor_vent", "W-Y Floor Vent", 0);

    public static final BLibHolder<Block> STUD_MAT = cube("wy_floor_rubber_stud", "W-Y Stud Mat", 0);

    public static final BLibHolder<Block> CHEVRON_MAT = cube("wy_floor_rubber_herringbone", "W-Y Chevron Mat", 0);

    public static final BLibHolder<Block> TREAD_MAT = cube("wy_floor_rubber_tread", "W-Y Tread Mat", 0);

    // ---- Ceiling
    public static final BLibHolder<Block> CEILING_PANEL = cube("wy_ceiling_panel", "W-Y Ceiling Panel", 0);

    public static final BLibHolder<Block> LOUVRE_LIGHT = cube("wy_ceiling_louvre_light", "W-Y Louvre Light", 15);

    public static final BLibHolder<Block> CEILING_DIFFUSER = cube("wy_ceiling_diffuser", "W-Y Ceiling Diffuser", 0);

    public static final BLibHolder<Block> CEILING_GRILLE = cube("wy_ceiling_grille", "W-Y Ceiling Grille", 0);

    public static final BLibHolder<Block> CEILING_FAN = cube("wy_ceiling_fan", "W-Y Ceiling Fan", 0);

    // ---- Locker
    public static final BLibHolder<Block> LOCKER = block("wy_vented_locker", "W-Y Locker", () -> new LockerBlock(steel()));

    // ---- Window
    public static final BLibHolder<Block> WINDOW = block(
        "wy_window",
        "W-Y Window",
        () -> new ConnectedWindowBlock(HumanBlockProperties.INDUSTRIAL_GLASS.build())
    );

    /**
     * The W-Y locker's own block entity type. {@link LockerBlockEntity} picks this type for the W-Y locker and the USCM
     * type for the USCM lockers, so each type only ever holds its own set's blocks.
     */
    public static final BLibHolder<BlockEntityType<LockerBlockEntity>> LOCKER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.createHolder(
        "wy_locker",
        () -> BlockEntityType.Builder.of(LockerBlockEntity::new, LOCKER.get()).build(null)
    );

    private HumanWyBlocks() {}

    /**
     * Steel-strength properties, from a FRESH builder every call, so a light level chained onto one block can never
     * leak into another block's properties.
     */
    private static BlockBehaviour.Properties steel(int light) {
        var properties = HumanBlockProperties.STEEL_SUPPLIER.get().build();
        return light > 0 ? properties.lightLevel(state -> light) : properties;
    }

    private static BlockBehaviour.Properties steel() {
        return steel(0);
    }

    /** A plain cube (also the banded walls - their model shows the band on the sides, the plain wall on top). */
    private static BLibHolder<Block> cube(String id, String name, int light) {
        return block(id, name, () -> new Block(steel(light)));
    }

    /** A wall with a strip at one edge: placed like a slab, it can go upside down. */
    private static BLibHolder<Block> flippable(String id, String name, int light) {
        return block(id, name, () -> new FlippableWallBlock(steel(light)));
    }

    /** A pattern with a direction on a wall: a pillar axis, so it runs vertical or horizontal. */
    private static BLibHolder<Block> axis(String id, String name, int light) {
        return block(id, name, () -> new RotatedPillarBlock(steel(light)));
    }

    /** A floor (or ceiling) pattern with a direction: it faces the way the player looks when placed. */
    private static BLibHolder<Block> floor(String id, String name, int light) {
        return block(id, name, () -> new FloorFacingBlock(steel(light)));
    }

    /** A block with a front: the front faces the player who placed it. */
    private static BLibHolder<Block> front(String id, String name, int light) {
        return block(id, name, () -> new FrontFacingBlock(steel(light)));
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
