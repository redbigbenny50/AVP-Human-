package com.human.common.gameplay.acid;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The three xeno acids, poured as fluids. [stated] Oct 17: each behaves like the blood it comes from in avp_alien - it
 * eats blocks at the blood's rate with the blood's immunities, and does to anything standing in it what the blood does.
 * <ul>
 * <li>{@link #XENO} - normal acid blood: burns and smokes.</li>
 * <li>{@link #NETHER} - the nether xenos' blue blood: burns, smokes and sets fire.</li>
 * <li>{@link #IRRADIATED} - irradiated blood: no burning, no smoke - it FREEZES, turns what it eats to blue ice, and
 * irradiates whoever stands in it.</li>
 * </ul>
 * <h2>NO HARD avp_alien DEPENDENCY</h2> Every avp_alien tag is named by id, never imported, so these fluids load and
 * work without avp_alien; its tags are then simply empty. Without avp_alien the fluids cannot be MADE (the press
 * recipes only load with it) and are hidden from creative and the recipe viewers - see {@link AcidAvailability}.
 */
public enum AcidKind {

    XENO("xeno_acid", 0xA09E09, "acid_immune"),
    NETHER("nether_xeno_acid", 0x4E65E5, "nether_acid_immune"),
    IRRADIATED("irradiated_xeno_acid", 0x36C1BE, "irradiated_acid_immune");

    /** avp_alien's resin veins and webs: irradiated acid destroys them outright rather than freezing them. */
    private static final TagKey<Block> RESIN_VEINS = alienBlockTag("resin_veins");

    private static final TagKey<Block> RESIN_WEBS = alienBlockTag("resin_webs");

    /** avp_alien's nether resin: irradiated acid turns it to netherrack, and eats it even though it is acid-immune. */
    private static final TagKey<Block> NETHER_RESIN = alienBlockTag("nether_resin");

    private final String id;

    private final int color;

    private final TagKey<Block> immune;

    AcidKind(String id, int color, String immuneTag) {
        this.id = id;
        this.color = color;
        this.immune = alienBlockTag(immuneTag);
    }

    /**
     * The fluid's id, and its liquid block's: {@code xeno_acid}, {@code nether_xeno_acid},
     * {@code irradiated_xeno_acid}.
     */
    public String id() {
        return id;
    }

    /** The blood's colour - avp_alien's own acid particle colour (irradiated: its chitin's teal). */
    public int color() {
        return color;
    }

    /**
     * Whether this acid eats {@code state} - avp_alien's own rule (AcidBlockDamageUtil.canAcidDestroyBlock), the same
     * immunity tag per strain. Irradiated acid always takes nether resin, which it turns to netherrack.
     */
    public boolean canEat(BlockState state) {
        if (state.isAir()) {
            return false;
        }

        if (this == IRRADIATED && state.is(NETHER_RESIN)) {
            return true;
        }

        return !state.is(immune);
    }

    /** Irradiated acid: what a block it has eaten through becomes - null when it simply goes (veins and webs). */
    public static BlockState frozenRemains(BlockState eaten) {
        if (eaten.is(RESIN_VEINS) || eaten.is(RESIN_WEBS)) {
            return null;
        }

        return eaten.is(NETHER_RESIN)
            ? net.minecraft.world.level.block.Blocks.NETHERRACK.defaultBlockState()
            : net.minecraft.world.level.block.Blocks.BLUE_ICE.defaultBlockState();
    }

    private static TagKey<Block> alienBlockTag(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("avp_alien", path));
    }
}
