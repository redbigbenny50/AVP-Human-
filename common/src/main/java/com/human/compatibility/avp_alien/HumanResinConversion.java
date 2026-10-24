package com.human.compatibility.avp_alien;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * [stated] Oct 20 - avp_human's own heat and cold reach avp_alien's resin: infernium (vapor) burns ordinary resin to
 * basalt, and cryo - cryofluid, cryo vapor and irradium (vapor, and the irradiated acid liquid) - freezes nether resin
 * to netherrack, each in the piece's own shape, exactly as avp_alien's BurntResin does for fire and powder snow. Veins
 * and webs are wisps: they are simply destroyed. Everything is found BY NAME (tags and registry ids), so this needs no
 * compile dependency on avp_alien and does nothing at all when avp_alien is not installed (no block is ever in its
 * tags).
 */
public final class HumanResinConversion {

    private static final TagKey<Block> RESIN = tag("resin");

    private static final TagKey<Block> NETHER_RESIN = tag("nether_resin");

    private static final TagKey<Block> RESIN_VEINS = tag("resin_veins");

    private static final TagKey<Block> RESIN_WEBS = tag("resin_webs");

    private HumanResinConversion() {}

    /** Infernium on ordinary resin: basalt in the same shape. Nether resin is fire-native and untouched. */
    public static boolean burn(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.is(RESIN) || state.is(NETHER_RESIN)) {
            return false;
        }

        return convert(level, pos, state, Blocks.BASALT, "basalt");
    }

    /** Cryo on nether resin: netherrack in the same shape. Other resin is untouched by cold. */
    public static boolean freeze(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.is(NETHER_RESIN)) {
            return false;
        }

        return convert(level, pos, state, Blocks.NETHERRACK, "netherrack");
    }

    private static boolean convert(ServerLevel level, BlockPos pos, BlockState state, Block full, String material) {
        if (state.is(RESIN_VEINS) || state.is(RESIN_WEBS)) {
            level.destroyBlock(pos, false);
            return true;
        }

        level.setBlock(pos, shaped(state, full, material), Block.UPDATE_ALL);
        return true;
    }

    private static BlockState shaped(BlockState from, Block full, String material) {
        var block = from.getBlock();
        String suffix = null;

        if (block instanceof StairBlock) {
            suffix = "_stairs";
        } else if (block instanceof SlabBlock) {
            suffix = "_slab";
        } else if (block instanceof WallBlock) {
            suffix = "_wall";
        }

        if (suffix == null) {
            return full.defaultBlockState();
        }

        var shapedBlock = BuiltInRegistries.BLOCK.getOptional(
            ResourceLocation.fromNamespaceAndPath("avp_alien", material + suffix)
        );

        // An older avp_alien without the shaped pieces: the full block, as it always was.
        if (shapedBlock.isEmpty()) {
            return full.defaultBlockState();
        }

        var result = shapedBlock.get().defaultBlockState();

        for (var property : from.getProperties()) {
            if (result.hasProperty(property)) {
                result = copy(from, result, property);
            }
        }

        return result;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }

    private static TagKey<Block> tag(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("avp_alien", path));
    }
}
