package com.human.common.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What avp_human counts as a FIRE block.
 * <p>
 * 🚨 Oct 24 - [stated] "fire when burnt is installed doesnt seem to count as fire ... please add compat for this."
 * Burnt's flames (its wood fire, stairs fire, grass flames, smouldering crops) are blocks of their own, in Burnt's
 * {@code burnt:fire} tag - not vanilla fire blocks and not in {@code minecraft:fire}. So cryofluid vapor could not put
 * them out, infernium vapor would "ignite" a cell already burning, and marines neither noticed they were standing in
 * one nor doused it. Every one of those checks now asks this instead: vanilla's fire tag, any vanilla fire block, or
 * {@code avp_human:fires} - vanilla's fire plus Burnt's (optional, so nothing breaks without Burnt), and any other
 * mod's flames a data pack adds.
 * </p>
 */
public final class HumanFire {

    public static final TagKey<Block> FIRES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("avp_human", "fires"));

    private HumanFire() {}

    public static boolean isFire(BlockState state) {
        return state.is(BlockTags.FIRE) || state.getBlock() instanceof BaseFireBlock || state.is(FIRES);
    }
}
