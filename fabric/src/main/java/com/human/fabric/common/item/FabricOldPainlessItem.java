package com.human.fabric.common.item;

import com.human.common.gameplay.item.GunItem;
import com.human.common.gameplay.item.old_painless.OldPainlessItem;
import com.human.common.registry.tag.HumanEnchantmentTags;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public class FabricOldPainlessItem extends OldPainlessItem {

    @Override
    public boolean canBeEnchantedWith(ItemStack stack, Holder<Enchantment> enchantment, EnchantingContext context) {
        return enchantment.is(HumanEnchantmentTags.GUN_ENCHANTMENTS) && !GunItem.isBlockedGunEnchantment(enchantment);
    }
}
