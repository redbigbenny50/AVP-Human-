package com.human.client;

import com.human.common.gameplay.cosmetic.CosmeticBlock;
import com.human.common.registry.init.HumanCosmetics;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * The dyed cosmetic fabric colour ([stated] Oct 31: only cushions and fabrics take dye). The fabric faces of a dyeable
 * piece's baked model carry tint index 0 over a greyscale texture, so the colour returned here IS the colour the player
 * sees: the dye's, or the faction's own fabric colour while undyed.
 * <p>
 * A REAL provider in vanilla's colour table, because Sodium and Embeddium look providers up there directly instead of
 * calling {@code getColor} - post-processing that call (as the nuked-foliage tint does) would leave dyed fabric grey
 * under them. BLib has no block-colour registration, so it goes in from two places:
 * </p>
 * <ul>
 * <li>{@code BlockColors.createDefault} - where vanilla builds the table. ⚠ Nov 1: on NeoForge the cosmetic blocks were
 * NOT in the block registry yet at that moment, and resolving the first holder crashed the game on startup ("Trying to
 * access unbound value: avp_human:lab_bench"). So nothing is resolved unless EVERY cosmetic holder is bound.</li>
 * <li>{@code ClientLevel} creation - joining a world, long after registration and before any chunk is meshed. If the
 * first attempt was too early, this one registers it.</li>
 * </ul>
 * Registered once; later calls do nothing.
 */
public final class CosmeticFabricColors {

    private static boolean registered;

    private CosmeticFabricColors() {}

    /**
     * Registers the fabric colour provider if it isn't yet and every cosmetic block is registered; otherwise nothing.
     */
    public static void register(@Nullable BlockColors colors) {
        if (registered || colors == null) {
            return;
        }
        var dyeable = new ArrayList<Block>();
        for (var holder : HumanCosmetics.DECOR_BLOCKS.values()) {
            if (!holder.isBound()) {
                // Not registered yet - never resolve it (that is the crash). Retried when a world is joined.
                return;
            }
            if (holder.get() instanceof CosmeticBlock block && CosmeticBlock.dyeable(block.design())) {
                dyeable.add(block);
            }
        }
        registered = true;
        if (dyeable.isEmpty()) {
            return;
        }
        colors.register(
            (state, level, pos, tintIndex) -> tintIndex == 0 && state.getBlock() instanceof CosmeticBlock block
                ? block.fabricTint(state)
                : -1,
            dyeable.toArray(Block[]::new)
        );
    }
}
