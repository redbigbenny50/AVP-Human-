package com.human.common.gameplay.quarry;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.TransparentBlock;
import org.jetbrains.annotations.NotNull;

/**
 * A quarry frame block ([stated] Oct 14): a plain cube with a 2D wire texture - a thin border and an X corner to corner
 * on every side, see-through in between. Crafted 16 at a time from five steel ingots in an X.
 * <p>
 * Glass-like: a face touching another frame block is not drawn (vanilla's same-block rule in
 * {@code HalfTransparentBlock}), so a run of frames reads as one lattice instead of showing every X inside it. The
 * cutout render layer is registered in {@code HumanQuarryClient} (NeoForge also reads it from the model).
 * </p>
 */
public class QuarryFrameBlock extends TransparentBlock {

    public static final MapCodec<QuarryFrameBlock> CODEC = simpleCodec(QuarryFrameBlock::new);

    public QuarryFrameBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends TransparentBlock> codec() {
        return CODEC;
    }
}
