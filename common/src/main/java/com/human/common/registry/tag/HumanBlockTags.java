package com.human.common.registry.tag;

import com.human.HumanResources;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class HumanBlockTags {

    public static final TagKey<Block> CONCRETE = create("concrete");

    /**
     * [stated] Sep 30: devices a fuse panel can power wirelessly as they are - terminals, the resonator, and later
     * lights, doors and access panels. Machines are not in here: they need a hub chip.
     */
    public static final TagKey<Block> FUSE_DEVICES = create("fuse_devices");

    /**
     * Blocks that irradiate anything standing NEAR them, not merely on top of them. Datapack-driven and cross-mod:
     * avp_alien injects the irradiated hive materials (resin, veins, webs, the hive slab) into these, which is what
     * turns an irradiated hive into somewhere you cannot linger without a suit.
     */
    public static final TagKey<Block> RADIOACTIVE_BLOCKS = create("radioactive_blocks");

    public static final TagKey<Block> HIGHLY_RADIOACTIVE_BLOCKS = create("highly_radioactive_blocks");

    public static final TagKey<Block> FERROALUMINUM = create("ferroaluminum");

    public static final TagKey<Block> INDUSTRIAL_CONCRETE = create("industrial_concrete");

    public static final TagKey<Block> INDUSTRIAL_GLASS = create("industrial_glass");

    public static final TagKey<Block> INDUSTRIAL_GLASS_BLOCK = create("industrial_glass_block");

    public static final TagKey<Block> INDUSTRIAL_GLASS_PANE = create("industrial_glass_pane");

    public static final TagKey<Block> MARINE_SPAWN_BLOCKS = create("marine_spawn_blocks");

    public static final TagKey<Block> PADDING = create("padding");

    public static final TagKey<Block> PLASTIC = create("plastic");

    public static final TagKey<Block> RAZOR_WIRE = create("razor_wire");

    public static final TagKey<Block> STEEL = create("steel");

    /** [stated] Oct 3: every composite block - as strong as steel, so mined and tooled the same. */
    public static final TagKey<Block> COMPOSITE = create("composite");

    /**
     * [stated] Oct 3: blocks that stand up to xenomorph acid THREE TIMES longer than steel. Read by avp_alien's acid,
     * which multiplies its eat-through time by 3 for these. Composite is the first.
     */
    public static final TagKey<Block> ACID_RESISTANT_3X = create("acid_resistant_3x");

    public static final TagKey<Block> TITANIUM = create("titanium");

    private static TagKey<Block> create(String name) {
        return TagKey.create(Registries.BLOCK, HumanResources.location(name));
    }
}
