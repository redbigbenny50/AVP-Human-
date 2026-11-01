package com.human.common.gameplay.gene.geneticist;

import com.human.common.gameplay.component.SyringeContents;
import com.human.common.gameplay.component.SyringeMode;
import com.human.common.gameplay.item.SyringeItem;
import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Gene cleansing at the Geneticist Station.
 * <p>
 * [stated] Oct 29: a syringe goes in one slot and a cleanser in the other; a few seconds later a new syringe comes out
 * with added integrity. It cancels the injection's own cost at minimum and raises the receiver's integrity (he
 * suggested 25 - 35%; 35% is the value used, so even a full 5-gene splice, which costs 30%, comes out ahead). Filled or
 * EMPTY syringes both work - an empty one becomes a pure cleansing shot. One cleanse per syringe.
 * </p>
 */
public final class GeneCleanser {

    /** Integrity a cleansed syringe adds when injected. */
    public static final double BONUS = 0.35;

    /** How long a cleanse takes (5 seconds). */
    public static final int TICKS = 100;

    /** The syringe's custom-data key that marks it cleansed (its value is the bonus). */
    public static final String KEY = "avp_human_cleansed";

    private GeneCleanser() {}

    public static boolean isCleansed(ItemStack syringe) {
        var data = syringe.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(KEY);
    }

    public static double bonusOf(ItemStack syringe) {
        var data = syringe.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(KEY) ? data.copyTag().getDouble(KEY) : 0.0;
    }

    /** One cleansed copy of the syringe, ready to inject. */
    public static ItemStack cleanse(ItemStack syringe) {
        var out = syringe.copyWithCount(1);
        CustomData.update(DataComponents.CUSTOM_DATA, out, tag -> tag.putDouble(KEY, BONUS));
        out.set(HumanDataComponents.SYRINGE_MODE.get(), SyringeMode.INJECT);
        return out;
    }

    /** True when the syringe holds no genes (a cleanse turns it into a pure cleansing shot). */
    public static boolean isPureShot(ItemStack syringe) {
        var contents = syringe.getOrDefault(HumanDataComponents.SYRINGE_CONTENTS.get(), SyringeContents.EMPTY);
        return contents.geneBonusDataEntries().isEmpty();
    }

    /** What injecting this syringe costs in integrity (negative), the same rule the injection itself uses. */
    public static double injectionCost(ItemStack syringe) {
        return SyringeItem.injectionIntegrityCost(
            syringe.getOrDefault(HumanDataComponents.SYRINGE_CONTENTS.get(), SyringeContents.EMPTY)
        );
    }

    /**
     * The receiver's new integrity after an injection. Without a cleanse it is just the cost. With one, the bonus is
     * added but integrity never rises above full (0.0) - or above what it already was, if it was somehow higher.
     */
    public static double applyTo(double current, double cost, double bonus) {
        if (bonus <= 0.0) {
            return current + cost;
        }
        return Math.min(current + cost + bonus, Math.max(current, 0.0));
    }
}
