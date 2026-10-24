package com.human.common.gameplay.quarry;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.HalfTransparentBlock;
import org.jetbrains.annotations.NotNull;

/**
 * The quarry's CONTAINMENT FIELD ([stated] Oct 15: "edge fluids held back by a containment field that clears when the
 * drill moves on"). A see-through energy block the drill leaves in place of a dug block that liquid touches. It is
 * solid and not replaceable, so water and lava cannot flow into it - that is the whole of how it holds them back, with
 * no ticking and no scanning.
 * <p>
 * It drops nothing, has no item, and breaks easily by hand, so a player is never trapped by one. The quarry takes its
 * own field down a few blocks a tick when the drill moves on, and all of it if the terminal is broken.
 * </p>
 */
public class QuarryContainmentBlock extends HalfTransparentBlock {

    public static final MapCodec<QuarryContainmentBlock> CODEC = simpleCodec(QuarryContainmentBlock::new);

    public QuarryContainmentBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends HalfTransparentBlock> codec() {
        return CODEC;
    }
}
