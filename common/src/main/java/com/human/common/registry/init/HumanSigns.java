package com.human.common.registry.init;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.block.sign.HumanCeilingHangingSignBlock;
import com.human.common.gameplay.block.sign.HumanHangingSignBlockEntity;
import com.human.common.gameplay.block.sign.HumanSignBlockEntity;
import com.human.common.gameplay.block.sign.HumanStandingSignBlock;
import com.human.common.gameplay.block.sign.HumanWallHangingSignBlock;
import com.human.common.gameplay.block.sign.HumanWallSignBlock;
import com.human.mixin.WoodTypeInvoker;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Metal signs and hanging signs.
 * <p>
 * [stated] Oct 23 - "we want to add new signs to the mod metal signs for all the metals we made chains and such for ...
 * hanging versions too." Steel, titanium, ferroaluminum, aluminum and composite, matching the five metal chains. Each
 * metal has a standing sign, a wall sign, a hanging sign and a wall-mounted hanging sign, behind two items (a sign and
 * a hanging sign) exactly as vanilla wood does.
 * </p>
 * <ul>
 * <li><b>Metal, not wood:</b> they do not burn and lava does not light them; they sound like metal, and the hanging
 * signs ring like chain. Mined with a pickaxe, like the metal ladders and chains (no tool is required to collect them,
 * as with every sign).</li>
 * <li><b>Their own block entity types.</b> Vanilla's sign and hanging-sign types only accept vanilla's blocks, so ours
 * carry their own two types, drawn by vanilla's own sign renderers.</li>
 * <li><b>Textures by sign type name.</b> Every loader looks a sign's art up from its WoodType's name: the board at
 * {@code minecraft:textures/entity/signs/<name>.png}, the hanging board at {@code .../signs/hanging/<name>.png}, the
 * hanging-sign edit screen at {@code minecraft:textures/gui/hanging_signs/<name>.png}. Vanilla (and so Fabric) builds
 * those paths in the {@code minecraft} namespace whatever the name, so the names carry our mod id as a prefix
 * ({@code avp_human_steel}) rather than a namespace - one set of files that works on both loaders.</li>
 * </ul>
 */
public final class HumanSigns {

    private static final BLibRegistry<Block> BLOCKS = Human.MOD.registries().create(BuiltInRegistries.BLOCK);

    private static final BLibRegistry<Item> ITEMS = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    private static final BLibRegistry<BlockEntityType<?>> BLOCK_ENTITY_TYPES = Human.MOD.registries()
        .create(BuiltInRegistries.BLOCK_ENTITY_TYPE);

    private static final List<SignSet> SETS = new ArrayList<>();

    public static final SignSet STEEL = set("steel", MapColor.COLOR_GRAY);

    public static final SignSet TITANIUM = set("titanium", MapColor.SAND);

    public static final SignSet FERROALUMINUM = set("ferroaluminum", MapColor.COLOR_BLACK);

    public static final SignSet ALUMINUM = set("aluminum", MapColor.METAL);

    public static final SignSet COMPOSITE = set("composite", MapColor.COLOR_GREEN);

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final BLibHolder<BlockEntityType<SignBlockEntity>> SIGN_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.createHolder(
        "sign",
        () -> (BlockEntityType) BlockEntityType.Builder.of(
            HumanSignBlockEntity::new,
            SETS.stream().flatMap(set -> Stream.of(set.sign().get(), set.wallSign().get())).toArray(Block[]::new)
        ).build(null)
    );

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final BLibHolder<BlockEntityType<SignBlockEntity>> HANGING_SIGN_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.createHolder(
        "hanging_sign",
        () -> (BlockEntityType) BlockEntityType.Builder.of(
            HumanHangingSignBlockEntity::new,
            SETS.stream().flatMap(set -> Stream.of(set.hangingSign().get(), set.wallHangingSign().get())).toArray(Block[]::new)
        ).build(null)
    );

    /** One metal's signs: the four blocks and their two items. {@code metal} is the id prefix, e.g. "steel". */
    public record SignSet(
        String metal,
        WoodType woodType,
        BLibHolder<Block> sign,
        BLibHolder<Block> wallSign,
        BLibHolder<Block> hangingSign,
        BLibHolder<Block> wallHangingSign,
        BLibHolder<Item> signItem,
        BLibHolder<Item> hangingSignItem
    ) {

        /** All four blocks: standing, wall, hanging, wall hanging. */
        public List<BLibHolder<Block>> blocks() {
            return List.of(sign, wallSign, hangingSign, wallHangingSign);
        }
    }

    public static List<SignSet> all() {
        return List.copyOf(SETS);
    }

    private static SignSet set(String metal, MapColor color) {
        // Registered at once, while this class loads during mod setup - before any renderer, model layer or texture
        // sheet is built from WoodType.values().
        var woodType = WoodTypeInvoker.avp_human$register(
            new WoodType(
                Human.MOD_ID + "_" + metal,
                BlockSetType.IRON,
                SoundType.METAL,
                SoundType.CHAIN,
                SoundEvents.IRON_DOOR_CLOSE,
                SoundEvents.IRON_DOOR_OPEN
            )
        );

        BLibHolder<Block> sign = BLOCKS.createHolder(metal + "_sign", () -> new HumanStandingSignBlock(woodType, properties(color)));
        BLibHolder<Block> wallSign = BLOCKS.createHolder(
            metal + "_wall_sign",
            () -> new HumanWallSignBlock(woodType, properties(color).dropsLike(sign.get()))
        );
        BLibHolder<Block> hangingSign = BLOCKS.createHolder(
            metal + "_hanging_sign",
            () -> new HumanCeilingHangingSignBlock(woodType, properties(color))
        );
        BLibHolder<Block> wallHangingSign = BLOCKS.createHolder(
            metal + "_wall_hanging_sign",
            () -> new HumanWallHangingSignBlock(woodType, properties(color).dropsLike(hangingSign.get()))
        );

        BLibHolder<Item> signItem = ITEMS.createHolder(
            metal + "_sign",
            () -> new SignItem(new Item.Properties().stacksTo(16), sign.get(), wallSign.get())
        );
        BLibHolder<Item> hangingSignItem = ITEMS.createHolder(
            metal + "_hanging_sign",
            () -> new HangingSignItem(hangingSign.get(), wallHangingSign.get(), new Item.Properties().stacksTo(16))
        );

        var set = new SignSet(metal, woodType, sign, wallSign, hangingSign, wallHangingSign, signItem, hangingSignItem);
        SETS.add(set);
        return set;
    }

    /**
     * Vanilla's sign properties without the wood: no burning, no bass note, and a touch harder, like the metal ladders.
     * The sound comes from the sign type (metal, and chain for the hanging boards).
     */
    private static BlockBehaviour.Properties properties(MapColor color) {
        return BlockBehaviour.Properties.of()
            .mapColor(color)
            .forceSolidOn()
            .noCollission()
            .strength(1.5F, 6.0F);
    }

    private HumanSigns() {}

    public static void initialize() {
        BLOCKS.registerAll();
        ITEMS.registerAll();
        BLOCK_ENTITY_TYPES.registerAll();
    }
}
