package com.human.common.data;

import com.blib.api.common.advancement.v1.BLibAdvancement;
import com.human.Human;

public class HumanAdvancements {

    public static final BLibAdvancement BLAST_STEEL = create("blast_steel");

    public static final BLibAdvancement EQUIP_FULL_ARMOR_SET_WITH_ARMOR_CASE = create("equip_full_armor_set_with_armor_case");

    public static final BLibAdvancement FILL_CANISTER = create("fill_canister");

    public static final BLibAdvancement HAS_GUN = create("has_gun");

    public static final BLibAdvancement HIRE_MARINE = create("hire_marine");

    public static final BLibAdvancement ROOT = create("root");

    public static final BLibAdvancement SMELT_BRASS = create("smelt_brass");

    public static final BLibAdvancement SMELT_PLASTIC = create("smelt_plastic");

    public static final BLibAdvancement SMELT_TITANIUM = create("smelt_titanium");

    /** [stated] Oct 30: the rare challenge for milking yourself (Milkable gene, empty bucket used in the air). */
    public static final BLibAdvancement SELF_MILKER = create("self_milker");

    private static BLibAdvancement create(String path) {
        return new BLibAdvancement(Human.MOD_ID, "humans", path);
    }
}
