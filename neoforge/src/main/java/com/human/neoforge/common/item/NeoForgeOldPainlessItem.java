package com.human.neoforge.common.item;

import com.human.common.gameplay.item.GunItem;
import com.human.common.gameplay.item.old_painless.OldPainlessItem;
import com.human.common.registry.tag.HumanEnchantmentTags;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import org.jetbrains.annotations.NotNull;

public class NeoForgeOldPainlessItem extends OldPainlessItem implements IItemExtension {

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack stack, Holder<Enchantment> enchantment) {
        return enchantment.is(HumanEnchantmentTags.GUN_ENCHANTMENTS) && !GunItem.isBlockedGunEnchantment(enchantment);
    }
}
